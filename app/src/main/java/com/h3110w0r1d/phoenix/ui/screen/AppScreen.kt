package com.h3110w0r1d.phoenix.ui.screen

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h3110w0r1d.phoenix.R
import com.h3110w0r1d.phoenix.data.config.AppConfig
import com.h3110w0r1d.phoenix.data.config.KeepAliveConfig
import com.h3110w0r1d.phoenix.data.config.LocalGlobalAppConfig
import com.h3110w0r1d.phoenix.model.AppViewModel
import com.h3110w0r1d.phoenix.model.LocalGlobalViewModel
import com.h3110w0r1d.phoenix.ui.components.CountdownWarningDialog
import com.h3110w0r1d.phoenix.ui.components.KeepServiceSelectionDialog
import com.h3110w0r1d.phoenix.ui.components.LargeFlexibleTopAppBar
import com.h3110w0r1d.phoenix.ui.components.LazyAppIcon
import com.h3110w0r1d.phoenix.ui.components.MaxAdjDialog
import com.h3110w0r1d.phoenix.ui.components.rememberTopAppBarState
import kotlin.text.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    val viewModel = LocalGlobalViewModel.current
    val appConfig = LocalGlobalAppConfig.current
    val apps by viewModel.searchAppList.collectAsState()
    val moduleConfig = appConfig.moduleConfig
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isBatchMenuExpanded by remember { mutableStateOf(false) }
    var showEnableAllWarning by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    BackHandler(enabled = isSearching) {
        isSearching = false
    }
    LaunchedEffect(apps) {
        listState.scrollToItem(0)
    }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        } else {
            searchText = ""
            viewModel.searchApps("")
        }
    }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            snapAnimationSpec = null,
        )

    val isScrolledToTop by remember {
        derivedStateOf {
            scrollBehavior.state.collapsedFraction < 0.01f
        }
    }

    // 检测是否正在滚动
    var isScrolling by remember { mutableStateOf(false) }

    // 监听滚动状态变化
    LaunchedEffect(listState.isScrollInProgress) {
        isScrolling =
            if (listState.isScrollInProgress) {
                !isScrolledToTop
            } else {
                false
            }
    }

    // 计算是否应该启用pullToRefresh
    val shouldEnablePullToRefresh by remember {
        derivedStateOf {
            isScrolledToTop && !isScrolling && !isSearching
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isSearching) {
                SearchBar(
                    expanded = false,
                    onExpandedChange = {},
                    inputField = {
                        SearchBarDefaults.InputField(
                            expanded = false,
                            query = searchText,
                            onQueryChange = {
                                searchText = it
                                viewModel.searchApps(searchText)
                            },
                            onSearch = {},
                            onExpandedChange = {},
                            modifier = Modifier.focusRequester(focusRequester),
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearching = false
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchText = ""
                                        viewModel.searchApps("")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            },
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { }
            } else {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.app_list)) },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(
                            onClick = {
                                isSearching = !isSearching
                                searchText = ""
                            },
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                            )
                        }
                        Box {
                            IconButton(
                                onClick = {
                                    isBatchMenuExpanded = true
                                },
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = null,
                                )
                            }
                            DropdownMenu(
                                expanded = isBatchMenuExpanded,
                                onDismissRequest = { isBatchMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.enable_all)) },
                                    onClick = {
                                        isBatchMenuExpanded = false
                                        if (appConfig.warnBeforeEnableAll) {
                                            showEnableAllWarning = true
                                        } else {
                                            viewModel.enableAllLoadedAppsExcludeSystem()
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.disable_all)) },
                                    onClick = {
                                        isBatchMenuExpanded = false
                                        viewModel.disableAllLoadedApps()
                                    },
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        val pullRefreshState = rememberPullToRefreshState()
        Box(
            modifier =
                Modifier.fillMaxSize().pullToRefresh(
                    isRefreshing = isLoadingApps,
                    state = pullRefreshState,
                    enabled = shouldEnablePullToRefresh,
                    onRefresh = { viewModel.refreshApps() },
                ),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
            ) {
                items(apps.size) { i ->
                    val appName = apps[i].appName
                    val packageName = apps[i].packageName
                    val keepAliveConfig = moduleConfig.appKeepAliveConfigs[apps[i].packageName]
                    var isExpanded by remember { mutableStateOf(false) }
                    ListItem(
                        leadingContent = {
                            LazyAppIcon(
                                appInfo = apps[i],
                                contentDescription = appName,
                                modifier =
                                    Modifier
                                        .width(44.dp)
                                        .aspectRatio(1f),
                            )
                        },
                        headlineContent = {
                            Text(
                                appName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Row {
                                if (keepAliveConfig?.enabled != true) return@Row

                                if (keepAliveConfig.maxAdj != null) {
                                    Tag("MaxAdj:${keepAliveConfig.maxAdj}")
                                } else {
                                    Tag("MaxAdj:" + stringResource(R.string.max_adj_default) + "(${moduleConfig.globalMaxAdj})")
                                }
                                if (keepAliveConfig.persistent) {
                                    Tag("Persistent")
                                }
                                if (keepAliveConfig.keepActivity) {
                                    Tag("Activity")
                                }
                                if (keepAliveConfig.keepService) {
                                    Tag("Service")
                                }
                            }
                        },
                        trailingContent = {
                            Box(modifier = Modifier.padding(vertical = 10.dp)) {
                                Switch(
                                    checked = keepAliveConfig?.enabled ?: false,
                                    onCheckedChange = {
                                        viewModel.toggleApp(packageName)
                                    },
                                )
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = true,
                                    onClick = { isExpanded = !isExpanded },
                                ),
                    )
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter =
                            expandVertically(
                                expandFrom = Alignment.Top,
                            ) + fadeIn(),
                        exit =
                            shrinkVertically(
                                shrinkTowards = Alignment.Top,
                            ) + fadeOut(),
                    ) {
                        ExpandCard(
                            viewModel = viewModel,
                            appConfig = appConfig,
                            packageName = packageName,
                            keepAliveConfig = keepAliveConfig,
                            isPersistent = apps[i].isPersistent,
                            targetApi = apps[i].targetApi,
                        )
                    }
                    DisposableEffect(packageName) {
                        onDispose {
                            isExpanded = false
                        }
                    }
                }
            }

            Indicator(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = innerPadding.calculateTopPadding()),
                isRefreshing = isLoadingApps,
                state = pullRefreshState,
            )
        }
    }

    CountdownWarningDialog(
        visible = showEnableAllWarning,
        onDismissRequest = { showEnableAllWarning = false },
        title = { Text(stringResource(R.string.enable_all_warning_title)) },
        text = {
            Text(
                stringResource(R.string.enable_all_warning_message),
                style = typography.bodyMedium,
            )
        },
        countdownSeconds = 3,
        onCancel = {},
        onContinue = { dontRemindAgain ->
            if (dontRemindAgain) {
                viewModel.updateAppConfig(
                    appConfig.copy(warnBeforeEnableAll = false),
                )
            }
            viewModel.enableAllLoadedAppsExcludeSystem()
        },
    )
}

