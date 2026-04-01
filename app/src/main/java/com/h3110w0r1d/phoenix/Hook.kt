package com.h3110w0r1d.phoenix

import android.os.Build
import android.os.FileObserver
import androidx.annotation.Keep
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
import de.robv.android.xposed.XposedHelpers.setObjectField
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlinx.serialization.json.Json
import java.io.File

@Keep
class Hook : IXposedHookLoadPackage {
    companion object {
        const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
    }

    private lateinit var fileObserver: FileObserver
    private var moduleConfig = ModuleConfig()
    private var amsInstance: Any? = null

    fun updateConfig() {
        try {
            val configJson = File(CONFIG_FILE).readText()
            var changedProcess =
                moduleConfig.appKeepAliveConfigs.keys.filter {
                    moduleConfig.appKeepAliveConfigs[it]?.enabled ?: false
                }
            moduleConfig = Json.decodeFromString(configJson)
            moduleConfig.appKeepAliveConfigs.forEach { (processName, config) ->
                setProcessMaxAdjQ(processName, config.maxAdj ?: moduleConfig.globalMaxAdj, config.persistent)
                changedProcess = changedProcess.minus(processName)
            }
            XposedBridge.log("Config updated: $configJson")
        } catch (e: Throwable) {
            XposedBridge.log("Failed to update config: $e")
        }
    }

//    fun restoreProcess(processName: String) {
//        val uid = ConfigServer.getPackageUid(processName)
//        if (uid <= 0) {
//            return
//        }
//        val mProcessList = getObjectField(amsInstance, "mProcessList")
//        if (mProcessList == null) {
//            XposedBridge.log("mProcessList is null")
//        }
//        val processRecord =
//            callMethod(
//                mProcessList,
//                "getProcessRecordLocked",
//                processName,
//                uid,
//            )
//        if (processRecord == null) {
//            XposedBridge.log("processRecord is null")
//            return
//        }
//        callMethod(
//            processRecord,
//            "setMaxAdj",
//            maxAdj,
//        )
//    }

    fun setProcessRecordMaxAdjQ(
        processRecord: Any,
        maxAdj: Int,
        persistent: Boolean = false,
    ) {
        val mState = processRecord.get<Any>("mState")
        if (mState == null) {
            // android-16_r4+
            // https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/base/services/core/java/com/android/server/am/psc/ProcessRecordInternal.java
            callMethod(processRecord, "setMaxAdj", maxAdj)
            if (persistent) {
                callMethod(processRecord, "setPersistent", true)
            }
        } else {
            callMethod(mState, "setMaxAdj", maxAdj)
            if (persistent) {
                callMethod(mState, "setPersistent", true)
            }
        }
    }

    fun setProcessMaxAdjQ(
        processName: String,
        maxAdj: Int,
        persistent: Boolean = false,
    ) {
        val uid = ConfigServer.getPackageUid(processName)
        if (uid <= 0) {
            return
        }
        val mProcessList = getObjectField(amsInstance, "mProcessList") ?: return
        val processRecord =
            callMethod(
                mProcessList,
                "getProcessRecordLocked",
                processName,
                uid,
            ) ?: return
        setProcessRecordMaxAdjQ(processRecord, maxAdj, persistent)
    }

    @Suppress("DEPRECATION")
    fun startFileWatching() {
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
            XposedBridge.log("Failed to hook $pmsClassName, $e")
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
                }
            },
        )
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
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    var processName = param.result.get<String>("processName") ?: return
                    processName = processName.substringBefore(':')
                    val keepAliveConfig = moduleConfig.appKeepAliveConfigs[processName]
                    if (keepAliveConfig == null || !keepAliveConfig.enabled) return

                    try {
                        val finalMaxAdj = keepAliveConfig.maxAdj ?: moduleConfig.globalMaxAdj

                        setProcessRecordMaxAdjQ(
                            param.result,
                            finalMaxAdj,
                            keepAliveConfig.persistent,
                        )
                        XposedBridge.log("Set $processName adj to $finalMaxAdj")
                    } catch (e: Throwable) {
                        XposedBridge.log("Failed to set $processName adj: $e")
                    }
                }
            },
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
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    var processName = param.result.get<String>("processName") ?: return
                    processName = processName.substringBefore(':')
                    val keepAliveConfig = moduleConfig.appKeepAliveConfigs[processName]
                    if (keepAliveConfig == null || !keepAliveConfig.enabled) return
                    try {
                        val finalMaxAdj = keepAliveConfig.maxAdj ?: moduleConfig.globalMaxAdj
                        param.result.set<Int>("maxAdj", finalMaxAdj)

                        if (keepAliveConfig.persistent) {
                            param.result.set<Boolean>("persistent", true)
                        }
                    } catch (e: Throwable) {
                        XposedBridge.log("Failed to set $processName adj: $e")
                    }
                }
            },
        )
    }

    inline fun <reified T> Any.get(field: String): T? =
        try {
            getObjectField(this, field) as? T
        } catch (_: Throwable) {
            null
        }

    inline fun <reified T> Any.set(
        field: String,
        value: T,
    ) {
        try {
            setObjectField(this, field, value)
        } catch (_: Throwable) {
        }
    }
}
