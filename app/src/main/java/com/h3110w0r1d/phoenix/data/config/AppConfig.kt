package com.h3110w0r1d.phoenix.data.config

/**
 * 配置类
 */
data class AppConfig(
    val isUseSystemColor: Boolean = true,
    val themeColor: String = "blue",
    val nightModeFollowSystem: Boolean = true,
    val nightModeEnabled: Boolean = false,
    val pureBlackDarkTheme: Boolean = false,
    val warnBeforeEnableAll: Boolean = true,
    val warnBeforeEnablePersistent: Boolean = true,
    val warnBeforeEnableKeepActivity: Boolean = true,
    val isConfigInitialized: Boolean = false,
    val moduleConfig: ModuleConfig = ModuleConfig(),
)
