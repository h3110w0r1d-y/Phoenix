package com.h3110w0r1d.phoenix.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun PhoenixTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = true,
    customColorScheme: String = "blue",
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        if (!dynamicColor) {
            if (darkTheme) {
                if (highContrast) {
                    when (customColorScheme) {
                        "amber" -> HighContrastDarkAmberTheme
                        "blue_grey" -> HighContrastDarkBlueGreyTheme
                        "blue" -> HighContrastDarkBlueTheme
                        "brown" -> HighContrastDarkBrownTheme
                        "cyan" -> HighContrastDarkCyanTheme
                        "deep_orange" -> HighContrastDarkDeepOrangeTheme
                        "deep_purple" -> HighContrastDarkDeepPurpleTheme
                        "green" -> HighContrastDarkGreenTheme
                        "indigo" -> HighContrastDarkIndigoTheme
                        "light_blue" -> HighContrastDarkLightBlueTheme
                        "light_green" -> HighContrastDarkLightGreenTheme
                        "lime" -> HighContrastDarkLimeTheme
                        "orange" -> HighContrastDarkOrangeTheme
                        "pink" -> HighContrastDarkPinkTheme
                        "purple" -> HighContrastDarkPurpleTheme
                        "red" -> HighContrastDarkRedTheme
                        "teal" -> HighContrastDarkTealTheme
                        "yellow" -> HighContrastDarkYellowTheme
                        else -> HighContrastDarkBlueTheme
                    }
                } else {
                    when (customColorScheme) {
                        "amber" -> DarkAmberTheme
                        "blue_grey" -> DarkBlueGreyTheme
                        "blue" -> DarkBlueTheme
                        "brown" -> DarkBrownTheme
                        "cyan" -> DarkCyanTheme
                        "deep_orange" -> DarkDeepOrangeTheme
                        "deep_purple" -> DarkDeepPurpleTheme
                        "green" -> DarkGreenTheme
                        "indigo" -> DarkIndigoTheme
                        "light_blue" -> DarkLightBlueTheme
                        "light_green" -> DarkLightGreenTheme
                        "lime" -> DarkLimeTheme
                        "orange" -> DarkOrangeTheme
                        "pink" -> DarkPinkTheme
                        "purple" -> DarkPurpleTheme
                        "red" -> DarkRedTheme
                        "teal" -> DarkTealTheme
                        "yellow" -> DarkYellowTheme
                        else -> DarkBlueTheme
                    }
                }
            } else {
                if (highContrast) {
                    when (customColorScheme) {
                        "amber" -> HighContrastLightAmberTheme
                        "blue_grey" -> HighContrastLightBlueGreyTheme
                        "blue" -> HighContrastLightBlueTheme
                        "brown" -> HighContrastLightBrownTheme
                        "cyan" -> HighContrastLightCyanTheme
                        "deep_orange" -> HighContrastLightDeepOrangeTheme
                        "deep_purple" -> HighContrastLightDeepPurpleTheme
                        "green" -> HighContrastLightGreenTheme
                        "indigo" -> HighContrastLightIndigoTheme
                        "light_blue" -> HighContrastLightLightBlueTheme
                        "light_green" -> HighContrastLightLightGreenTheme
                        "lime" -> HighContrastLightLimeTheme
                        "orange" -> HighContrastLightOrangeTheme
                        "pink" -> HighContrastLightPinkTheme
                        "purple" -> HighContrastLightPurpleTheme
                        "red" -> HighContrastLightRedTheme
                        "teal" -> HighContrastLightTealTheme
                        "yellow" -> HighContrastLightYellowTheme
                        else -> HighContrastLightBlueTheme
                    }
                } else {
                    when (customColorScheme) {
                        "amber" -> LightAmberTheme
                        "blue_grey" -> LightBlueGreyTheme
                        "blue" -> LightBlueTheme
                        "brown" -> LightBrownTheme
                        "cyan" -> LightCyanTheme
                        "deep_orange" -> LightDeepOrangeTheme
                        "deep_purple" -> LightDeepPurpleTheme
                        "green" -> LightGreenTheme
                        "indigo" -> LightIndigoTheme
                        "light_blue" -> LightLightBlueTheme
                        "light_green" -> LightLightGreenTheme
                        "lime" -> LightLimeTheme
                        "orange" -> LightOrangeTheme
                        "pink" -> LightPinkTheme
                        "purple" -> LightPurpleTheme
                        "red" -> LightRedTheme
                        "teal" -> LightTealTheme
                        "yellow" -> LightYellowTheme
                        else -> LightBlueTheme
                    }
                }
            }
        } else {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }

                darkTheme -> DarkBlueTheme
                else -> LightBlueTheme
            }
        }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
