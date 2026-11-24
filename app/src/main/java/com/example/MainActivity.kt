package com.example

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutGoogle
import com.example.security.BiometricAuthManager
import com.example.security.BiometricCapability
import com.example.security.BiometricFailureKind
import com.example.security.BiometricStatusInfo
import com.example.ui.components.ExportReportSheet
import com.example.ui.components.SakuWidgetGallerySheet
import com.example.ui.components.TransactionBottomSheet
import com.example.ui.screens.AnalisisScreen
import com.example.ui.screens.AnggaranScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.BerandaScreen
import com.example.ui.screens.CashflowCalendarScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.DompetScreen
import com.example.ui.screens.EmoneyNfcScreen
import com.example.ui.screens.MonthlyOverviewScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PersonalizationScreen
import com.example.ui.screens.RecurringTransactionsScreen
import com.example.ui.screens.SavingsGoalsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionHistoryScreen
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LocalSakuBottomPadding
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.theme.SakuTheme
import com.example.ui.viewmodel.AppLockGateState
import com.example.ui.viewmodel.BudgetAlertLevel
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.OnboardingGateState
import com.example.ui.viewmodel.SheetInputTab
import com.example.widget.EXTRA_OPEN_ROUTE
import com.example.widget.EXTRA_OPEN_TAB
import com.example.widget.SakuBalanceWidgetReceiver
import com.example.widget.SakuWidgetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ROUTE_BERANDA = "beranda"
const val ROUTE_ANALISIS = "analisis"
const val ROUTE_ANGGARAN = "anggaran"
const val ROUTE_DOMPET = "dompet"
const val ROUTE_BERULANG = "berulang"
const val ROUTE_PENGATURAN = "pengaturan"
const val ROUTE_PERSONALISASI = "personalisasi"
const val ROUTE_RINGKASAN_BULANAN = "ringkasan_bulanan"
const val ROUTE_RIWAYAT = "riwayat"
const val ROUTE_SAVINGS_GOALS = "target_tabungan"
const val ROUTE_DEBTS = "hutang_piutang"
const val ROUTE_NFC_EMONEY = "cek_emoney_nfc"
const val ROUTE_KALENDER = "kalender_cashflow"

private val NAVBAR_ROUTES = setOf(
    ROUTE_BERANDA,
    ROUTE_ANALISIS,
    ROUTE_ANGGARAN,
    ROUTE_DOMPET
)

private enum class SakuNavIconKind {
    HOME,
    ANALYTICS,
    BUDGET,
    WALLET
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val iconKind: SakuNavIconKind,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navItems = listOf(
    NavDestinationItem(ROUTE_BERANDA, "Beranda", SakuNavIconKind.HOME, Icons.Rounded.SpaceDashboard, Icons.Outlined.SpaceDashboard),
    NavDestinationItem(ROUTE_ANALISIS, "Analisis", SakuNavIconKind.ANALYTICS, Icons.Rounded.Analytics, Icons.Outlined.Analytics),
    NavDestinationItem(ROUTE_ANGGARAN, "Anggaran", SakuNavIconKind.BUDGET, Icons.Rounded.Savings, Icons.Outlined.Savings),
    NavDestinationItem(ROUTE_DOMPET, "Dompet", SakuNavIconKind.WALLET, Icons.Rounded.Wallet, Icons.Outlined.Wallet)
)

class MainActivity : FragmentActivity() {
    private var widgetLaunchTab by mutableStateOf<SheetInputTab?>(null)
    private var widgetLaunchRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)
        handleWidgetIntent(intent)

        // Step 2: Edge-to-Edge display
        enableEdgeToEdge()

        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val personalization by viewModel.personalization.collectAsStateWithLifecycle()

            SakuTheme(
                themeMode = themeMode,
                accentPreset = personalization.accentPreset,
                cardCornerStyle = personalization.cardCornerStyle
            ) {
                SakuAppRoot(
                    viewModel = viewModel,
                    pendingWidgetTab = widgetLaunchTab,
                    onConsumeWidgetTab = { widgetLaunchTab = null },
                    pendingWidgetRoute = widgetLaunchRoute,
                    onConsumeWidgetRoute = { widgetLaunchRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        val rawTab = intent?.getStringExtra(EXTRA_OPEN_TAB)
        if (rawTab != null) {
            widgetLaunchTab = SheetInputTab.entries.firstOrNull { it.name == rawTab } ?: SheetInputTab.MANUAL
            intent.removeExtra(EXTRA_OPEN_TAB)
        }
        val rawRoute = intent?.getStringExtra(EXTRA_OPEN_ROUTE)
        if (rawRoute != null) {
            widgetLaunchRoute = rawRoute
            intent.removeExtra(EXTRA_OPEN_ROUTE)
        }
    }
}

private fun requestPinSakuWidget(
    context: Context,
    receiverClass: Class<*> = SakuBalanceWidgetReceiver::class.java
): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, receiverClass)
        if (manager.isRequestPinAppWidgetSupported) {
            val requested = manager.requestPinAppWidget(provider, null, null)
            return if (requested) {
                "Ikuti dialog sistem untuk menempel widget Saku di layar utama"
            } else {
                "Tambahkan widget Saku dari menu Widget di layar utama"
            }
        }
    }
    return "Tekan lama layar utama HP -> pilih Widget -> pilih Saku"
}

