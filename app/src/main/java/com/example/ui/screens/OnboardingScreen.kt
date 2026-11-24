package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.data.remote.CloudSyncStatus
import com.example.ui.components.OnboardingAuthHeroStage
import com.example.ui.components.OnboardingDiamondNextButton
import com.example.ui.components.OnboardingIsometricSlide1
import com.example.ui.components.OnboardingIsometricSlide2
import com.example.ui.components.OnboardingIsometricSlide3
import com.example.ui.components.OnboardingPalette
import com.example.ui.components.OnboardingPillDotsIndicator
import com.example.ui.components.SakuBrandMark
import com.example.ui.theme.DmSansFontFamily
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.OutfitFontFamily
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class OnboardingFlowPhase(val order: Int) {
    SLIDES(0),
    AUTH(1),
    SETUP_STEP_1(2),
    SETUP_STEP_2(3),
    SETUP_STEP_3(4)
}

private data class WalletPreset(
    val name: String,
    val type: String
)

private val quickWalletPresets = listOf(
    WalletPreset("BCA", "Bank"),
    WalletPreset("Mandiri", "Bank"),
    WalletPreset("BRI", "Bank"),
    WalletPreset("GoPay", "E-Wallet"),
    WalletPreset("OVO", "E-Wallet"),
    WalletPreset("Tunai", "Cash")
)

private val quickBalancePresets = listOf(
    "Rp 0" to "0",
    "Rp 500rb" to "500000",
    "Rp 1jt" to "1000000",
    "Rp 5jt" to "5000000"
)

private val onboardingWalletColors = listOf(
    0xFF0D24BDL, // Cobalt
    0xFFF8A32BL, // Warm Amber
    0xFF18B894L, // Mint
    0xFFFF7A45L, // Coral
    0xFF14161AL  // Obsidian
)

private data class OnboardingStorySlide(
    val title: String,
    val subtitle: String,
    val textOnTop: Boolean,
    val backgroundColor: Color,
    val titleColor: Color,
    val subtitleColor: Color
)

private val storySlides = listOf(
    OnboardingStorySlide(
        title = "Dompet &\nAnggaran Rapi",
        subtitle = "Kelola rekening bank, e-wallet, tunai, hingga cek saldo e-money NFC dalam satu aplikasi.",
        textOnTop = false,
        backgroundColor = OnboardingPalette.CobaltBlue,
        titleColor = OnboardingPalette.PureWhite,
        subtitleColor = OnboardingPalette.PureWhite.copy(alpha = 0.88f)
    ),
    OnboardingStorySlide(
        title = "Asisten AI &\nAnalisis Cerdas",
        subtitle = "Catat lewat chat, suara, atau foto struk — Saku bantu rangkum pola pengeluaranmu.",
        textOnTop = true,
        backgroundColor = OnboardingPalette.PureWhite,
        titleColor = OnboardingPalette.CobaltBlue,
        subtitleColor = Color(0xFF334155)
    ),
    OnboardingStorySlide(
        title = "Kendali Penuh\nArus Kas",
        subtitle = "Pantau kalender harian, target tabungan, hingga hutang piutang tanpa ribet.",
        textOnTop = false,
        backgroundColor = OnboardingPalette.WarmAmber,
        titleColor = OnboardingPalette.PureWhite,
        subtitleColor = OnboardingPalette.PureWhite.copy(alpha = 0.94f)
    )
)

