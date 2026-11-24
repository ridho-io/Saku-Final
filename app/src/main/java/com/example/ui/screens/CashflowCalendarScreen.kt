package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.ObsidianCircleIconButton
import com.example.ui.components.ObsidianSubScreenHeader
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.viewmodel.RecurringUiItem
import com.example.ui.viewmodel.TransactionUiItem
import com.example.util.CalendarDaySummary
import com.example.util.CashflowCalendarCalculator
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class CalendarFilterMode(val label: String) {
    ALL("Semua"),
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran"),
    NO_SPEND("Hemat")
}

@Composable
fun CashflowCalendarScreen(
    allTransactionItems: List<TransactionUiItem>,
    recurringItems: List<RecurringUiItem>,
    hideBalance: Boolean,
    onBack: () -> Unit,
    onAddTransactionForDate: (Long) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current

    val todayCal = remember { Calendar.getInstance() }
    val currentYear = remember { todayCal.get(Calendar.YEAR) }
    val currentMonth = remember { todayCal.get(Calendar.MONTH) }
    val currentDay = remember { todayCal.get(Calendar.DAY_OF_MONTH) }

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var selectedDayOfMonth by remember { mutableIntStateOf(currentDay) }
    var filterMode by remember { mutableStateOf(CalendarFilterMode.ALL) }

    val monthState = remember(selectedYear, selectedMonth, allTransactionItems, recurringItems) {
        CashflowCalendarCalculator.buildMonth(
            year = selectedYear,
            month = selectedMonth,
            transactions = allTransactionItems,
            recurringItems = recurringItems
        )
    }

    // Clamp selectedDayOfMonth when switching months (e.g., 31 -> 28/30)
    LaunchedEffect(monthState.days.size) {
        if (selectedDayOfMonth > monthState.days.size) {
            selectedDayOfMonth = monthState.days.size.coerceAtLeast(1)
        }
    }

    val selectedDaySummary = monthState.days.firstOrNull { it.dayOfMonth == selectedDayOfMonth }
        ?: monthState.days.lastOrNull()

    val isViewingCurrentMonth = selectedYear == currentYear && selectedMonth == currentMonth

    fun shiftMonth(delta: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH)
        selectedDayOfMonth = if (selectedYear == currentYear && selectedMonth == currentMonth) {
            currentDay
        } else {
            1
        }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("cashflow_calendar_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        FreshAtmosphericBackdrop(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(top = LocalSakuTopPadding.current, bottom = 0.dp)
        ) {
            // 1. Clean Adaptive Header + Month Selector + Summary & Streak Card
            item(key = "calendar_obsidian_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ObsidianSubScreenHeader(
                        title = "Kalender Cashflow",
                        subtitle = "Arus kas harian & streak hemat",
                        onBack = onBack,
                        backTestTag = "calendar_back_btn",
                        trailingAction = if (!isViewingCurrentMonth) {
                            {
                                ObsidianCircleIconButton(
                                    icon = Icons.Rounded.Today,
                                    contentDescription = "Kembali ke bulan ini",
                                    isActive = true,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedYear = currentYear
                                        selectedMonth = currentMonth
                                        selectedDayOfMonth = currentDay
                                    },
                                    modifier = Modifier.testTag("calendar_jump_today_btn")
                                )
                            }
                        } else null
                    )

                    // Month Switcher Bar
                    FintechCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ObsidianCircleIconButton(
                                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Bulan sebelumnya",
                                onClick = { shiftMonth(-1) },
                                modifier = Modifier.testTag("calendar_prev_month_btn")
                            )

                            AnimatedContent(
                                targetState = monthState.monthTitle,
                                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                                label = "calendar_month_title"
                            ) { label ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = extended.lime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            ObsidianCircleIconButton(
                                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Bulan berikutnya",
                                onClick = { shiftMonth(1) },
                                modifier = Modifier.testTag("calendar_next_month_btn")
                            )
                        }
                    }

                    // Monthly KPIs + No-Spend Streak Bento Card
                    FintechCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = extended.cardRadius,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CalendarHeroKpi(
                                    label = "Masuk",
                                    value = if (hideBalance) "•••" else CurrencyFormatter.formatCompactRupiah(monthState.totalIncome),
                                    valueColor = extended.income,
                                    modifier = Modifier.weight(1f)
                                )
                                CalendarHeroKpi(
                                    label = "Keluar",
                                    value = if (hideBalance) "•••" else CurrencyFormatter.formatCompactRupiah(monthState.totalExpense),
                                    valueColor = extended.expense,
                                    modifier = Modifier.weight(1f)
                                )
                                CalendarHeroKpi(
                                    label = "Bersih",
                                    value = if (hideBalance) "•••" else {
                                        val prefix = if (monthState.netCashflow >= 0L) "+" else "-"
                                        "$prefix${CurrencyFormatter.formatCompactRupiah(kotlin.math.abs(monthState.netCashflow))}"
                                    },
                                    valueColor = if (monthState.netCashflow >= 0L) MaterialTheme.colorScheme.onSurface else extended.expense,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(extended.limeContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = extended.onLimeContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Streak Hemat: ${monthState.currentNoSpendStreak} hari",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${monthState.noSpendDaysCount} hari tanpa pengeluaran • Rekor ${monthState.bestNoSpendStreak} hari",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                monthState.highestSpendDay?.let { peak ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = extended.expenseContainer
                                    ) {
                                        Text(
                                            text = "Puncak Tgl ${peak.dayOfMonth}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = extended.onExpenseContainer,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Filter + Calendar Grid
            item(key = "calendar_grid_sheet") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                        // Highlight filter pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CalendarFilterMode.entries.forEach { mode ->
                                val isSelected = filterMode == mode
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        filterMode = mode
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isSelected) extended.lime else extended.elevatedCard,
                                    border = if (isSelected) null else BorderStroke(0.8.dp, extended.cardBorder),
                                    modifier = Modifier.testTag("calendar_filter_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) extended.onLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Calendar Grid Card
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = extended.cardRadius,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            // Weekday Headers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEachIndexed { idx, label ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (idx >= 5) extended.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val totalCells = monthState.leadingEmptySlots + monthState.days.size
                            val rowCount = (totalCells + 6) / 7

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (row in 0 until rowCount) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (col in 0 until 7) {
                                            val cellIndex = row * 7 + col
                                            val dayIndex = cellIndex - monthState.leadingEmptySlots
                                            if (dayIndex in monthState.days.indices) {
                                                val day = monthState.days[dayIndex]
                                                CalendarDayCell(
                                                    day = day,
                                                    isSelected = day.dayOfMonth == selectedDayOfMonth,
                                                    filterMode = filterMode,
                                                    hideBalance = hideBalance,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        selectedDayOfMonth = day.dayOfMonth
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Legend
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CalendarLegendDot(color = extended.income, label = "Hemat / Masuk")
                                CalendarLegendDot(color = extended.expense, label = "Pengeluaran")
                                CalendarLegendDot(color = extended.periwinkle, label = "Jadwal Rutin")
                            }
                        }

                        // Selected Day Header + Add Action
                        selectedDaySummary?.let { day ->
                            SelectedDayBanner(
                                day = day,
                                hideBalance = hideBalance,
                                onAddTransaction = { onAddTransactionForDate(day.dayStartMillis) }
                            )
                        }
                }
            }

            // 3. Selected Day Due Recurring Bills (if any)
            val dueRecurring = selectedDaySummary?.dueRecurring.orEmpty()
            if (dueRecurring.isNotEmpty()) {
                item(key = "calendar_due_recurring") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = extended.cardRadius
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = null,
                                    tint = extended.periwinkle,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Jadwal Rutin Tanggal Ini (${dueRecurring.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            dueRecurring.forEachIndexed { idx, rec ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.rule.merchant,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${rec.rule.category} • ${rec.walletName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val isRecIncome = rec.rule.type == TransactionType.INCOME
                                    Text(
                                        text = if (hideBalance) "Rp •••" else "${if (isRecIncome) "+" else "-"}${CurrencyFormatter.formatRupiah(rec.rule.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = if (isRecIncome) extended.income else extended.expense,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (idx < dueRecurring.lastIndex) {
                                    HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Selected Day Transactions List or Empty State
            val dayTransactions = selectedDaySummary?.transactions.orEmpty()
            if (dayTransactions.isEmpty()) {
                item(key = "calendar_day_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = extended.cardRadius
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Spa,
                                    contentDescription = null,
                                    tint = if (selectedDaySummary?.isNoSpendDay == true) extended.income else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = if (selectedDaySummary?.isNoSpendDay == true) {
                                        "Hari Hemat (No-Spend Day)!"
                                    } else {
                                        "Belum ada transaksi di tanggal ini"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (selectedDaySummary?.isNoSpendDay == true) {
                                        "Tidak ada pengeluaran tercatat pada tanggal ini. Pertahankan kebiasaan hematmu!"
                                    } else {
                                        "Ketuk tombol + Catat di atas untuk menambahkan pemasukan atau pengeluaran pada tanggal ini."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = dayTransactions,
                    key = { _, item -> "cal_tx_${item.entity.id}" }
                ) { index, txItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 3.dp)
                    ) {
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            TransactionRowItem(
                                item = txItem,
                                onClick = { onEditTransaction(txItem.entity) },
                                onDelete = { onDeleteTransaction(txItem.entity) },
                                insideGroupedCard = true
                            )
                        }
                    }
                }
            }

            item(key = "calendar_bottom_spacer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(28.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarHeroKpi(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDaySummary,
    isSelected: Boolean,
    filterMode: CalendarFilterMode,
    hideBalance: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current

    val dimmedByFilter = when (filterMode) {
        CalendarFilterMode.ALL -> false
        CalendarFilterMode.INCOME -> day.incomeTotal == 0L
        CalendarFilterMode.EXPENSE -> day.expenseTotal == 0L
        CalendarFilterMode.NO_SPEND -> !day.isNoSpendDay
    }

    val cellBg = when {
        isSelected -> extended.lime.copy(alpha = 0.22f)
        dimmedByFilter -> Color.Transparent
        day.expenseIntensity == 3 -> extended.expense.copy(alpha = 0.18f)
        day.expenseIntensity == 2 -> extended.expense.copy(alpha = 0.10f)
        day.isNoSpendDay -> extended.income.copy(alpha = 0.10f)
        day.incomeTotal > 0L -> extended.income.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    }

    val borderStroke = when {
        isSelected -> BorderStroke(1.5.dp, extended.lime)
        day.isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
        else -> null
    }

    val miniMetricText = when {
        dimmedByFilter -> null
        day.expenseTotal > 0L && day.incomeTotal == 0L -> if (hideBalance) "•" else "-${compactShort(day.expenseTotal)}"
        day.incomeTotal > 0L && day.expenseTotal == 0L -> if (hideBalance) "•" else "+${compactShort(day.incomeTotal)}"
        day.expenseTotal > 0L && day.incomeTotal > 0L -> {
            if (hideBalance) "•" else {
                val net = day.netCashflow
                if (net >= 0L) "+${compactShort(net)}" else "-${compactShort(-net)}"
            }
        }
        else -> null
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = cellBg,
        border = borderStroke,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 54.dp)
            .testTag("calendar_day_${day.dayOfMonth}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = when {
                    dimmedByFilter -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    isSelected -> MaterialTheme.colorScheme.onSurface
                    day.isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (miniMetricText != null) {
                Text(
                    text = miniMetricText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (day.netCashflow >= 0L && day.incomeTotal > 0L) extended.income else extended.expense,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (day.isNoSpendDay && !dimmedByFilter) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(extended.income)
                        )
                    }
                    if (day.dueRecurring.isNotEmpty() && !dimmedByFilter) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(extended.periwinkle)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectedDayBanner(
    day: CalendarDaySummary,
    hideBalance: Boolean,
    onAddTransaction: () -> Unit
) {
    val extended = LocalSakuColors.current
    val locale = remember { Locale.Builder().setLanguage("in").setRegion("ID").build() }
    val dateLabel = remember(day.dayStartMillis) {
        SimpleDateFormat("EEEE, d MMM yyyy", locale)
            .format(Date(day.dayStartMillis))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    FintechCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = extended.cardRadius
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (day.incomeTotal > 0L) {
                        Text(
                            text = if (hideBalance) "+•••" else "+${CurrencyFormatter.formatCompactRupiah(day.incomeTotal)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = extended.income
                        )
                    }
                    if (day.expenseTotal > 0L) {
                        Text(
                            text = if (hideBalance) "-•••" else "-${CurrencyFormatter.formatCompactRupiah(day.expenseTotal)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = extended.expense
                        )
                    }
                    if (day.incomeTotal == 0L && day.expenseTotal == 0L) {
                        Text(
                            text = if (day.isNoSpendDay) "Hari Hemat (No-Spend)" else "Jadwal Mendatang",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (day.isNoSpendDay) extended.income else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = onAddTransaction,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("calendar_add_tx_btn")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Catat", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun compactShort(amount: Long): String {
    return when {
        amount >= 1_000_000L -> {
            val jt = amount / 1_000_000.0
            if (amount % 1_000_000L == 0L) "${amount / 1_000_000L}jt"
            else String.format(Locale.US, "%.1fjt", jt)
        }
        amount >= 1_000L -> "${amount / 1_000L}rb"
        else -> amount.toString()
    }
}
