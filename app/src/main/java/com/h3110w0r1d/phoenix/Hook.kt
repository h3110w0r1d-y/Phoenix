package com.h3110w0r1d.phoenix

import android.os.Build
import android.os.FileObserver
import androidx.annotation.Keep
import com.h3110w0r1d.phoenix.data.config.KeepAliveConfig
import com.h3110w0r1d.phoenix.data.config.ModuleConfig
import com.h3110w0r1d.phoenix.utils.ConfigServer
import com.h3110w0r1d.phoenix.utils.ConfigServer.CONFIG_FILE
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.hookAllConstructors
import de.robv.android.xposed.XposedBridge.hookAllMethods
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.getStaticIntField
import de.robv.android.xposed.XposedHelpers.setObjectField
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlinx.serialization.json.Json
import java.io.File

@Keep
class Hook : IXposedHookLoadPackage {
    private companion object {
        private const val PACKAGE_NAME = BuildConfig.APPLICATION_ID

        /** 与 AOSP ProcessList.UNKNOWN_ADJ 常见取值一致，反射失败时使用 */
        private const val FALLBACK_DEFAULT_MAX_ADJ = 1001

        private var moduleConfig = ModuleConfig()
        private var amsInstance: Any? = null
        private var androidClassLoader: ClassLoader? = null
        private var lastManagedConfigKeys: Set<String> = emptySet()
        private var cachedDefaultMaxAdj: Int? = null

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
                    callMethod(processRecord, "setMaxAdj", maxAdj)
                } else {
                    // android 12~16_r3
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java
                    callMethod(mState, "setMaxAdj", maxAdj)
                }
            } else {
                // android 8~11
                // https://cs.android.com/android/platform/superproject/+/android-8.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java
                processRecord.set<Int>("maxAdj", maxAdj)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // android 10+
                // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java
                callMethod(processRecord, "setPersistent", persistent)
            } else {
                // android 8~9
                // https://cs.android.com/android/platform/superproject/+/android-8.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java
                processRecord.set<Boolean>("persistent", true)
            }
        }

        private inline fun <reified T> Any.get(field: String): T? =
            try {
                getObjectField(this, field) as? T
            } catch (_: Throwable) {
                null
            }

        private inline fun <reified T> Any.set(
            field: String,
            value: T,
        ) {
            try {
                setObjectField(this, field, value)
            } catch (_: Throwable) {
            }
        }
    }

    private lateinit var fileObserver: FileObserver

    private fun updateConfig() {
        try {
            val configJson = File(CONFIG_FILE).readText()
            moduleConfig = Json.decodeFromString(configJson)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                applyKeepAliveFromModuleConfigQ()
            }
            XposedBridge.log("Config updated: $configJson")
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
            findClass("com.android.server.am.ProcessList", cl) ?: return FALLBACK_DEFAULT_MAX_ADJ
        for (fieldName in listOf("CACHED_APP_MAX_ADJ", "UNKNOWN_ADJ")) {
            try {
                val v = getStaticIntField(clazz, fieldName)
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
                getObjectField(mProcessList, "mLruProcesses") as? ArrayList<*>
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
                val mProcessList = getObjectField(ams, "mProcessList") ?: return@synchronized
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

    @Suppress("DEPRECATION")
    private fun startFileWatching() {
        if (::fileObserver.isInitialized) {
            return
        }
        fileObserver =
            object : FileObserver(CONFIG_FILE, CLOSE_WRITE) {
                override fun onEvent(
                    event: Int,
                    path: String?,
                ) {
                    updateConfig()
                }
            }
        // 开始监听
        fileObserver.startWatching()
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == PACKAGE_NAME) {
            hookSelf(lpparam)
        } else if (lpparam.packageName == "android") {
            try {
                androidClassLoader = lpparam.classLoader
                if (hookSms(lpparam)) {
                    updateConfig()
                    startFileWatching()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        hookAdjQ(lpparam)
                        hookAmsQ(lpparam)
                    } else {
                        hookAdjLegacy(lpparam)
                    }
                }
            } catch (e: Throwable) {
                XposedBridge.log("Failed to hook: $e")
            }
        }
    }

    private fun hookSms(lpparam: LoadPackageParam): Boolean {
        val pmsClassName = "com.android.server.StorageManagerService"
        try {
            findAndHookMethod(
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

    private fun hookSelf(lpparam: LoadPackageParam) {
        val clazz =
            findClass(
                "$PACKAGE_NAME.utils.XposedUtil",
                lpparam.classLoader,
            )
        if (clazz == null) {
            return
        }
        findAndHookMethod(
            clazz,
            "isModuleEnabled",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    param?.result = true
                }
            },
        )
        findAndHookMethod(
            clazz,
            "getModuleVersion",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    param?.result = XposedBridge.getXposedVersion()
                }
            },
        )
    }

    private fun hookAmsQ(lpparam: LoadPackageParam) {
        val clazz =
            findClass(
                "com.android.server.am.ActivityManagerService",
                lpparam.classLoader,
            )
        if (clazz == null) {
            return
        }
        hookAllConstructors(
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
            super.afterHookedMethod(param)
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

    private fun hookAdjQ(lpparam: LoadPackageParam) {
        val clazz =
            findClass(
                "com.android.server.am.ProcessList",
                lpparam.classLoader,
            )
        if (clazz == null) {
            XposedBridge.log("Failed to find ProcessList class")
            return
        }
        hookAllMethods(
            clazz,
            "newProcessRecordLocked",
            NewProcessRecordLockedHook,
        )
    }

    private fun hookAdjLegacy(lpparam: LoadPackageParam) {
        val clazz =
            findClass(
                "com.android.server.am.ActivityManagerService",
                lpparam.classLoader,
            )
        if (clazz == null) {
            return
        }
        hookAllMethods(
            clazz,
            "newProcessRecordLocked",
            NewProcessRecordLockedHook,
        )
    }
}
