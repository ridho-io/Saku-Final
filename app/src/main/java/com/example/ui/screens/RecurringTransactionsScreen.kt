package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.ObsidianSubScreenHeader
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.viewmodel.RecurringUiItem
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.util.FinanceCategories

@Composable
fun RecurringTransactionsScreen(
    items: List<RecurringUiItem>,
    wallets: List<WalletEntity>,
    onBack: () -> Unit,
    onSaveRule: (
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
    ) -> Unit,
    onToggleActive: (RecurringTransactionEntity) -> Unit,
    onDeleteRule: (RecurringTransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    var showSheet by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

    BackHandler(onBack = onBack)

    val activeCount = remember(items) { items.count { it.rule.isActive } }
    val activeMonthlyOutflow = remember(items) {
        items.filter { it.rule.isActive && it.rule.type == TransactionType.EXPENSE && it.rule.frequency == RecurrenceFrequency.MONTHLY }
            .sumOf { it.rule.amount }
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = LocalSakuTopPadding.current, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Studio Light Ice / Dark Navy Header + 2-Column FintechCard Bento Stats
            item(key = "recurring_hero_stats") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ObsidianSubScreenHeader(
                        title = "Transaksi Berulang",
                        subtitle = "Otomasi tagihan & pemasukan rutin",
                        onBack = onBack,
                        backTestTag = "recurring_back_btn",
                        trailingAction = {
                            Surface(
                                onClick = {
                                    editingRule = null
                                    showSheet = true
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = extended.lime,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("add_recurring_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = extended.onLime,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = "Baru",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = extended.onLime
                                    )
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FintechCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 20.dp,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(extended.limeContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EventRepeat,
                                        contentDescription = null,
                                        tint = extended.onLimeContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Jadwal Aktif",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "$activeCount dari ${items.size}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Otomatis saat jatuh tempo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FintechCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 20.dp,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Text(
                                text = "Beban Bulanan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = CurrencyFormatter.formatCompactRupiah(activeMonthlyOutflow),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = extended.expense
                            )
                            Text(
                                text = "Tagihan rutin tiap bulan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2. Recurring Rules List
            if (items.isEmpty()) {
                item(key = "recurring_empty") {
                    FintechCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(extended.limeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = null,
                                    tint = extended.onLimeContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = "Belum ada transaksi berulang",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Tambahkan gaji bulanan, sewa kos, atau langganan rutin agar tercatat otomatis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items.forEach { item ->
                    item(key = "recurring_rule_${item.rule.id}") {
                        RecurringRuleCard(
                            item = item,
                            onToggle = { onToggleActive(item.rule) },
                            onEdit = {
                                editingRule = item.rule
                                showSheet = true
                            },
                            onDelete = { onDeleteRule(item.rule) }
                        )
                    }
                }
            }
        }
    }

    if (showSheet && wallets.isNotEmpty()) {
        RecurringRuleBottomSheet(
            editingRule = editingRule,
            wallets = wallets,
            onDismiss = { showSheet = false },
            onSave = { merchant, amount, type, category, walletId, frequency, startDate, endDate, note ->
                onSaveRule(
                    editingRule,
                    merchant,
                    amount,
                    type,
                    category,
                    walletId,
                    frequency,
                    startDate,
                    endDate,
                    note
                )
                showSheet = false
            }
        )
    }
}

@Composable
private fun RecurringRuleCard(
    item: RecurringUiItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extended = LocalSakuColors.current
    val rule = item.rule
    val meta = FinanceCategories.getMeta(rule.category)
    val isIncome = rule.type == TransactionType.INCOME
    val amountColor = if (isIncome) extended.income else extended.expense

    FintechCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(meta.color.copy(alpha = if (extended.isDark) 0.20f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(meta.icon, contentDescription = rule.category, tint = meta.color)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.merchant,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = CurrencyFormatter.formatRupiah(rule.amount, showSign = true, isPositive = isIncome),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = amountColor
                )
            }

            Switch(
                checked = rule.isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = extended.onLime,
                    checkedTrackColor = extended.lime
                ),
                modifier = Modifier.testTag("recurring_switch_${rule.id}")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = extended.periwinkleContainer
            ) {
                Text(
                    text = RecurrenceFrequency.label(rule.frequency),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (extended.isDark) extended.periwinkle else Color(0xFF4338CA),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = item.walletName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = rule.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val endLabel = rule.endDate?.let { DateFormatter.formatShortDate(it) } ?: "Tanpa batas"
                Text(
                    text = "Mulai: ${DateFormatter.formatShortDate(rule.startDate)} • Akhir: $endLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (rule.isActive) {
                        "Jadwal berikutnya: ${DateFormatter.formatShortDate(rule.nextDueDate)}"
                    } else {
                        "Dijeda / Selesai"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (rule.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit jadwal", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus jadwal", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringRuleBottomSheet(
    editingRule: RecurringTransactionEntity?,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onSave: (
        merchant: String,
        amount: Long,
        type: String,
        category: String,
        walletId: Long,
        frequency: String,
        startDate: Long,
        endDate: Long?,
        note: String
    ) -> Unit
) {
    val extended = LocalSakuColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var txType by remember(editingRule) { mutableStateOf(editingRule?.type ?: TransactionType.EXPENSE) }
    var merchant by remember(editingRule) { mutableStateOf(editingRule?.merchant ?: "") }
    var amountText by remember(editingRule) { mutableStateOf(editingRule?.amount?.toString() ?: "") }
    var selectedCategory by remember(editingRule) {
        mutableStateOf(editingRule?.category ?: "Tagihan & Kos")
    }
    var selectedWalletId by remember(editingRule, wallets) {
        mutableStateOf(editingRule?.walletId ?: wallets.first().id)
    }
    var frequency by remember(editingRule) {
        mutableStateOf(editingRule?.frequency ?: RecurrenceFrequency.MONTHLY)
    }
    var startDate by remember(editingRule) {
        mutableStateOf(editingRule?.startDate ?: System.currentTimeMillis())
    }
    var hasEndDate by remember(editingRule) {
        mutableStateOf(editingRule?.endDate != null)
    }
    var endDate by remember(editingRule) {
        mutableStateOf(editingRule?.endDate ?: DateFormatter.advanceByFrequency(System.currentTimeMillis(), RecurrenceFrequency.YEARLY))
    }
    var note by remember(editingRule) { mutableStateOf(editingRule?.note ?: "") }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingRule != null) "Edit Jadwal Berulang" else "Tambah Transaksi Berulang",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                }
            }

            val typeOptions = listOf(
                TransactionType.EXPENSE to "Pengeluaran",
                TransactionType.INCOME to "Pemasukan"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                typeOptions.forEachIndexed { index, (code, label) ->
                    SegmentedButton(
                        selected = txType == code,
                        onClick = {
                            txType = code
                            if (code == TransactionType.INCOME && selectedCategory !in FinanceCategories.incomeCategoryNames) {
                                selectedCategory = "Gaji"
                            } else if (code == TransactionType.EXPENSE && selectedCategory !in FinanceCategories.expenseCategoryNames) {
                                selectedCategory = "Tagihan & Kos"
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = typeOptions.size)
                    ) {
                        Text(label)
                    }
                }
            }

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Nama Transaksi / Merchant") },
                placeholder = { Text("cth: Sewa Kos, Spotify, Gaji Bulanan") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_merchant_input")
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { raw -> amountText = raw.filter { it.isDigit() } },
                label = { Text("Nominal (Rp)") },
                placeholder = { Text("cth: 1500000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_amount_input")
            )

            Text(
                text = "Pola Pengulangan",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecurrenceFrequency.all.forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        label = { Text(RecurrenceFrequency.label(freq)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extended.lime,
                            selectedLabelColor = extended.onLime
                        ),
                        modifier = Modifier.testTag("freq_chip_${freq.lowercase()}")
                    )
                }
            }

            Text(
                text = "Kategori",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val categories = if (txType == TransactionType.INCOME) {
                FinanceCategories.incomeCategoryNames
            } else {
                FinanceCategories.expenseCategoryNames
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Text(
                text = "Dompet",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wallets.forEach { wallet ->
                    FilterChip(
                        selected = selectedWalletId == wallet.id,
                        onClick = { selectedWalletId = wallet.id },
                        label = { Text(wallet.name) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("recurring_start_date_btn")
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Mulai", style = MaterialTheme.typography.labelSmall)
                        Text(DateFormatter.formatShortDate(startDate), style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (hasEndDate) {
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("recurring_end_date_btn")
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Berakhir", style = MaterialTheme.typography.labelSmall)
                            Text(DateFormatter.formatShortDate(endDate), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(0.8.dp, extended.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { hasEndDate = !hasEndDate }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tetapkan tanggal berakhir",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = hasEndDate,
                        onCheckedChange = {
                            hasEndDate = it
                            if (it && endDate < startDate) {
                                endDate = DateFormatter.advanceByFrequency(startDate, frequency)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = extended.onLime,
                            checkedTrackColor = extended.lime
                        )
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (opsional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val parsedAmount = amountText.toLongOrNull() ?: 0L
            val validDateRange = !hasEndDate || DateFormatter.startOfDay(endDate) >= DateFormatter.startOfDay(startDate)

            if (!validDateRange) {
                Text(
                    text = "Tanggal berakhir harus sama atau setelah tanggal mulai.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.expense
                )
            }

            Button(
                onClick = {
                    onSave(
                        merchant,
                        parsedAmount,
                        txType,
                        selectedCategory,
                        selectedWalletId,
                        frequency,
                        startDate,
                        if (hasEndDate) endDate else null,
                        note
                    )
                },
                enabled = parsedAmount > 0L && validDateRange,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_recurring_btn")
            ) {
                Text(
                    text = if (editingRule != null) "Simpan Perubahan Jadwal" else "Simpan Jadwal Berulang",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateFormatter.localMillisToUtcPickerMillis(startDate)
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { pickedUtc ->
                            startDate = DateFormatter.utcPickerMillisToLocalNoon(pickedUtc)
                            if (hasEndDate && endDate < startDate) {
                                endDate = startDate
                            }
                        }
                        showStartPicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateFormatter.localMillisToUtcPickerMillis(endDate)
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { pickedUtc ->
                            endDate = DateFormatter.utcPickerMillisToLocalNoon(pickedUtc)
                        }
                        showEndPicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
