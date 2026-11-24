package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Mode Clean Studio Slate-Ice Canvas & Crisp Pure White Elevated Cards
val LightBackground = Color(0xFFEBF0F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E9F4)
val LightBorder = Color(0xFFCFD9EA)
val LightDivider = Color(0xFFE6ECF5)
val LightPrimary = Color(0xFF1D56F5)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFEEF3FF)
val LightOnPrimaryContainer = Color(0xFF0E2B7A)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF5A6B82)

// Deep Clean Fintech Navy Dark Mode Palette
val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF141B2B)
val DarkSurfaceElevated = Color(0xFF1B2438)
val DarkSurfaceVariant = Color(0xFF212B42)
val DarkBorder = Color(0xFF2E3A58)
val DarkDivider = Color(0xFF202A40)
val DarkPrimary = Color(0xFF3B74FF)
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF17274F)
val DarkOnPrimaryContainer = Color(0xFFDCE6FF)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)

// Signature Fintech Cobalt + Violet + Emerald Accents (Matching Reference Mockup)
val SakuCobalt = Color(0xFF1D56F5)
val SakuCobaltDeep = Color(0xFF1340CA)
val SakuCobaltBright = Color(0xFF3872FF)
val SakuCobaltContainerLight = Color(0xFFEEF3FF)
val SakuCobaltContainerDark = Color(0xFF17274F)

val SakuViolet = Color(0xFF7C5DFA)
val SakuVioletContainerLight = Color(0xFFF3EEFF)
val SakuVioletContainerDark = Color(0xFF261E47)

// Preserved signature constants mapped coherently for legacy/preset support
val ElectricLime = SakuCobalt
val ElectricLimeSoft = SakuCobaltBright
val ElectricLimeDarkText = Color(0xFFFFFFFF)
val ElectricLimeContainerLight = SakuCobaltContainerLight
val ElectricLimeContainerDark = SakuCobaltContainerDark

val SoftPeriwinkle = SakuViolet
val SoftPeriwinkleLight = Color(0xFFA794FF)
val SoftPeriwinkleContainerLight = SakuVioletContainerLight
val SoftPeriwinkleContainerDark = SakuVioletContainerDark

// Canvas header tokens cleanly adapted for Light Ice / Deep Navy
val MatteObsidianTop = LightBackground
val MatteObsidianSurface = LightSurface
val MatteObsidianBorder = LightBorder

// Brand Semantic Colors
val IncomeEmerald = Color(0xFF10B981)
val SakuEmeraldDeep = Color(0xFF059669)
val IncomeEmeraldDarkText = Color(0xFF065F46)
val IncomeEmeraldLightText = Color(0xFFA7F3D0)
val IncomeEmeraldContainerLight = Color(0xFFECFDF5)
val IncomeEmeraldContainerDark = Color(0xFF0E2F26)

val ExpenseCoral = Color(0xFFEF4444)
val ExpenseCoralDarkText = Color(0xFF991B1B)
val ExpenseCoralLightText = Color(0xFFFECACA)
val ExpenseCoralContainerLight = Color(0xFFFEF2F2)
val ExpenseCoralContainerDark = Color(0xFF3B141A)

val WarningAmber = Color(0xFFF59E0B)
val WarningAmberContainerLight = Color(0xFFFEF3C7)
val WarningAmberContainerDark = Color(0xFF3A280A)

val AiAccent = SakuViolet
val AiAccentLight = SakuVioletContainerLight
val AiAccentDark = SakuVioletContainerDark
val AiTeal = Color(0xFF14B8A6)

// Legacy compatibility tokens for hero/cards
val FreshAzure = SakuCobalt
val FreshAzureContainerLight = SakuCobaltContainerLight
val FreshAzureContainerDark = SakuCobaltContainerDark
val FreshHeroStartLight = Color(0xFF2A66FF)
val FreshHeroMidLight = SakuCobalt
val FreshHeroEndLight = SakuCobaltDeep
val FreshHeroStartDark = Color(0xFF2559E8)
val FreshHeroMidDark = Color(0xFF1944BF)
val FreshHeroEndDark = Color(0xFF113194)

val ObsidianCardStart = Color(0xFF2A66FF)
val ObsidianCardMid = SakuCobalt
val ObsidianCardEnd = SakuCobaltDeep
val ObsidianCardBorder = Color(0x33FFFFFF)

// Chart & Category Palette
val ChartPalette = listOf(
    Color(0xFF1D56F5), // Cobalt Blue
    Color(0xFF7C5DFA), // Electric Violet
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber Gold
    Color(0xFFEF4444), // Coral Red
    Color(0xFF06B6D4), // Cyan
    Color(0xFFEC4899), // Pink
    Color(0xFF64748B)  // Slate
)
