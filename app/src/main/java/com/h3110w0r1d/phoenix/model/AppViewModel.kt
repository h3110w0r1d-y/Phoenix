package com.h3110w0r1d.phoenix.model

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.h3110w0r1d.phoenix.R
import com.h3110w0r1d.phoenix.data.app.AppInfo
import com.h3110w0r1d.phoenix.data.app.AppRepository
import com.h3110w0r1d.phoenix.data.config.AppConfig
import com.h3110w0r1d.phoenix.data.config.AppConfigManager
import com.h3110w0r1d.phoenix.data.config.KeepAliveConfig
import com.h3110w0r1d.phoenix.utils.ConfigClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val appRepository: AppRepository,
        private val configManager: AppConfigManager,
    ) : ViewModel() {
        private val configClient = ConfigClient(context)
        val appConfig: StateFlow<AppConfig> = configManager.appConfig
        private val _isLoadingApps: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val isLoadingApps: StateFlow<Boolean> = _isLoadingApps

        private val _searchAppList: MutableStateFlow<List<AppInfo>> = MutableStateFlow(listOf())
        val searchAppList: StateFlow<List<AppInfo>> = _searchAppList
        private var searchText = ""

        fun isModuleActive() = configClient.isModuleActive

        fun loadApps() {
            if (appRepository.apps.value.isNotEmpty()) return
            refreshApps()
        }

        fun toggleModule() {
            val newModuleConfig =
                appConfig.value.moduleConfig.copy(
                    moduleEnabled = !appConfig.value.moduleConfig.moduleEnabled,
                )
            configClient.updateConfig(Json.encodeToString(newModuleConfig))
            viewModelScope.launch {
                configManager.updateModuleConfig(newModuleConfig)
            }
        }

        fun toggleApp(packageName: String) {
            // 创建新的 enabledApps 映射，而不是直接引用
            val currentEnabledApps = appConfig.value.moduleConfig.appKeepAliveConfigs
            val newEnabledApps = currentEnabledApps.toMutableMap()

            val keepAliveConfig = newEnabledApps[packageName]
            if (keepAliveConfig == null) {
                newEnabledApps[packageName] =
                    KeepAliveConfig(
                        enabled = true,
                        maxAdj = null,
                    )
            } else {
                if (keepAliveConfig.enabled && keepAliveConfig.maxAdj == null) {
                    newEnabledApps.remove(packageName)
                } else {
                    newEnabledApps[packageName] =
                        keepAliveConfig.copy(
                            enabled = !keepAliveConfig.enabled,
                        )
                }
            }

            val newModuleConfig =
                appConfig.value.moduleConfig.copy(
                    appKeepAliveConfigs = newEnabledApps as HashMap<String, KeepAliveConfig>,
                )
            configClient.updateConfig(Json.encodeToString(newModuleConfig))
            viewModelScope.launch {
                configManager.updateModuleConfig(newModuleConfig)
            }
        }

        fun updateAppMaxAdj(
            packageName: String,
            maxAdj: Int?,
        ) {
            val currentEnabledApps = appConfig.value.moduleConfig.appKeepAliveConfigs
            val newEnabledApps = currentEnabledApps.toMutableMap()

            val keepAliveConfig = newEnabledApps[packageName]
            if (keepAliveConfig != null) {
                if (maxAdj == null && !keepAliveConfig.enabled) {
                    // 如果 maxAdj 为 null 且未启用，直接移除
                    newEnabledApps.remove(packageName)
                } else {
                    // 更新 maxAdj
                    newEnabledApps[packageName] = keepAliveConfig.copy(maxAdj = maxAdj)
                }
            } else if (maxAdj != null) {
                // 如果配置不存在但设置了 maxAdj，创建新配置
                newEnabledApps[packageName] =
                    KeepAliveConfig(
                        enabled = false,
                        maxAdj = maxAdj,
                    )
            }

            val newModuleConfig =
                appConfig.value.moduleConfig.copy(
                    appKeepAliveConfigs = newEnabledApps as HashMap<String, KeepAliveConfig>,
                )
            configClient.updateConfig(Json.encodeToString(newModuleConfig))
            viewModelScope.launch {
                configManager.updateModuleConfig(newModuleConfig)
            }
        }

        fun updateGlobalMaxAdj(globalMaxAdj: Int) {
            val newModuleConfig =
                appConfig.value.moduleConfig.copy(
                    globalMaxAdj = globalMaxAdj,
                )
            configClient.updateConfig(Json.encodeToString(newModuleConfig))
            viewModelScope.launch {
                configManager.updateModuleConfig(newModuleConfig)
            }
        }

        fun updateAppPersistent(
            packageName: String,
            persistent: Boolean,
        ) {
            val currentEnabledApps = appConfig.value.moduleConfig.appKeepAliveConfigs
            val newEnabledApps = currentEnabledApps.toMutableMap()

            val keepAliveConfig = newEnabledApps[packageName]
            if (keepAliveConfig != null) {
                newEnabledApps[packageName] = keepAliveConfig.copy(persistent = persistent)
            } else {
                // 如果配置不存在，创建新配置
                newEnabledApps[packageName] =
                    KeepAliveConfig(
                        enabled = false,
                        maxAdj = null,
                        persistent = persistent,
                    )
            }

            val newModuleConfig =
                appConfig.value.moduleConfig.copy(
                    appKeepAliveConfigs = newEnabledApps as HashMap<String, KeepAliveConfig>,
                )
            configClient.updateConfig(Json.encodeToString(newModuleConfig))
            viewModelScope.launch {
                configManager.updateModuleConfig(newModuleConfig)
            }
        }

        fun refreshApps() {
            if (_isLoadingApps.value) return
            _isLoadingApps.value = true
            viewModelScope.launch {
                appRepository.loadApps()
                _isLoadingApps.value = false
                searchApps("")
            }
        }

        fun searchApps(key: String? = null) {
            val key = key?.lowercase(Locale.getDefault()) ?: searchText
            searchText = key
            val appInfo = mutableListOf<AppInfo>()
            val currentAppList = appRepository.apps.value
            val moduleConfig = appConfig.value.moduleConfig

            for (app in currentAppList) {
                if (app.isSystemApp) {
                    continue
                }
                if (app.packageName
                        .contains(key) ||
                    app.appName
                        .lowercase(Locale.getDefault())
                        .contains(key)
                ) {
                    appInfo.add(app)
                }
            }
            appInfo.sortWith(
                Comparator { o1, o2 ->
                    val o1Enabled = moduleConfig.appKeepAliveConfigs[o1.packageName]?.enabled ?: false
                    val o2Enabled = moduleConfig.appKeepAliveConfigs[o2.packageName]?.enabled ?: false
                    if (o1Enabled && !o2Enabled) {
                        return@Comparator -1
                    }
                    if (!o1Enabled && o2Enabled) {
                        return@Comparator 1
                    }
                    return@Comparator o1.appName.compareTo(o2.appName, true)
                },
            )
            _searchAppList.value = appInfo
        }

        fun openLSPosedManager() {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage("org.lsposed.manager")
            if (intent == null) {
                Toast.makeText(context, context.getString(R.string.lsposed_manager_not_install), Toast.LENGTH_SHORT).show()
                return
            }
            context.startActivity(intent)
        }

        fun updateAppConfig(config: AppConfig) {
            viewModelScope.launch {
                configManager.updateAppConfig(config)
            }
        }
    }

val LocalGlobalViewModel =
    staticCompositionLocalOf<AppViewModel> {
        error("AppViewModel not provided!")
    }
