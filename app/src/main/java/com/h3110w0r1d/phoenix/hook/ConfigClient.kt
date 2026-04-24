package com.h3110w0r1d.phoenix.hook

import android.content.Context
import android.content.Context.STORAGE_SERVICE
import android.os.storage.StorageManager
import com.h3110w0r1d.phoenix.hook.ConfigServer.QUERY_CONFIG
import com.h3110w0r1d.phoenix.hook.ConfigServer.QUERY_KEEP_SERVICE
import com.h3110w0r1d.phoenix.hook.ConfigServer.SERVER_VERSION_CODE
import com.h3110w0r1d.phoenix.hook.ConfigServer.SERVER_VERSION_NAME
import com.h3110w0r1d.phoenix.hook.ConfigServer.UPDATE_CONFIG
import kotlinx.serialization.json.Json

class ConfigClient(
    context: Context,
) {
    private val storageManager = context.getSystemService(STORAGE_SERVICE) as StorageManager

    val serverVersionName: String? = storageManager.getMountedObbPath(SERVER_VERSION_NAME)

    val serverVersionCode = (storageManager.getMountedObbPath(SERVER_VERSION_CODE) ?: "0").toInt()
    val isModuleActive = serverVersionName != null

    var configJson = storageManager.getMountedObbPath(QUERY_CONFIG) ?: "{}"

    fun updateConfig(configJson: String) {
        this.configJson = configJson
        storageManager.getMountedObbPath("$UPDATE_CONFIG$configJson")
    }

    fun queryKeepService(): Array<String> {
        try {
            val jsonStr = storageManager.getMountedObbPath(QUERY_KEEP_SERVICE) ?: "[]"
            return Json.decodeFromString(jsonStr)
        } catch (_: Exception) {
            return arrayOf()
        }
    }
}
