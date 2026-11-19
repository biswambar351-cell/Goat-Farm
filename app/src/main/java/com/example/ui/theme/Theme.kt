package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenDarkPrimary,
    onPrimary = ForestGreenDarkOnPrimary,
    primaryContainer = ForestGreenDarkPrimaryContainer,
    onPrimaryContainer = ForestGreenDarkOnPrimaryContainer,
    secondary = GoldenAmberDarkSecondary,
    onSecondary = GoldenAmberDarkOnSecondary,
    secondaryContainer = GoldenAmberDarkSecondaryContainer,
    onSecondaryContainer = GoldenAmberDarkOnSecondaryContainer,
    background = FarmBackgroundDark,
    surface = FarmSurfaceDark,
    surfaceVariant = FarmSurfaceVariantDark,
    onBackground = FarmOnSurfaceDark,
    onSurface = FarmOnSurfaceDark,
    onSurfaceVariant = FarmOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = ForestGreenOnPrimary,
    primaryContainer = ForestGreenPrimaryContainer,
    onPrimaryContainer = ForestGreenOnPrimaryContainer,
    secondary = GoldenAmberSecondary,
    onSecondary = GoldenAmberOnSecondary,
    secondaryContainer = GoldenAmberSecondaryContainer,
    onSecondaryContainer = GoldenAmberOnSecondaryContainer,
    tertiary = SageTertiary,
    onTertiary = SageOnTertiary,
    tertiaryContainer = SageTertiaryContainer,
    onTertiaryContainer = SageOnTertiaryContainer,
    background = FarmBackgroundLight,
    surface = FarmSurfaceLight,
    surfaceVariant = FarmSurfaceVariantLight,
    onBackground = FarmOnSurfaceLight,
    onSurface = FarmOnSurfaceLight,
    onSurfaceVariant = FarmOnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
