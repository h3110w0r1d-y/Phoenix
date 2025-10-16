package com.h3110w0r1d.phoenix.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.Typography
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.h3110w0r1d.phoenix.R
import com.h3110w0r1d.phoenix.model.LocalGlobalViewModel
import com.h3110w0r1d.phoenix.ui.components.LargeFlexibleTopAppBar
import com.h3110w0r1d.phoenix.utils.XposedUtil.getModuleVersion
import com.h3110w0r1d.phoenix.utils.XposedUtil.isModuleEnabled

fun getAppIconBitmap(context: Context): Bitmap? =
    ResourcesCompat
        .getDrawable(
            context.resources,
            R.mipmap.ic_launcher,
            context.theme,
        )?.let { drawable ->
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight).apply {
                val canvas = Canvas(this)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }

fun withoutFontPadding(): TextStyle =
    Typography(
        titleSmall = TextStyle(),
    ).titleSmall.copy(
        platformStyle =
            PlatformTextStyle(
                includeFontPadding = false,
            ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val showAboutDialog = remember { mutableStateOf(false) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
        )
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    getAppIconBitmap(context)?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .height(32.dp)
                                    .width(48.dp),
                        )
                    }
                },
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showAboutDialog.value = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.about),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ModuleCard()
            StatusCard()
        }

        if (showAboutDialog.value) {
            InfoDialog(onDismiss = { showAboutDialog.value = false })
        }
    }
}

@Composable
fun ModuleCard() {
    val viewModel = LocalGlobalViewModel.current
    val isModuleActive = viewModel.isModuleActive()

    var moduleStatus = stringResource(R.string.module_inactivated)
    var cardBackground = MaterialTheme.colorScheme.error
    var textColor = MaterialTheme.colorScheme.onError
    var iconVector = Icons.Filled.AddCircle
    var deg = 45f
    if (isModuleEnabled()) {
        moduleStatus = stringResource(R.string.module_activated)
        cardBackground = MaterialTheme.colorScheme.primary
        textColor = MaterialTheme.colorScheme.onPrimary
        iconVector = Icons.Filled.CheckCircle
        deg = 0f
    }

    Card(
        colors = cardColors(containerColor = cardBackground),
        modifier = Modifier.fillMaxWidth(),
        onClick = { viewModel.openLSPosedManager() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(26.dp, 32.dp)
                        .size(24.dp)
                        .rotate(deg),
                tint = textColor,
            )
            Column {
                Text(
                    text = moduleStatus,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    // 					style = withoutFontPadding(),
                )
                if (isModuleEnabled()) {
                    Text(
                        text =
                            if (isModuleActive) {
                                "Xposed API Version: " + getModuleVersion()
                            } else {
                                stringResource(R.string.please_restart_system)
                            },
                        fontSize = 12.sp,
                        color = textColor,
                        style = withoutFontPadding(),
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard() {
    val viewModel = LocalGlobalViewModel.current
    val appConfig by viewModel.appConfig.collectAsState()
    val moduleConfig = appConfig.moduleConfig

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            viewModel.toggleModule()
        },
        colors =
            cardColors(
                containerColor =
                    if (moduleConfig.moduleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector =
                    if (moduleConfig.moduleEnabled) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.PlayArrow
                    },
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(26.dp, 32.dp)
                        .size(24.dp),
                tint =
                    if (moduleConfig.moduleEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text =
                        if (moduleConfig.moduleEnabled) {
                            stringResource(R.string.enabled)
                        } else {
                            stringResource(R.string.disabled)
                        },
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color =
                        if (moduleConfig.moduleEnabled) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
    }
}

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(24.dp)
                        .width(280.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                fun packageVersion(): String {
                    val manager = context.packageManager
                    var version = "1.0"
                    try {
                        val info = manager.getPackageInfo(context.packageName, 0)
                        version = info.versionName ?: "1.0"
                    } catch (_: PackageManager.NameNotFoundException) {
                    }
                    return version
                }
                // 应用 Logo
                getAppIconBitmap(context)?.let {
                    Image(bitmap = it.asImageBitmap(), contentDescription = null)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 应用名称
                Text(
                    text = stringResource(R.string.app_name), // 替换你的应用名称资源
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))
                // 版本信息
                Text(
                    text = "Version ${packageVersion()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 开发者信息
                Text(
                    text = "Developed by",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "@h3110w0r1d-y",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .clickable {
                                    val intent =
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            "https://github.com/h3110w0r1d-y".toUri(),
                                        )
                                    context.startActivity(intent)
                                }.padding(4.dp),
                    )
                }
            }
        }
    }
}
