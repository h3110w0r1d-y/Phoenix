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
                setProcessMaxAdj(processName, config.maxAdj ?: moduleConfig.globalMaxAdj)
                changedProcess = changedProcess.minus(processName)
            }
            XposedBridge.log("Config updated: $configJson")
        } catch (e: Exception) {
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

    fun setProcessMaxAdj(
        processName: String,
        maxAdj: Int,
    ) {
        val uid = ConfigServer.getPackageUid(processName)
        if (uid <= 0) {
            return
        }
        val mProcessList = getObjectField(amsInstance, "mProcessList")
        if (mProcessList == null) {
            XposedBridge.log("mProcessList is null")
        }
        val processRecord =
            callMethod(
                mProcessList,
                "getProcessRecordLocked",
                processName,
                uid,
            )
        if (processRecord == null) {
            XposedBridge.log("processRecord is null")
            return
        }
        val mState = processRecord.get<Any>("mState")
        if (mState == null) {
            XposedBridge.log("mState is null")
            return
        }
        callMethod(
            mState,
            "setMaxAdj",
            maxAdj,
        )
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
            } catch (e: Exception) {
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
        } catch (e: Exception) {
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
            return
        }
        hookAllMethods(
            clazz,
            "newProcessRecordLocked",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    try {
                        val processName = param.result.get<String>("processName")
                        val keepAliveConfig = moduleConfig.appKeepAliveConfigs[processName]
                        if (keepAliveConfig == null || !keepAliveConfig.enabled) return
                        val mState = param.result.get<Any>("mState")
                        callMethod(
                            mState,
                            "setMaxAdj",
                            keepAliveConfig.maxAdj ?: moduleConfig.globalMaxAdj,
                        )
                        if (keepAliveConfig.persistent) {
                            callMethod(
                                param.result,
                                "setPersistent",
                                true,
                            )
                        }
                    } catch (_: Exception) {
                        XposedBridge.log("Failed to hook adj")
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
                    try {
                        val processName = param.result.get<String>("processName")
                        val keepAliveConfig = moduleConfig.appKeepAliveConfigs[processName]
                        if (keepAliveConfig == null || !keepAliveConfig.enabled) return
                        param.result.set<Int>(
                            "maxAdj",
                            keepAliveConfig.maxAdj ?: moduleConfig.globalMaxAdj,
                        )

                        if (keepAliveConfig.persistent) {
                            param.result.set<Boolean>("persistent", true)
                        }
                    } catch (_: Exception) {
                        XposedBridge.log("Failed to hook adj")
                    }
                }
            },
        )
    }

    inline fun <reified T> Any.get(field: String): T? =
        try {
            val clazz = this.javaClass
            val declaredField = clazz.getDeclaredField(field)
            declaredField.isAccessible = true
            declaredField.get(this) as T
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    inline fun <reified T> Any.set(
        field: String,
        value: T,
    ) {
        try {
            val clazz = this.javaClass
            val declaredField = clazz.getDeclaredField(field)
            declaredField.isAccessible = true
            declaredField.set(this, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
