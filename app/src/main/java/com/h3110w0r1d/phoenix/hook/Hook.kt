package com.h3110w0r1d.phoenix.hook

import android.R.drawable.stat_notify_sync
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import com.h3110w0r1d.phoenix.BuildConfig
import com.h3110w0r1d.phoenix.data.config.KeepAliveConfig
import com.h3110w0r1d.phoenix.hook.ConfigServer.moduleConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

@Keep
class Hook : IXposedHookLoadPackage {
    private companion object {
        private const val PACKAGE_NAME = BuildConfig.APPLICATION_ID

        /** 与 AOSP ProcessList.UNKNOWN_ADJ 常见取值一致，反射失败时使用 */
        private const val FALLBACK_DEFAULT_MAX_ADJ = 1001
        private var amsInstance: Any? = null
        private var androidClassLoader: ClassLoader? = null
        private var lastManagedConfigKeys: Set<String> = emptySet()
        private var cachedDefaultMaxAdj: Int? = null

        private val ignoreCallAddToStopping =
            setOf(
                "com.android.server.wm.ActivityRecord.makeInvisible",
                "com.android.server.wm.TaskFragment.completePause",
            )

        private fun resolveKeepAliveConfig(processName: String): KeepAliveConfig? {
            val configs = moduleConfig.appKeepAliveConfigs
            val idx = processName.indexOf(':')
            val finalProcessName = if (idx > 0) processName.substring(0, idx) else processName
            if (configs[finalProcessName]?.enabled == true) {
                return configs[finalProcessName]
            }
            return null
        }

        private fun setProcessRecordMaxAdj(
            processRecord: Any,
            maxAdj: Int,
            persistent: Boolean = false,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // android 12+
                val mState = processRecord.get<Any>("mState")
                if (mState == null) {
                    // android-16_r4+
                    // https://cs.android.com/android/platform/superproject/+/android-16.0.0_r4:frameworks/base/services/core/java/com/android/server/am/psc/ProcessRecordInternal.java
                    XposedHelpers.callMethod(processRecord, "setMaxAdj", maxAdj)
                } else {
                    // android 12~16_r3
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java
                    XposedHelpers.callMethod(mState, "setMaxAdj", maxAdj)
                }
            } else {
                // android 8~11
                // https://cs.android.com/android/platform/superproject/+/android-8.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java
                processRecord.set<Int>("maxAdj", maxAdj)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // android 10+
                // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java
                XposedHelpers.callMethod(processRecord, "setPersistent", persistent)
            } else {
                // android 8~9
                // https://cs.android.com/android/platform/superproject/+/android-8.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java
                processRecord.set<Boolean>("persistent", true)
            }
        }

        private inline fun <reified T> Any.get(field: String): T? =
            try {
                XposedHelpers.getObjectField(this, field) as? T
            } catch (_: Throwable) {
                null
            }

