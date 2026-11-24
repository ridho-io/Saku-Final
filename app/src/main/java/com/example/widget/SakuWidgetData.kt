package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import com.example.MainActivity
import com.example.data.local.DanaKuDatabase
import com.example.data.local.TransactionType
import com.example.data.local.UserPreferences
import com.example.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

const val EXTRA_OPEN_TAB = "extra_saku_open_tab"
const val EXTRA_OPEN_ROUTE = "extra_saku_open_route"

enum class SakuWidgetType(
    val title: String,
    val sizeBadge: String,
    val subtitle: String,
    val receiverClass: Class<out AppWidgetProvider>
) {
    BALANCE_HERO(
        title = "Saldo & Arus Kas",
        sizeBadge = "4 × 2",
        subtitle = "Saldo bersih, pemasukan, pengeluaran, serta tombol cepat Catat & AI.",
        receiverClass = SakuBalanceWidgetReceiver::class.java
    ),
    QUICK_CAPTURE(
        title = "Aksi Cepat Catat",
        sizeBadge = "4 × 2",
        subtitle = "4 tombol instan 1-ketuk: Manual, Tanya AI, Suara, dan Scan Struk.",
        receiverClass = SakuQuickActionWidgetReceiver::class.java
    ),
    BUDGET_PULSE(
        title = "Pantau Anggaran",
        sizeBadge = "4 × 2",
        subtitle = "Progress anggaran bulanan dengan bar tersegmentasi & kategori teratas.",
        receiverClass = SakuBudgetPulseWidgetReceiver::class.java
    ),
    COMPACT_COBALT(
        title = "Kartu Cobalt Mini",
        sizeBadge = "2 × 2",
        subtitle = "Kartu ringkas biru Cobalt untuk saldo dompet utama & catat kilat.",
        receiverClass = SakuCompactStatWidgetReceiver::class.java
    )
}

data class WidgetBudgetProgress(
    val category: String,
    val spent: Long,
    val limit: Long,
    val percent: Int
)

data class SakuWidgetSnapshot(
    val netBalance: Long,
    val income: Long,
    val expense: Long,
    val todayExpense: Long,
    val periodLabel: String,
    val monthLabel: String,
    val walletCount: Int,
    val primaryWalletName: String,
    val totalBudgetLimit: Long,
    val totalBudgetSpent: Long,
    val topBudgets: List<WidgetBudgetProgress>,
    val topGoalTitle: String?,
    val topGoalPercent: Int,
    val hideBalance: Boolean
)

suspend fun loadSakuWidgetSnapshot(context: Context): SakuWidgetSnapshot = withContext(Dispatchers.IO) {
    val db = DanaKuDatabase.getInstance(context)
    val prefs = UserPreferences(context)
    val wallets = db.walletDao().getAllWalletsOnce()
    val txList = db.transactionDao().getAllTransactionsOnce()
    val personal = prefs.personalization.first()
    val hideBalance = prefs.hideBalance.first()

    val now = System.currentTimeMillis()
    val (start, end) = DateFormatter.cycleBounds(now, personal.billingCycleStartDay)
    val (monthStart, monthEnd) = DateFormatter.monthBounds(now)
    val todayStart = DateFormatter.startOfDay(now)
    val todayEnd = DateFormatter.endOfDay(now)
    val monthKey = DateFormatter.currentMonthKey(now)

    val budgets = db.budgetDao().getBudgetsForMonthOnce(monthKey)
    val goals = db.savingsGoalDao().getAllOnce()

    val cycleTx = txList.filter { it.timestamp in start until end }
    val income = cycleTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense = cycleTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val todayExpense = txList
        .filter { it.timestamp in todayStart..todayEnd && it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }

    val spentByCatThisMonth = txList
        .filter { it.timestamp in monthStart until monthEnd && it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount } }

    val budgetItems = budgets.map { b ->
        val spent = spentByCatThisMonth[b.category] ?: 0L
        val pct = if (b.monthlyLimit > 0L) ((spent.toDouble() / b.monthlyLimit) * 100.0).roundToInt() else 0
        WidgetBudgetProgress(
            category = b.category,
            spent = spent,
            limit = b.monthlyLimit,
            percent = pct.coerceAtLeast(0)
        )
    }.sortedByDescending { it.percent }

    val totalBudgetLimit = budgets.sumOf { it.monthlyLimit }
    val totalBudgetSpent = budgets.sumOf { spentByCatThisMonth[it.category] ?: 0L }

    val activeGoal = goals.firstOrNull { !it.isCompleted && it.targetAmount > 0L }
    val goalPct = if (activeGoal != null) {
        ((activeGoal.savedAmount.toDouble() / activeGoal.targetAmount) * 100.0).roundToInt().coerceIn(0, 100)
    } else 0

    return@withContext SakuWidgetSnapshot(
        netBalance = wallets.sumOf { it.balance },
        income = income,
        expense = expense,
        todayExpense = todayExpense,
        periodLabel = DateFormatter.cycleLabel(now, personal.billingCycleStartDay),
        monthLabel = DateFormatter.currentMonthLabel(now),
        walletCount = wallets.size,
        primaryWalletName = wallets.firstOrNull()?.name ?: "Dompet Utama",
        totalBudgetLimit = totalBudgetLimit,
        totalBudgetSpent = totalBudgetSpent,
        topBudgets = budgetItems.take(2),
        topGoalTitle = activeGoal?.title,
        topGoalPercent = goalPct,
        hideBalance = hideBalance
    )
}

fun buildWidgetLaunchIntent(
    context: Context,
    openTab: String? = null,
    openRoute: String? = null
): Intent {
    return Intent(context, MainActivity::class.java).apply {
        action = "com.example.saku.WIDGET_ACTION_${openTab ?: openRoute ?: "HOME"}"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (openTab != null) {
            putExtra(EXTRA_OPEN_TAB, openTab)
        }
        if (openRoute != null) {
            putExtra(EXTRA_OPEN_ROUTE, openRoute)
        }
    }
}

object WidgetStateUpdater {
    suspend fun requestUpdate(context: Context) {
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val manager = runCatching { AppWidgetManager.getInstance(appContext) }.getOrNull()
            val targets: List<Pair<Class<out AppWidgetProvider>, () -> GlanceAppWidget>> = listOf(
                SakuBalanceWidgetReceiver::class.java to { SakuBalanceWidget() },
                SakuQuickActionWidgetReceiver::class.java to { SakuQuickActionWidget() },
                SakuBudgetPulseWidgetReceiver::class.java to { SakuBudgetPulseWidget() },
                SakuCompactStatWidgetReceiver::class.java to { SakuCompactStatWidget() }
            )
            supervisorScope {
                for ((receiverClass, widgetFactory) in targets) {
                    val hasPinnedIds = runCatching {
                        manager?.getAppWidgetIds(ComponentName(appContext, receiverClass))?.isNotEmpty() ?: true
                    }.getOrDefault(true)
                    if (hasPinnedIds) {
                        launch {
                            runCatching { widgetFactory().updateAll(appContext) }
                        }
                    }
                }
            }
        }
    }
}