@Composable
fun OnboardingScreen(
    canCancel: Boolean,
    syncStatus: CloudSyncStatus = CloudSyncStatus(),
    onGoogleSignIn: () -> Unit = {},
    onCancel: () -> Unit,
    onCompleteClean: (
        displayName: String,
        billingCycleDay: Int,
        walletName: String,
        walletType: String,
        initialBalance: Long,
        accentHex: Long
    ) -> Unit,
    onCompleteWithSampleData: (displayName: String, billingCycleDay: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var flowPhase by remember { mutableStateOf(OnboardingFlowPhase.SLIDES) }
    val pagerState = rememberPagerState(pageCount = { storySlides.size })

    // Step-by-step setup state
    var displayName by remember { mutableStateOf("") }
    var hasEditedDisplayName by remember { mutableStateOf(false) }
    var cycleDay by remember { mutableIntStateOf(1) }
    var startCleanMode by remember { mutableStateOf(true) }
    var walletName by remember { mutableStateOf("Dompet Utama") }
    var walletType by remember { mutableStateOf("Bank") }
    var balanceText by remember { mutableStateOf("0") }
    var accentHex by remember { mutableLongStateOf(0xFF0D24BDL) }
    val parsedBalance = balanceText.filter { it.isDigit() }.toLongOrNull() ?: 0L

    // Auto-prefill nickname from Google account if user hasn't typed one manually
    LaunchedEffect(syncStatus.isSignedIn, syncStatus.displayName) {
        if (syncStatus.isSignedIn && !hasEditedDisplayName && displayName.isBlank()) {
            val suggested = syncStatus.displayName
                ?.trim()
                ?.substringBefore(" ")
                ?.take(24)
                .orEmpty()
            if (suggested.isNotBlank()) {
                displayName = suggested
            }
        }
    }

    // Dynamic status bar icon contrast per slide/phase
    val useDarkStatusIcons = when (flowPhase) {
        OnboardingFlowPhase.SLIDES -> pagerState.currentPage != 0
        else -> !extended.isDark
    }
    if (!view.isInEditMode) {
        LaunchedEffect(useDarkStatusIcons) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(window, view)
            if (controller.isAppearanceLightStatusBars != useDarkStatusIcons) {
                controller.isAppearanceLightStatusBars = useDarkStatusIcons
            }
        }
        val restoreLightStatusBars = !extended.isDark
        DisposableEffect(restoreLightStatusBars) {
            onDispose {
                val window = (view.context as? Activity)?.window ?: return@onDispose
                val controller = WindowCompat.getInsetsController(window, view)
                if (controller.isAppearanceLightStatusBars != restoreLightStatusBars) {
                    controller.isAppearanceLightStatusBars = restoreLightStatusBars
                }
            }
        }
    }

    BackHandler(
        enabled = flowPhase != OnboardingFlowPhase.SLIDES || pagerState.currentPage > 0 || canCancel
    ) {
        when (flowPhase) {
            OnboardingFlowPhase.SETUP_STEP_3 -> flowPhase = OnboardingFlowPhase.SETUP_STEP_2
            OnboardingFlowPhase.SETUP_STEP_2 -> flowPhase = OnboardingFlowPhase.SETUP_STEP_1
            OnboardingFlowPhase.SETUP_STEP_1 -> flowPhase = OnboardingFlowPhase.AUTH
            OnboardingFlowPhase.AUTH -> flowPhase = OnboardingFlowPhase.SLIDES
            OnboardingFlowPhase.SLIDES -> when {
                pagerState.currentPage > 0 -> scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
                canCancel -> onCancel()
            }
        }
    }

    val pagerPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
    val slideInterpolatedBg = remember(pagerPosition) {
        val t = pagerPosition.coerceIn(0f, 2f)
        when {
            t <= 1f -> lerp(storySlides[0].backgroundColor, storySlides[1].backgroundColor, t)
            else -> lerp(storySlides[1].backgroundColor, storySlides[2].backgroundColor, t - 1f)
        }
    }
    val setupSurfaceBg = if (extended.isDark) MaterialTheme.colorScheme.background else Color(0xFFFCFDFF)
    val rootBackground = if (flowPhase == OnboardingFlowPhase.SLIDES) slideInterpolatedBg else setupSurfaceBg

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(rootBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("onboarding_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = flowPhase,
            transitionSpec = {
                val forward = targetState.order >= initialState.order
                if (forward) {
                    (slideInHorizontally(tween(340)) { it / 2 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(260)) { -it / 3 } + fadeOut(tween(220)))
                } else {
                    (slideInHorizontally(tween(340)) { -it / 2 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(260)) { it / 3 } + fadeOut(tween(220)))
                }
            },
            label = "onboarding_phase_flow"
        ) { phase ->
            when (phase) {
                OnboardingFlowPhase.SLIDES -> {
                    val isFinalSlide = pagerState.currentPage == storySlides.lastIndex
                    val isWhiteSlide = pagerState.currentPage == 1

                    val skipColor by animateColorAsState(
                        targetValue = if (isWhiteSlide) OnboardingPalette.CobaltBlue else OnboardingPalette.PureWhite,
                        animationSpec = tween(220),
                        label = "skip_color"
                    )
                    val diamondBg by animateColorAsState(
                        targetValue = if (isWhiteSlide) OnboardingPalette.CobaltBlue else OnboardingPalette.PureWhite,
                        animationSpec = tween(240),
                        label = "diamond_bg_color"
                    )
                    val diamondIcon by animateColorAsState(
                        targetValue = if (isWhiteSlide) OnboardingPalette.PureWhite else OnboardingPalette.CobaltBlue,
                        animationSpec = tween(240),
                        label = "diamond_icon_color"
                    )
                    val indicatorActive = if (isWhiteSlide) OnboardingPalette.CobaltBlue else OnboardingPalette.PureWhite
                    val indicatorInactive = indicatorActive.copy(alpha = 0.36f)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 560.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SakuBrandMark(size = 34.dp, cornerRadius = 10.dp)
                                Text(
                                    text = "Saku",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    letterSpacing = (-0.3).sp,
                                    color = skipColor
                                )
                            }
                            if (canCancel) {
                                TextButton(onClick = onCancel) {
                                    Text(
                                        text = "Tutup",
                                        fontFamily = DmSansFontFamily,
                                        color = skipColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("onboarding_pager")
                        ) { page ->
                            val slide = storySlides[page]
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val textAlpha = (1f - abs(pageOffset) * 1.2f).coerceIn(0f, 1f)
                            val textShiftY = abs(pageOffset) * 26f

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 26.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                if (slide.textOnTop) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SlideEditorialCopy(
                                        title = slide.title,
                                        subtitle = slide.subtitle,
                                        titleColor = slide.titleColor,
                                        subtitleColor = slide.subtitleColor,
                                        modifier = Modifier.graphicsLayer {
                                            alpha = textAlpha
                                            translationY = textShiftY
                                        }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        OnboardingIsometricSlide2(parallaxOffset = pageOffset)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(top = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (page == 0) {
                                            OnboardingIsometricSlide1(parallaxOffset = pageOffset)
                                        } else {
                                            OnboardingIsometricSlide3(parallaxOffset = pageOffset)
                                        }
                                    }
                                    SlideEditorialCopy(
                                        title = slide.title,
                                        subtitle = slide.subtitle,
                                        titleColor = slide.titleColor,
                                        subtitleColor = slide.subtitleColor,
                                        modifier = Modifier
                                            .padding(bottom = 18.dp)
                                            .graphicsLayer {
                                                alpha = textAlpha
                                                translationY = textShiftY
                                            }
                                    )
                                }
                            }
                        }

                        // Reference bottom bar: Skip (left), 45-deg diamond CTA (center), pill+dots (right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 4.dp, bottom = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier.width(86.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        flowPhase = OnboardingFlowPhase.AUTH
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("onboarding_skip_btn")
                                ) {
                                    Text(
                                        text = "Lewati",
                                        fontFamily = DmSansFontFamily,
                                        color = skipColor,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            OnboardingDiamondNextButton(
                                isFinalSlide = isFinalSlide,
                                containerColor = diamondBg,
                                iconColor = diamondIcon,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (!isFinalSlide) {
                                        scope.launch {
                                            pagerState.animateScrollToPage(
                                                page = pagerState.currentPage + 1,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    } else {
                                        flowPhase = OnboardingFlowPhase.AUTH
                                    }
                                }
                            )

                            Box(
                                modifier = Modifier.width(86.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                OnboardingPillDotsIndicator(
                                    pageCount = storySlides.size,
                                    currentPage = pagerState.currentPage,
                                    activeColor = indicatorActive,
                                    inactiveColor = indicatorInactive
                                )
                            }
                        }
                    }
                }

                OnboardingFlowPhase.AUTH -> {
                    OnboardingAuthStage(
                        canCancel = canCancel,
                        syncStatus = syncStatus,
                        isDark = extended.isDark,
                        onBackToSlides = { flowPhase = OnboardingFlowPhase.SLIDES },
                        onCancel = onCancel,
                        onGoogleSignIn = onGoogleSignIn,
                        onContinueToSetup = { flowPhase = OnboardingFlowPhase.SETUP_STEP_1 }
                    )
                }

                OnboardingFlowPhase.SETUP_STEP_1 -> {
                    SetupWizardScaffold(
                        stepNumber = 1,
                        totalSteps = if (startCleanMode) 3 else 2,
                        stepBadge = "Langkah 1 • Profil & Siklus",
                        title = "Biar Saku\nlebih personal",
                        subtitle = "Nama panggilan dan tanggal gajian membantu Saku menghitung laporanmu lebih pas.",
                        canCancel = canCancel,
                        isDark = extended.isDark,
                        onBack = { flowPhase = OnboardingFlowPhase.AUTH },
                        onCancel = onCancel,
                        primaryCtaText = "Lanjut",
                        primaryCtaTestTag = "onboarding_step1_next_btn",
                        onPrimaryCta = { flowPhase = OnboardingFlowPhase.SETUP_STEP_2 }
                    ) {
                        SetupStep1ProfileContent(
                            displayName = displayName,
                            onDisplayNameChange = {
                                hasEditedDisplayName = true
                                displayName = it.take(24)
                            },
                            cycleDay = cycleDay,
                            onCycleDayChange = { cycleDay = it },
                            isDark = extended.isDark
                        )
                    }
                }

                OnboardingFlowPhase.SETUP_STEP_2 -> {
                    SetupWizardScaffold(
                        stepNumber = 2,
                        totalSteps = if (startCleanMode) 3 else 2,
                        stepBadge = "Langkah 2 • Ruang Kerja",
                        title = "Pilih cara\nmemulai",
                        subtitle = "Mulai dengan ruang kerja bersih atau langsung eksplorasi fitur dengan data contoh.",
                        canCancel = canCancel,
                        isDark = extended.isDark,
                        onBack = { flowPhase = OnboardingFlowPhase.SETUP_STEP_1 },
                        onCancel = onCancel,
                        primaryCtaText = if (startCleanMode) "Lanjut Buat Dompet" else "Muat Data Contoh & Masuk",
                        primaryCtaTestTag = if (startCleanMode) "onboarding_step2_next_btn" else "onboarding_finish_btn",
                        onPrimaryCta = {
                            if (startCleanMode) {
                                flowPhase = OnboardingFlowPhase.SETUP_STEP_3
                            } else {
                                onCompleteWithSampleData(displayName, cycleDay)
                            }
                        }
                    ) {
                        SetupStep2WorkspaceModeContent(
                            startCleanMode = startCleanMode,
                            onSelectCleanMode = { startCleanMode = it },
                            isDark = extended.isDark
                        )
                    }
                }

                OnboardingFlowPhase.SETUP_STEP_3 -> {
                    SetupWizardScaffold(
                        stepNumber = 3,
                        totalSteps = 3,
                        stepBadge = "Langkah 3 • Dompet Awal",
                        title = "Siapkan dompet\npertamamu",
                        subtitle = "Pilih cepat atau ketik rekening utamamu. Dompet lain bebas ditambah nanti.",
                        canCancel = canCancel,
                        isDark = extended.isDark,
                        onBack = { flowPhase = OnboardingFlowPhase.SETUP_STEP_2 },
                        onCancel = onCancel,
                        primaryCtaText = "Selesai & Masuk Saku",
                        primaryCtaTestTag = "onboarding_finish_btn",
                        primaryCtaEnabled = walletName.isNotBlank(),
                        onPrimaryCta = {
                            onCompleteClean(
                                displayName,
                                cycleDay,
                                walletName,
                                walletType,
                                parsedBalance,
                                accentHex
                            )
                        }
                    ) {
                        SetupStep3FirstWalletContent(
                            walletName = walletName,
                            onWalletNameChange = { walletName = it.take(30) },
                            walletType = walletType,
                            onWalletTypeChange = { walletType = it },
                            balanceText = balanceText,
                            onBalanceTextChange = { balanceText = it.filter { ch -> ch.isDigit() }.take(14) },
                            parsedBalance = parsedBalance,
                            accentHex = accentHex,
                            onAccentHexChange = { accentHex = it },
                            isDark = extended.isDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlideEditorialCopy(
    title: String,
    subtitle: String,
    titleColor: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 35.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.8).sp,
            color = titleColor
        )
        Text(
            text = subtitle,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = subtitleColor,
            modifier = Modifier.widthIn(max = 330.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingAuthStage(
    canCancel: Boolean,
    syncStatus: CloudSyncStatus,
    isDark: Boolean,
    onBackToSlides: () -> Unit,
    onCancel: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onContinueToSetup: () -> Unit
) {
    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current
    val headlineColor = if (isDark) Color(0xFFF8FAFC) else OnboardingPalette.Obsidian
    val subtitleColor = if (isDark) Color(0xFFA8B3C5) else OnboardingPalette.SlateInk
    val primaryCobalt = if (isDark) OnboardingPalette.CobaltBright else OnboardingPalette.CobaltBlue
    val cardSurface = if (isDark) MaterialTheme.colorScheme.surface else OnboardingPalette.PureWhite

    var isLaunchingGoogle by remember { mutableStateOf(false) }
    LaunchedEffect(isLaunchingGoogle, syncStatus.isSignedIn, syncStatus.errorMessage) {
        if (syncStatus.isSignedIn || syncStatus.errorMessage != null) {
            isLaunchingGoogle = false
        } else if (isLaunchingGoogle) {
            delay(4500)
            isLaunchingGoogle = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 560.dp)
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToSlides,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Kembali ke slide",
                    tint = headlineColor
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = primaryCobalt.copy(alpha = 0.11f),
                border = BorderStroke(0.8.dp, primaryCobalt.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    SakuBrandMark(size = 20.dp, cornerRadius = 6.dp)
                    Text(
                        text = "Saku • Pilihan Simpan",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryCobalt
                    )
                }
            }

            if (canCancel) {
                TextButton(onClick = onCancel) {
                    Text(
                        text = "Tutup",
                        fontFamily = DmSansFontFamily,
                        color = subtitleColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Architectural Cloud-to-Device Illustration
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OnboardingAuthHeroStage(
                    isSignedIn = syncStatus.isSignedIn,
                    isDark = isDark
                )
            }

            // Editorial Title + Copy
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (syncStatus.isSignedIn) {
                        "Akun Google\nsudah terhubung"
                    } else {
                        "Pilih cara simpan\ndatamu"
                    },
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 31.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.7).sp,
                    color = headlineColor
                )
                Text(
                    text = if (syncStatus.isSignedIn) {
                        "Semua catatan keuanganmu siap dicadangkan otomatis ke cloud. Yuk lanjut ke setup 2 menit."
                    } else {
                        "Masuk dengan Google untuk backup cloud otomatis, atau lanjut tanpa login agar data 100% tersimpan lokal di HP."
                    },
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                    color = subtitleColor
                )
            }

            if (syncStatus.isSignedIn) {
                val userName = syncStatus.displayName?.takeIf { it.isNotBlank() } ?: "Pengguna Google"
                val initialChar = userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = OnboardingPalette.MintAccent.copy(alpha = if (isDark) 0.16f else 0.09f),
                    border = BorderStroke(1.3.dp, OnboardingPalette.MintAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = OnboardingPalette.MintAccent,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initialChar,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = OnboardingPalette.PureWhite
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = userName,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = headlineColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!syncStatus.email.isNullOrBlank()) {
                                Text(
                                    text = syncStatus.email,
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 13.sp,
                                    color = subtitleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    tint = if (isDark) OnboardingPalette.MintBright else OnboardingPalette.MintDeep,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Cloud Sync Aktif",
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) OnboardingPalette.MintBright else OnboardingPalette.MintDeep
                                )
                            }
                        }
                    }
                }
            } else {
                // Primary Option Card: Google Cloud Sync
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardSurface,
                    shadowElevation = if (isDark) 0.dp else 3.dp,
                    border = BorderStroke(1.2.dp, primaryCobalt.copy(alpha = 0.24f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoogleBadgeIcon(modifier = Modifier.size(34.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sinkronisasi Cloud",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = headlineColor
                                    )
                                    Text(
                                        text = "Cadangkan data aman di akun Google",
                                        fontFamily = DmSansFontFamily,
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp,
                                        color = subtitleColor
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = primaryCobalt.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Disarankan",
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryCobalt,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniFeaturePill(
                                icon = Icons.Rounded.CloudDone,
                                label = "Auto Backup",
                                tint = primaryCobalt,
                                modifier = Modifier.weight(1f)
                            )
                            MiniFeaturePill(
                                icon = Icons.Rounded.Devices,
                                label = "Lanjut Ganti HP",
                                tint = primaryCobalt,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isLaunchingGoogle = true
                                onGoogleSignIn()
                            },
                            enabled = !isLaunchingGoogle,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OnboardingPalette.CobaltBlue,
                                contentColor = OnboardingPalette.PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("onboarding_google_signin_btn")
                        ) {
                            if (isLaunchingGoogle) {
                                CircularProgressIndicator(
                                    color = OnboardingPalette.PureWhite,
                                    strokeWidth = 2.2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Membuka Google...",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            } else {
                                GoogleBadgeIcon(modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Masuk dengan Google",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Clean Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = extended.cardBorder
                    )
                    Text(
                        text = "atau pakai secara lokal",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtitleColor
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = extended.cardBorder
                    )
                }

                // Secondary Option Card: Continue Without Login (Local Mode)
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onContinueToSetup()
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = cardSurface,
                    border = BorderStroke(1.2.dp, OnboardingPalette.WarmAmber.copy(alpha = 0.72f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_local_mode_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = OnboardingPalette.WarmAmber.copy(alpha = if (isDark) 0.22f else 0.16f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (isDark) OnboardingPalette.WarmAmber else OnboardingPalette.WarmAmberDeep,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Lanjut Tanpa Login",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = headlineColor,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = OnboardingPalette.WarmAmber.copy(alpha = 0.18f),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text(
                                        text = "Mode Lokal",
                                        fontFamily = DmSansFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = if (isDark) OnboardingPalette.WarmAmber else OnboardingPalette.WarmAmberDeep,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Data tersimpan 100% offline di HP ini. Tetap bisa login nanti dari Pengaturan.",
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = subtitleColor
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Lanjut tanpa login",
                            tint = subtitleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!syncStatus.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = syncStatus.errorMessage,
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Quiet Privacy Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = subtitleColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Data keuanganmu selalu aman & terenkripsi",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = subtitleColor.copy(alpha = 0.85f)
                )
            }
        }

        if (syncStatus.isSignedIn) {
            HorizontalDivider(color = extended.cardBorder.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onContinueToSetup,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnboardingPalette.CobaltBlue,
                    contentColor = OnboardingPalette.PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("onboarding_auth_continue_btn")
            ) {
                Text(
                    text = "Lanjut ke Setup (2 Menit)",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            TextButton(
                onClick = onContinueToSetup,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("onboarding_local_mode_btn")
            ) {
                Text(
                    text = "Atur ruang kerja lokal saja",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = subtitleColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun MiniFeaturePill(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GoogleBadgeIcon(modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = OnboardingPalette.PureWhite,
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val w = size.width
            val h = size.height
            val stroke = (w * 0.22f).coerceAtLeast(2.2f)
            val inset = stroke / 2f
            val arcSize = Size(w - stroke, h - stroke)
            val tl = Offset(inset, inset)

            drawArc(
                color = Color(0xFFEA4335),
                startAngle = -145f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = tl,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 140f,
                sweepAngle = 75f,
                useCenter = false,
                topLeft = tl,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 35f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = tl,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -10f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = tl,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(w * 0.50f, h * 0.50f),
                end = Offset(w * 0.94f, h * 0.50f),
                strokeWidth = stroke,
                cap = StrokeCap.Butt
            )
        }
    }
}

@Composable
private fun SetupWizardScaffold(
    stepNumber: Int,
    totalSteps: Int,
    stepBadge: String,
    title: String,
    subtitle: String,
    canCancel: Boolean,
    isDark: Boolean,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    primaryCtaText: String,
    primaryCtaTestTag: String,
    primaryCtaEnabled: Boolean = true,
    onPrimaryCta: () -> Unit,
    content: @Composable () -> Unit
) {
    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current
    val headlineColor = if (isDark) Color(0xFFF8FAFC) else OnboardingPalette.Obsidian
    val subtitleColor = if (isDark) Color(0xFFA8B3C5) else OnboardingPalette.SlateInk
    val primaryCobalt = if (isDark) OnboardingPalette.CobaltBright else OnboardingPalette.CobaltBlue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 560.dp)
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
        // Top navigation + "Setup 2 Menit" chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Langkah sebelumnya",
                    tint = headlineColor
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = OnboardingPalette.WarmAmber.copy(alpha = if (isDark) 0.20f else 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (isDark) OnboardingPalette.WarmAmber else OnboardingPalette.WarmAmberDeep,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Setup ±2 Menit • $stepNumber/$totalSteps",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) OnboardingPalette.WarmAmber else OnboardingPalette.WarmAmberDeep
                    )
                }
            }

            if (canCancel) {
                TextButton(onClick = onCancel) {
                    Text(
                        text = "Tutup",
                        fontFamily = DmSansFontFamily,
                        color = subtitleColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Tactile Segmented Step Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalSteps) { idx ->
                val isCompletedOrCurrent = idx < stepNumber
                val barColor by animateColorAsState(
                    targetValue = if (isCompletedOrCurrent) primaryCobalt else primaryCobalt.copy(alpha = 0.14f),
                    animationSpec = tween(280),
                    label = "seg_bar_$idx"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = stepBadge.uppercase(),
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp,
                    color = primaryCobalt
                )
                Text(
                    text = title,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 31.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.7).sp,
                    color = headlineColor
                )
                Text(
                    text = subtitle,
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                    color = subtitleColor
                )
            }

            content()

            Spacer(modifier = Modifier.height(6.dp))
        }

        HorizontalDivider(color = extended.cardBorder.copy(alpha = 0.65f))
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPrimaryCta()
            },
            enabled = primaryCtaEnabled,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OnboardingPalette.CobaltBlue,
                contentColor = OnboardingPalette.PureWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .testTag(primaryCtaTestTag)
        ) {
            Text(
                text = primaryCtaText,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (primaryCtaTestTag == "onboarding_finish_btn") {
                    Icons.Rounded.Check
                } else {
                    Icons.AutoMirrored.Rounded.ArrowForward
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupStep1ProfileContent(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    cycleDay: Int,
    onCycleDayChange: (Int) -> Unit,
    isDark: Boolean
) {
    val extended = LocalSakuColors.current
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else OnboardingPalette.PureWhite
    val headlineColor = if (isDark) Color(0xFFF8FAFC) else OnboardingPalette.Obsidian
    val subtitleColor = if (isDark) Color(0xFFA8B3C5) else OnboardingPalette.SlateInk
    val primaryCobalt = if (isDark) OnboardingPalette.CobaltBright else OnboardingPalette.CobaltBlue

    // Card 1: Display Name
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        shadowElevation = if (isDark) 0.dp else 1.5.dp,
        border = BorderStroke(0.9.dp, extended.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primaryCobalt.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PersonOutline,
                            contentDescription = null,
                            tint = primaryCobalt,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Sapaan di Beranda",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = headlineColor
                    )
                    Text(
                        text = "Opsional — bisa diubah nanti di Pengaturan",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = subtitleColor
                    )
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                placeholder = { Text("Contoh: Ridho, Sinta", fontFamily = DmSansFontFamily) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                trailingIcon = {
                    if (displayName.isNotEmpty()) {
                        IconButton(onClick = { onDisplayNameChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Hapus nama",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryCobalt,
                    unfocusedBorderColor = extended.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_name_input")
            )

            AnimatedVisibility(visible = displayName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primaryCobalt.copy(alpha = 0.09f)
                ) {
                    Text(
                        text = "Halo, ${displayName.trim()}! Selamat datang di Saku.",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryCobalt,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Card 2: Salary / Billing Cycle Date Grid
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        shadowElevation = if (isDark) 0.dp else 1.5.dp,
        border = BorderStroke(0.9.dp, extended.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OnboardingPalette.WarmAmber.copy(alpha = 0.18f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = if (isDark) OnboardingPalette.WarmAmber else OnboardingPalette.WarmAmberDeep,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Tanggal Mulai Siklus / Gajian",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = headlineColor
                    )
                    Text(
                        text = if (cycleDay == 1) {
                            "Siklus standar: Tgl 1 – akhir bulan"
                        } else {
                            "Siklus gajian: Tgl $cycleDay – Tgl ${cycleDay - 1} bulan depan"
                        },
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.5.sp,
                        color = subtitleColor
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                    val selected = cycleDay == day
                    Surface(
                        onClick = { onCycleDayChange(day) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) OnboardingPalette.CobaltBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) OnboardingPalette.CobaltBlue else extended.cardBorder
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = "Tgl $day",
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) OnboardingPalette.PureWhite else headlineColor,
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupStep2WorkspaceModeContent(
    startCleanMode: Boolean,
    onSelectCleanMode: (Boolean) -> Unit,
    isDark: Boolean
) {
    val extended = LocalSakuColors.current
    val headlineColor = if (isDark) Color(0xFFF8FAFC) else OnboardingPalette.Obsidian
    val subtitleColor = if (isDark) Color(0xFFA8B3C5) else OnboardingPalette.SlateInk
    val primaryCobalt = if (isDark) OnboardingPalette.CobaltBright else OnboardingPalette.CobaltBlue
    val sampleAmber = if (isDark) OnboardingPalette.WarmAmber else OnboardingPalette.WarmAmberDeep

    // Option 1: Start Clean
    Surface(
        onClick = { onSelectCleanMode(true) },
        shape = RoundedCornerShape(24.dp),
        color = if (startCleanMode) {
            primaryCobalt.copy(alpha = if (isDark) 0.16f else 0.06f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (startCleanMode) 2.dp else 1.dp,
            color = if (startCleanMode) primaryCobalt else extended.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_mode_clean")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = primaryCobalt.copy(alpha = 0.14f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            tint = primaryCobalt,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mulai dari Nol",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = headlineColor
                        )
                        if (startCleanMode) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Dipilih",
                                tint = primaryCobalt,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text = "Mulai dengan ruang kerja bersih. Kamu langsung buat 1 dompet aslimu di langkah terakhir.",
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = subtitleColor
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModeTagChip("1 dompet kustom", primaryCobalt)
                ModeTagChip("Langsung catat transaksi asli", primaryCobalt)
            }
        }
    }

    // Option 2: Sample Data
    Surface(
        onClick = { onSelectCleanMode(false) },
        shape = RoundedCornerShape(24.dp),
        color = if (!startCleanMode) {
            OnboardingPalette.WarmAmber.copy(alpha = if (isDark) 0.15f else 0.10f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (!startCleanMode) 2.dp else 1.dp,
            color = if (!startCleanMode) sampleAmber else extended.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_mode_sample")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = OnboardingPalette.WarmAmber.copy(alpha = 0.20f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = sampleAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jelajahi Data Contoh",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = headlineColor
                        )
                        if (!startCleanMode) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Dipilih",
                                tint = sampleAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text = "Coba langsung grafik arus kas, anggaran, kalender harian, dan Asisten AI tanpa input dulu.",
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = subtitleColor
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModeTagChip("Contoh transaksi & AI", sampleAmber)
                ModeTagChip("Bisa direset kapan saja", sampleAmber)
            }
        }
    }
}

@Composable
private fun ModeTagChip(
    label: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.11f)
    ) {
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupStep3FirstWalletContent(
    walletName: String,
    onWalletNameChange: (String) -> Unit,
    walletType: String,
    onWalletTypeChange: (String) -> Unit,
    balanceText: String,
    onBalanceTextChange: (String) -> Unit,
    parsedBalance: Long,
    accentHex: Long,
    onAccentHexChange: (Long) -> Unit,
    isDark: Boolean
) {
    val extended = LocalSakuColors.current
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else OnboardingPalette.PureWhite
    val headlineColor = if (isDark) Color(0xFFF8FAFC) else OnboardingPalette.Obsidian
    val subtitleColor = if (isDark) Color(0xFFA8B3C5) else OnboardingPalette.SlateInk
    val primaryCobalt = if (isDark) OnboardingPalette.CobaltBright else OnboardingPalette.CobaltBlue
    val selectedCardColor by animateColorAsState(
        targetValue = Color(accentHex),
        animationSpec = tween(250),
        label = "wallet_preview_color"
    )

    // Live Debit/Wallet Card Preview with subtle geometric accents
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = selectedCardColor,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle geometric circle watermark on top-right
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                drawCircle(
                    color = OnboardingPalette.PureWhite.copy(alpha = 0.08f),
                    radius = size.height * 0.65f,
                    center = Offset(size.width * 0.92f, size.height * 0.15f)
                )
                drawCircle(
                    color = OnboardingPalette.PureWhite.copy(alpha = 0.06f),
                    radius = size.height * 0.42f,
                    center = Offset(size.width * 0.78f, size.height * 0.95f)
                )
            }

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = OnboardingPalette.PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = walletName.ifBlank { "Dompet Utama" },
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = OnboardingPalette.PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = OnboardingPalette.PureWhite.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = walletType,
                            fontFamily = DmSansFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnboardingPalette.PureWhite,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Saldo Awal",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = OnboardingPalette.PureWhite.copy(alpha = 0.82f)
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(parsedBalance),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp,
                        color = OnboardingPalette.PureWhite
                    )
                }
            }
        }
    }

    // Form Configuration Card
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        shadowElevation = if (isDark) 0.dp else 1.5.dp,
        border = BorderStroke(0.9.dp, extended.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick Wallet Name Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih cepat atau ketik nama",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = subtitleColor
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickWalletPresets.forEach { preset ->
                        val isMatch = walletName.equals(preset.name, ignoreCase = true)
                        Surface(
                            onClick = {
                                onWalletNameChange(preset.name)
                                onWalletTypeChange(preset.type)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMatch) primaryCobalt else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = 0.9.dp,
                                color = if (isMatch) primaryCobalt else extended.cardBorder
                            )
                        ) {
                            Text(
                                text = preset.name,
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMatch) OnboardingPalette.PureWhite else headlineColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = walletName,
                onValueChange = onWalletNameChange,
                label = { Text("Nama Dompet", fontFamily = DmSansFontFamily) },
                placeholder = { Text("Contoh: BCA, GoPay, Tunai", fontFamily = DmSansFontFamily) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryCobalt,
                    unfocusedBorderColor = extended.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_wallet_name_input")
            )

            // Segmented Wallet Type Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipe Dompet",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = headlineColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        Triple("Bank", Icons.Rounded.AccountBalance, "Bank"),
                        Triple("E-Wallet", Icons.Rounded.Smartphone, "E-Wallet"),
                        Triple("Cash", Icons.Rounded.Payments, "Cash")
                    )
                    types.forEach { (type, icon, label) ->
                        val selected = walletType == type
                        Surface(
                            onClick = { onWalletTypeChange(type) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) OnboardingPalette.CobaltBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) OnboardingPalette.CobaltBlue else extended.cardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) OnboardingPalette.PureWhite else headlineColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = if (selected) OnboardingPalette.PureWhite else headlineColor
                                )
                            }
                        }
                    }
                }
            }

            // Initial Balance + Quick Balance Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = onBalanceTextChange,
                    label = { Text("Saldo Awal (Rp)", fontFamily = DmSansFontFamily) },
                    prefix = {
                        Text(
                            text = "Rp ",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = subtitleColor
                        )
                    },
                    supportingText = {
                        Text(
                            text = "Nominal: ${CurrencyFormatter.formatRupiah(parsedBalance)}",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            color = primaryCobalt
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryCobalt,
                        unfocusedBorderColor = extended.cardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_wallet_balance_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickBalancePresets.forEach { (label, raw) ->
                        val isMatch = balanceText == raw
                        Surface(
                            onClick = { onBalanceTextChange(raw) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMatch) primaryCobalt.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            border = BorderStroke(
                                width = 0.9.dp,
                                color = if (isMatch) primaryCobalt else extended.cardBorder
                            )
                        ) {
                            Text(
                                text = label,
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMatch) primaryCobalt else subtitleColor,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Card Accent Color Swatches
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Warna Kartu",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = headlineColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    onboardingWalletColors.forEach { hex ->
                        val isSel = accentHex == hex
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .then(
                                    if (isSel) Modifier.border(2.5.dp, headlineColor, CircleShape)
                                    else Modifier
                                )
                                .clickable { onAccentHexChange(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Warna dipilih",
                                    tint = OnboardingPalette.PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
