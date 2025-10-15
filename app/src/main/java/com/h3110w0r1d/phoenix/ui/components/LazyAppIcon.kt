package com.h3110w0r1d.phoenix.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.h3110w0r1d.phoenix.data.app.AppInfo

@Composable
fun LazyAppIcon(
    appInfo: AppInfo,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(!appInfo.isIconLoaded) }

    // 当图标未加载时，异步加载
    LaunchedEffect(appInfo.packageName) {
        if (!appInfo.isIconLoaded) {
            isLoading = true
            icon = appInfo.loadIcon(context)
            isLoading = false
        } else {
            icon = appInfo.appIcon
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        } else {
            icon?.let { imageBitmap ->
                Image(
                    bitmap = imageBitmap,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
