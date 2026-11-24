package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AutoTagSuggestion
import com.example.ai.CategoryAutoTagger
import com.example.ai.FinanceAiService
import com.example.ai.ParsedTransactionDraft
import com.example.auth.authStateFlow
import com.example.data.local.AppSecurityPrefs
import com.example.data.local.BudgetEntity
import com.example.data.local.DanaKuDatabase
import com.example.data.local.DebtDirection
import com.example.data.local.DebtRecordEntity
import com.example.data.local.EmoneyCardRecord
import com.example.data.local.HomeShortcutOption
import com.example.data.local.NotificationPrefs
import com.example.data.local.PersonalizationPrefs
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.UserPreferences
import com.example.data.local.WalletEntity
import com.example.data.remote.CloudSyncStatus
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.remote.SakuSyncManager
import com.example.data.repository.FinanceRepository
import com.example.nfc.EmoneyCardSnapshot
import com.example.notifications.SakuNotificationHelper
import com.example.notifications.SakuReminderScheduler
import com.example.ui.theme.AccentPalettePreset
import com.example.ui.theme.CardCornerStyle
import com.example.ui.theme.ThemeMode
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.util.FinanceCategories
import com.example.util.ReportExporter
import com.example.widget.WidgetStateUpdater
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "FinanceViewModel"

enum class SheetInputTab(val title: String) {
    MANUAL("Manual"),
    AI_MAGIC("AI Magic"),
    VOICE("Suara"),
    SCAN("Scan Struk")
}

enum class BudgetAlertLevel {
    SAFE,
    WARNING_75,
    CRITICAL_90
}

data class TransactionUiItem(
    val entity: TransactionEntity,
    val walletName: String,
    val targetWalletName: String?
)

data class RecurringUiItem(
    val rule: RecurringTransactionEntity,
    val walletName: String
)

data class CashflowSeriesPoint(
    val shortLabel: String,
    val fullLabel: String,
    val income: Long,
    val expense: Long
)

data class BerandaUiState(
    val wallets: List<WalletEntity> = emptyList(),
    val selectedWalletId: Long? = null,
    val searchQuery: String = "",
    val typeFilter: String? = null,
    val netBalance: Long = 0L,
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlyIncomeCount: Int = 0,
    val monthlyExpenseCount: Int = 0,
    val avgDailyExpense: Long = 0L,
    val topExpenseCategory: String? = null,
    val cashflowTrend: List<CashflowSeriesPoint> = emptyList(),
    val monthlyBudgetTotal: Long = 0L,
    val monthlyBudgetSpent: Long = 0L,
    val budgetUsageFraction: Float = 0f,
    val groupedTransactions: List<Pair<String, List<TransactionUiItem>>> = emptyList(),
    val monthLabel: String = DateFormatter.currentMonthLabel()
)

data class CategorySlice(
    val category: String,
    val amount: Long,
    val percentage: Float,
    val color: Color
)

data class BudgetProgressItem(
    val budget: BudgetEntity,
    val spent: Long,
    val ratio: Float,
    val alertLevel: BudgetAlertLevel
)

data class AnggaranUiState(
    val items: List<BudgetProgressItem> = emptyList(),
    val totalLimit: Long = 0L,
    val totalSpent: Long = 0L,
    val monthLabel: String = DateFormatter.currentMonthLabel()
)

data class MonthlyBarPoint(
    val monthKey: String,
    val shortLabel: String,
    val fullLabel: String,
    val income: Long,
    val expense: Long,
    val net: Long,
    val txCount: Int,
    val topExpenseCategory: String?,
    val topExpenseAmount: Long
)

data class MonthlyOverviewUiState(
    val months: List<MonthlyBarPoint> = emptyList(),
    val totalIncome6M: Long = 0L,
    val totalExpense6M: Long = 0L,
    val net6M: Long = 0L,
    val avgMonthlyIncome: Long = 0L,
    val avgMonthlyExpense: Long = 0L,
    val savingsRatePercent: Int = 0
)

sealed interface AiTextState {
    data object Idle : AiTextState
    data object Loading : AiTextState
    data class Streaming(val text: String) : AiTextState
    data class Success(val text: String) : AiTextState
    data class Error(val message: String) : AiTextState
}

data class QaExchange(
    val question: String,
    val answer: String,
    val isError: Boolean = false
)

sealed interface AiParseState {
    data object Idle : AiParseState
    data object Loading : AiParseState
    data class Preview(val drafts: List<ParsedTransactionDraft>) : AiParseState
    data class Error(val message: String) : AiParseState
}

data class TransactionSheetState(
    val isOpen: Boolean = false,
    val activeTab: SheetInputTab = SheetInputTab.MANUAL,
    val editingTransaction: TransactionEntity? = null,
    val initialTimestamp: Long? = null,
    val aiInputText: String = "",
    val aiParseState: AiParseState = AiParseState.Idle,
    val autoStartVoice: Boolean = false
)

enum class OnboardingGateState {
    LOADING,
    NEEDS_ONBOARDING,
    COMPLETED
}