        private inline fun <reified T> Any.set(
            field: String,
            value: T,
        ) {
            try {
                XposedHelpers.setObjectField(this, field, value)
            } catch (_: Throwable) {
            }
        }
    }

    private lateinit var configClient: ConfigClient

    private fun isKeepService(service: Service): Boolean {
        if (!::configClient.isInitialized) {
            configClient = ConfigClient(service)
        }
        val services = configClient.queryKeepService()
        return services.contains(service.javaClass.name)
    }

    private fun updateConfigCallback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                applyKeepAliveFromModuleConfigQ()
            }
            XposedBridge.log("Config updated")
        } catch (e: Throwable) {
            XposedBridge.log("Failed to update config: $e")
        }
    }

    private fun resetProcessRecordQ(processRecord: Any) {
        val defaultAdj = getDefaultMaxAdjQ()
        setProcessRecordMaxAdj(processRecord, defaultAdj, persistent = false)
    }

    private fun getDefaultMaxAdjQ(): Int {
        cachedDefaultMaxAdj?.let { return it }
        val cl = androidClassLoader ?: return FALLBACK_DEFAULT_MAX_ADJ
        val clazz =
            XposedHelpers.findClass("com.android.server.am.ProcessList", cl) ?: return FALLBACK_DEFAULT_MAX_ADJ
        for (fieldName in listOf("CACHED_APP_MAX_ADJ", "UNKNOWN_ADJ")) {
            try {
                val v = XposedHelpers.getStaticIntField(clazz, fieldName)
                cachedDefaultMaxAdj = v
                return v
            } catch (_: Throwable) {
            }
        }
        return FALLBACK_DEFAULT_MAX_ADJ
    }

    private fun processMatchesConfigKeyForReset(
        processName: String,
        key: String,
    ): Boolean = processName == key || processName.startsWith("$key:")

    private fun collectLruProcessRecords(mProcessList: Any): List<Any> {
        val raw =
            try {
                XposedHelpers.getObjectField(mProcessList, "mLruProcesses") as? ArrayList<*>
            } catch (_: Throwable) {
                null
            } ?: return emptyList()
        return raw.mapNotNull { it }
    }

    /**
     * 在 system_server 内对 LRU 进程应用/恢复保活策略。
     * 须在持有 AMS 监视器时调用（与 AMS 内部同步策略一致，降低与系统锁交错风险）。
     */
    private fun applyKeepAliveFromModuleConfigQ() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val ams = amsInstance ?: return
        val newEnabledKeys =
            if (moduleConfig.moduleEnabled) {
                moduleConfig.appKeepAliveConfigs
                    .filter { it.value.enabled }
                    .keys
                    .toSet()
            } else {
                emptySet()
            }
        val toResetKeys = lastManagedConfigKeys - newEnabledKeys
        try {
            synchronized(ams) {
                val mProcessList = XposedHelpers.getObjectField(ams, "mProcessList") ?: return@synchronized
                val lru = collectLruProcessRecords(mProcessList)
                for (processRecord in lru) {
                    val prName = processRecord.get<String>("processName") ?: continue
                    if (toResetKeys.any { processMatchesConfigKeyForReset(prName, it) }) {
                        try {
                            resetProcessRecordQ(processRecord)
                        } catch (e: Throwable) {
                            XposedBridge.log("Failed to reset $prName: $e")
                        }
                        continue
                    }
                    val cfg = resolveKeepAliveConfig(prName) ?: continue
                    try {
                        val finalMaxAdj = cfg.maxAdj ?: moduleConfig.globalMaxAdj
                        setProcessRecordMaxAdj(
                            processRecord,
                            finalMaxAdj,
                            cfg.persistent,
                        )
                    } catch (e: Throwable) {
                        XposedBridge.log("Failed to apply keep-alive to $prName: $e")
                    }
                }
                lastManagedConfigKeys = newEnabledKeys
            }
        } catch (e: Throwable) {
            XposedBridge.log("applyKeepAliveFromModuleConfigQ failed: $e")
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            PACKAGE_NAME -> {
                hookSelf(lpparam)
            }

            "android" -> {
                try {
                    androidClassLoader = lpparam.classLoader
                    if (hookSms(lpparam)) {
                        ConfigServer.updateConfigCallback = ::updateConfigCallback

                        // MaxAdj相关
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            hookAdjQ(lpparam)
                            hookAmsQ(lpparam)
                        } else {
                            hookAdjLegacy(lpparam)
                        }

                        // Activity保活相关
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            hookAddToStoppingR(lpparam)
                        }

                        // Debug
                        // hookActivityDestroy(lpparam)
                    }
                } catch (e: Throwable) {
                    XposedBridge.log("Failed to hook: $e")
                }
            }

            else -> {
                // 服务保活相关
                hookService(lpparam)
            }
        }
    }

    private fun hookService(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook 所有 Service 的 onCreate
        XposedHelpers.findAndHookMethod(
            "android.app.Service",
            lpparam.classLoader,
            "onCreate",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    val service: Service = param.thisObject as? Service? ?: return
                    try {
                        if (isKeepService(service)) {
                            makeForeground(service)
                        }
                    } catch (t: Throwable) {
                        XposedBridge.log(t)
                    }
                }
            },
        )
    }

    private fun hookSms(lpparam: XC_LoadPackage.LoadPackageParam): Boolean {
        val pmsClassName = "com.android.server.StorageManagerService"
        try {
            XposedHelpers.findAndHookMethod(
                pmsClassName,
                lpparam.classLoader,
                "getMountedObbPath",
                String::class.java,
                ConfigServer,
            )
            XposedBridge.log("Hooked $pmsClassName")
            return true
        } catch (e: Throwable) {
            XposedBridge.log("Failed to hook $pmsClassName: $e")
        }
        return false
    }

    private fun makeForeground(service: Service) {
        val channelId = "xposed_keep_alive"

        val nm =
            service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel =
            NotificationChannel(
                channelId,
                "Keep Alive",
                NotificationManager.IMPORTANCE_LOW,
            )
        nm.createNotificationChannel(channel)

        val notification =
            Notification
                .Builder(service, channelId)
                .setContentTitle("Running")
                .setContentText(service.javaClass.name)
                .setSmallIcon(stat_notify_sync)
                .build()

        service.startForeground(
            service.javaClass.name.hashCode(), // 防止冲突
            notification,
        )
    }

    private fun hookSelf(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "$PACKAGE_NAME.utils.XposedUtil",
            lpparam.classLoader,
            "isModuleEnabled",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    param?.result = true
                }
            },
        )
        XposedHelpers.findAndHookMethod(
            "$PACKAGE_NAME.utils.XposedUtil",
            lpparam.classLoader,
            "getModuleVersion",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    param?.result = XposedBridge.getXposedVersion()
                }
            },
        )
    }

    private fun hookAmsQ(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz =
            XposedHelpers.findClass(
                "com.android.server.am.ActivityManagerService",
                lpparam.classLoader,
            )
        if (clazz == null) {
            return
        }
        XposedBridge.hookAllConstructors(
            clazz,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    amsInstance = param.thisObject
                    XposedBridge.log("ActivityManagerService 实例已捕获: $amsInstance")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            applyKeepAliveFromModuleConfigQ()
                        } catch (e: Throwable) {
                            XposedBridge.log("applyKeepAlive after AMS init failed: $e")
                        }
                    }
                }
            },
        )
    }

    private object NewProcessRecordLockedHook : XC_MethodHook() {
        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodHookParam) {
            if (!moduleConfig.moduleEnabled) return

            val processName = param.result.get<String>("processName") ?: return
            val keepAliveConfig = resolveKeepAliveConfig(processName) ?: return

            try {
                val finalMaxAdj = keepAliveConfig.maxAdj ?: moduleConfig.globalMaxAdj

                setProcessRecordMaxAdj(
                    param.result,
                    finalMaxAdj,
                    keepAliveConfig.persistent,
                )
                XposedBridge.log("Set $processName adj to $finalMaxAdj")
            } catch (e: Throwable) {
                XposedBridge.log("Failed to set $processName adj: $e")
            }
        }
    }

    private object PrintActivityRecordHook : XC_MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (!moduleConfig.moduleEnabled) return

            val activityRecord = param.thisObject
            val activityName =
                XposedHelpers.getObjectField(
                    activityRecord,
                    "shortComponentName",
                ) as String?

            val packageName =
                XposedHelpers.getObjectField(activityRecord, "packageName") as? String? ?: return
            val keepAliveConfig = resolveKeepAliveConfig(packageName) ?: return

            if (!keepAliveConfig.enabled || !keepAliveConfig.keepActivity) return

            XposedBridge.log("Package: $packageName\nActivity: $activityName\nMethod: ${param.method.name}")

            XposedBridge.log(
                "Stack Trace:\n" +
                    Log.getStackTraceString(
                        Throwable(),
                    ),
            )
        }
    }

    private fun hookAddToStoppingR(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz =
            XposedHelpers.findClass(
                "com.android.server.wm.ActivityRecord",
                lpparam.classLoader,
            )
        XposedBridge.hookAllMethods(
            clazz,
            "addToStopping",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!moduleConfig.moduleEnabled) return

                    val activityRecord = param.thisObject
                    val packageName =
                        XposedHelpers.getObjectField(activityRecord, "packageName") as? String?
                            ?: return
                    val keepAliveConfig = resolveKeepAliveConfig(packageName) ?: return
                    // XposedBridge.log("addToStopping: $packageName")
                    if (keepAliveConfig.keepActivity && this.isCalledFromMakeInvisible) {
                        param.result = null
                    }
                }

                private val isCalledFromMakeInvisible: Boolean
                    get() {
                        val stack = Thread.currentThread().stackTrace

                        for (e in stack) {
                            val callAt = "${e.className}.${e.methodName}"
                            if (callAt in ignoreCallAddToStopping) {
                                XposedBridge.log("ignore addToStopping")
                                return true
                            }
                        }
                        // XposedBridge.log(
                        //     "Stack Trace:\n" +
                        //         Log.getStackTraceString(Throwable()),
                        // )
                        return false
                    }
            },
        )
    }

    private fun hookActivityDestroy(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz =
            XposedHelpers.findClass(
                "com.android.server.wm.ActivityRecord",
                lpparam.classLoader,
            )
        try {
            XposedBridge.hookAllMethods(
                clazz,
                "removeIfPossible",
                PrintActivityRecordHook,
            )
            XposedBridge.hookAllMethods(
                clazz,
                "removeFromHistory",
                PrintActivityRecordHook,
            )
            XposedBridge.hookAllMethods(
                clazz,
                "destroyImmediately",
                PrintActivityRecordHook,
            )
            XposedBridge.hookAllMethods(
                clazz,
                "destroyIfPossible",
                PrintActivityRecordHook,
            )
        } catch (e: Throwable) {
            XposedBridge.log("Failed to hook ActivityRecord: $e")
        }
    }

    private fun hookAdjQ(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz =
            XposedHelpers.findClass(
                "com.android.server.am.ProcessList",
                lpparam.classLoader,
            )
        if (clazz == null) {
            XposedBridge.log("Failed to find ProcessList class")
            return
        }
        XposedBridge.hookAllMethods(
            clazz,
            "newProcessRecordLocked",
            NewProcessRecordLockedHook,
        )
    }

    private fun hookAdjLegacy(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz =
            XposedHelpers.findClass(
                "com.android.server.am.ActivityManagerService",
                lpparam.classLoader,
            )
        if (clazz == null) {
            return
        }
        XposedBridge.hookAllMethods(
            clazz,
            "newProcessRecordLocked",
            NewProcessRecordLockedHook,
        )
    }
}
