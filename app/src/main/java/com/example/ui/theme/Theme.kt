package com.example.ui.theme

import android.app.Activity
import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
    LIGHT("Terang"),
    DARK("Gelap"),
    SYSTEM("Sistem")
}

enum class AccentPalettePreset(
    val label: String,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val containerLight: Color,
    val containerDark: Color,
    val secondaryAccent: Color,
    val secondaryContainerLight: Color,
    val secondaryContainerDark: Color
) {
    COBALT_BLUE(
        label = "Cobalt",
        primaryAccent = Color(0xFF1D56F5),
        onPrimaryAccent = Color(0xFFFFFFFF),
        containerLight = Color(0xFFEEF3FF),
        containerDark = Color(0xFF17274F),
        secondaryAccent = Color(0xFF7C5DFA),
        secondaryContainerLight = Color(0xFFF3EEFF),
        secondaryContainerDark = Color(0xFF261E47)
    ),
    ELECTRIC_LIME(
        label = "Lime",
        primaryAccent = Color(0xFF84CC16),
        onPrimaryAccent = Color(0xFF13161A),
        containerLight = Color(0xFFECFCCB),
        containerDark = Color(0xFF243015),
        secondaryAccent = Color(0xFF6366F1),
        secondaryContainerLight = Color(0xFFEEF0FF),
        secondaryContainerDark = Color(0xFF222640)
    ),
    OCEAN_MINT(
        label = "Mint",
        primaryAccent = Color(0xFF10B981),
        onPrimaryAccent = Color(0xFFFFFFFF),
        containerLight = Color(0xFFD1FAE5),
        containerDark = Color(0xFF113327),
        secondaryAccent = Color(0xFF0EA5E9),
        secondaryContainerLight = Color(0xFFE0F2FE),
        secondaryContainerDark = Color(0xFF152C3D)
    ),
    COBALT_INDIGO(
        label = "Indigo",
        primaryAccent = Color(0xFF4F46E5),
        onPrimaryAccent = Color(0xFFFFFFFF),
        containerLight = Color(0xFFE0E7FF),
        containerDark = Color(0xFF1E2246),
        secondaryAccent = Color(0xFF8B5CF6),
        secondaryContainerLight = Color(0xFFF3E8FF),
        secondaryContainerDark = Color(0xFF2B2145)
    ),
    SUNSET_GOLD(
        label = "Amber",
        primaryAccent = Color(0xFFF59E0B),
        onPrimaryAccent = Color(0xFF1F1604),
        containerLight = Color(0xFFFEF3C7),
        containerDark = Color(0xFF3B2808),
        secondaryAccent = Color(0xFFEF4444),
        secondaryContainerLight = Color(0xFFFFE4E6),
        secondaryContainerDark = Color(0xFF3D1621)
    ),
    SAKURA_CORAL(
        label = "Coral",
        primaryAccent = Color(0xFFF43F5E),
        onPrimaryAccent = Color(0xFFFFFFFF),
        containerLight = Color(0xFFFFE4E6),
        containerDark = Color(0xFF3E1420),
        secondaryAccent = Color(0xFFEC4899),
        secondaryContainerLight = Color(0xFFFDF2F8),
        secondaryContainerDark = Color(0xFF3A182D)
    )
}

enum class CardCornerStyle(
    val label: String,
    val cardRadius: Dp,
    val sheetRadius: Dp
) {
    COMPACT("Tegas", cardRadius = 14.dp, sheetRadius = 20.dp),
    ROUNDED("Standar", cardRadius = 22.dp, sheetRadius = 28.dp),
    SOFT("Lembut", cardRadius = 28.dp, sheetRadius = 32.dp)
}

data class SakuExtendedColors(
    val cardBorder: Color,
    val subtleDivider: Color,
    val elevatedCard: Color,
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val aiAccent: Color,
    val aiContainer: Color,
    val lime: Color,
    val limeContainer: Color,
    val onLime: Color,
    val onLimeContainer: Color,
    val periwinkle: Color,
    val periwinkleContainer: Color,
    val obsidianHeader: Color,
    val obsidianSurface: Color,
    val obsidianBorder: Color,
    val freshBlue: Color,
    val freshBlueContainer: Color,
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val cardRadius: Dp,
    val sheetRadius: Dp,
    val isDark: Boolean
)

