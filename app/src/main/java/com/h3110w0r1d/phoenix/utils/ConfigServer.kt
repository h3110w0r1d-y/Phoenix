package com.h3110w0r1d.phoenix.utils

import android.os.Binder
import android.util.Log
import androidx.annotation.Keep
import com.h3110w0r1d.phoenix.BuildConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.io.File

@Keep
object ConfigServer : XC_MethodHook() {
    const val SERVER_VERSION_NAME = "serverVersionName:"
    const val SERVER_VERSION_CODE = "serverVersionCode:"
    const val QUERY_CONFIG = "queryConfig:"
    const val UPDATE_CONFIG = "updateConfig:"
    const val CONFIG_FILE = "/data/system/${BuildConfig.APPLICATION_ID}/config.json"
    private var packageUid: Int? = null
    private val configFile = File(CONFIG_FILE)

    override fun beforeHookedMethod(param: MethodHookParam) {
        val firstArg = param.args.first()?.toString() ?: return
        if (!firstArg.contains(":")) return

        if (Binder.getCallingUid() != getPackageUid(BuildConfig.APPLICATION_ID)) {
            return
        }
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
    }

    private fun queryConfig(): String =
        try {
            configFile.readText()
        } catch (_: Exception) {
            "{}"
        }

    private fun updateConfig(configJson: String) {
        configFile.parentFile?.mkdirs()
        try {
            configFile.writeText(configJson)
        } catch (e: Exception) {
            Log.e("ConfigServer", "Update config error:" + e.message)
        }
    }

    fun getPackageUid(packageName: String): Int {
        try {
            if (packageName == BuildConfig.APPLICATION_ID) {
                packageUid?.let { return it }
            }
            val packageManager =
                XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", null),
                    "getPackageManager",
                )
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
        } catch (_: Throwable) {
        }
        return -1
    }
}