@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        fontSize = 10.sp,
        maxLines = 1,
        modifier =
            modifier
                .padding(end = 4.dp)
                .background(
                    color = colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(4.dp),
                ).padding(horizontal = 4.dp),
    )
}

@Composable
fun ExpandCard(
    viewModel: AppViewModel,
    appConfig: AppConfig,
    packageName: String,
    keepAliveConfig: KeepAliveConfig?,
    isPersistent: Boolean,
    targetApi: Int,
) {
    var showPersistentWarning by remember { mutableStateOf(false) }
    var showKeepActivityWarning by remember { mutableStateOf(false) }
    var showKeepServiceWarning by remember { mutableStateOf(false) }
    var showKeepServiceSelectionDialog by remember { mutableStateOf(false) }
    var keepServiceOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var keepServiceSelected by remember { mutableStateOf<List<String>>(emptyList()) }
    var openServiceSelectionAfterWarning by remember { mutableStateOf(false) }
    val canUseKeepService = targetApi < 34

    fun requestEnablePersistent() {
        if (!appConfig.warnBeforeEnablePersistent) {
            viewModel.updateAppPersistent(packageName, true)
        } else {
            showPersistentWarning = true
        }
    }

    fun requestEnableKeepActivity() {
        if (!appConfig.warnBeforeEnableKeepActivity) {
            viewModel.updateAppKeepActivity(packageName, true)
        } else {
            showKeepActivityWarning = true
        }
    }

    fun openKeepServiceSelectionDialog() {
        keepServiceOptions = viewModel.getAppServiceList(packageName)
        val currentSelected = keepAliveConfig?.keepServiceList.orEmpty()
        keepServiceSelected =
            if (currentSelected.isNotEmpty()) {
                currentSelected
            } else {
                emptyList()
            }
        showKeepServiceSelectionDialog = true
    }

    fun requestEnableKeepService() {
        openServiceSelectionAfterWarning = true
        if (!appConfig.warnBeforeEnableKeepService) {
            openKeepServiceSelectionDialog()
        } else {
            showKeepServiceWarning = true
        }
    }

    fun dismissPersistentWarning() {
        showPersistentWarning = false
    }

    fun dismissKeepActivityWarning() {
        showKeepActivityWarning = false
    }

    fun dismissKeepServiceWarning() {
        showKeepServiceWarning = false
        openServiceSelectionAfterWarning = false
    }
    Card(
        border = BorderStroke(1.dp, colorScheme.outlineVariant),
        colors = cardColors().copy(containerColor = Color.Transparent),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
    ) {
        Column {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.max_adj_setting),
                    style = typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                val currentMaxAdj = keepAliveConfig?.maxAdj
                var maxAdjInput by remember(currentMaxAdj) {
                    mutableStateOf(currentMaxAdj?.toString() ?: "")
                }
                var inputError by remember { mutableStateOf(false) }
                var showMaxAdjHelp by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedTextField(
                        value = maxAdjInput,
                        onValueChange = {
                            maxAdjInput = it
                            inputError = false
                        },
                        label = { Text(stringResource(R.string.max_adj_value)) },
                        placeholder = { Text(stringResource(R.string.input_number_hint)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                        isError = inputError,
                        supportingText =
                            if (inputError) {
                                { Text(stringResource(R.string.please_input_valid_integer)) }
                            } else {
                                null
                            },
                        singleLine = true,
                    )
                    IconButton(onClick = { showMaxAdjHelp = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription =
                                stringResource(R.string.adj_help_icon_description),
                            tint = colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (showMaxAdjHelp) {
                    MaxAdjDialog(onDismissRequest = { showMaxAdjHelp = false })
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            // 恢复默认（设置为 null）
                            viewModel.updateAppMaxAdj(packageName, null)
                            maxAdjInput = ""
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.restore_default))
                    }

                    Button(
                        onClick = {
                            val maxAdjValue = maxAdjInput.toIntOrNull()
                            if (maxAdjValue != null) {
                                viewModel.updateAppMaxAdj(packageName, maxAdjValue)
                                inputError = false
                            } else if (maxAdjInput.isNotEmpty()) {
                                inputError = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }
            }

            // Persistent 设置
            ListItem(
                headlineContent = {
                    Text(
                        text = "Persistent",
                        style = typography.titleMedium,
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.persistent_description),
                        style = typography.bodySmall,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = (keepAliveConfig?.persistent ?: false) or isPersistent,
                        enabled = !isPersistent,
                        onCheckedChange = { checked ->
                            if (checked) {
                                requestEnablePersistent()
                            } else {
                                viewModel.updateAppPersistent(packageName, false)
                            }
                        },
                    )
                },
                modifier =
                    Modifier.clickable(!isPersistent) {
                        val on = (keepAliveConfig?.persistent ?: false) or isPersistent
                        if (on) {
                            viewModel.updateAppPersistent(packageName, false)
                        } else {
                            requestEnablePersistent()
                        }
                    },
            )

            // KeepService 设置
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.keep_service),
                        style = typography.titleMedium,
                    )
                },
                supportingContent = {
                    Text(
                        text =
                            if (canUseKeepService) {
                                stringResource(R.string.keep_service_description)
                            } else {
                                stringResource(R.string.keep_service_target_api_not_supported, targetApi)
                            },
                        style = typography.bodySmall,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = keepAliveConfig?.keepService ?: false,
                        enabled = canUseKeepService,
                        onCheckedChange = { checked ->
                            if (checked) {
                                requestEnableKeepService()
                            } else {
                                viewModel.updateAppKeepService(packageName, false)
                            }
                        },
                    )
                },
                modifier =
                    Modifier.clickable(enabled = canUseKeepService) {
                        if (keepAliveConfig?.keepService ?: false) {
                            openKeepServiceSelectionDialog()
                        } else {
                            requestEnableKeepService()
                        }
                    },
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return@Column
            }

            // KeepActivity 设置
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.keep_activity),
                        style = typography.titleMedium,
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.keep_activity_description),
                        style = typography.bodySmall,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = keepAliveConfig?.keepActivity ?: false,
                        onCheckedChange = { checked ->
                            if (checked) {
                                requestEnableKeepActivity()
                            } else {
                                viewModel.updateAppKeepActivity(packageName, false)
                            }
                        },
                    )
                },
                modifier =
                    Modifier.clickable {
                        if (keepAliveConfig?.keepActivity ?: false) {
                            viewModel.updateAppKeepActivity(packageName, false)
                        } else {
                            requestEnableKeepActivity()
                        }
                    },
            )
        }
    }

    if (showKeepServiceSelectionDialog) {
        KeepServiceSelectionDialog(
            allServices = keepServiceOptions,
            selectedServices = keepServiceSelected,
            onDismissRequest = { showKeepServiceSelectionDialog = false },
            onSave = { selected ->
                showKeepServiceSelectionDialog = false
                viewModel.updateAppKeepServiceConfig(
                    packageName = packageName,
                    keepService = true,
                    keepServiceList = selected,
                )
            },
        )
    }

    CountdownWarningDialog(
        visible = showPersistentWarning,
        onDismissRequest = { dismissPersistentWarning() },
        title = { Text(stringResource(R.string.persistent_warning_title)) },
        text = {
            Text(
                stringResource(R.string.persistent_warning_message),
                style = typography.bodyMedium,
            )
        },
        onContinue = { dontRemindAgain ->
            if (dontRemindAgain) {
                viewModel.updateAppConfig(
                    appConfig.copy(warnBeforeEnablePersistent = false),
                )
            }
            viewModel.updateAppPersistent(packageName, true)
        },
    )

    CountdownWarningDialog(
        visible = showKeepActivityWarning,
        onDismissRequest = { dismissKeepActivityWarning() },
        title = { Text(stringResource(R.string.keep_activity_warning_title)) },
        text = {
            Text(
                stringResource(R.string.keep_activity_warning_message),
                style = typography.bodyMedium,
            )
        },
        onContinue = { dontRemindAgain ->
            if (dontRemindAgain) {
                viewModel.updateAppConfig(
                    appConfig.copy(warnBeforeEnableKeepActivity = false),
                )
            }
            viewModel.updateAppKeepActivity(packageName, true)
        },
    )

    CountdownWarningDialog(
        visible = showKeepServiceWarning,
        onDismissRequest = { dismissKeepServiceWarning() },
        title = { Text(stringResource(R.string.keep_service_warning_title)) },
        text = {
            Text(
                stringResource(R.string.keep_service_warning_message),
                style = typography.bodyMedium,
            )
        },
        onContinue = { dontRemindAgain ->
            if (dontRemindAgain) {
                viewModel.updateAppConfig(
                    appConfig.copy(warnBeforeEnableKeepService = false),
                )
            }
            if (openServiceSelectionAfterWarning) {
                openKeepServiceSelectionDialog()
            } else {
                viewModel.updateAppKeepService(packageName, true)
            }
            openServiceSelectionAfterWarning = false
        },
    )
}
