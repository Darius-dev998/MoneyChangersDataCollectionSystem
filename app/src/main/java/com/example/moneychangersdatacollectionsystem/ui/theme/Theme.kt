package com.example.moneychangersdatacollectionsystem.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * MoneyChangersDataCollectionSystemTheme
 *
 * FIX: window.statusBarColor removed.
 *
 * `window.statusBarColor` is deprecated at API 35 (targetSdk = 35) and has
 * no effect on Android 15+ which enforces edge-to-edge by default.
 * `enableEdgeToEdge()` in MainActivity already handles status bar appearance
 * correctly via `WindowCompat.setDecorFitsSystemWindows(false)`.
 * Duplicating the status bar color here via the deprecated API causes
 * a deprecation warning and is redundant.
 *
 * The `insetsController.isAppearanceLightStatusBars` call is retained —
 * this is the correct modern API for controlling status bar icon tint.
 */
private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = GoldPrimary,
    tertiary = GoldDark,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = GoldPrimary,
    tertiary = GoldDark,
    background = Background,
    surface = Surface,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface
)

@Composable
fun MoneyChangersDataCollectionSystemTheme(
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // FIX: window.statusBarColor removed — deprecated at API 35.
            // Status bar appearance is controlled by WindowInsetsController only.
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}