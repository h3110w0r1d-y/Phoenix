package com.h3110w0r1d.phoenix.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.h3110w0r1d.phoenix.R
import com.h3110w0r1d.phoenix.data.config.LocalGlobalAppConfig
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
    val enableCount = moduleConfig.appKeepAliveConfigs.filter { it.value.enabled }.size
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
                    title = { Text("仓库") },
//                    title = { Text(stringResource(R.string.app_list)) },
                    subtitle = { Text("所有模块均已最新") },
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
                    },
                )
            }
        },
    ) { innerPadding ->
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = isLoadingApps,
            state = pullRefreshState,
            indicator = {
                Indicator(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = innerPadding.calculateTopPadding()),
                    isRefreshing = isLoadingApps,
                    state = pullRefreshState,
                )
            },
            onRefresh = { viewModel.refreshApps() },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
            ) {
                items(apps.size) { i ->
                    val appName = apps[i].appName
                    val packageName = apps[i].packageName
                    val keepAliveConfigs = moduleConfig.appKeepAliveConfigs[apps[i].packageName]
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
//                                Text(text = "UID: ${apps[i].packageUid}")
                            }
                        },
                        trailingContent = {
                            Box(modifier = Modifier.padding(vertical = 10.dp)) {
                                Switch(
                                    checked = keepAliveConfigs?.enabled ?: false,
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
                        Text(
                            text = "item.content",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    DisposableEffect(packageName) {
                        onDispose {
                            isExpanded = false
                        }
                    }
                }
            }
        }
    }
}
