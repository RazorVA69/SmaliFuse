package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val id: String, val title: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    AMOLED("amoled", "AMOLED Black"),
    SYSTEM("system", "Follow System")
}

enum class AppAccentColor(
    val id: String,
    val title: String,
    val primary: Color,
    val primaryVariant: Color,
    val lightContainer: Color,
    val onLightContainer: Color
) {
    TEAL("teal", "Emerald", Color(0xFF0F766E), Color(0xFF14B8A6), Color(0xFFECFDF5), Color(0xFF047857)),
    BLUE("blue", "Ocean Blue", Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFF0F9FF), Color(0xFF0369A1)),
    CORAL("coral", "Sunset Coral", Color(0xFFEA580C), Color(0xFFFB923C), Color(0xFFFFF7ED), Color(0xFFC2410C)),
    PURPLE("purple", "Amethyst", Color(0xFF7C3AED), Color(0xFFA78BFA), Color(0xFFFAF5FF), Color(0xFF6D28D9)),
    ROSE("rose", "Rose Pink", Color(0xFFE11D48), Color(0xFFFB7185), Color(0xFFFFF1F2), Color(0xFFBE123C)),
    MINT("mint", "Forest Mint", Color(0xFF059669), Color(0xFF34D399), Color(0xFFECFDF5), Color(0xFF065F46)),
    CRIMSON("crimson", "Ruby Red", Color(0xFFDC2626), Color(0xFFF87171), Color(0xFFFEF2F2), Color(0xFF991B1B)),
    AMBER("amber", "Amber Gold", Color(0xFFD97706), Color(0xFFFBBF24), Color(0xFFFFFBEB), Color(0xFFB45309)),
    INDIGO("indigo", "Electric Indigo", Color(0xFF4F46E5), Color(0xFF818CF8), Color(0xFFEEF2FF), Color(0xFF3730A3)),
    CYAN("cyan", "Midnight Cyan", Color(0xFF0891B2), Color(0xFF22D3EE), Color(0xFFECFEFF), Color(0xFF0E7490))
}

@Composable
fun MyApplicationTheme(
    fontTheme: AppFontTheme = AppFontTheme.SYSTEM,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: AppAccentColor = AppAccentColor.TEAL,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
        AppThemeMode.SYSTEM -> isSystemDark
    }
    val isAmoled = themeMode == AppThemeMode.AMOLED

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor.primaryVariant,
            secondary = accentColor.primary,
            tertiary = accentColor.primaryVariant,
            background = if (isAmoled) Color(0xFF000000) else Color(0xFF0F172A),
            surface = if (isAmoled) Color(0xFF121212) else Color(0xFF1E293B),
            surfaceVariant = if (isAmoled) Color(0xFF1E1E1E) else Color(0xFF334155),
            onPrimary = if (isAmoled) Color(0xFF000000) else Color(0xFF0F172A),
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFFF1F5F9),
            onSurface = Color(0xFFF1F5F9),
            outline = if (isAmoled) Color(0xFF2E2E2E) else Color(0xFF475569),
            outlineVariant = if (isAmoled) Color(0xFF1C1C1C) else Color(0xFF334155)
        )
    } else {
        lightColorScheme(
            primary = accentColor.primary,
            secondary = accentColor.primaryVariant,
            tertiary = accentColor.primaryVariant,
            background = SoftBg,
            surface = PureWhite,
            surfaceVariant = Slate50,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Slate900,
            onSurface = Slate900,
            onSurfaceVariant = Slate600,
            outline = Slate200,
            outlineVariant = Slate100
        )
    }

    val typography = createAppTypography(fontTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