val LocalSakuColors = staticCompositionLocalOf {
    SakuExtendedColors(
        cardBorder = LightBorder,
        subtleDivider = LightDivider,
        elevatedCard = LightSurfaceElevated,
        income = IncomeEmerald,
        incomeContainer = IncomeEmeraldContainerLight,
        onIncomeContainer = IncomeEmeraldDarkText,
        expense = ExpenseCoral,
        expenseContainer = ExpenseCoralContainerLight,
        onExpenseContainer = ExpenseCoralDarkText,
        warning = WarningAmber,
        warningContainer = WarningAmberContainerLight,
        aiAccent = SakuViolet,
        aiContainer = SakuVioletContainerLight,
        lime = SakuCobalt,
        limeContainer = SakuCobaltContainerLight,
        onLime = Color.White,
        onLimeContainer = SakuCobalt,
        periwinkle = SakuViolet,
        periwinkleContainer = SakuVioletContainerLight,
        obsidianHeader = LightBackground,
        obsidianSurface = LightSurface,
        obsidianBorder = LightBorder,
        freshBlue = SakuCobalt,
        freshBlueContainer = SakuCobaltContainerLight,
        heroStart = FreshHeroStartLight,
        heroMid = FreshHeroMidLight,
        heroEnd = FreshHeroEndLight,
        cardRadius = 22.dp,
        sheetRadius = 28.dp,
        isDark = false
    )
}

val MaterialTheme.sakuColors: SakuExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSakuColors.current

val LocalSakuTopPadding = staticCompositionLocalOf { 8.dp }
val LocalSakuBottomPadding = staticCompositionLocalOf { 28.dp }

val SakuLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = SakuViolet,
    onSecondary = Color.White,
    secondaryContainer = SakuVioletContainerLight,
    onSecondaryContainer = Color(0xFF2E1E72),
    tertiary = ExpenseCoral,
    onTertiary = Color.White,
    tertiaryContainer = ExpenseCoralContainerLight,
    onTertiaryContainer = ExpenseCoralDarkText,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = ExpenseCoral,
    onError = Color.White,
    errorContainer = ExpenseCoralContainerLight,
    onErrorContainer = ExpenseCoralDarkText
)

val SakuDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = SakuViolet,
    onSecondary = Color.White,
    secondaryContainer = SakuVioletContainerDark,
    onSecondaryContainer = Color(0xFFE5DEFF),
    tertiary = ExpenseCoral,
    onTertiary = Color.White,
    tertiaryContainer = ExpenseCoralContainerDark,
    onTertiaryContainer = ExpenseCoralLightText,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = ExpenseCoral,
    onError = Color.White,
    errorContainer = ExpenseCoralContainerDark,
    onErrorContainer = ExpenseCoralLightText
)

val SakuShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun rememberSystemDarkTheme(): Boolean {
    val configDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configDark, configuration.uiMode) {
        val uiNightMask = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val uiConfigDark = uiNightMask == Configuration.UI_MODE_NIGHT_YES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uiModeManager = context.getSystemService(UiModeManager::class.java)
            when (uiModeManager?.nightMode) {
                UiModeManager.MODE_NIGHT_NO -> false
                UiModeManager.MODE_NIGHT_YES -> true
                else -> uiConfigDark || configDark
            }
        } else {
            uiConfigDark
        }
    }
}