@Composable
private fun SakuAppRoot(
    viewModel: FinanceViewModel,
    pendingWidgetTab: SheetInputTab?,
    onConsumeWidgetTab: () -> Unit,
    pendingWidgetRoute: String?,
    onConsumeWidgetRoute: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val personalization by viewModel.personalization.collectAsStateWithLifecycle()
    val hideBalance by viewModel.hideBalance.collectAsStateWithLifecycle()
    val berandaState by viewModel.berandaUiState.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val allTransactionItems by viewModel.allTransactionItems.collectAsStateWithLifecycle()
    val categorySlices by viewModel.categorySlices.collectAsStateWithLifecycle()
    val anggaranState by viewModel.anggaranUiState.collectAsStateWithLifecycle()
    val monthlyOverviewState by viewModel.monthlyOverviewState.collectAsStateWithLifecycle()
    val sheetState by viewModel.sheetState.collectAsStateWithLifecycle()

    val insightsState by viewModel.insightsState.collectAsStateWithLifecycle()
    val qaActiveState by viewModel.qaActiveState.collectAsStateWithLifecycle()
    val qaHistory by viewModel.qaHistory.collectAsStateWithLifecycle()
    val recurringItems by viewModel.recurringItems.collectAsStateWithLifecycle()
    val activeRecurringCount = remember(recurringItems) { recurringItems.count { it.rule.isActive } }
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val emoneyHistory by viewModel.emoneyHistory.collectAsStateWithLifecycle()
    val securityPrefs by viewModel.securityPrefs.collectAsStateWithLifecycle()
    val notificationPrefs by viewModel.notificationPrefs.collectAsStateWithLifecycle()
    val onboardingState by viewModel.onboardingState.collectAsStateWithLifecycle()
    val lockGateState by viewModel.lockGateState.collectAsStateWithLifecycle()
    val lockSessionId by viewModel.lockSessionId.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val pinCooldownSeconds by viewModel.pinCooldownSeconds.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val credentialManagerLazy = remember(context) {
        lazy(LazyThreadSafetyMode.NONE) { CredentialManager.create(context) }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showExportSheet by remember { mutableStateOf(false) }
    var showWidgetGallerySheet by remember { mutableStateOf(false) }
    var biometricStatus by remember {
        mutableStateOf(
            BiometricStatusInfo(
                capability = BiometricCapability.UNAVAILABLE,
                supportsStrongBiometric = false,
                supportsDeviceCredential = false,
                statusLabel = "Memeriksa biometrik..."
            )
        )
    }
    var isBiometricPromptActive by remember { mutableStateOf(false) }
    var suppressNextBackgroundLock by remember { mutableStateOf(false) }

    val triggerGoogleSignIn: () -> Unit = {
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManagerLazy.value,
            onAuthSuccess = viewModel::onGoogleSignInSucceeded,
            onAuthError = viewModel::onGoogleSignInError,
            scope = scope
        )
    }

    val triggerGoogleSignOut: () -> Unit = {
        signOutGoogle(
            credentialManager = credentialManagerLazy.value,
            scope = scope,
            onSignOutComplete = viewModel::onSignOutCompleted
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    suppressNextBackgroundLock = false
                    scope.launch(Dispatchers.Default) {
                        val inspected = BiometricAuthManager.inspectStatus(context)
                        biometricStatus = inspected
                    }
                    viewModel.processDueRecurringNow()
                }
                Lifecycle.Event.ON_STOP -> {
                    val changingConfig = activity?.isChangingConfigurations == true
                    if (!changingConfig && !isBiometricPromptActive && !suppressNextBackgroundLock) {
                        viewModel.lockAppOnBackground()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        biometricStatus = withContext(Dispatchers.Default) {
            BiometricAuthManager.inspectStatus(context)
        }
        delay(500)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManagerLazy.value,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    LaunchedEffect(Unit) {
        viewModel.uiNotice.collect { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(pendingWidgetTab, pendingWidgetRoute, onboardingState, isAppUnlocked) {
        if (onboardingState == OnboardingGateState.COMPLETED && isAppUnlocked) {
            if (pendingWidgetRoute != null) {
                navController.navigate(pendingWidgetRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                onConsumeWidgetRoute()
            }
            if (pendingWidgetTab != null) {
                viewModel.openTransactionSheet(tab = pendingWidgetTab)
                onConsumeWidgetTab()
            }
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (lockGateState == AppLockGateState.LOADING || onboardingState == OnboardingGateState.LOADING) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalSakuColors.current.obsidianHeader),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = LocalSakuColors.current.lime)
        }
        return
    }

    if (lockGateState == AppLockGateState.LOCKED || (!isAppUnlocked && securityPrefs.appLockEnabled)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppLockScreen(
                displayName = personalization.displayName,
                hasPin = securityPrefs.hasPin,
                biometricEnabled = securityPrefs.biometricEnabled,
                lockSessionId = lockSessionId,
                biometricStatusLabel = biometricStatus.statusLabel,
                pinCooldownSeconds = pinCooldownSeconds,
                onRequestBiometric = {
                    val act = activity ?: return@AppLockScreen
                    if (isBiometricPromptActive) return@AppLockScreen
                    BiometricAuthManager.authenticate(
                        activity = act,
                        title = "Buka Kunci Saku",
                        subtitle = "Verifikasi sidik jari atau wajah untuk mengakses data keuanganmu",
                        allowDeviceCredentialFallback = !securityPrefs.hasPin,
                        negativeButtonText = "Gunakan PIN",
                        onPromptStarted = { isBiometricPromptActive = true },
                        onPromptFinished = { isBiometricPromptActive = false },
                        onSuccess = viewModel::unlockWithBiometric,
                        onAuthFailedAttempt = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Biometrik tidak cocok, silakan coba lagi")
                            }
                        },
                        onFailure = { kind, message ->
                            if (kind != BiometricFailureKind.USER_CANCELED &&
                                kind != BiometricFailureKind.USE_PIN_FALLBACK
                            ) {
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        }
                    )
                },
                onVerifyPin = viewModel::verifyPinAndUnlock
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        return
    }

    if (onboardingState == OnboardingGateState.NEEDS_ONBOARDING) {
        Box(modifier = Modifier.fillMaxSize()) {
            OnboardingScreen(
                canCancel = wallets.isNotEmpty(),
                syncStatus = syncStatus,
                onGoogleSignIn = triggerGoogleSignIn,
                onCancel = viewModel::dismissOnboardingIfReady,
                onCompleteClean = viewModel::completeOnboardingClean,
                onCompleteWithSampleData = viewModel::completeOnboardingWithSampleData
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp
        val showBottomChrome = currentDestination?.route != ROUTE_BERULANG &&
            currentDestination?.route != ROUTE_PENGATURAN &&
            currentDestination?.route != ROUTE_PERSONALISASI &&
            currentDestination?.route != ROUTE_RINGKASAN_BULANAN &&
            currentDestination?.route != ROUTE_RIWAYAT &&
            currentDestination?.route != ROUTE_SAVINGS_GOALS &&
            currentDestination?.route != ROUTE_DEBTS &&
            currentDestination?.route != ROUTE_NFC_EMONEY &&
            currentDestination?.route != ROUTE_KALENDER
        val haptic = LocalHapticFeedback.current

        val budgetBadgeColor = remember(anggaranState.items) {
            when {
                anggaranState.items.any { it.alertLevel == BudgetAlertLevel.CRITICAL_90 } -> Color(0xFFF43F5E)
                anggaranState.items.any { it.alertLevel == BudgetAlertLevel.WARNING_75 } -> Color(0xFFF59E0B)
                else -> null
            }
        }
        val walletBadgeColor = remember(syncStatus) {
            when {
                syncStatus.isSyncing -> IncomeEmerald
                syncStatus.isSignedIn && syncStatus.pendingChangesCount > 0 -> Color(0xFFF59E0B)
                else -> null
            }
        }

        val defaultFabTab = remember(personalization.defaultFabTabName) {
            SheetInputTab.entries.firstOrNull { it.name == personalization.defaultFabTabName }
                ?: SheetInputTab.MANUAL
        }

        val handleFabClick: () -> Unit = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (sheetState.isOpen) {
                viewModel.closeTransactionSheet()
            } else {
                viewModel.openTransactionSheet(tab = defaultFabTab)
            }
        }

        val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val tabTopPadding = statusBarTopInset + 4.dp
        val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val tabBottomPadding = if (!isWideScreen && showBottomChrome) {
            93.dp + navBarBottomInset
        } else {
            28.dp + navBarBottomInset
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            CompositionLocalProvider(
                LocalSakuTopPadding provides tabTopPadding,
                LocalSakuBottomPadding provides tabBottomPadding
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                if (isWideScreen) {
                    val extended = LocalSakuColors.current
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            SakuCenterActionOrb(
                                isSheetOpen = sheetState.isOpen,
                                onClick = handleFabClick,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val badgeColor = when (item.route) {
                                ROUTE_ANGGARAN -> budgetBadgeColor
                                ROUTE_DOMPET -> walletBadgeColor
                                else -> null
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navigateTo(item.route)
                                    }
                                },
                                modifier = Modifier.testTag("nav_${item.route}"),
                                icon = {
                                    Box {
                                        SakuCustomNavIcon(
                                            kind = item.iconKind,
                                            selected = selected,
                                            tint = if (selected) extended.onLimeContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            cutoutColor = if (selected) extended.limeContainer else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        if (badgeColor != null) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 3.dp, y = (-2).dp)
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(badgeColor)
                                            )
                                        }
                                    }
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = extended.onLimeContainer,
                                    selectedTextColor = extended.onLimeContainer,
                                    indicatorColor = extended.limeContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_BERANDA,
                    enterTransition = {
                        val fromNav = initialState.destination.route in NAVBAR_ROUTES
                        val toNav = targetState.destination.route in NAVBAR_ROUTES
                        if (fromNav && toNav) {
                            EnterTransition.None
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) { fullWidth -> fullWidth } + fadeIn(
                                animationSpec = tween(durationMillis = 220)
                            )
                        }
                    },
                    exitTransition = {
                        val fromNav = initialState.destination.route in NAVBAR_ROUTES
                        val toNav = targetState.destination.route in NAVBAR_ROUTES
                        if (fromNav && toNav) {
                            ExitTransition.None
                        } else {
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) { fullWidth -> -fullWidth / 4 } + fadeOut(
                                animationSpec = tween(durationMillis = 200)
                            )
                        }
                    },
                    popEnterTransition = {
                        val fromNav = initialState.destination.route in NAVBAR_ROUTES
                        val toNav = targetState.destination.route in NAVBAR_ROUTES
                        if (fromNav && toNav) {
                            EnterTransition.None
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) { fullWidth -> -fullWidth / 4 } + fadeIn(
                                animationSpec = tween(durationMillis = 220)
                            )
                        }
                    },
                    popExitTransition = {
                        val fromNav = initialState.destination.route in NAVBAR_ROUTES
                        val toNav = targetState.destination.route in NAVBAR_ROUTES
                        if (fromNav && toNav) {
                            ExitTransition.None
                        } else {
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) { fullWidth -> fullWidth } + fadeOut(
                                animationSpec = tween(durationMillis = 200)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_BERANDA) {
                        BerandaScreen(
                            uiState = berandaState,
                            personalization = personalization,
                            activeRecurringCount = activeRecurringCount,
                            syncStatus = syncStatus,
                            hideBalance = hideBalance,
                            onToggleHideBalance = viewModel::toggleHideBalance,
                            onSyncBadgeClick = {
                                if (syncStatus.isSignedIn) {
                                    viewModel.syncCloudNow()
                                } else {
                                    triggerGoogleSignIn()
                                }
                            },
                            onOpenSettings = { navController.navigate(ROUTE_PENGATURAN) },
                            onOpenPersonalization = { navController.navigate(ROUTE_PERSONALISASI) },
                            onOpenRecurring = { navController.navigate(ROUTE_BERULANG) },
                            onOpenManual = {
                                viewModel.openTransactionSheet(tab = SheetInputTab.MANUAL)
                            },
                            onOpenAnalisis = { navigateTo(ROUTE_ANALISIS) },
                            onOpenAnggaran = { navigateTo(ROUTE_ANGGARAN) },
                            onOpenWallets = { navigateTo(ROUTE_DOMPET) },
                            onOpenMonthlyOverview = { navController.navigate(ROUTE_RINGKASAN_BULANAN) },
                            onOpenHistory = { navController.navigate(ROUTE_RIWAYAT) },
                            onOpenSavingsGoals = { navController.navigate(ROUTE_SAVINGS_GOALS) },
                            onOpenDebts = { navController.navigate(ROUTE_DEBTS) },
                            onOpenExport = { showExportSheet = true },
                            onOpenEmoneyNfc = { navController.navigate(ROUTE_NFC_EMONEY) },
                            onOpenCalendar = { navController.navigate(ROUTE_KALENDER) },
                            onSelectWallet = viewModel::selectWalletFilter,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onTypeFilterChange = viewModel::setTypeFilter,
                            onOpenAi = {
                                viewModel.openTransactionSheet(tab = SheetInputTab.AI_MAGIC)
                            },
                            onQuickVoice = {
                                viewModel.openTransactionSheet(
                                    tab = SheetInputTab.VOICE,
                                    autoStartVoice = true
                                )
                            },
                            onQuickScan = {
                                viewModel.openTransactionSheet(tab = SheetInputTab.SCAN)
                            },
                            onEditTransaction = { tx ->
                                viewModel.openTransactionSheet(editingTx = tx)
                            },
                            onDeleteTransaction = { tx ->
                                viewModel.deleteTransaction(tx)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Transaksi \"${tx.merchant}\" dihapus",
                                        actionLabel = "Urungkan",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreTransaction(tx)
                                    }
                                }
                            }
                        )
                    }

                    composable(ROUTE_ANALISIS) {
                        AnalisisScreen(
                            slices = categorySlices,
                            allTransactionItems = allTransactionItems,
                            monthlyOverview = monthlyOverviewState,
                            insightsState = insightsState,
                            qaActiveState = qaActiveState,
                            qaHistory = qaHistory,
                            onGenerateInsights = viewModel::generateMonthlyInsights,
                            onAskQuestion = viewModel::askFinanceQuestion,
                            onOpenMonthlyOverview = { navController.navigate(ROUTE_RINGKASAN_BULANAN) },
                            onOpenCalendar = { navController.navigate(ROUTE_KALENDER) }
                        )
                    }

                    composable(ROUTE_ANGGARAN) {
                        AnggaranScreen(
                            uiState = anggaranState,
                            onUpsertBudget = viewModel::upsertBudget,
                            onDeleteBudget = viewModel::deleteBudget
                        )
                    }

                    composable(ROUTE_DOMPET) {
                        DompetScreen(
                            wallets = wallets,
                            activeRecurringCount = activeRecurringCount,
                            syncStatus = syncStatus,
                            onGoogleSignIn = triggerGoogleSignIn,
                            onSyncNow = viewModel::syncCloudNow,
                            onSignOut = triggerGoogleSignOut,
                            onOpenRecurring = { navController.navigate(ROUTE_BERULANG) },
                            onOpenSettings = { navController.navigate(ROUTE_PENGATURAN) },
                            onOpenEmoneyNfc = { navController.navigate(ROUTE_NFC_EMONEY) },
                            onTransfer = viewModel::transferBetweenWallets,
                            onAddWallet = viewModel::addWallet,
                            onUpdateWallet = viewModel::updateWallet,
                            onCountWalletTransactions = viewModel::countTransactionsForWallet,
                            onDeleteWallet = viewModel::deleteWallet
                        )
                    }

                    composable(ROUTE_BERULANG) {
                        RecurringTransactionsScreen(
                            items = recurringItems,
                            wallets = wallets,
                            onBack = { navController.popBackStack() },
                            onSaveRule = viewModel::saveRecurringRule,
                            onToggleActive = viewModel::toggleRecurringActive,
                            onDeleteRule = viewModel::deleteRecurringRule
                        )
                    }

                    composable(ROUTE_PENGATURAN) {
                        SettingsScreen(
                            syncStatus = syncStatus,
                            themeMode = themeMode,
                            accentLabel = personalization.accentPreset.label,
                            displayName = personalization.displayName,
                            hideBalance = hideBalance,
                            activeRecurringCount = activeRecurringCount,
                            walletCount = wallets.size,
                            savingsGoalCount = savingsGoals.size,
                            activeDebtCount = debts.count { !it.isSettled },
                            securityPrefs = securityPrefs,
                            biometricStatus = biometricStatus,
                            notificationPrefs = notificationPrefs,
                            onBack = { navController.popBackStack() },
                            onOpenPersonalization = { navController.navigate(ROUTE_PERSONALISASI) },
                            onSetThemeMode = viewModel::setThemeMode,
                            onSetHideBalance = viewModel::setHideBalance,
                            onGoogleSignIn = triggerGoogleSignIn,
                            onSyncNow = viewModel::syncCloudNow,
                            onSignOut = triggerGoogleSignOut,
                            onOpenRecurring = { navController.navigate(ROUTE_BERULANG) },
                            onOpenWallets = { navigateTo(ROUTE_DOMPET) },
                            onOpenSavingsGoals = { navController.navigate(ROUTE_SAVINGS_GOALS) },
                            onOpenDebts = { navController.navigate(ROUTE_DEBTS) },
                            onOpenExport = { showExportSheet = true },
                            onSetupPin = viewModel::setupPinLock,
                            onRequestToggleAppLock = { enable, openPinDialog ->
                                val act = activity
                                if (enable) {
                                    when {
                                        biometricStatus.canAuthenticateAtAll && act != null -> {
                                            BiometricAuthManager.authenticate(
                                                activity = act,
                                                title = "Aktifkan Kunci Biometrik",
                                                subtitle = "Konfirmasi biometrik untuk mengunci akses saat Saku dibuka",
                                                allowDeviceCredentialFallback = true,
                                                confirmationRequired = true,
                                                onPromptStarted = { isBiometricPromptActive = true },
                                                onPromptFinished = { isBiometricPromptActive = false },
                                                onSuccess = {
                                                    if (securityPrefs.hasPin) {
                                                        viewModel.setAppLockEnabled(true)
                                                    } else {
                                                        viewModel.enableBiometricLockOnly()
                                                    }
                                                },
                                                onFailure = { kind, message ->
                                                    if (kind != BiometricFailureKind.USER_CANCELED) {
                                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                                    }
                                                }
                                            )
                                        }
                                        securityPrefs.hasPin -> viewModel.setAppLockEnabled(true)
                                        else -> {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Biometrik perangkat belum siap — buat PIN Saku terlebih dahulu"
                                                )
                                            }
                                            openPinDialog()
                                        }
                                    }
                                } else {
                                    if (securityPrefs.biometricEnabled && biometricStatus.canAuthenticateAtAll && act != null) {
                                        BiometricAuthManager.authenticate(
                                            activity = act,
                                            title = "Nonaktifkan Kunci Aplikasi",
                                            subtitle = "Verifikasi identitasmu sebelum mematikan perlindungan",
                                            allowDeviceCredentialFallback = true,
                                            confirmationRequired = true,
                                            onPromptStarted = { isBiometricPromptActive = true },
                                            onPromptFinished = { isBiometricPromptActive = false },
                                            onSuccess = { viewModel.setAppLockEnabled(false) },
                                            onFailure = { kind, message ->
                                                if (kind != BiometricFailureKind.USER_CANCELED) {
                                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                                }
                                            }
                                        )
                                    } else {
                                        viewModel.setAppLockEnabled(false)
                                    }
                                }
                            },
                            onSetBiometricEnabled = viewModel::setBiometricEnabled,
                            onSetLockOnBackground = viewModel::setLockOnBackground,
                            onOpenBiometricEnrollment = {
                                suppressNextBackgroundLock = true
                                runCatching {
                                    context.startActivity(BiometricAuthManager.buildEnrollIntent(context))
                                }.onFailure {
                                    suppressNextBackgroundLock = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Buka Pengaturan HP -> Keamanan untuk mendaftarkan sidik jari")
                                    }
                                }
                            },
                            onClearAppLock = viewModel::clearAppLock,
                            onSetDailyReminder = viewModel::setDailyReminder,
                            onSetBudgetAlertNotif = viewModel::setBudgetAlertNotifEnabled,
                            onSendTestNotification = viewModel::sendTestNotification,
                            onRequestPinWidget = {
                                showWidgetGallerySheet = true
                            },
                            onReopenOnboarding = viewModel::reopenOnboardingWizard
                        )
                    }

                    composable(ROUTE_SAVINGS_GOALS) {
                        SavingsGoalsScreen(
                            goals = savingsGoals,
                            wallets = wallets,
                            onBack = { navController.popBackStack() },
                            onSaveGoal = viewModel::saveSavingsGoal,
                            onContributeGoal = viewModel::contributeToSavingsGoal,
                            onDeleteGoal = viewModel::deleteSavingsGoal
                        )
                    }

                    composable(ROUTE_DEBTS) {
                        DebtsScreen(
                            debts = debts,
                            wallets = wallets,
                            onBack = { navController.popBackStack() },
                            onSaveDebt = viewModel::saveDebtRecord,
                            onRecordPayment = viewModel::recordDebtPayment,
                            onDeleteDebt = viewModel::deleteDebtRecord
                        )
                    }

                    composable(ROUTE_PERSONALISASI) {
                        PersonalizationScreen(
                            prefs = personalization,
                            themeMode = themeMode,
                            wallets = wallets,
                            sampleNetBalance = berandaState.netBalance,
                            onBack = { navController.popBackStack() },
                            onSetThemeMode = viewModel::setThemeMode,
                            onUpdateDisplayName = viewModel::updateDisplayName,
                            onSetAccentPreset = viewModel::setAccentPreset,
                            onSetCardCornerStyle = viewModel::setCardCornerStyle,
                            onSetShowWalletsOnHome = viewModel::setShowWalletsOnHome,
                            onSetShowBudgetOnHome = viewModel::setShowBudgetOnHome,
                            onSetCompactCurrencyOnHome = viewModel::setCompactCurrencyOnHome,
                            onSetHomeShortcuts = viewModel::setHomeShortcuts,
                            onSetDefaultWalletId = viewModel::setDefaultWalletId,
                            onSetDefaultFabTab = viewModel::setDefaultFabTab,
                            onSetBudgetAlertPercent = viewModel::setBudgetAlertPercent,
                            onSetBillingCycleStartDay = viewModel::setBillingCycleStartDay,
                            onResetPersonalization = viewModel::resetPersonalization
                        )
                    }

                    composable(ROUTE_RINGKASAN_BULANAN) {
                        MonthlyOverviewScreen(
                            uiState = monthlyOverviewState,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(ROUTE_RIWAYAT) {
                        TransactionHistoryScreen(
                            allTransactionItems = allTransactionItems,
                            wallets = wallets,
                            hideBalance = hideBalance,
                            onToggleHideBalance = viewModel::toggleHideBalance,
                            onBack = { navController.popBackStack() },
                            onAddTransaction = {
                                viewModel.openTransactionSheet(tab = SheetInputTab.MANUAL)
                            },
                            onOpenExport = { showExportSheet = true },
                            onEditTransaction = { tx ->
                                viewModel.openTransactionSheet(editingTx = tx)
                            },
                            onDeleteTransaction = { tx ->
                                viewModel.deleteTransaction(tx)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Transaksi \"${tx.merchant}\" dihapus",
                                        actionLabel = "Urungkan",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreTransaction(tx)
                                    }
                                }
                            },
                            initialWalletId = berandaState.selectedWalletId
                        )
                    }

                    composable(ROUTE_NFC_EMONEY) {
                        EmoneyNfcScreen(
                            history = emoneyHistory,
                            wallets = wallets,
                            hideBalance = hideBalance,
                            onToggleHideBalance = viewModel::toggleHideBalance,
                            onBack = { navController.popBackStack() },
                            onSaveScan = viewModel::saveEmoneyScan,
                            onDeleteRecord = viewModel::deleteEmoneyRecord,
                            onClearHistory = viewModel::clearEmoneyHistory,
                            onRecordTransaction = viewModel::recordEmoneyTransaction,
                            onBeforeOpenNfcSettings = { suppressNextBackgroundLock = true }
                        )
                    }

                    composable(ROUTE_KALENDER) {
                        CashflowCalendarScreen(
                            allTransactionItems = allTransactionItems,
                            recurringItems = recurringItems,
                            hideBalance = hideBalance,
                            onBack = { navController.popBackStack() },
                            onAddTransactionForDate = { dayStartMillis ->
                                val noonCal = java.util.Calendar.getInstance().apply {
                                    timeInMillis = dayStartMillis
                                    set(java.util.Calendar.HOUR_OF_DAY, 12)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                viewModel.openTransactionSheet(
                                    tab = SheetInputTab.MANUAL,
                                    initialTimestamp = noonCal.timeInMillis
                                )
                            },
                            onEditTransaction = { tx ->
                                viewModel.openTransactionSheet(editingTx = tx)
                            },
                            onDeleteTransaction = { tx ->
                                viewModel.deleteTransaction(tx)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Transaksi \"${tx.merchant}\" dihapus",
                                        actionLabel = "Urungkan",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreTransaction(tx)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        }

        if (!isWideScreen && showBottomChrome) {
            SakuBottomBar(
                currentRoute = currentDestination?.route,
                isSheetOpen = sheetState.isOpen,
                budgetBadgeColor = budgetBadgeColor,
                walletBadgeColor = walletBadgeColor,
                onNavigate = { route ->
                    if (currentDestination?.route != route) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navigateTo(route)
                    }
                },
                onFabClick = handleFabClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (!isWideScreen && showBottomChrome) 89.dp else 16.dp
                )
        )

        TransactionBottomSheet(
            sheetState = sheetState,
            wallets = wallets,
            onDismiss = viewModel::closeTransactionSheet,
            onSelectTab = viewModel::selectSheetTab,
            onAiInputChange = viewModel::updateAiInputText,
            onParseAiText = viewModel::parseAiText,
            onParseVoiceAudio = viewModel::parseVoiceAudio,
            onParseReceiptBitmap = viewModel::parseReceiptBitmap,
            onUpdateDraft = viewModel::updateDraft,
            onRemoveDraft = viewModel::removeDraft,
            onSaveDrafts = viewModel::saveDraftsToDatabase,
            onClearPreview = viewModel::clearAiPreview,
            onSaveManual = viewModel::saveManualTransaction,
            onDeleteExisting = viewModel::deleteTransaction,
            defaultWalletId = personalization.defaultWalletId,
            onAutoTagCategory = viewModel::suggestCategoryForInput
        )

        if (showExportSheet) {
            ExportReportSheet(
                wallets = wallets,
                billingCycleStartDay = personalization.billingCycleStartDay,
                initialWalletId = berandaState.selectedWalletId,
                onDismiss = { showExportSheet = false },
                onExportPdf = viewModel::exportStatementPdf,
                onExportCsv = viewModel::exportStatementCsv,
                onError = { err ->
                    scope.launch { snackbarHostState.showSnackbar(err) }
                }
            )
        }

        if (showWidgetGallerySheet) {
            SakuWidgetGallerySheet(
                netBalance = berandaState.netBalance,
                monthlyIncome = berandaState.monthlyIncome,
                monthlyExpense = berandaState.monthlyExpense,
                walletCount = wallets.size,
                primaryWalletName = wallets.firstOrNull()?.name ?: "Dompet Utama",
                anggaranState = anggaranState,
                hideBalance = hideBalance,
                billingCycleStartDay = personalization.billingCycleStartDay,
                onDismiss = { showWidgetGallerySheet = false },
                onPinWidget = { widgetType ->
                    showWidgetGallerySheet = false
                    val msg = requestPinSakuWidget(context, widgetType.receiverClass)
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
        }
    }
}

@Composable
private fun SakuBottomBar(
    currentRoute: String?,
    isSheetOpen: Boolean,
    budgetBadgeColor: Color?,
    walletBadgeColor: Color?,
    onNavigate: (String) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val dockSurfaceColor = if (extended.isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .testTag("saku_floating_navbar"),
                shape = RoundedCornerShape(28.dp),
                color = dockSurfaceColor,
                border = BorderStroke(
                    width = if (extended.isDark) 1.dp else 0.9.dp,
                    color = extended.cardBorder.copy(alpha = 0.85f)
                ),
                shadowElevation = if (extended.isDark) 10.dp else 14.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { item ->
                        val badgeColor = when (item.route) {
                            ROUTE_ANGGARAN -> budgetBadgeColor
                            ROUTE_DOMPET -> walletBadgeColor
                            else -> null
                        }
                        SakuDockTabItem(
                            item = item,
                            selected = currentRoute == item.route,
                            badgeColor = badgeColor,
                            dockSurfaceColor = dockSurfaceColor,
                            onClick = { onNavigate(item.route) }
                        )
                    }
                }
            }

            SakuSeparateAddButton(
                isSheetOpen = isSheetOpen,
                onClick = onFabClick
            )
        }
    }
}

@Composable
private fun SakuSeparateAddButton(
    isSheetOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "separate_fab_press_scale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (isSheetOpen) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "separate_fab_icon_rotation"
    )
    val closedColor = extended.lime
    val containerColor by animateColorAsState(
        targetValue = if (isSheetOpen) MaterialTheme.colorScheme.surfaceVariant else closedColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "separate_fab_bg"
    )
    val contentTint by animateColorAsState(
        targetValue = if (isSheetOpen) MaterialTheme.colorScheme.onSurface else extended.onLime,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "separate_fab_tint"
    )

    Surface(
        modifier = modifier
            .size(54.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .testTag("add_transaction_fab"),
        shape = CircleShape,
        color = containerColor,
        border = if (isSheetOpen) {
            BorderStroke(0.8.dp, extended.cardBorder)
        } else {
            null
        },
        shadowElevation = if (extended.isDark) 10.dp else 14.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(
                        bounded = true,
                        color = if (isSheetOpen) MaterialTheme.colorScheme.onSurface else extended.onLime
                    ),
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = if (isSheetOpen) "Tutup Catat Transaksi" else "Catat Transaksi Baru",
                tint = contentTint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = iconRotation
                    }
            )
        }
    }
}

