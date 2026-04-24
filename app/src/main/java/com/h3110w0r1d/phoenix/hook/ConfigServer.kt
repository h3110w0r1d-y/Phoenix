package com.h3110w0r1d.phoenix.hook

import android.os.Binder
import android.util.Log
import androidx.annotation.Keep
import com.h3110w0r1d.phoenix.BuildConfig
import com.h3110w0r1d.phoenix.data.config.ModuleConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.serialization.json.Json
import java.io.File

@Keep
object ConfigServer : XC_MethodHook() {
    const val SERVER_VERSION_NAME = "serverVersionName:"
    const val SERVER_VERSION_CODE = "serverVersionCode:"
    const val QUERY_CONFIG = "queryConfig:"
    const val UPDATE_CONFIG = "updateConfig:"
    const val QUERY_KEEP_SERVICE = "queryKeepService:"
    const val CONFIG_FILE = "/data/system/${BuildConfig.APPLICATION_ID}/config.json"
    private var packageUid: Int? = null
    private val configFile = File(CONFIG_FILE)
    private var configJson = "{}"
    private var _moduleConfig: ModuleConfig? = null
    var updateConfigCallback: (() -> Unit)? = null
    private lateinit var _packageManager: Any

    val packageManager: Any
        get() {
            if (::_packageManager.isInitialized) {
                return _packageManager
            }
            _packageManager =
                XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", null),
                    "getPackageManager",
                )
            return _packageManager
        }

    val moduleConfig: ModuleConfig
        get() {
            _moduleConfig?.let { return it }

            try {
                if (configFile.exists()) {
                    configJson = File(CONFIG_FILE).readText()
                    _moduleConfig = Json.Default.decodeFromString(configJson)
                } else {
                    _moduleConfig = ModuleConfig()
                }
            } catch (_: Throwable) {
                _moduleConfig = ModuleConfig()
            }
            _moduleConfig?.let { return it }
            return ModuleConfig()
        }

    override fun beforeHookedMethod(param: MethodHookParam) {
        val firstArg = param.args.first()?.toString() ?: return
        if (!firstArg.contains(":")) return
        val callingUid = Binder.getCallingUid()
        if (callingUid == getModulePackageUid(BuildConfig.APPLICATION_ID)) {
            when {
                firstArg.startsWith(SERVER_VERSION_NAME) -> {
                    param.result = BuildConfig.VERSION_CODE.toString()
                }

                firstArg.startsWith(QUERY_CONFIG) -> {
                    param.result = queryConfig()
                }

                firstArg.startsWith(UPDATE_CONFIG) -> {
                    val arg = firstArg.substring(UPDATE_CONFIG.length)
                    updateConfig(arg)
                    param.result = ""
                }
            }
        } else {
            if (!moduleConfig.moduleEnabled) return
            when {
                firstArg.startsWith(QUERY_KEEP_SERVICE) -> {
                    param.result = getKeepService(callingUid)
                }
            }
        }
    }

    fun getKeepService(uid: Int): String {
        val packages = getPackagesForUid(uid)
        for (pkgName in packages) {
            val cfg = moduleConfig.appKeepAliveConfigs[pkgName] ?: continue
            val pkgEnabled = cfg.enabled
            val keepEnabled = cfg.keepService
            if (pkgEnabled && keepEnabled) {
                return Json.encodeToString(cfg.keepServiceList)
            }
        }
        return "[]"
    }

    private fun queryConfig(): String =
        try {
            configFile.readText()
        } catch (_: Exception) {
            "{}"
        }

    private fun updateConfig(cfg: String) {
        configFile.parentFile?.mkdirs()
        try {
            configJson = cfg
            _moduleConfig = Json.Default.decodeFromString(configJson)
            configFile.writeText(configJson)
            updateConfigCallback?.invoke()
        } catch (e: Exception) {
            Log.e("ConfigServer", "Update config error:" + e.message)
        }
    }

    fun getPackagesForUid(uid: Int): Array<String> {
        try {
            @Suppress("UNCHECKED_CAST")
            return XposedHelpers.callMethod(
                packageManager,
                "getPackagesForUid",
                uid,
            ) as Array<String>
        } catch (e: Throwable) {
            XposedBridge.log("Failed to get package uid: $e")
        }
        return emptyArray()
    }

    fun getModulePackageUid(packageName: String): Int {
        try {
            if (packageName == BuildConfig.APPLICATION_ID) {
                packageUid?.let { return it }
            }
            val resultUid =
                XposedHelpers.callMethod(
                    packageManager,
                    "getPackageUid",
                    packageName,
                    0,
                    0,
                ) as Int

            if (packageName == BuildConfig.APPLICATION_ID) {
                packageUid = resultUid
            }
            return resultUid
        } catch (e: Throwable) {
            XposedBridge.log("Failed to get package uid: $e")
        }
        return -1
    }
}