@Composable
fun SakuTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accentPreset: AccentPalettePreset = AccentPalettePreset.COBALT_BLUE,
    cardCornerStyle: CardCornerStyle = CardCornerStyle.ROUNDED,
    content: @Composable () -> Unit
) {
    val systemDark = rememberSystemDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) {
        SakuDarkColorScheme.copy(
            primary = if (accentPreset == AccentPalettePreset.COBALT_BLUE) DarkPrimary else accentPreset.primaryAccent,
            onPrimary = accentPreset.onPrimaryAccent,
            primaryContainer = accentPreset.containerDark,
            secondary = accentPreset.secondaryAccent,
            secondaryContainer = accentPreset.secondaryContainerDark
        )
    } else {
        SakuLightColorScheme.copy(
            primary = accentPreset.primaryAccent,
            onPrimary = accentPreset.onPrimaryAccent,
            primaryContainer = accentPreset.containerLight,
            secondary = accentPreset.secondaryAccent,
            secondaryContainer = accentPreset.secondaryContainerLight
        )
    }

    val shapes = remember(cardCornerStyle) {
        Shapes(
            small = RoundedCornerShape((cardCornerStyle.cardRadius.value * 0.55f).dp),
            medium = RoundedCornerShape((cardCornerStyle.cardRadius.value * 0.82f).dp),
            large = RoundedCornerShape(cardCornerStyle.cardRadius),
            extraLarge = RoundedCornerShape(cardCornerStyle.sheetRadius)
        )
    }

    val extendedColors = if (useDarkTheme) {
        SakuExtendedColors(
            cardBorder = DarkBorder,
            subtleDivider = DarkDivider,
            elevatedCard = DarkSurfaceElevated,
            income = Color(0xFF34D399),
            incomeContainer = IncomeEmeraldContainerDark,
            onIncomeContainer = IncomeEmeraldLightText,
            expense = Color(0xFFF87171),
            expenseContainer = ExpenseCoralContainerDark,
            onExpenseContainer = ExpenseCoralLightText,
            warning = WarningAmber,
            warningContainer = WarningAmberContainerDark,
            aiAccent = accentPreset.secondaryAccent,
            aiContainer = accentPreset.secondaryContainerDark,
            lime = if (accentPreset == AccentPalettePreset.COBALT_BLUE) DarkPrimary else accentPreset.primaryAccent,
            limeContainer = accentPreset.containerDark,
            onLime = accentPreset.onPrimaryAccent,
            onLimeContainer = if (accentPreset == AccentPalettePreset.COBALT_BLUE) DarkPrimary else accentPreset.primaryAccent,
            periwinkle = accentPreset.secondaryAccent,
            periwinkleContainer = accentPreset.secondaryContainerDark,
            obsidianHeader = DarkBackground,
            obsidianSurface = DarkSurface,
            obsidianBorder = DarkBorder,
            freshBlue = DarkPrimary,
            freshBlueContainer = DarkPrimaryContainer,
            heroStart = FreshHeroStartDark,
            heroMid = FreshHeroMidDark,
            heroEnd = FreshHeroEndDark,
            cardRadius = cardCornerStyle.cardRadius,
            sheetRadius = cardCornerStyle.sheetRadius,
            isDark = true
        )
    } else {
        SakuExtendedColors(
            cardBorder = LightBorder,
            subtleDivider = LightDivider,
            elevatedCard = LightSurfaceElevated,
            income = IncomeEmerald,
            incomeContainer = IncomeEmeraldContainerLight,
            onIncomeContainer = IncomeEmeraldDarkText,
            expense = ExpenseCoral,
            expenseContainer = ExpenseCoralContainerLight,
            onExpenseContainer = ExpenseCoralDarkText,
            warning = WarningAmber,
            warningContainer = WarningAmberContainerLight,
            aiAccent = accentPreset.secondaryAccent,
            aiContainer = accentPreset.secondaryContainerLight,
            lime = accentPreset.primaryAccent,
            limeContainer = accentPreset.containerLight,
            onLime = accentPreset.onPrimaryAccent,
            onLimeContainer = if (accentPreset.onPrimaryAccent == Color.White) accentPreset.primaryAccent else Color(0xFF1E293B),
            periwinkle = accentPreset.secondaryAccent,
            periwinkleContainer = accentPreset.secondaryContainerLight,
            obsidianHeader = LightBackground,
            obsidianSurface = LightSurface,
            obsidianBorder = LightBorder,
            freshBlue = SakuCobalt,
            freshBlueContainer = SakuCobaltContainerLight,
            heroStart = FreshHeroStartLight,
            heroMid = FreshHeroMidLight,
            heroEnd = FreshHeroEndLight,
            cardRadius = cardCornerStyle.cardRadius,
            sheetRadius = cardCornerStyle.sheetRadius,
            isDark = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(useDarkTheme) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(window, view)
            val lightAppearance = !useDarkTheme
            if (controller.isAppearanceLightStatusBars != lightAppearance) {
                controller.isAppearanceLightStatusBars = lightAppearance
            }
            if (controller.isAppearanceLightNavigationBars != lightAppearance) {
                controller.isAppearanceLightNavigationBars = lightAppearance
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                @Suppress("DEPRECATION")
                if (window.statusBarColor != android.graphics.Color.TRANSPARENT) {
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                }
                @Suppress("DEPRECATION")
                if (window.navigationBarColor != android.graphics.Color.TRANSPARENT) {
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                }
            }
        }
    }

    CompositionLocalProvider(LocalSakuColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DanaKuTypography,
            shapes = shapes,
            content = content
        )
    }
}
