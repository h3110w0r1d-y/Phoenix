package com.h3110w0r1d.phoenix.data.config

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

/**
 * 配置键管理
 */
private object ConfigKeys {
    // 主题
    val isUseSystemColor = booleanPreferencesKey("is_use_system_color")
    val themeColor = stringPreferencesKey("theme_color")
    val nightModeFollowSystem = booleanPreferencesKey("night_mode_follow_system")
    val nightModeEnabled = booleanPreferencesKey("night_mode_enabled")
    val highContrastEnabled = booleanPreferencesKey("high_contrast_enabled")
    val moduleConfig = stringPreferencesKey("module_config")
}

class AppConfigManager(
    private val dataStore: DataStore<Preferences>,
) {
    val appConfig: StateFlow<AppConfig> =
        dataStore.data
            .map { preferences ->
                AppConfig(
                    isUseSystemColor = preferences[ConfigKeys.isUseSystemColor] ?: true,
                    themeColor = preferences[ConfigKeys.themeColor] ?: "blue",
                    nightModeFollowSystem = preferences[ConfigKeys.nightModeFollowSystem] ?: true,
                    nightModeEnabled = preferences[ConfigKeys.nightModeEnabled] ?: false,
                    highContrastEnabled = preferences[ConfigKeys.highContrastEnabled] ?: false,
                    moduleConfig = Json.decodeFromString(preferences[ConfigKeys.moduleConfig] ?: "{}"),
                    isConfigInitialized = true,
                )
            }.stateIn(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                started = SharingStarted.Eagerly,
                initialValue = AppConfig(),
            )

    suspend fun updateAppConfig(appConfig: AppConfig) {
        dataStore.edit { preferences ->
            preferences[ConfigKeys.isUseSystemColor] = appConfig.isUseSystemColor
            preferences[ConfigKeys.themeColor] = appConfig.themeColor
            preferences[ConfigKeys.nightModeFollowSystem] = appConfig.nightModeFollowSystem
            preferences[ConfigKeys.nightModeEnabled] = appConfig.nightModeEnabled
            preferences[ConfigKeys.highContrastEnabled] = appConfig.highContrastEnabled
        }
    }

    suspend fun updateModuleConfig(moduleConfig: ModuleConfig) {
        dataStore.edit { preferences ->
            preferences[ConfigKeys.moduleConfig] = Json.encodeToString(moduleConfig)
        }
    }
}

val LocalGlobalAppConfig =
    staticCompositionLocalOf<AppConfig> {
        error("AppConfig not provided!")
    }
