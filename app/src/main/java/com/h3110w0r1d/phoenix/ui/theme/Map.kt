package com.h3110w0r1d.phoenix.ui.theme

import androidx.compose.ui.graphics.Color

fun getPrimaryColorMap(
    darkTheme: Boolean,
    colorScheme: String,
): Color =
    if (darkTheme) {
        when (colorScheme) {
            "amber" -> DarkAmberTheme.primary
            "blue_grey" -> DarkBlueGreyTheme.primary
            "blue" -> DarkBlueTheme.primary
            "brown" -> DarkBrownTheme.primary
            "cyan" -> DarkCyanTheme.primary
            "deep_orange" -> DarkDeepOrangeTheme.primary
            "deep_purple" -> DarkDeepPurpleTheme.primary
            "green" -> DarkGreenTheme.primary
            "indigo" -> DarkIndigoTheme.primary
            "light_blue" -> DarkLightBlueTheme.primary
            "light_green" -> DarkLightGreenTheme.primary
            "lime" -> DarkLimeTheme.primary
            "orange" -> DarkOrangeTheme.primary
            "pink" -> DarkPinkTheme.primary
            "purple" -> DarkPurpleTheme.primary
            "red" -> DarkRedTheme.primary
            "teal" -> DarkTealTheme.primary
            "yellow" -> DarkYellowTheme.primary
            "sakura" -> DarkSakuraTheme.primary
            else -> DarkBlueTheme.primary
        }
    } else {
        when (colorScheme) {
            "amber" -> LightAmberTheme.primary
            "blue_grey" -> LightBlueGreyTheme.primary
            "blue" -> LightBlueTheme.primary
            "brown" -> LightBrownTheme.primary
            "cyan" -> LightCyanTheme.primary
            "deep_orange" -> LightDeepOrangeTheme.primary
            "deep_purple" -> LightDeepPurpleTheme.primary
            "green" -> LightGreenTheme.primary
            "indigo" -> LightIndigoTheme.primary
            "light_blue" -> LightLightBlueTheme.primary
            "light_green" -> LightLightGreenTheme.primary
            "lime" -> LightLimeTheme.primary
            "orange" -> LightOrangeTheme.primary
            "pink" -> LightPinkTheme.primary
            "purple" -> LightPurpleTheme.primary
            "red" -> LightRedTheme.primary
            "teal" -> LightTealTheme.primary
            "yellow" -> LightYellowTheme.primary
            "sakura" -> LightSakuraTheme.primary
            else -> LightBlueTheme.primary
        }
    }