@Composable
private fun SakuDockTabItem(
    item: NavDestinationItem,
    selected: Boolean,
    badgeColor: Color?,
    dockSurfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tab_press_scale"
    )

    val activePillColor = extended.limeContainer
    val activeContentColor = extended.onLimeContainer
    val pillBackground by animateColorAsState(
        targetValue = if (selected) activePillColor else Color.Transparent,
        animationSpec = tween(durationMillis = 160),
        label = "tab_pill_bg"
    )
    val inactiveIconTint = MaterialTheme.colorScheme.onSurfaceVariant
    val contentTint by animateColorAsState(
        targetValue = if (selected) activeContentColor else inactiveIconTint,
        animationSpec = tween(durationMillis = 160),
        label = "tab_content_tint"
    )
    val iconCutoutColor by animateColorAsState(
        targetValue = if (selected) activePillColor else dockSurfaceColor,
        animationSpec = tween(durationMillis = 160),
        label = "tab_cutout_tint"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(999.dp))
            .background(pillBackground)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .semantics {
                this.selected = selected
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = if (selected) 14.dp else 11.dp)
            .testTag("nav_${item.route}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                SakuCustomNavIcon(
                    kind = item.iconKind,
                    selected = selected,
                    tint = contentTint,
                    cutoutColor = iconCutoutColor,
                    modifier = Modifier.size(21.dp)
                )
                if (badgeColor != null) {
                    Box(
                        modifier = Modifier
                            .offset(x = 3.dp, y = (-2).dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                }
            }

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = tween(180)) + expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut(animationSpec = tween(120)) + shrinkHorizontally(
                    animationSpec = tween(150),
                    shrinkTowards = Alignment.Start
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = activeContentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Custom rounded geometric vector icons matching the reference mockup's clean fintech bottom bar:
 * - HOME: Rounded house silhouette with center horizontal pill notch
 * - ANALYTICS: Rounded square container with 3 crisp vertical equalizer bars
 * - BUDGET: Clean geometric pie/ring arc with detached top-right wedge
 * - WALLET: Rounded horizontal wallet with upper flap line, right clasp tab, and circular coin dot
 */
@Composable
private fun SakuCustomNavIcon(
    kind: SakuNavIconKind,
    selected: Boolean,
    tint: Color,
    cutoutColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = (w * 0.088f).coerceAtLeast(1.6.dp.toPx())

        when (kind) {
            SakuNavIconKind.HOME -> {
                val housePath = Path().apply {
                    moveTo(w * 0.50f, h * 0.11f)
                    cubicTo(w * 0.54f, h * 0.11f, w * 0.88f, h * 0.35f, w * 0.88f, h * 0.44f)
                    lineTo(w * 0.88f, h * 0.78f)
                    cubicTo(w * 0.88f, h * 0.88f, w * 0.82f, h * 0.90f, w * 0.72f, h * 0.90f)
                    lineTo(w * 0.28f, h * 0.90f)
                    cubicTo(w * 0.18f, h * 0.90f, w * 0.12f, h * 0.88f, w * 0.12f, h * 0.78f)
                    lineTo(w * 0.12f, h * 0.44f)
                    cubicTo(w * 0.12f, h * 0.35f, w * 0.46f, h * 0.11f, w * 0.50f, h * 0.11f)
                    close()
                }
                if (selected) {
                    drawPath(path = housePath, color = tint)
                    // Bottom center horizontal pill door dash
                    drawLine(
                        color = cutoutColor,
                        start = Offset(w * 0.41f, h * 0.67f),
                        end = Offset(w * 0.59f, h * 0.67f),
                        strokeWidth = strokeW * 1.15f,
                        cap = StrokeCap.Round
                    )
                } else {
                    drawPath(
                        path = housePath,
                        color = tint,
                        style = Stroke(width = strokeW, join = StrokeJoin.Round, cap = StrokeCap.Round)
                    )
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.41f, h * 0.68f),
                        end = Offset(w * 0.59f, h * 0.68f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }

            SakuNavIconKind.ANALYTICS -> {
                val inset = w * 0.11f
                val boxSize = w - inset * 2f
                val corner = CornerRadius(w * 0.24f, w * 0.24f)
                if (selected) {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(inset, inset),
                        size = Size(boxSize, boxSize),
                        cornerRadius = corner
                    )
                    val barW = strokeW * 1.05f
                    drawLine(
                        color = cutoutColor,
                        start = Offset(w * 0.33f, h * 0.48f),
                        end = Offset(w * 0.33f, h * 0.71f),
                        strokeWidth = barW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = cutoutColor,
                        start = Offset(w * 0.50f, h * 0.31f),
                        end = Offset(w * 0.50f, h * 0.71f),
                        strokeWidth = barW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = cutoutColor,
                        start = Offset(w * 0.67f, h * 0.54f),
                        end = Offset(w * 0.67f, h * 0.71f),
                        strokeWidth = barW,
                        cap = StrokeCap.Round
                    )
                } else {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(inset, inset),
                        size = Size(boxSize, boxSize),
                        cornerRadius = corner,
                        style = Stroke(width = strokeW)
                    )
                    val barW = strokeW * 0.95f
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.33f, h * 0.49f),
                        end = Offset(w * 0.33f, h * 0.70f),
                        strokeWidth = barW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.50f, h * 0.32f),
                        end = Offset(w * 0.50f, h * 0.70f),
                        strokeWidth = barW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.67f, h * 0.55f),
                        end = Offset(w * 0.67f, h * 0.70f),
                        strokeWidth = barW,
                        cap = StrokeCap.Round
                    )
                }
            }

            SakuNavIconKind.BUDGET -> {
                val center = Offset(w * 0.47f, h * 0.53f)
                val r = w * 0.35f
                if (selected) {
                    // Main rounded pie arc + detached top-right slice
                    drawArc(
                        color = tint,
                        startAngle = 12f,
                        sweepAngle = 258f,
                        useCenter = true,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2f, r * 2f)
                    )
                    val wedgeOffset = Offset(w * 0.08f, -h * 0.08f)
                    val wr = w * 0.32f
                    drawArc(
                        color = tint,
                        startAngle = -84f,
                        sweepAngle = 80f,
                        useCenter = true,
                        topLeft = Offset(center.x + wedgeOffset.x - wr, center.y + wedgeOffset.y - wr),
                        size = Size(wr * 2f, wr * 2f)
                    )
                } else {
                    drawArc(
                        color = tint,
                        startAngle = 15f,
                        sweepAngle = 252f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    val wedgeOffset = Offset(w * 0.07f, -h * 0.07f)
                    val wr = w * 0.29f
                    drawArc(
                        color = tint,
                        startAngle = -84f,
                        sweepAngle = 76f,
                        useCenter = true,
                        topLeft = Offset(center.x + wedgeOffset.x - wr, center.y + wedgeOffset.y - wr),
                        size = Size(wr * 2f, wr * 2f),
                        style = Stroke(width = strokeW * 0.9f, join = StrokeJoin.Round, cap = StrokeCap.Round)
                    )
                }
            }

            SakuNavIconKind.WALLET -> {
                val left = w * 0.09f
                val top = h * 0.20f
                val width = w * 0.82f
                val height = h * 0.62f
                val corner = CornerRadius(w * 0.20f, w * 0.20f)
                if (selected) {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = corner
                    )
                    // Subtle upper card slot seam + right clasp indent & coin dot
                    drawLine(
                        color = cutoutColor,
                        start = Offset(w * 0.24f, h * 0.34f),
                        end = Offset(w * 0.56f, h * 0.34f),
                        strokeWidth = strokeW * 0.85f,
                        cap = StrokeCap.Round
                    )
                    // Right clasp pocket notch
                    drawRoundRect(
                        color = cutoutColor,
                        topLeft = Offset(w * 0.60f, h * 0.43f),
                        size = Size(w * 0.33f, h * 0.20f),
                        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
                    )
                    drawCircle(
                        color = tint,
                        radius = w * 0.055f,
                        center = Offset(w * 0.72f, h * 0.53f)
                    )
                } else {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = corner,
                        style = Stroke(width = strokeW)
                    )
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.24f, h * 0.34f),
                        end = Offset(w * 0.56f, h * 0.34f),
                        strokeWidth = strokeW * 0.82f,
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = cutoutColor,
                        topLeft = Offset(w * 0.60f, h * 0.44f),
                        size = Size(w * 0.32f, h * 0.18f),
                        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
                    )
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.60f, h * 0.44f),
                        size = Size(w * 0.32f, h * 0.18f),
                        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
                        style = Stroke(width = strokeW * 0.88f)
                    )
                    drawCircle(
                        color = tint,
                        radius = w * 0.045f,
                        center = Offset(w * 0.71f, h * 0.53f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SakuCenterActionOrb(
    isSheetOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_press_scale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (isSheetOpen) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_icon_rotation"
    )
    val closedContainerColor = extended.lime
    val containerColor by animateColorAsState(
        targetValue = if (isSheetOpen) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            closedContainerColor
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fab_container_color"
    )
    val contentTint by animateColorAsState(
        targetValue = if (isSheetOpen) {
            MaterialTheme.colorScheme.onSurface
        } else {
            extended.onLime
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fab_content_tint"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    bounded = true,
                    color = if (isSheetOpen) MaterialTheme.colorScheme.onSurface else extended.onLime
                ),
                role = Role.Button,
                onClick = onClick
            )
            .testTag("add_transaction_fab"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = if (isSheetOpen) "Tutup Catat Transaksi" else "Catat Transaksi Baru",
            tint = contentTint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    rotationZ = iconRotation
                }
        )
    }
}
