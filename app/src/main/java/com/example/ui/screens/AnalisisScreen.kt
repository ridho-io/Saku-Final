package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionType
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.MonthlyIncomeExpenseChartCard
import com.example.ui.components.ObsidianCircleIconButton
import com.example.ui.components.SakuBrandMark
import com.example.ui.theme.LocalSakuBottomPadding
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.viewmodel.AiTextState
import com.example.ui.viewmodel.CategorySlice
import com.example.ui.viewmodel.MonthlyOverviewUiState
import com.example.ui.viewmodel.QaExchange
import com.example.ui.viewmodel.TransactionUiItem
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class AnalisisCurveFilter(val label: String) {
    ALL("Semua"),
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran")
}

@Composable
fun AnalisisScreen(
    slices: List<CategorySlice>,
    allTransactionItems: List<TransactionUiItem>,
    monthlyOverview: MonthlyOverviewUiState,
    insightsState: AiTextState,
    qaActiveState: AiTextState,
    qaHistory: List<QaExchange>,
    onGenerateInsights: (forceRefresh: Boolean) -> Unit,
    onAskQuestion: (String) -> Unit,
    onOpenMonthlyOverview: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current
    var selectedFilter by remember { mutableStateOf(AnalisisCurveFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var questionText by remember { mutableStateOf("") }

    val totalExpense = remember(slices) { slices.sumOf { it.amount } }
    val monthMetrics = remember(allTransactionItems) {
        val (monthStart, monthEnd) = DateFormatter.monthBounds()
        var incomeSum = 0L
        var incomeCount = 0
        var expenseCount = 0
        allTransactionItems.forEach { item ->
            if (item.entity.timestamp in monthStart until monthEnd) {
                when (item.entity.type) {
                    TransactionType.INCOME -> {
                        incomeSum += item.entity.amount
                        incomeCount++
                    }
                    TransactionType.EXPENSE -> expenseCount++
                    else -> Unit
                }
            }
        }
        Triple(incomeSum, incomeCount, expenseCount)
    }
    val currentMonthIncome = monthMetrics.first
    val monthIncomeTxCount = monthMetrics.second
    val monthExpenseTxCount = monthMetrics.third
    val netMonthCashflow = currentMonthIncome - totalExpense

    val dayOfMonth = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    }
    val dailyAverage = remember(totalExpense, dayOfMonth) { totalExpense / dayOfMonth }
    val topSlice = remember(slices) { slices.firstOrNull() }
    val weeklyPoints = remember(allTransactionItems) {
        buildLast7DaysSpend(allTransactionItems)
    }

    val momBannerInfo = remember(monthlyOverview.months, topSlice) {
        val list = monthlyOverview.months
        val current = list.lastOrNull()
        val prev = if (list.size >= 2) list[list.lastIndex - 1] else null
        when {
            current != null && prev != null && prev.expense > 0L -> {
                val deltaPct = (((current.expense - prev.expense).toFloat() / prev.expense.toFloat()) * 100f).roundToInt()
                val headline = when {
                    deltaPct > 0 -> "↗ $deltaPct%"
                    deltaPct < 0 -> "↘ ${-deltaPct}%"
                    else -> "→ 0%"
                }
                val isHigher = deltaPct > 0
                val desc = when {
                    deltaPct > 0 -> "Pengeluaran naik vs bulan ${prev.shortLabel}"
                    deltaPct < 0 -> "Lebih hemat vs bulan ${prev.shortLabel}"
                    else -> "Stabil dibanding bulan ${prev.shortLabel}"
                }
                Triple(headline, desc, isHigher)
            }
            topSlice != null -> {
                Triple(
                    "${topSlice.percentage.roundToInt()}% Teratas",
                    "${topSlice.category} jadi pos terbesar bulan ini",
                    false
                )
            }
            else -> {
                Triple(
                    "6 Bulan",
                    "Bandingkan tren arus kas bulananmu",
                    false
                )
            }
        }
    }

    val selectedCategoryTx = remember(selectedCategory, allTransactionItems) {
        if (selectedCategory == null) emptyList()
        else {
            val (monthStart, monthEnd) = DateFormatter.monthBounds()
            allTransactionItems.filter {
                it.entity.category == selectedCategory &&
                    it.entity.type == TransactionType.EXPENSE &&
                    it.entity.timestamp in monthStart until monthEnd
            }
        }
    }
    val suggestedQuestions = listOf(
        "Berapa total jajan kopi minggu ini?",
        "Pengeluaran terbesar bulan ini apa?",
        "Sisa anggaran makanan berapa?"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        FreshAtmosphericBackdrop(modifier = Modifier.height(210.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = LocalSakuTopPadding.current,
                bottom = LocalSakuBottomPadding.current
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Clean Top Header Bar (Avatar + Title + Calendar & Overview Icons)
            item(key = "analisis_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SakuBrandMark(size = 40.dp)
                        Column {
                            Text(
                                text = "Analisis",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${DateFormatter.currentMonthLabel()} • Laporan Arus Kas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ObsidianCircleIconButton(
                            icon = Icons.Rounded.CalendarMonth,
                            contentDescription = "Kalender Cashflow",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenCalendar()
                            },
                            modifier = Modifier.testTag("analisis_open_calendar_btn")
                        )
                        ObsidianCircleIconButton(
                            icon = Icons.Rounded.BarChart,
                            contentDescription = "Ringkasan 6 Bulan",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenMonthlyOverview()
                            },
                            modifier = Modifier.testTag("analisis_open_monthly_overview_btn")
                        )
                    }
                }
            }

            // 2. Segmented Filter Strip (Matching Screen 2 Pill Bar)
            item(key = "analisis_filter_strip") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnalisisCurveFilter.entries.forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFilter = filter
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected && !extended.isDark) 1.5.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("analisis_filter_${filter.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filter.label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) extended.lime else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Big Hero Metric + Inline Delta Badge + 2 Crisp White KPI Cards (Matching Screen 2)
            item(key = "analisis_hero_kpi") {
                val (heroLabel, heroValueText) = when (selectedFilter) {
                    AnalisisCurveFilter.ALL -> "Arus Kas Bersih Bulan Ini" to CurrencyFormatter.formatRupiah(
                        netMonthCashflow,
                        showSign = true,
                        isPositive = netMonthCashflow >= 0L
                    )
                    AnalisisCurveFilter.INCOME -> "Total Pemasukan Bulan Ini" to CurrencyFormatter.formatRupiah(currentMonthIncome)
                    AnalisisCurveFilter.EXPENSE -> "Total Pengeluaran Bulan Ini" to CurrencyFormatter.formatRupiah(totalExpense)
                }

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = heroLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = heroValueText,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                onClick = onOpenMonthlyOverview,
                                shape = RoundedCornerShape(999.dp),
                                color = if (momBannerInfo.third) extended.expenseContainer else extended.incomeContainer,
                                modifier = Modifier.testTag("analisis_mom_insight_banner")
                            ) {
                                Text(
                                    text = momBannerInfo.first,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = if (momBannerInfo.third) extended.expense else extended.income,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FintechCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Text(
                                text = "Pemasukan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.formatCompactRupiah(currentMonthIncome),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = extended.income,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${monthIncomeTxCount}x transaksi masuk",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FintechCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Text(
                                text = "Pengeluaran",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.formatCompactRupiah(totalExpense),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Avg ${CurrencyFormatter.formatCompactRupiah(dailyAverage)}/hari (${monthExpenseTxCount}x)",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. Rounded Daily Spending Vertical Cobalt Bar Chart (Matching Screen 2's Main Chart!)
            item(key = "daily_spending_bars") {
                WeeklySpendTrendCard(points = weeklyPoints)
            }

            // 5. Category Breakdown Donut + Progress List (Matching Screen 2's Breakdown Card)
            item(key = "category_breakdown") {
                FintechCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rincian Kategori",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedCategory != null) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = extended.limeContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { selectedCategory = null }
                            ) {
                                Text(
                                    text = "Reset Filter",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = extended.lime,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ExpenseDonutChart(
                        slices = slices,
                        totalExpense = totalExpense,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it }
                    )
                    if (selectedCategory != null && selectedCategoryTx.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Transaksi Teratas • $selectedCategory (${selectedCategoryTx.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedCategoryTx.take(5).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.entity.merchant,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.walletName} • ${DateFormatter.formatShortDate(item.entity.timestamp)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(item.entity.amount),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = extended.expense
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. 6-Month Curve Card
            item(key = "monthly_trend_spline") {
                MonthlyIncomeExpenseChartCard(
                    months = monthlyOverview.months,
                    showIncomeCurve = selectedFilter != AnalisisCurveFilter.EXPENSE,
                    showExpenseCurve = selectedFilter != AnalisisCurveFilter.INCOME,
                    onOpenDetailClick = onOpenMonthlyOverview,
                    onObsidianCanvas = false
                )
            }

            // 7. AI Financial Advisor Card
            item(key = "ai_advisor_card") {
                FintechCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(extended.limeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Insights,
                                contentDescription = null,
                                tint = extended.lime,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Financial Advisor",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Evaluasi arus kas, kebiasaan belanja, & peringatan anomali",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { onGenerateInsights(false) },
                        enabled = insightsState !is AiTextState.Loading && insightsState !is AiTextState.Streaming,
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extended.lime,
                            contentColor = extended.onLime
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("analyze_month_btn")
                    ) {
                        if (insightsState is AiTextState.Loading || insightsState is AiTextState.Streaming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = extended.onLime
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Menganalisis data...",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analisis Keuangan Bulan Ini",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }

                    when (insightsState) {
                        is AiTextState.Idle -> Unit
                        is AiTextState.Loading -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Sedang merangkum & mengevaluasi transaksi bulan ini...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiTextState.Streaming -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = extended.elevatedCard,
                                border = BorderStroke(0.6.dp, extended.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = insightsState.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                        is AiTextState.Success -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = extended.elevatedCard,
                                border = BorderStroke(0.6.dp, extended.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = insightsState.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Gemini 3.8 Flash • Hemat kuota",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Perbarui",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = extended.lime,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onGenerateInsights(true) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        is AiTextState.Error -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = insightsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.expense
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(onClick = { onGenerateInsights(true) }) { Text("Coba Lagi") }
                        }
                    }
                }
            }

            // 8. Ask Your Finances Q&A Card
            item(key = "ai_qa_card") {
                FintechCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(extended.periwinkleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = extended.periwinkle,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Tanya Keuanganmu",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tanyakan apa saja soal riwayat transaksi atau sisa anggaranmu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedQuestions.forEach { q ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    questionText = ""
                                    onAskQuestion(q)
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = extended.elevatedCard
                                ),
                                label = {
                                    Text(
                                        text = q,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        placeholder = { Text("cth: Berapa total jajan kopi minggu ini?") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = extended.cardBorder
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (questionText.isNotBlank()) {
                                val q = questionText
                                questionText = ""
                                onAskQuestion(q)
                            }
                        }),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (questionText.isNotBlank()) {
                                        val q = questionText
                                        questionText = ""
                                        onAskQuestion(q)
                                    }
                                },
                                enabled = questionText.isNotBlank() && qaActiveState !is AiTextState.Loading,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("qa_send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Kirim pertanyaan",
                                    tint = if (questionText.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qa_input_field")
                    )

                    when (qaActiveState) {
                        is AiTextState.Loading -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Mencari jawaban dari datamu...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is AiTextState.Streaming -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = extended.elevatedCard,
                                border = BorderStroke(0.6.dp, extended.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = qaActiveState.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        is AiTextState.Error -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = qaActiveState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.expense
                            )
                        }
                        else -> Unit
                    }
                }
            }

            if (qaHistory.isNotEmpty()) {
                items(qaHistory) { exchange ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                                color = extended.limeContainer
                            ) {
                                Text(
                                    text = exchange.question,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (extended.isDark) Color(0xFFDCE6FF) else extended.lime,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }

                        FintechCard(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(extended.periwinkleContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = extended.periwinkle,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "Saku AI",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = exchange.answer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

private data class DailySpendPoint(
    val dayShort: String,
    val fullDate: String,
    val amount: Long,
    val isToday: Boolean
)

private fun buildLast7DaysSpend(items: List<TransactionUiItem>): List<DailySpendPoint> {
    val locale = Locale.Builder().setLanguage("in").setRegion("ID").build()
    val shortFmt = SimpleDateFormat("EEE", locale)
    val fullFmt = SimpleDateFormat("EEE, d MMM", locale)
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -6)
    }

    return (0..6).map { offset ->
        val dayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayEnd = cal.timeInMillis
        val spent = items
            .asSequence()
            .filter {
                it.entity.type == TransactionType.EXPENSE &&
                    it.entity.timestamp in dayStart until dayEnd
            }
            .sumOf { it.entity.amount }
        val dateObj = Date(dayStart)
        DailySpendPoint(
            dayShort = shortFmt.format(dateObj).take(3),
            fullDate = fullFmt.format(dateObj),
            amount = spent,
            isToday = offset == 6
        )
    }
}

/**
 * Rounded vertical Cobalt Blue bar chart inside soft pill tracks — inspired directly by
 * Screen 2's "Daily Spending" card in the reference design.
 */
@Composable
private fun WeeklySpendTrendCard(points: List<DailySpendPoint>) {
    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current
    var selectedIdx by remember { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }

    val maxAmount = remember(points) { points.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L }
    val weeklyTotal = remember(points) { points.sumOf { it.amount } }
    val selectedPoint = points.getOrNull(selectedIdx)
    val trackPillBg = if (extended.isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color(0xFFF1F5FA)

    FintechCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pengeluaran Harian",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (selectedPoint != null) {
                        "${selectedPoint.fullDate} • ${CurrencyFormatter.formatRupiah(selectedPoint.amount)}"
                    } else {
                        "Ketuk batang untuk detail harian"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(extended.lime)
                )
                Text(
                    text = CurrencyFormatter.formatCompactRupiah(weeklyTotal),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEachIndexed { index, point ->
                val isSelected = index == selectedIdx
                val targetFraction = if (point.amount == 0L) {
                    0.08f
                } else {
                    (point.amount.toFloat() / maxAmount.toFloat()).coerceIn(0.16f, 0.92f)
                }
                val animatedFraction by animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec = tween(550),
                    label = "weekly_bar_$index"
                )
                val barColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> extended.lime
                        point.amount > 0L -> extended.lime.copy(alpha = 0.80f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    animationSpec = spring(),
                    label = "weekly_bar_color_$index"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedIdx = index
                        }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(114.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(trackPillBg),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedFraction)
                                .clip(RoundedCornerShape(12.dp))
                                .background(barColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = if (point.isToday) "Hari Ini" else point.dayShort,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) extended.lime else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
