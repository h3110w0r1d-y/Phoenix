package com.h3110w0r1d.phoenix.data.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import kotlin.collections.mutableListOf

class AppRepository(
    private val context: Context,
) {
    private val _apps = MutableStateFlow<List<AppInfo>>(listOf())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    // 排序相关
    private val collator: Collator = Collator.getInstance(Locale.CHINA)

    /**
     * 对应用列表进行排序
     */
    private fun sortAppList(appList: List<AppInfo>): List<AppInfo> {
        val sortedList = appList.toMutableList()
        sortedList.sortWith(compareBy(collator) { it.appName })
        return sortedList
    }

    /**
     * 处理单个packageInfo的通用函数
     * @param packageInfo 要处理的packageInfo
     * @param packageManager PackageManager实例
     * @return 处理结果
     */
    private fun processResolveInfo(
        packageInfo: PackageInfo,
        packageManager: PackageManager,
    ): AppInfo? {
        val packageName = packageInfo.packageName

        val appName = packageInfo.applicationInfo?.loadLabel(packageManager).toString()

        if (appName.isEmpty()) {
            return null
        }
//        val appIcon =
//            packageInfo.applicationInfo
//                ?.loadIcon(packageManager)
//                ?.toBitmap()
//                ?.asImageBitmap()
//        if (appIcon == null) {
//            return null
//        }
        val applicationInfo = packageInfo.applicationInfo
        val isSystemApp = (applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val packageUid = packageInfo.applicationInfo?.uid ?: 0
        return AppInfo(
            packageName,
            appName,
            null,
            isSystemApp,
            packageUid,
        )
    }

    @SuppressLint("QueryPermissionsNeeded")
    suspend fun loadApps() {
        withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val packageInfoList =
                packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            val result = mutableListOf<AppInfo>()
            for (packageInfo in packageInfoList) {
                val processResult = processResolveInfo(packageInfo, packageManager)
                if (processResult != null) {
                    result.add(processResult)
                }
            }
            // 对应用列表进行排序
            val sortedResult = sortAppList(result)

            // 更新StateFlow
            _apps.value = sortedResult
        }
    }
}
