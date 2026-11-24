package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.GroupedTransactionDayCard
import com.example.ui.components.ObsidianCircleIconButton
import com.example.ui.components.ObsidianSubScreenHeader
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.viewmodel.TransactionUiItem
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.util.FinanceCategories
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PERIOD_ALL = "ALL"
private const val PERIOD_THIS_MONTH = "THIS_MONTH"
private const val PERIOD_7_DAYS = "7D"
private const val PERIOD_30_DAYS = "30D"
private const val MONTH_PREFIX = "MONTH:"

private data class PeriodFilterChipOption(
    val key: String,
    val label: String
)

@Composable
fun TransactionHistoryScreen(
    allTransactionItems: List<TransactionUiItem>,
    wallets: List<WalletEntity>,
    hideBalance: Boolean,
    onToggleHideBalance: () -> Unit,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    onOpenExport: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    initialWalletId: Long? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPeriodKey by rememberSaveable { mutableStateOf(PERIOD_ALL) }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedWalletId by rememberSaveable { mutableStateOf(initialWalletId) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var advancedFilterExpanded by rememberSaveable { mutableStateOf(false) }

    val periodOptions = remember(allTransactionItems) {
        val locale = Locale.Builder().setLanguage("in").setRegion("ID").build()
        val keyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val labelFmt = SimpleDateFormat("MMM yyyy", locale)
        val currentKey = DateFormatter.currentMonthKey()

        val pastMonths = allTransactionItems
            .map { keyFmt.format(Date(it.entity.timestamp)) to it.entity.timestamp }
            .distinctBy { it.first }
            .filter { it.first != currentKey }
            .sortedByDescending { it.first }
            .take(6)
            .map { (monthKey, sampleTs) ->
                val pretty = labelFmt.format(Date(sampleTs))
                    .replace(".", "")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                PeriodFilterChipOption(key = "$MONTH_PREFIX$monthKey", label = pretty)
            }

        listOf(
            PeriodFilterChipOption(PERIOD_ALL, "Semua"),
            PeriodFilterChipOption(PERIOD_THIS_MONTH, "Bulan Ini"),
            PeriodFilterChipOption(PERIOD_7_DAYS, "7 Hari"),
            PeriodFilterChipOption(PERIOD_30_DAYS, "30 Hari")
        ) + pastMonths
    }

    val availableCategories = remember(allTransactionItems) {
        val fromTx = allTransactionItems.map { it.entity.category }.toSet()
        FinanceCategories.allCategories.filter { it.name in fromTx || it.name != "Transfer" }
    }

    val filteredItems = remember(
        allTransactionItems,
        searchQuery,
        selectedPeriodKey,
        selectedType,
        selectedWalletId,
        selectedCategory
    ) {
        val now = System.currentTimeMillis()
        val (monthStart, monthEnd) = DateFormatter.monthBounds(now)
        val sevenDaysStart = Calendar.getInstance().apply {
            timeInMillis = DateFormatter.startOfDay(now)
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis
        val thirtyDaysStart = Calendar.getInstance().apply {
            timeInMillis = DateFormatter.startOfDay(now)
            add(Calendar.DAY_OF_YEAR, -29)
        }.timeInMillis
        val cleanQuery = searchQuery.trim()
        val targetMonthKey = if (selectedPeriodKey.startsWith(MONTH_PREFIX)) {
            selectedPeriodKey.removePrefix(MONTH_PREFIX)
        } else {
            null
        }

        allTransactionItems.filter { item ->
            val tx = item.entity
            val matchesPeriod = when (selectedPeriodKey) {
                PERIOD_ALL -> true
                PERIOD_THIS_MONTH -> tx.timestamp in monthStart until monthEnd
                PERIOD_7_DAYS -> tx.timestamp >= sevenDaysStart
                PERIOD_30_DAYS -> tx.timestamp >= thirtyDaysStart
                else -> if (targetMonthKey != null) {
                    DateFormatter.currentMonthKey(tx.timestamp) == targetMonthKey
                } else {
                    true
                }
            }
            val matchesType = selectedType == null || tx.type == selectedType
            val matchesWallet = selectedWalletId == null ||
                tx.walletId == selectedWalletId ||
                tx.targetWalletId == selectedWalletId
            val matchesCategory = selectedCategory == null ||
                tx.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = cleanQuery.isEmpty() ||
                tx.merchant.contains(cleanQuery, ignoreCase = true) ||
                tx.category.contains(cleanQuery, ignoreCase = true) ||
                tx.note.contains(cleanQuery, ignoreCase = true) ||
                item.walletName.contains(cleanQuery, ignoreCase = true) ||
                (item.targetWalletName?.contains(cleanQuery, ignoreCase = true) == true)

            matchesPeriod && matchesType && matchesWallet && matchesCategory && matchesQuery
        }
    }

    val groupedTransactions = remember(filteredItems) {
        val now = System.currentTimeMillis()
        filteredItems
            .groupBy { item -> DateFormatter.formatDateHeader(item.entity.timestamp, now) }
            .entries
            .map { it.key to it.value }
    }

    val summaryMetrics = remember(filteredItems, selectedWalletId) {
        val income = filteredItems.sumOf { item ->
            val tx = item.entity
            when {
                tx.type == TransactionType.INCOME -> tx.amount
                selectedWalletId != null && tx.type == TransactionType.TRANSFER && tx.targetWalletId == selectedWalletId -> tx.amount
                else -> 0L
            }
        }
        val expense = filteredItems.sumOf { item ->
            val tx = item.entity
            when {
                tx.type == TransactionType.EXPENSE -> tx.amount
                selectedWalletId != null && tx.type == TransactionType.TRANSFER && tx.walletId == selectedWalletId -> tx.amount
                else -> 0L
            }
        }
        Triple(income, expense, income - expense)
    }

    val extraFilterCount = listOfNotNull(selectedWalletId, selectedCategory).size
    val hasAnyFilter = searchQuery.isNotBlank() ||
        selectedPeriodKey != PERIOD_ALL ||
        selectedType != null ||
        selectedWalletId != null ||
        selectedCategory != null

    fun resetAllFilters() {
        searchQuery = ""
        selectedPeriodKey = PERIOD_ALL
        selectedType = null
        selectedWalletId = null
        selectedCategory = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        FreshAtmosphericBackdrop(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = LocalSakuTopPadding.current, bottom = 28.dp)
        ) {
            // 1. Clean Adaptive Header + Crisp Search Field
            item(key = "history_obsidian_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ObsidianSubScreenHeader(
                        title = "Riwayat Transaksi",
                        subtitle = if (hasAnyFilter) {
                            "${filteredItems.size} dari ${allTransactionItems.size} transaksi ditampilkan"
                        } else {
                            "${allTransactionItems.size} transaksi tercatat"
                        },
                        onBack = onBack,
                        backTestTag = "history_back_btn",
                        trailingAction = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    ObsidianCircleIconButton(
                                        icon = Icons.Rounded.Tune,
                                        contentDescription = "Filter dompet dan kategori",
                                        isActive = advancedFilterExpanded || extraFilterCount > 0,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            advancedFilterExpanded = !advancedFilterExpanded
                                        },
                                        modifier = Modifier.testTag("history_filter_toggle_btn")
                                    )
                                    if (extraFilterCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = extended.lime,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 1.dp, y = (-1).dp)
                                                .size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$extraFilterCount",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = extended.onLime
                                                )
                                            }
                                        }
                                    }
                                }

                                ObsidianCircleIconButton(
                                    icon = Icons.Rounded.Download,
                                    contentDescription = "Ekspor mutasi PDF atau CSV",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onOpenExport()
                                    },
                                    modifier = Modifier.testTag("history_export_btn")
                                )

                                ObsidianCircleIconButton(
                                    icon = Icons.Rounded.Add,
                                    contentDescription = "Catat transaksi baru",
                                    isActive = true,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAddTransaction()
                                    },
                                    modifier = Modifier.testTag("history_add_btn")
                                )
                            }
                        }
                    )

                    // Crisp Search Bar
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.8.dp, extended.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Cari merchant, kategori, dompet, atau catatan...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = extended.lime
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Hapus pencarian",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = extended.lime,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_search_input")
                        )
                    }
                }
            }

            // 2. Filters, Summary & Grouped Ledger
            item(key = "history_rising_sheet") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                        // Period & Type Filter Strips
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                periodOptions.forEach { option ->
                                    val selected = selectedPeriodKey == option.key
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedPeriodKey = option.key
                                        },
                                        label = {
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = extended.limeContainer,
                                            selectedLabelColor = extended.onLimeContainer
                                        ),
                                        modifier = Modifier.testTag("history_period_${option.key}")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val typeOptions = listOf(
                                    null to "Semua Tipe",
                                    TransactionType.EXPENSE to "Pengeluaran",
                                    TransactionType.INCOME to "Pemasukan",
                                    TransactionType.TRANSFER to "Transfer"
                                )
                                typeOptions.forEach { (typeKey, label) ->
                                    val selected = selectedType == typeKey
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedType = typeKey
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = extended.lime,
                                            selectedLabelColor = extended.onLime
                                        ),
                                        modifier = Modifier.testTag("history_type_${typeKey ?: "all"}")
                                    )
                                }
                            }
                        }

                        // Expandable Wallet & Category Filter Panel
                        AnimatedVisibility(visible = advancedFilterExpanded || extraFilterCount > 0) {
                            Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                                FintechCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 20.dp,
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Filter Dompet & Kategori",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (hasAnyFilter) {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .clickable { resetAllFilters() }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        .testTag("history_reset_filters_inline"),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.RestartAlt,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "Reset",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "Dompet",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                FilterChip(
                                                    selected = selectedWalletId == null,
                                                    onClick = { selectedWalletId = null },
                                                    label = { Text("Semua Dompet", style = MaterialTheme.typography.labelSmall) },
                                                    shape = RoundedCornerShape(999.dp)
                                                )
                                                wallets.forEach { wallet ->
                                                    val isSelected = selectedWalletId == wallet.id
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            selectedWalletId = if (isSelected) null else wallet.id
                                                        },
                                                        leadingIcon = {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(wallet.accentHex))
                                                            )
                                                        },
                                                        label = { Text(wallet.name, style = MaterialTheme.typography.labelSmall) },
                                                        shape = RoundedCornerShape(999.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "Kategori",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                FilterChip(
                                                    selected = selectedCategory == null,
                                                    onClick = { selectedCategory = null },
                                                    label = { Text("Semua Kategori", style = MaterialTheme.typography.labelSmall) },
                                                    shape = RoundedCornerShape(999.dp)
                                                )
                                                availableCategories.forEach { meta ->
                                                    val isSelected = selectedCategory.equals(meta.name, ignoreCase = true)
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            selectedCategory = if (isSelected) null else meta.name
                                                        },
                                                        leadingIcon = {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .clip(CircleShape)
                                                                    .background(meta.color)
                                                            )
                                                        },
                                                        label = { Text(meta.name, style = MaterialTheme.typography.labelSmall) },
                                                        shape = RoundedCornerShape(999.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Filtered Summary Strip Card
                        val activePeriodLabel = periodOptions.firstOrNull { it.key == selectedPeriodKey }?.label ?: "Semua"
                        val netPositive = summaryMetrics.third >= 0L

                        Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                            FintechCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("history_summary_card"),
                                cornerRadius = 20.dp,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Ringkasan • $activePeriodLabel",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${filteredItems.size} transaksi cocok",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = onToggleHideBalance,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .minimumInteractiveComponentSize()
                                            .testTag("history_toggle_privacy_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (hideBalance) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = if (hideBalance) "Tampilkan nominal" else "Sembunyikan nominal",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val incomeText = if (hideBalance) "Rp ••••" else CurrencyFormatter.formatCompactRupiah(summaryMetrics.first)
                                val expenseText = if (hideBalance) "Rp ••••" else CurrencyFormatter.formatCompactRupiah(summaryMetrics.second)
                                val netText = if (hideBalance) {
                                    "Rp ••••"
                                } else {
                                    val sign = if (netPositive) "+" else "-"
                                    "$sign${CurrencyFormatter.formatCompactRupiah(kotlin.math.abs(summaryMetrics.third))}"
                                }
                                val netColor = if (netPositive) extended.income else extended.expense
                                val fontScale = LocalDensity.current.fontScale

                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val useTwoPlusOne = maxWidth < 336.dp || fontScale > 1.05f
                                    if (useTwoPlusOne) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                HistorySummaryMetric(
                                                    label = "Pemasukan",
                                                    valueText = incomeText,
                                                    valueColor = extended.income,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                HistorySummaryMetric(
                                                    label = "Pengeluaran",
                                                    valueText = expenseText,
                                                    valueColor = extended.expense,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Arus Bersih",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = netText,
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontFeatureSettings = "tnum"
                                                        ),
                                                        color = netColor,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            HistorySummaryMetric(
                                                label = "Pemasukan",
                                                valueText = incomeText,
                                                valueColor = extended.income,
                                                modifier = Modifier.weight(1f)
                                            )
                                            HistorySummaryMetric(
                                                label = "Pengeluaran",
                                                valueText = expenseText,
                                                valueColor = extended.expense,
                                                modifier = Modifier.weight(1f)
                                            )
                                            HistorySummaryMetric(
                                                label = "Arus Bersih",
                                                valueText = netText,
                                                valueColor = netColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (groupedTransactions.isEmpty()) {
                            Box(modifier = Modifier.padding(horizontal = 18.dp)) {
                                FintechCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("history_empty_card"),
                                    cornerRadius = 22.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 26.dp, horizontal = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(extended.limeContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                                contentDescription = null,
                                                tint = extended.onLimeContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Text(
                                            text = if (hasAnyFilter) {
                                                "Tidak ada transaksi yang cocok"
                                            } else {
                                                "Belum ada riwayat transaksi"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (hasAnyFilter) {
                                                "Ubah kata kunci pencarian, periode, atau filter dompet/kategori."
                                            } else {
                                                "Catat pengeluaran atau pemasukan pertamamu sekarang."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (hasAnyFilter) {
                                            OutlinedButton(
                                                onClick = { resetAllFilters() },
                                                shape = RoundedCornerShape(999.dp),
                                                modifier = Modifier.testTag("history_reset_filters_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.RestartAlt,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Reset Semua Filter")
                                            }
                                        } else {
                                            Button(
                                                onClick = onAddTransaction,
                                                shape = RoundedCornerShape(999.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = extended.lime,
                                                    contentColor = extended.onLime
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Catat Transaksi", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(
                    items = groupedTransactions,
                    key = { it.first }
                ) { (dateHeader, dayTransactions) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 7.dp)
                    ) {
                        GroupedTransactionDayCard(
                            dateHeader = dateHeader,
                            transactions = dayTransactions,
                            flatSheetStyle = false,
                            hideBalance = hideBalance,
                            onEditTransaction = { item -> onEditTransaction(item.entity) },
                            onDeleteTransaction = { item -> onDeleteTransaction(item.entity) }
                        )
                    }
                }
            }
        }
    }

@Composable
private fun HistorySummaryMetric(
    label: String,
    valueText: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = valueColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
