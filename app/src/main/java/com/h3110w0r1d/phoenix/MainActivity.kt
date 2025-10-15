package com.h3110w0r1d.phoenix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.h3110w0r1d.phoenix.data.config.LocalGlobalAppConfig
import com.h3110w0r1d.phoenix.model.AppViewModel
import com.h3110w0r1d.phoenix.model.LocalGlobalViewModel
import com.h3110w0r1d.phoenix.ui.AppNavigation
import com.h3110w0r1d.phoenix.ui.theme.PhoenixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val appConfig by appViewModel.appConfig.collectAsState()
            // 只有在配置初始化完成后才显示主界面，防止配置未加载完成时闪现引导界面
            if (!appConfig.isConfigInitialized) return@setContent

            val isDarkMode =
                if (appConfig.nightModeFollowSystem) {
                    isSystemInDarkTheme()
                } else {
                    appConfig.nightModeEnabled
                }
            enableEdgeToEdge(
                statusBarStyle =
                    SystemBarStyle.auto(
                        Color.Transparent.toArgb(),
                        Color.Transparent.toArgb(),
                    ) { isDarkMode },
            )
            PhoenixTheme(
                darkTheme = isDarkMode,
                dynamicColor = appConfig.isUseSystemColor,
                highContrast = appConfig.highContrastEnabled,
                customColorScheme = appConfig.themeColor,
            ) {
//                packageManager.getInstallerPackageName("$UPDATE_CONFIG{}")
//                val result = packageManager.getInstallerPackageName(QUERY_CONFIG) ?: "{}"
//                Log.i("DEBUG", result)
                CompositionLocalProvider(
                    LocalGlobalViewModel provides appViewModel,
                    LocalGlobalAppConfig provides appConfig,
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
