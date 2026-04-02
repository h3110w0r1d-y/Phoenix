package com.h3110w0r1d.phoenix.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.h3110w0r1d.phoenix.R
import com.h3110w0r1d.phoenix.data.config.KeepAliveConfig
import com.h3110w0r1d.phoenix.data.config.LocalGlobalAppConfig
import com.h3110w0r1d.phoenix.data.config.ModuleConfig
import com.h3110w0r1d.phoenix.model.AppViewModel
import com.h3110w0r1d.phoenix.model.LocalGlobalViewModel
import com.h3110w0r1d.phoenix.ui.components.LargeFlexibleTopAppBar
import com.h3110w0r1d.phoenix.ui.components.LazyAppIcon
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
                                        viewModel.enableAllLoadedAppsExcludeSystem()
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
                            Column {
                                Text(
                                    packageName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
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
                                    onClick = {
                                        isExpanded = !isExpanded
                                    },
                                ),
                    )
                    AnimatedVisibility(visible = isExpanded) {
                        ExpandCard(
                            viewModel = viewModel,
                            packageName = packageName,
                            keepAliveConfig = keepAliveConfig,
                            moduleConfig = moduleConfig,
                            isPersistent = apps[i].isPersistent,
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
}

@Composable
fun ExpandCard(
    viewModel: AppViewModel,
    packageName: String,
    keepAliveConfig: KeepAliveConfig?,
    moduleConfig: ModuleConfig,
    isPersistent: Boolean,
) {
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
                val maxAdjHelpScroll = rememberScrollState()

                Text(
                    text =
                        if (currentMaxAdj != null) {
                            stringResource(
                                R.string.current_max_adj,
                                currentMaxAdj,
                            )
                        } else {
                            stringResource(
                                R.string.current_max_adj_default,
                                moduleConfig.globalMaxAdj,
                            )
                        },
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                    AlertDialog(
                        onDismissRequest = { showMaxAdjHelp = false },
                        title = {
                            Text(text = stringResource(R.string.adj_help_dialog_title))
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.adj_help_description),
                                modifier =
                                    Modifier
                                        .heightIn(max = 320.dp)
                                        .verticalScroll(maxAdjHelpScroll),
                                style = typography.bodyMedium,
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showMaxAdjHelp = false }) {
                                Text(stringResource(R.string.confirm))
                            }
                        },
                    )
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
                            viewModel.updateAppPersistent(packageName, checked)
                        },
                    )
                },
                modifier =
                    Modifier.clickable(!isPersistent) {
                        viewModel.updateAppPersistent(
                            packageName,
                            !(keepAliveConfig?.persistent ?: false),
                        )
                    },
            )
        }
    }
}
