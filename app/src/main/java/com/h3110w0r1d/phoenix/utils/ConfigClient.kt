package com.h3110w0r1d.phoenix.utils

import android.content.Context
import android.content.Context.STORAGE_SERVICE
import android.os.storage.StorageManager
import com.h3110w0r1d.phoenix.utils.ConfigServer.QUERY_CONFIG
import com.h3110w0r1d.phoenix.utils.ConfigServer.SERVER_VERSION_CODE
import com.h3110w0r1d.phoenix.utils.ConfigServer.SERVER_VERSION_NAME
import com.h3110w0r1d.phoenix.utils.ConfigServer.UPDATE_CONFIG

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
}
