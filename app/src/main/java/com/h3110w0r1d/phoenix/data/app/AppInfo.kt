package com.h3110w0r1d.phoenix.data.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppInfo(
    val packageName: String,
    var appName: String,
    private var _appIcon: ImageBitmap? = null,
    var isSystemApp: Boolean,
    var isPersistent: Boolean = false,
    var targetApi: Int = 0,
) {
    // 懒加载的appIcon属性
    var appIcon: ImageBitmap?
        get() = _appIcon
        set(value) {
            _appIcon = value
        }

    // 是否已加载图标
    var isIconLoaded: Boolean = false
        private set

    // 异步加载图标
    suspend fun loadIcon(
        context: Context,
        sizePx: Int,
    ): ImageBitmap? {
        if (isIconLoaded && _appIcon != null) {
            return _appIcon
        }

        return withContext(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                val icon =
                    applicationInfo
                        .loadIcon(packageManager)
                        ?.toBitmap(
                            width = sizePx.coerceAtLeast(1),
                            height = sizePx.coerceAtLeast(1),
                            config = Bitmap.Config.ARGB_8888,
                        )
                        ?.asImageBitmap()

                _appIcon = icon
                isIconLoaded = true
                icon
            } catch (_: Exception) {
                null
            }
        }
    }
}
