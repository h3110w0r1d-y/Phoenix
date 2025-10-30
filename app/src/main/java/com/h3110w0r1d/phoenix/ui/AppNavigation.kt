package com.h3110w0r1d.phoenix.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.h3110w0r1d.phoenix.R
import com.h3110w0r1d.phoenix.ui.screen.AppScreen
import com.h3110w0r1d.phoenix.ui.screen.HomeScreen
import com.h3110w0r1d.phoenix.ui.screen.SettingScreen

val Right2LeftEnterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
    )
}

val Left2RightEnterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth },
    )
}

val Right2LeftExitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
    )
}

val Left2RightExitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
    )
}

enum class Destination(
    val route: String,
    val label: String,
    val icon: @Composable () -> ImageVector,
    val selectedIcon: @Composable () -> ImageVector,
    val view: @Composable (() -> Unit),
    val enterTransition: (String) -> @JvmSuppressWildcards (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        Right2LeftEnterTransition
    },
    val exitTransition: (String) -> @JvmSuppressWildcards (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        Right2LeftExitTransition
    },
    val popEnterTransition: (String) -> @JvmSuppressWildcards (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        Left2RightEnterTransition
    },
    val popExitTransition: (String) -> @JvmSuppressWildcards (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        Left2RightExitTransition
    },
) {
    APP(
        "app",
        "App",
        { ImageVector.vectorResource(R.drawable.shield_locked_24px) },
        { ImageVector.vectorResource(R.drawable.shield_locked_fill_24px) },
        { AppScreen() },
        enterTransition = { fromRoute ->
            Left2RightEnterTransition
        },
        exitTransition = { toRoute ->
            Right2LeftExitTransition
        },
    ),
    HOME(
        "home",
        "Home",
        { Icons.Outlined.Home },
        { Icons.Default.Home },
        { HomeScreen() },
        enterTransition = { fromRoute ->
            when (fromRoute) {
                SETTING.route -> Left2RightEnterTransition
                else -> Right2LeftEnterTransition
            }
        },
        exitTransition = { toRoute ->
            when (toRoute) {
                APP.route -> Left2RightExitTransition
                else -> Right2LeftExitTransition
            }
        },
    ),
    SETTING(
        "setting",
        "Setting",
        { Icons.Outlined.Settings },
        { Icons.Default.Settings },
        { SettingScreen() },
        enterTransition = { fromRoute ->
            Right2LeftEnterTransition
        },
        exitTransition = { toRoute ->
            Left2RightExitTransition
        },
    ),
}

@Composable
fun BottomBar(onNavItemClick: (Destination) -> Unit) {
    val navController = LocalNavController.current!!
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    NavigationBar {
        Destination.entries.forEach { item ->
            val selected = item.route == navBackStackEntry?.destination?.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    onNavItemClick(item)
                },
                alwaysShowLabel = false,
                icon = {
                    Icon(
                        if (selected) item.selectedIcon() else item.icon(),
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val startDestination = Destination.HOME
    val currentBackStack by navController.currentBackStack.collectAsState()
    val currentDestination = navController.currentDestination
    var nextDestination by rememberSaveable { mutableStateOf(startDestination) }

    LaunchedEffect(nextDestination) {
        val currentRoute = currentDestination?.route
        val nextRoute = nextDestination.route

        if (currentRoute != nextRoute && currentRoute != null) {
            navController.navigate(route = nextRoute) {
                if (currentBackStack.size >= 2) {
                    popUpTo(currentBackStack[1].destination.id) {
                        inclusive = true
                    }
                }
                launchSingleTop = true
            }
        }
    }

    CompositionLocalProvider(LocalNavController provides navController) {
        Scaffold(
            bottomBar = {
                BottomBar({ destination ->
                    nextDestination = destination
                })
            },
        ) { contentPadding ->
            NavHost(
                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
                navController = navController,
                startDestination = startDestination.route,
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                    )
                },
            ) {
                Destination.entries.forEach { item ->
                    composable(
                        route = item.route,
                        content = { item.view() },
                        enterTransition = item.enterTransition(currentDestination?.route ?: ""),
                        exitTransition = item.exitTransition(nextDestination.route),
                        popEnterTransition = item.popEnterTransition(currentDestination?.route ?: ""),
                        popExitTransition = item.popExitTransition(nextDestination.route),
                    )
                }
            }
        }
    }
}

val LocalNavController = staticCompositionLocalOf<NavHostController?> { null }