enum class AppLockGateState {
    LOADING,
    LOCKED,
    UNLOCKED
}

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(DanaKuDatabase.getInstance(application))
    private val userPreferences = UserPreferences(application)
    private val remoteSyncRepository = FirestoreSyncRepository(application)
    private val syncManager = SakuSyncManager(
        localRepository = repository,
        remoteRepository = remoteSyncRepository,
        userPreferences = userPreferences,
        scope = viewModelScope
    )
    private val aiService = FinanceAiService()

    val syncStatus: StateFlow<CloudSyncStatus> = syncManager.status

    private val _selectedWalletId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<String?>(null)

    val themeMode: StateFlow<ThemeMode> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.LIGHT)

    val hideBalance: StateFlow<Boolean> = userPreferences.hideBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val personalization: StateFlow<PersonalizationPrefs> = userPreferences.personalization
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalizationPrefs())

    val securityPrefs: StateFlow<AppSecurityPrefs> = userPreferences.securityPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSecurityPrefs())

    val notificationPrefs: StateFlow<NotificationPrefs> = userPreferences.notificationPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationPrefs())

    val emoneyHistory: StateFlow<List<EmoneyCardRecord>> = userPreferences.emoneyHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _onboardingState = MutableStateFlow(OnboardingGateState.LOADING)
    val onboardingState: StateFlow<OnboardingGateState> = _onboardingState.asStateFlow()

    private val _lockGateState = MutableStateFlow(AppLockGateState.LOADING)
    val lockGateState: StateFlow<AppLockGateState> = _lockGateState.asStateFlow()

    private val _lockSessionId = MutableStateFlow(0)
    val lockSessionId: StateFlow<Int> = _lockSessionId.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _pinCooldownSeconds = MutableStateFlow(0)
    val pinCooldownSeconds: StateFlow<Int> = _pinCooldownSeconds.asStateFlow()

    private var failedPinAttempts = 0
    private var pinCooldownJob: Job? = null

    private val _sheetState = MutableStateFlow(TransactionSheetState())
    val sheetState: StateFlow<TransactionSheetState> = _sheetState.asStateFlow()

    private val _insightsState = MutableStateFlow<AiTextState>(AiTextState.Idle)
    val insightsState: StateFlow<AiTextState> = _insightsState.asStateFlow()

    private val _qaActiveState = MutableStateFlow<AiTextState>(AiTextState.Idle)
    val qaActiveState: StateFlow<AiTextState> = _qaActiveState.asStateFlow()

    private val _qaHistory = MutableStateFlow<List<QaExchange>>(emptyList())
    val qaHistory: StateFlow<List<QaExchange>> = _qaHistory.asStateFlow()

    private val _uiNotice = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiNotice: SharedFlow<String> = _uiNotice.asSharedFlow()

    private var insightsJob: Job? = null
    private var qaJob: Job? = null
    private var parseJob: Job? = null

    val wallets: StateFlow<List<WalletEntity>> = repository.allWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allTransactions = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTransactionItems: StateFlow<List<TransactionUiItem>> = combine(
        allTransactions,
        wallets
    ) { txList, walletList ->
        val walletMap = walletList.associateBy { it.id }
        txList.map { tx ->
            TransactionUiItem(
                entity = tx,
                walletName = walletMap[tx.walletId]?.name ?: "Dompet",
                targetWalletName = tx.targetWalletId?.let { walletMap[it]?.name }
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val currentBudgets = repository.observeBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allRecurring = repository.allRecurringTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recurringItems: StateFlow<List<RecurringUiItem>> = combine(
        allRecurring,
        wallets
    ) { rules, walletList ->
        val walletMap = walletList.associateBy { it.id }
        rules.map { rule ->
            RecurringUiItem(
                rule = rule,
                walletName = walletMap[rule.walletId]?.name ?: "Dompet"
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val debts: StateFlow<List<DebtRecordEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val berandaUiState: StateFlow<BerandaUiState> = combine(
        combine(wallets, allTransactions, currentBudgets) { w, t, b -> Triple(w, t, b) },
        combine(_selectedWalletId, _searchQuery, _typeFilter) { wid, q, tf -> Triple(wid, q, tf) },
        personalization
    ) { (walletList, txList, budgetList), (selectedId, query, activeTypeFilter), prefs ->
        val walletMap = walletList.associateBy { it.id }
        val filteredNetBalance = if (selectedId == null) {
            walletList.sumOf { it.balance }
        } else {
            walletMap[selectedId]?.balance ?: 0L
        }

        val now = System.currentTimeMillis()
        val (monthStart, monthEnd) = DateFormatter.cycleBounds(now, prefs.billingCycleStartDay)
        val walletFilteredTx = if (selectedId == null) {
            txList
        } else {
            txList.filter { it.walletId == selectedId || it.targetWalletId == selectedId }
        }

        val monthTx = walletFilteredTx.filter { it.timestamp in monthStart until monthEnd }
        val incomeMonthTx = monthTx.filter {
            it.type == TransactionType.INCOME ||
                (selectedId != null && it.type == TransactionType.TRANSFER && it.targetWalletId == selectedId)
        }
        val expenseMonthTx = monthTx.filter {
            it.type == TransactionType.EXPENSE ||
                (selectedId != null && it.type == TransactionType.TRANSFER && it.walletId == selectedId)
        }
        val monthlyIncome = incomeMonthTx.sumOf { it.amount }
        val monthlyExpense = expenseMonthTx.sumOf { it.amount }
        val elapsedCycleDays = (((now - monthStart).coerceAtLeast(0L) / 86_400_000L) + 1L).coerceAtLeast(1L)
        val avgDailyExpense = monthlyExpense / elapsedCycleDays
        val topExpenseCategory = expenseMonthTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .maxByOrNull { (_, items) -> items.sumOf { it.amount } }
            ?.key

        val localeId = Locale.Builder().setLanguage("in").setRegion("ID").build()
        val shortDayFmt = SimpleDateFormat("dd", localeId)
        val fullDayFmt = SimpleDateFormat("dd MMM", localeId)
        val todayStart = DateFormatter.startOfDay(now)
        val cashflowTrend = (6 downTo 0).map { daysAgo ->
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }.timeInMillis
            val dayEnd = Calendar.getInstance().apply {
                timeInMillis = dayStart
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            val dayTx = walletFilteredTx.filter { it.timestamp in dayStart until dayEnd }
            val dayIncome = dayTx
                .filter {
                    it.type == TransactionType.INCOME ||
                        (selectedId != null && it.type == TransactionType.TRANSFER && it.targetWalletId == selectedId)
                }
                .sumOf { it.amount }
            val dayExpense = dayTx
                .filter {
                    it.type == TransactionType.EXPENSE ||
                        (selectedId != null && it.type == TransactionType.TRANSFER && it.walletId == selectedId)
                }
                .sumOf { it.amount }
            CashflowSeriesPoint(
                shortLabel = shortDayFmt.format(Date(dayStart)),
                fullLabel = if (daysAgo == 0) "Hari ini" else fullDayFmt.format(Date(dayStart)),
                income = dayIncome,
                expense = dayExpense
            )
        }

        val totalBudget = budgetList.sumOf { it.monthlyLimit }
        val globalMonthExpense = txList
            .filter { it.timestamp in monthStart until monthEnd && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val budgetFraction = if (totalBudget > 0L) {
            (globalMonthExpense.toFloat() / totalBudget.toFloat()).coerceAtLeast(0f)
        } else {
            0f
        }

        val cleanQuery = query.trim()
        val displayedTx = walletFilteredTx.filter { tx ->
            val matchesType = activeTypeFilter == null || tx.type == activeTypeFilter
            val matchesQuery = cleanQuery.isEmpty() ||
                tx.merchant.contains(cleanQuery, ignoreCase = true) ||
                tx.category.contains(cleanQuery, ignoreCase = true) ||
                tx.note.contains(cleanQuery, ignoreCase = true) ||
                (walletMap[tx.walletId]?.name?.contains(cleanQuery, ignoreCase = true) == true)
            matchesType && matchesQuery
        }

        val grouped = displayedTx
            .map { tx ->
                TransactionUiItem(
                    entity = tx,
                    walletName = walletMap[tx.walletId]?.name ?: "Dompet",
                    targetWalletName = tx.targetWalletId?.let { walletMap[it]?.name }
                )
            }
            .groupBy { item -> DateFormatter.formatDateHeader(item.entity.timestamp, now) }
            .entries
            .map { it.key to it.value }

        BerandaUiState(
            wallets = walletList,
            selectedWalletId = selectedId,
            searchQuery = query,
            typeFilter = activeTypeFilter,
            netBalance = filteredNetBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlyIncomeCount = incomeMonthTx.size,
            monthlyExpenseCount = expenseMonthTx.size,
            avgDailyExpense = avgDailyExpense,
            topExpenseCategory = topExpenseCategory,
            cashflowTrend = cashflowTrend,
            monthlyBudgetTotal = totalBudget,
            monthlyBudgetSpent = globalMonthExpense,
            budgetUsageFraction = budgetFraction,
            groupedTransactions = grouped,
            monthLabel = DateFormatter.cycleLabel(now, prefs.billingCycleStartDay)
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BerandaUiState())

    val categorySlices: StateFlow<List<CategorySlice>> = combine(
        allTransactions,
        currentBudgets,
        personalization
    ) { txList, _, prefs ->
        val (monthStart, monthEnd) = DateFormatter.cycleBounds(startDay = prefs.billingCycleStartDay)
        val monthExpenses = txList.filter {
            it.timestamp in monthStart until monthEnd && it.type == TransactionType.EXPENSE
        }
        val total = monthExpenses.sumOf { it.amount }.coerceAtLeast(1L)
        monthExpenses
            .groupBy { it.category }
            .map { (cat, items) ->
                val sum = items.sumOf { it.amount }
                CategorySlice(
                    category = cat,
                    amount = sum,
                    percentage = (sum.toFloat() / total.toFloat()) * 100f,
                    color = FinanceCategories.getMeta(cat).color
                )
            }
            .sortedByDescending { it.amount }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val anggaranUiState: StateFlow<AnggaranUiState> = combine(
        currentBudgets,
        allTransactions,
        personalization
    ) { budgetList, txList, prefs ->
        val now = System.currentTimeMillis()
        val (monthStart, monthEnd) = DateFormatter.cycleBounds(now, prefs.billingCycleStartDay)
        val spentByCategory = txList
            .filter { it.timestamp in monthStart until monthEnd && it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val warningThreshold = (prefs.budgetAlertPercent / 100f).coerceIn(0.50f, 0.90f)

        val items = budgetList.map { budget ->
            val spent = spentByCategory[budget.category] ?: 0L
            val ratio = if (budget.monthlyLimit > 0L) {
                spent.toFloat() / budget.monthlyLimit.toFloat()
            } else {
                0f
            }
            val alertLevel = when {
                ratio >= 0.90f -> BudgetAlertLevel.CRITICAL_90
                ratio >= warningThreshold -> BudgetAlertLevel.WARNING_75
                else -> BudgetAlertLevel.SAFE
            }
            BudgetProgressItem(
                budget = budget,
                spent = spent,
                ratio = ratio,
                alertLevel = alertLevel
            )
        }.sortedByDescending { it.ratio }

        AnggaranUiState(
            items = items,
            totalLimit = items.sumOf { it.budget.monthlyLimit },
            totalSpent = items.sumOf { it.spent },
            monthLabel = DateFormatter.cycleLabel(now, prefs.billingCycleStartDay)
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnggaranUiState())

    val monthlyOverviewState: StateFlow<MonthlyOverviewUiState> = allTransactions
        .combine(wallets) { txList, _ ->
            buildMonthlyOverviewState(txList)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyOverviewUiState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val sec = userPreferences.securityPrefs.first()
            if (sec.appLockEnabled) {
                _isAppUnlocked.value = false
                _lockGateState.value = AppLockGateState.LOCKED
                _lockSessionId.update { it + 1 }
            } else {
                _isAppUnlocked.value = true
                _lockGateState.value = AppLockGateState.UNLOCKED
            }

            val completedOnboarding = userPreferences.getHasCompletedOnboarding()
            val walletCount = repository.walletCountOnce()
            val isCompleted = if (!completedOnboarding && walletCount == 0) {
                _onboardingState.value = OnboardingGateState.NEEDS_ONBOARDING
                false
            } else {
                if (!completedOnboarding && walletCount > 0) {
                    userPreferences.setHasCompletedOnboarding(true)
                }
                if (walletCount == 0) {
                    repository.ensureSeeded()
                }
                _onboardingState.value = OnboardingGateState.COMPLETED
                true
            }

            launch {
                SakuNotificationHelper.ensureChannels(getApplication())
                if (isCompleted) {
                    val created = repository.processDueRecurringTransactions()
                    if (created > 0) {
                        _uiNotice.tryEmit("$created transaksi berulang otomatis dicatat")
                    }
                }
                val notif = userPreferences.notificationPrefs.first()
                SakuReminderScheduler.syncSchedule(
                    context = getApplication(),
                    enabled = notif.dailyReminderEnabled,
                    budgetAlertsEnabled = notif.budgetAlertNotifEnabled,
                    hour = notif.reminderHour,
                    minute = notif.reminderMinute
                )
            }

            launch {
                delay(450)
                syncManager.start(Firebase.auth.authStateFlow())
                if (isCompleted) {
                    WidgetStateUpdater.requestUpdate(getApplication())
                }
            }
        }
    }

    fun syncCloudNow() {
        viewModelScope.launch {
            val result = syncManager.syncNow()
            result.onSuccess {
                WidgetStateUpdater.requestUpdate(getApplication())
                _uiNotice.tryEmit("Database lokal berhasil disinkronkan ke cloud")
            }.onFailure { err ->
                _uiNotice.tryEmit(err.localizedMessage ?: "Sinkronisasi cloud tertunda")
            }
        }
    }

    fun onGoogleSignInSucceeded() {
        _uiNotice.tryEmit("Akun Google terhubung. Sinkronisasi aktif")
    }

    fun onGoogleSignInError(message: String) {
        _uiNotice.tryEmit(message)
    }

    fun onSignOutCompleted() {
        viewModelScope.launch {
            userPreferences.clearSyncInfo()
            _uiNotice.tryEmit("Keluar dari sinkronisasi cloud. Data lokal tetap aman di HP.")
        }
    }

    fun processDueRecurringNow() {
        if (_onboardingState.value != OnboardingGateState.COMPLETED) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureSeeded()
            val created = repository.processDueRecurringTransactions()
            if (created > 0) {
                _uiNotice.tryEmit("$created transaksi berulang otomatis dicatat")
            }
        }
    }

    fun selectWalletFilter(walletId: Long?) {
        _selectedWalletId.value = walletId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: String?) {
        _typeFilter.value = type
    }

    fun cycleThemeMode() {
        val next = when (themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun toggleHideBalance() {
        setHideBalance(!hideBalance.value)
    }

    fun setHideBalance(hidden: Boolean) {
        viewModelScope.launch {
            userPreferences.setHideBalance(hidden)
            WidgetStateUpdater.requestUpdate(getApplication())
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            userPreferences.setDisplayName(name)
        }
    }

    fun setAccentPreset(preset: AccentPalettePreset) {
        viewModelScope.launch {
            userPreferences.setAccentPreset(preset)
        }
    }

    fun setCardCornerStyle(style: CardCornerStyle) {
        viewModelScope.launch {
            userPreferences.setCardCornerStyle(style)
        }
    }

    fun setShowWalletsOnHome(show: Boolean) {
        viewModelScope.launch {
            userPreferences.setShowWalletsOnHome(show)
        }
    }

    fun setShowBudgetOnHome(show: Boolean) {
        viewModelScope.launch {
            userPreferences.setShowBudgetOnHome(show)
        }
    }

    fun setCompactCurrencyOnHome(compact: Boolean) {
        viewModelScope.launch {
            userPreferences.setCompactCurrencyOnHome(compact)
        }
    }

    fun setHomeShortcuts(shortcuts: List<HomeShortcutOption>) {
        viewModelScope.launch {
            userPreferences.setHomeShortcuts(shortcuts)
        }
    }

    fun setDefaultWalletId(walletId: Long?) {
        viewModelScope.launch {
            userPreferences.setDefaultWalletId(walletId)
        }
    }

    fun setDefaultFabTab(tab: SheetInputTab) {
        viewModelScope.launch {
            userPreferences.setDefaultFabTab(tab.name)
        }
    }

    fun setBudgetAlertPercent(percent: Int) {
        viewModelScope.launch {
            userPreferences.setBudgetAlertPercent(percent)
        }
    }

    fun setBillingCycleStartDay(day: Int) {
        viewModelScope.launch {
            userPreferences.setBillingCycleStartDay(day)
        }
    }

    fun resetPersonalization() {
        viewModelScope.launch {
            userPreferences.resetPersonalization()
            _uiNotice.tryEmit("Pengaturan personalisasi dikembalikan ke awal")
        }
    }

    // --- Transaction Sheet Controls ---

    fun openTransactionSheet(
        tab: SheetInputTab = SheetInputTab.MANUAL,
        prefillText: String = "",
        autoTriggerAi: Boolean = false,
        autoStartVoice: Boolean = false,
        editingTx: TransactionEntity? = null,
        initialTimestamp: Long? = null
    ) {
        _sheetState.value = TransactionSheetState(
            isOpen = true,
            activeTab = if (editingTx != null) SheetInputTab.MANUAL else tab,
            editingTransaction = editingTx,
            initialTimestamp = initialTimestamp,
            aiInputText = prefillText,
            aiParseState = AiParseState.Idle,
            autoStartVoice = autoStartVoice
        )
        if (autoTriggerAi && prefillText.isNotBlank()) {
            parseAiText(prefillText)
        }
    }

    fun closeTransactionSheet() {
        parseJob?.cancel()
        _sheetState.update {
            it.copy(
                isOpen = false,
                editingTransaction = null,
                initialTimestamp = null,
                autoStartVoice = false
            )
        }
    }

    fun selectSheetTab(tab: SheetInputTab) {
        _sheetState.update {
            it.copy(
                activeTab = tab,
                autoStartVoice = false,
                aiParseState = if (tab == SheetInputTab.MANUAL) AiParseState.Idle else it.aiParseState
            )
        }
    }

    fun updateAiInputText(text: String) {
        _sheetState.update { it.copy(aiInputText = text) }
    }

    fun clearAiPreview() {
        _sheetState.update { it.copy(aiParseState = AiParseState.Idle) }
    }

    suspend fun suggestCategoryForInput(
        description: String,
        txType: String,
        allowCloudFallback: Boolean = true
    ): AutoTagSuggestion? {
        if (txType == TransactionType.TRANSFER) return null
        val clean = description.trim()
        if (clean.length < 2) return null
        return try {
            aiService.suggestCategoryWithAi(
                description = clean,
                txType = txType,
                history = allTransactions.value,
                allowCloudFallback = allowCloudFallback
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Auto-tagging failed, falling back to local check", e)
            CategoryAutoTagger.suggestLocally(clean, txType, allTransactions.value)
        }
    }

    fun parseAiText(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _sheetState.update { it.copy(aiInputText = trimmed, aiParseState = AiParseState.Loading) }
            try {
                val walletNames = wallets.value.map { it.name }
                val drafts = aiService.parseNaturalLanguage(trimmed, walletNames, allTransactions.value)
                _sheetState.update { it.copy(aiParseState = AiParseState.Preview(drafts)) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI text parsing timed out", e)
                _sheetState.update {
                    it.copy(aiParseState = AiParseState.Error("Waktu habis (45 detik). Periksa koneksi lalu coba lagi."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI text parsing failed", e)
                _sheetState.update {
                    it.copy(aiParseState = AiParseState.Error(formatAiError(e, "Gagal memproses teks dengan AI.")))
                }
            }
        }
    }

    fun parseReceiptBitmap(bitmap: Bitmap) {
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _sheetState.update { it.copy(aiParseState = AiParseState.Loading) }
            try {
                val walletNames = wallets.value.map { it.name }
                val drafts = aiService.parseReceiptImage(bitmap, walletNames, allTransactions.value)
                _sheetState.update { it.copy(aiParseState = AiParseState.Preview(drafts)) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt scanning timed out", e)
                _sheetState.update {
                    it.copy(aiParseState = AiParseState.Error("Scan struk kehabisan waktu. Coba foto yang lebih ringkas."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Receipt scanning failed", e)
                _sheetState.update {
                    it.copy(aiParseState = AiParseState.Error(formatAiError(e, "Gagal membaca struk dengan AI.")))
                }
            }
        }
    }

    fun parseVoiceAudio(audioBytes: ByteArray, mimeType: String = "audio/mp4") {
        if (audioBytes.isEmpty()) return
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _sheetState.update { it.copy(aiParseState = AiParseState.Loading) }
            try {
                val walletNames = wallets.value.map { it.name }
                val drafts = aiService.parseAudioTransaction(audioBytes, mimeType, walletNames, allTransactions.value)
                _sheetState.update { it.copy(aiParseState = AiParseState.Preview(drafts)) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Voice audio parsing timed out", e)
                _sheetState.update {
                    it.copy(aiParseState = AiParseState.Error("Pemrosesan suara kehabisan waktu. Coba ucapkan lebih singkat."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice audio parsing failed", e)
                _sheetState.update {
                    it.copy(aiParseState = AiParseState.Error(formatAiError(e, "Gagal memproses rekaman suara dengan AI.")))
                }
            }
        }
    }

    fun updateDraft(updated: ParsedTransactionDraft) {
        _sheetState.update { state ->
            val current = state.aiParseState
            if (current is AiParseState.Preview) {
                val newList = current.drafts.map { if (it.localId == updated.localId) updated else it }
                state.copy(aiParseState = AiParseState.Preview(newList))
            } else {
                state
            }
        }
    }

    fun removeDraft(localId: String) {
        _sheetState.update { state ->
            val current = state.aiParseState
            if (current is AiParseState.Preview) {
                val remaining = current.drafts.filterNot { it.localId == localId }
                if (remaining.isEmpty()) {
                    state.copy(aiParseState = AiParseState.Idle)
                } else {
                    state.copy(aiParseState = AiParseState.Preview(remaining))
                }
            } else {
                state
            }
        }
    }

    private fun ensureSavedTransactionVisible(walletId: Long, targetWalletId: Long?, type: String) {
        if (_selectedWalletId.value != null &&
            _selectedWalletId.value != walletId &&
            _selectedWalletId.value != targetWalletId
        ) {
            _selectedWalletId.value = null
        }
        if (_typeFilter.value != null && _typeFilter.value != type) {
            _typeFilter.value = null
        }
        if (_searchQuery.value.isNotBlank()) {
            _searchQuery.value = ""
        }
    }

    fun saveDraftsToDatabase(drafts: List<ParsedTransactionDraft>) {
        viewModelScope.launch {
            repository.ensureSeeded()
            val entities = drafts.mapNotNull { draft ->
                if (draft.amount <= 0L) return@mapNotNull null
                val wallet = repository.resolveWalletByName(draft.walletName)
                    ?: return@mapNotNull null
                TransactionEntity(
                    merchant = draft.merchant.trim().ifBlank { "Transaksi" },
                    amount = draft.amount,
                    type = draft.type,
                    category = draft.category,
                    walletId = wallet.id,
                    note = draft.note.trim(),
                    timestamp = System.currentTimeMillis(),
                    isAiGenerated = true
                )
            }
            if (entities.isNotEmpty()) {
                userPreferences.markCustomLocalData()
                repository.insertTransactions(entities)
                val first = entities.first()
                ensureSavedTransactionVisible(first.walletId, null, first.type)
                if (entities.size > 1 && entities.map { it.walletId }.distinct().size > 1) {
                    _selectedWalletId.value = null
                }
                val msg = if (entities.size == 1) {
                    "\"${first.merchant}\" disimpan ke riwayat"
                } else {
                    "${entities.size} transaksi disimpan ke riwayat"
                }
                _uiNotice.tryEmit(msg)
                WidgetStateUpdater.requestUpdate(getApplication())
            }
            closeTransactionSheet()
        }
    }

    fun saveManualTransaction(
        editingOld: TransactionEntity?,
        merchant: String,
        amount: Long,
        type: String,
        category: String,
        walletId: Long,
        targetWalletId: Long?,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0L) return
        viewModelScope.launch {
            repository.ensureSeeded()
            val validWalletId = repository.resolveWalletByName(
                wallets.value.firstOrNull { it.id == walletId }?.name.orEmpty()
            )?.takeIf { wallets.value.any { w -> w.id == walletId } }?.id
                ?: wallets.value.firstOrNull()?.id
                ?: walletId

            if (editingOld != null) {
                userPreferences.markCustomLocalData()
                val updated = editingOld.copy(
                    merchant = merchant.trim().ifBlank { category },
                    amount = amount,
                    type = type,
                    category = if (type == TransactionType.TRANSFER) "Transfer" else category,
                    walletId = validWalletId,
                    targetWalletId = if (type == TransactionType.TRANSFER) targetWalletId else null,
                    note = note.trim(),
                    timestamp = timestamp
                )
                repository.updateTransaction(editingOld, updated)
                ensureSavedTransactionVisible(validWalletId, updated.targetWalletId, type)
                _uiNotice.tryEmit("Perubahan \"${updated.merchant}\" disimpan")
            } else if (type == TransactionType.TRANSFER && targetWalletId != null) {
                userPreferences.markCustomLocalData()
                repository.transferBetweenWallets(
                    fromWalletId = validWalletId,
                    toWalletId = targetWalletId,
                    amount = amount,
                    note = note.trim()
                )
                ensureSavedTransactionVisible(validWalletId, targetWalletId, type)
                _uiNotice.tryEmit("Transfer ${CurrencyFormatter.formatRupiah(amount)} dicatat ke riwayat")
            } else {
                userPreferences.markCustomLocalData()
                val resolvedMerchant = merchant.trim().ifBlank { category }
                repository.insertTransaction(
                    TransactionEntity(
                        merchant = resolvedMerchant,
                        amount = amount,
                        type = type,
                        category = category,
                        walletId = validWalletId,
                        note = note.trim(),
                        timestamp = timestamp
                    )
                )
                ensureSavedTransactionVisible(validWalletId, null, type)
                _uiNotice.tryEmit("\"$resolvedMerchant\" (${CurrencyFormatter.formatRupiah(amount)}) disimpan")
            }
            WidgetStateUpdater.requestUpdate(getApplication())
            closeTransactionSheet()
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.deleteTransaction(tx)
            WidgetStateUpdater.requestUpdate(getApplication())
        }
    }

    fun restoreTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.insertTransaction(tx.copy(id = 0))
            WidgetStateUpdater.requestUpdate(getApplication())
            _uiNotice.tryEmit("Transaksi \"${tx.merchant}\" dipulihkan")
        }
    }

    // --- Budgets & Wallets ---

    fun upsertBudget(category: String, monthlyLimit: Long) {
        if (monthlyLimit <= 0L) return
        viewModelScope.launch {
            repository.ensureSeeded()
            userPreferences.markCustomLocalData()
            repository.upsertBudget(category, monthlyLimit)
            _uiNotice.tryEmit("Anggaran $category diperbarui")
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.deleteBudget(budget)
            _uiNotice.tryEmit("Anggaran ${budget.category} dihapus")
        }
    }

    fun addWallet(name: String, walletType: String, initialBalance: Long, accentHex: Long, hint: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.addWallet(name, walletType, initialBalance, accentHex, hint)
            WidgetStateUpdater.requestUpdate(getApplication())
            _uiNotice.tryEmit("Dompet \"${name.trim()}\" ditambahkan")
        }
    }

    fun updateWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.updateWallet(wallet)
            WidgetStateUpdater.requestUpdate(getApplication())
            _uiNotice.tryEmit("Dompet \"${wallet.name}\" diperbarui")
        }
    }

    suspend fun countTransactionsForWallet(walletId: Long): Int =
        repository.countTransactionsForWallet(walletId)

    fun deleteWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            val deleted = repository.deleteWalletSafely(wallet)
            if (deleted) {
                if (_selectedWalletId.value == wallet.id) {
                    _selectedWalletId.value = null
                }
                WidgetStateUpdater.requestUpdate(getApplication())
                _uiNotice.tryEmit("Dompet \"${wallet.name}\" dihapus")
            } else {
                _uiNotice.tryEmit("Minimal harus menyisakan 1 dompet aktif")
            }
        }
    }

    fun transferBetweenWallets(fromId: Long, toId: Long, amount: Long, note: String) {
        viewModelScope.launch {
            repository.ensureSeeded()
            userPreferences.markCustomLocalData()
            repository.transferBetweenWallets(fromId, toId, amount, note)
            ensureSavedTransactionVisible(fromId, toId, TransactionType.TRANSFER)
            WidgetStateUpdater.requestUpdate(getApplication())
            _uiNotice.tryEmit("Transfer ${CurrencyFormatter.formatRupiah(amount)} berhasil")
        }
    }

    // --- Recurring Transactions ---

    fun saveRecurringRule(
        editingRule: RecurringTransactionEntity?,
        merchant: String,
        amount: Long,
        type: String,
        category: String,
        walletId: Long,
        frequency: String,
        startDate: Long,
        endDate: Long?,
        note: String
    ) {
        if (amount <= 0L) return
        viewModelScope.launch {
            val normalizedStart = DateFormatter.noonOfDay(startDate)
            val normalizedEnd = endDate?.let { DateFormatter.endOfDay(it) }
            val nextDue = if (editingRule != null) {
                if (editingRule.frequency != frequency || DateFormatter.startOfDay(editingRule.startDate) != DateFormatter.startOfDay(normalizedStart)) {
                    normalizedStart
                } else {
                    editingRule.nextDueDate
                }
            } else {
                normalizedStart
            }
            val rule = RecurringTransactionEntity(
                id = editingRule?.id ?: 0L,
                merchant = merchant.trim().ifBlank { category },
                amount = amount,
                type = type,
                category = category,
                walletId = walletId,
                note = note.trim(),
                frequency = frequency,
                startDate = normalizedStart,
                endDate = normalizedEnd,
                nextDueDate = nextDue,
                lastProcessedDate = editingRule?.lastProcessedDate,
                isActive = editingRule?.isActive ?: true
            )
            repository.ensureSeeded()
            userPreferences.markCustomLocalData()
            val created = repository.upsertRecurringRule(rule)
            if (created > 0) {
                _uiNotice.tryEmit("$created transaksi berulang otomatis dicatat sesuai jadwal")
            } else {
                _uiNotice.tryEmit("Jadwal berulang \"${rule.merchant}\" disimpan")
            }
        }
    }

    fun toggleRecurringActive(rule: RecurringTransactionEntity) {
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            val created = repository.toggleRecurringActive(rule)
            if (created > 0) {
                _uiNotice.tryEmit("$created transaksi berulang otomatis dicatat")
            }
        }
    }

    fun deleteRecurringRule(rule: RecurringTransactionEntity) {
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.deleteRecurringRule(rule)
            _uiNotice.tryEmit("Jadwal berulang \"${rule.merchant}\" dihapus")
        }
    }

    // --- Savings Goals ---

    fun saveSavingsGoal(
        editingGoal: SavingsGoalEntity?,
        title: String,
        targetAmount: Long,
        savedAmount: Long,
        deadlineMillis: Long?,
        accentHex: Long,
        iconName: String,
        note: String
    ) {
        if (title.isBlank() || targetAmount <= 0L) return
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            val entity = (editingGoal ?: SavingsGoalEntity(
                title = title.trim(),
                targetAmount = targetAmount
            )).copy(
                title = title.trim(),
                targetAmount = targetAmount,
                savedAmount = savedAmount.coerceAtLeast(0L),
                deadlineMillis = deadlineMillis,
                accentHex = accentHex,
                iconName = iconName,
                note = note.trim()
            )
            repository.upsertSavingsGoal(entity)
            _uiNotice.tryEmit("Target tabungan \"${entity.title}\" disimpan")
        }
    }

    fun contributeToSavingsGoal(
        goal: SavingsGoalEntity,
        amount: Long,
        walletId: Long?,
        isWithdraw: Boolean = false
    ) {
        if (amount <= 0L) return
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.contributeToSavingsGoal(goal, amount, walletId, isWithdraw)
            WidgetStateUpdater.requestUpdate(getApplication())
            val verb = if (isWithdraw) "Penarikan" else "Setoran"
            _uiNotice.tryEmit("$verb ${CurrencyFormatter.formatRupiah(amount)} untuk \"${goal.title}\" dicatat")
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.deleteSavingsGoal(goal)
            _uiNotice.tryEmit("Target \"${goal.title}\" dihapus")
        }
    }

    // --- Debts & Receivables ---

    fun saveDebtRecord(
        editingDebt: DebtRecordEntity?,
        personName: String,
        amount: Long,
        direction: String,
        walletId: Long?,
        dueDateMillis: Long?,
        note: String
    ) {
        if (personName.isBlank() || amount <= 0L) return
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            val entity = (editingDebt ?: DebtRecordEntity(
                personName = personName.trim(),
                amount = amount,
                direction = direction
            )).copy(
                personName = personName.trim(),
                amount = amount,
                direction = direction,
                walletId = walletId,
                dueDateMillis = dueDateMillis,
                note = note.trim()
            )
            repository.upsertDebtRecord(entity)
            val label = if (direction == DebtDirection.LEND) "Piutang" else "Hutang"
            _uiNotice.tryEmit("$label \"${entity.personName}\" disimpan")
        }
    }

    fun recordDebtPayment(debt: DebtRecordEntity, paymentAmount: Long, walletId: Long?) {
        if (paymentAmount <= 0L) return
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.recordDebtPayment(debt, paymentAmount, walletId)
            WidgetStateUpdater.requestUpdate(getApplication())
            val remaining = (debt.amount - debt.paidAmount - paymentAmount).coerceAtLeast(0L)
            val msg = if (remaining == 0L) {
                "Catatan dengan ${debt.personName} lunas!"
            } else {
                "Pembayaran ${CurrencyFormatter.formatRupiah(paymentAmount)} dicatat"
            }
            _uiNotice.tryEmit(msg)
        }
    }

    fun deleteDebtRecord(debt: DebtRecordEntity) {
        viewModelScope.launch {
            userPreferences.markCustomLocalData()
            repository.deleteDebtRecord(debt)
            _uiNotice.tryEmit("Catatan \"${debt.personName}\" dihapus")
        }
    }

    // --- Export Bank Statement PDF / CSV ---

    suspend fun exportStatementPdf(
        selectedWalletId: Long?,
        periodStartMillis: Long?,
        periodEndMillis: Long?,
        periodLabel: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val report = ReportExporter.buildStatementReport(
                allTransactions = allTransactions.value,
                wallets = wallets.value,
                selectedWalletId = selectedWalletId,
                periodStartMillis = periodStartMillis,
                periodEndMillis = periodEndMillis,
                periodLabel = periodLabel
            )
            ReportExporter.exportPdfToFileUri(
                context = getApplication(),
                report = report,
                accountHolderName = personalization.value.displayName
            )
        }
    }

    suspend fun exportStatementCsv(
        selectedWalletId: Long?,
        periodStartMillis: Long?,
        periodEndMillis: Long?,
        periodLabel: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val report = ReportExporter.buildStatementReport(
                allTransactions = allTransactions.value,
                wallets = wallets.value,
                selectedWalletId = selectedWalletId,
                periodStartMillis = periodStartMillis,
                periodEndMillis = periodEndMillis,
                periodLabel = periodLabel
            )
            ReportExporter.exportCsvToFileUri(
                context = getApplication(),
                report = report
            )
        }
    }

    // --- App Lock Security ---

    suspend fun verifyPinAndUnlock(rawPin: String): Boolean {
        if (_pinCooldownSeconds.value > 0) return false
        val ok = userPreferences.verifyPin(rawPin)
        if (ok) {
            failedPinAttempts = 0
            pinCooldownJob?.cancel()
            _pinCooldownSeconds.value = 0
            _isAppUnlocked.value = true
            _lockGateState.value = AppLockGateState.UNLOCKED
        } else {
            failedPinAttempts += 1
            if (failedPinAttempts >= 5) {
                startPinCooldown(seconds = 30)
            }
        }
        return ok
    }

    private fun startPinCooldown(seconds: Int) {
        pinCooldownJob?.cancel()
        pinCooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _pinCooldownSeconds.value = remaining
                kotlinx.coroutines.delay(1_000L)
            }
            _pinCooldownSeconds.value = 0
            failedPinAttempts = 0
        }
    }

    fun unlockWithBiometric() {
        failedPinAttempts = 0
        pinCooldownJob?.cancel()
        _pinCooldownSeconds.value = 0
        _isAppUnlocked.value = true
        _lockGateState.value = AppLockGateState.UNLOCKED
    }

    fun lockAppIfEnabled() {
        if (securityPrefs.value.appLockEnabled && _lockGateState.value == AppLockGateState.UNLOCKED) {
            _isAppUnlocked.value = false
            _lockGateState.value = AppLockGateState.LOCKED
            _lockSessionId.update { it + 1 }
        }
    }

    fun lockAppOnBackground() {
        val sec = securityPrefs.value
        if (sec.appLockEnabled && sec.lockOnBackground && _lockGateState.value == AppLockGateState.UNLOCKED) {
            _isAppUnlocked.value = false
            _lockGateState.value = AppLockGateState.LOCKED
            _lockSessionId.update { it + 1 }
        }
    }

    fun enableBiometricLockOnly() {
        viewModelScope.launch {
            userPreferences.enableBiometricLock()
            _isAppUnlocked.value = true
            _lockGateState.value = AppLockGateState.UNLOCKED
            _uiNotice.tryEmit("Kunci biometrik saat buka aplikasi diaktifkan")
        }
    }

    fun setupPinLock(rawPin: String, biometricEnabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                userPreferences.setPinAndEnableLock(rawPin, biometricEnabled)
            }.onSuccess {
                _isAppUnlocked.value = true
                _lockGateState.value = AppLockGateState.UNLOCKED
                _uiNotice.tryEmit("Kunci PIN & keamanan aplikasi diaktifkan")
            }.onFailure { err ->
                _uiNotice.tryEmit(err.localizedMessage ?: "PIN harus 4-6 digit angka")
            }
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAppLockEnabled(enabled)
            if (!enabled) {
                _isAppUnlocked.value = true
                _lockGateState.value = AppLockGateState.UNLOCKED
            }
            _uiNotice.tryEmit(if (enabled) "Kunci aplikasi aktif" else "Kunci aplikasi dinonaktifkan")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setBiometricEnabled(enabled)
        }
    }

    fun setLockOnBackground(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setLockOnBackground(enabled)
        }
    }

    fun clearAppLock() {
        viewModelScope.launch {
            userPreferences.clearAppLock()
            _isAppUnlocked.value = true
            _lockGateState.value = AppLockGateState.UNLOCKED
            _uiNotice.tryEmit("PIN & kunci biometrik dihapus")
        }
    }

    // --- NFC E-Money ---

    suspend fun saveEmoneyScan(snapshot: EmoneyCardSnapshot): EmoneyCardRecord {
        return userPreferences.saveEmoneyScan(snapshot)
    }

    fun deleteEmoneyRecord(uidHex: String) {
        viewModelScope.launch {
            userPreferences.deleteEmoneyRecord(uidHex)
            _uiNotice.tryEmit("Riwayat kartu e-money dihapus")
        }
    }

    fun clearEmoneyHistory() {
        viewModelScope.launch {
            userPreferences.clearEmoneyHistory()
            _uiNotice.tryEmit("Semua riwayat cek NFC dibersihkan")
        }
    }

    fun recordEmoneyTransaction(
        card: EmoneyCardRecord,
        walletId: Long,
        amount: Long,
        isExpense: Boolean,
        note: String
    ) {
        if (amount <= 0L) return
        viewModelScope.launch {
            val type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
            val category = if (isExpense) "Transportasi" else "Lainnya"
            val merchant = if (isExpense) {
                "Pakai ${card.issuerName}"
            } else {
                "Top Up ${card.issuerName}"
            }
            repository.ensureSeeded()
            userPreferences.markCustomLocalData()
            repository.insertTransaction(
                TransactionEntity(
                    merchant = merchant,
                    amount = amount,
                    type = type,
                    category = category,
                    walletId = walletId,
                    note = note.trim().ifBlank { "Kartu ${card.cardNumber}" },
                    timestamp = System.currentTimeMillis()
                )
            )
            ensureSavedTransactionVisible(walletId = walletId, targetWalletId = null, type = type)
            WidgetStateUpdater.requestUpdate(getApplication())
            _uiNotice.tryEmit(
                "${if (isExpense) "Pengeluaran" else "Top Up"} ${CurrencyFormatter.formatRupiah(amount)} dicatat"
            )
        }
    }

    // --- Daily Reminder & Smart Notifications ---

    fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferences.setDailyReminder(enabled, hour, minute)
            val currentBudgetNotif = notificationPrefs.value.budgetAlertNotifEnabled
            SakuReminderScheduler.syncSchedule(
                context = getApplication(),
                enabled = enabled,
                budgetAlertsEnabled = currentBudgetNotif,
                hour = hour,
                minute = minute
            )
            if (enabled) {
                val timeStr = String.format(Locale.US, "%02d:%02d", hour, minute)
                _uiNotice.tryEmit("Pengingat harian dijadwalkan pukul $timeStr")
            } else {
                _uiNotice.tryEmit("Pengingat harian dimatikan")
            }
        }
    }

    fun setBudgetAlertNotifEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setBudgetAlertNotifEnabled(enabled)
            val current = notificationPrefs.value
            SakuReminderScheduler.syncSchedule(
                context = getApplication(),
                enabled = current.dailyReminderEnabled,
                budgetAlertsEnabled = enabled,
                hour = current.reminderHour,
                minute = current.reminderMinute
            )
        }
    }

    fun sendTestNotification() {
        SakuNotificationHelper.sendTestNotification(getApplication())
        _uiNotice.tryEmit("Notifikasi uji dikirim")
    }

    // --- Onboarding & Clean Workspace ---

    fun completeOnboardingClean(
        displayName: String,
        billingCycleDay: Int,
        walletName: String,
        walletType: String,
        initialBalance: Long,
        accentHex: Long
    ) {
        viewModelScope.launch {
            userPreferences.setDisplayName(displayName)
            userPreferences.setBillingCycleStartDay(billingCycleDay)
            userPreferences.markCustomLocalData()
            repository.resetAndCreateSingleWallet(
                walletName = walletName,
                walletType = walletType,
                initialBalance = initialBalance,
                accentHex = accentHex
            )
            userPreferences.setHasCompletedOnboarding(true)
            _selectedWalletId.value = null
            _onboardingState.value = OnboardingGateState.COMPLETED
            WidgetStateUpdater.requestUpdate(getApplication())
            _uiNotice.tryEmit("Selamat datang! Dompet pertamamu siap digunakan")
        }
    }

    fun completeOnboardingWithSampleData(
        displayName: String,
        billingCycleDay: Int
    ) {
        viewModelScope.launch {
            userPreferences.setDisplayName(displayName)
            userPreferences.setBillingCycleStartDay(billingCycleDay)
            userPreferences.clearCustomLocalDataFlag()
            repository.ensureSeeded()
            repository.processDueRecurringTransactions()
            userPreferences.setHasCompletedOnboarding(true)
            _selectedWalletId.value = null
            _onboardingState.value = OnboardingGateState.COMPLETED
            WidgetStateUpdater.requestUpdate(getApplication())
            _uiNotice.tryEmit("Data contoh dimuat. Selamat mencoba Saku!")
        }
    }

    fun reopenOnboardingWizard() {
        _onboardingState.value = OnboardingGateState.NEEDS_ONBOARDING
    }

    fun dismissOnboardingIfReady() {
        viewModelScope.launch {
            if (repository.walletCountOnce() > 0) {
                userPreferences.setHasCompletedOnboarding(true)
                _onboardingState.value = OnboardingGateState.COMPLETED
            }
        }
    }

    // --- AI Financial Advisor & Q&A ---

    fun generateMonthlyInsights(forceRefresh: Boolean = false) {
        insightsJob?.cancel()
        insightsJob = viewModelScope.launch {
            _insightsState.value = AiTextState.Loading
            try {
                val contextSummary = buildFinancialContextSummary()
                var finalText = ""
                aiService.streamMonthlyInsights(contextSummary, forceRefresh = forceRefresh).collect { partial ->
                    finalText = partial
                    _insightsState.value = AiTextState.Streaming(partial)
                }
                _insightsState.value = AiTextState.Success(finalText)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Monthly insights timed out", e)
                _insightsState.value = AiTextState.Error("Analisis AI kehabisan waktu. Coba lagi sebentar.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Monthly insights failed", e)
                _insightsState.value = AiTextState.Error(formatAiError(e, "Gagal memuat analisis AI."))
            }
        }
    }

    fun askFinanceQuestion(question: String) {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isBlank()) return
        qaJob?.cancel()
        qaJob = viewModelScope.launch {
            _qaActiveState.value = AiTextState.Loading
            try {
                val contextSummary = buildFinancialContextSummary()
                var finalAnswer = ""
                aiService.streamFinanceQa(cleanQuestion, contextSummary).collect { partial ->
                    finalAnswer = partial
                    _qaActiveState.value = AiTextState.Streaming(partial)
                }
                _qaHistory.update { list ->
                    (listOf(QaExchange(cleanQuestion, finalAnswer)) + list).take(6)
                }
                _qaActiveState.value = AiTextState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Finance Q&A timed out", e)
                _qaActiveState.value = AiTextState.Error("Pertanyaan kehabisan waktu (45 detik).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Finance Q&A failed", e)
                _qaActiveState.value = AiTextState.Error(formatAiError(e, "Gagal menjawab pertanyaan."))
            }
        }
    }

    private fun formatAiError(e: Exception, fallback: String): String {
        val raw = e.localizedMessage?.trim().orEmpty()
        return if (raw.contains("App Check", ignoreCase = true)) {
            "Verifikasi App Check Firebase gagal ($raw). Pastikan Google Play Services aktif dan koneksi internet stabil."
        } else {
            raw.ifBlank { fallback }
        }
    }

    private fun buildFinancialContextSummary(): String {
        val walletList = wallets.value
        val txList = allTransactions.value
        val budgetList = currentBudgets.value
        val (monthStart, monthEnd) = DateFormatter.monthBounds()
        val monthTx = txList.filter { it.timestamp in monthStart until monthEnd }
        val walletMap = walletList.associateBy { it.id }

        val indonesianLocale = Locale.Builder().setLanguage("in").setRegion("ID").build()
        val dateFormat = SimpleDateFormat("dd/MM", indonesianLocale)

        val walletSummary = walletList.joinToString("; ") {
            "${it.name}:${CurrencyFormatter.formatRupiah(it.balance)}"
        }

        val totalIncome = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val categoryBreakdown = monthTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .entries
            .sortedByDescending { it.value.sumOf { tx -> tx.amount } }
            .joinToString("; ") { (cat, list) ->
                "$cat=${CurrencyFormatter.formatRupiah(list.sumOf { it.amount })}(${list.size}x)"
            }

        val budgetSummary = budgetList.joinToString("; ") { b ->
            val spent = monthTx.filter { it.type == TransactionType.EXPENSE && it.category == b.category }.sumOf { it.amount }
            val pct = if (b.monthlyLimit > 0) (spent * 100 / b.monthlyLimit) else 0
            "${b.category}:${CurrencyFormatter.formatRupiah(spent)}/${CurrencyFormatter.formatRupiah(b.monthlyLimit)}($pct%)"
        }

        val recentTx = txList.take(12).joinToString("\n") { tx ->
            val d = dateFormat.format(Date(tx.timestamp))
            val w = walletMap[tx.walletId]?.name ?: "Dompet"
            "$d|${tx.type}|${tx.merchant}|${CurrencyFormatter.formatRupiah(tx.amount)}|${tx.category}|$w"
        }

        return """
            Bln:${DateFormatter.currentMonthLabel()} | Saldo:${CurrencyFormatter.formatRupiah(walletList.sumOf { it.balance })} ($walletSummary)
            ArusKas: Masuk=${CurrencyFormatter.formatRupiah(totalIncome)}, Keluar=${CurrencyFormatter.formatRupiah(totalExpense)}
            Kategori: ${categoryBreakdown.ifBlank { "-" }}
            Anggaran: ${budgetSummary.ifBlank { "-" }}
            12TxTerbaru:
            ${recentTx.ifBlank { "-" }}
        """.trimIndent()
    }

    private fun buildMonthlyOverviewState(txList: List<TransactionEntity>): MonthlyOverviewUiState {
        val locale = Locale.Builder().setLanguage("in").setRegion("ID").build()
        val keyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val shortFmt = SimpleDateFormat("MMM", locale)
        val fullFmt = SimpleDateFormat("MMMM yyyy", locale)

        val points = (5 downTo 0).map { monthsBack ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -monthsBack)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = cal.timeInMillis
            val dateObj = Date(startMs)
            cal.add(Calendar.MONTH, 1)
            val endMs = cal.timeInMillis

            val monthTx = txList.filter { it.timestamp in startMs until endMs }
            val income = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expenseTx = monthTx.filter { it.type == TransactionType.EXPENSE }
            val expense = expenseTx.sumOf { it.amount }
            val topCatEntry = expenseTx
                .groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .maxByOrNull { it.value }

            val shortLabel = shortFmt.format(dateObj)
                .replace(".", "")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            val fullLabel = fullFmt.format(dateObj)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

            MonthlyBarPoint(
                monthKey = keyFmt.format(dateObj),
                shortLabel = shortLabel,
                fullLabel = fullLabel,
                income = income,
                expense = expense,
                net = income - expense,
                txCount = monthTx.size,
                topExpenseCategory = topCatEntry?.key,
                topExpenseAmount = topCatEntry?.value ?: 0L
            )
        }

        val totalInc = points.sumOf { it.income }
        val totalExp = points.sumOf { it.expense }
        val net = totalInc - totalExp
        val activeMonthCount = points.count { it.income > 0L || it.expense > 0L }.coerceAtLeast(1)
        val savingsRate = if (totalInc > 0L) {
            ((net.toDouble() / totalInc.toDouble()) * 100.0).roundToInt().coerceIn(-999, 100)
        } else 0

        return MonthlyOverviewUiState(
            months = points,
            totalIncome6M = totalInc,
            totalExpense6M = totalExp,
            net6M = net,
            avgMonthlyIncome = totalInc / activeMonthCount,
            avgMonthlyExpense = totalExp / activeMonthCount,
            savingsRatePercent = savingsRate
        )
    }
}
