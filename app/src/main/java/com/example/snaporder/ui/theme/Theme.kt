package com.example.snaporder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * SnapOrders Light Color Scheme
 * 
 * White-green theme for premium restaurant/POS app feel
 */
private val LightColorScheme = lightColorScheme(
    primary = SnapOrdersColors.Primary,
    onPrimary = SnapOrdersColors.OnPrimary,
    background = SnapOrdersColors.Background,
    surface = SnapOrdersColors.Surface,
    onBackground = SnapOrdersColors.TextPrimary,
    onSurface = SnapOrdersColors.TextPrimary,
    outline = SnapOrdersColors.Outline,
    error = SnapOrdersColors.Error,
    onError = SnapOrdersColors.OnPrimary,
    surfaceVariant = SnapOrdersColors.Surface,
    onSurfaceVariant = SnapOrdersColors.TextSecondary
)

/**
 * SnapOrders Theme
 * 
 * Material 3 theme with custom white-green color palette
 * Light theme only for this app
 */
@Composable
fun SnapOrderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    // Force light theme for consistent restaurant/POS app experience
    val colorScheme = LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SnapOrdersTypography,
        shapes = SnapOrdersShapes,
        content = content
    )
}
