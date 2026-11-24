package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DebtDirection
import com.example.data.local.DebtRecordEntity
import com.example.data.local.WalletEntity
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.ObsidianCircleIconButton
import com.example.ui.components.ObsidianSubScreenHeader
import com.example.ui.components.SakuBottomDrawer
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.theme.WarningAmber
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter

@Composable
fun DebtsScreen(
    debts: List<DebtRecordEntity>,
    wallets: List<WalletEntity>,
    onBack: () -> Unit,
    onSaveDebt: (
        editingDebt: DebtRecordEntity?,
        personName: String,
        amount: Long,
        direction: String,
        walletId: Long?,
        dueDateMillis: Long?,
        note: String
    ) -> Unit,
    onRecordPayment: (debt: DebtRecordEntity, paymentAmount: Long, walletId: Long?) -> Unit,
    onDeleteDebt: (DebtRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val extended = LocalSakuColors.current
    var filterTab by rememberSaveable { mutableStateOf("ALL") } // ALL, LEND, BORROW, SETTLED
    var showFormDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtRecordEntity?>(null) }
    var payingDebt by remember { mutableStateOf<DebtRecordEntity?>(null) }

    val totalPiutangSisa = remember(debts) {
        debts.filter { it.direction == DebtDirection.LEND && !it.isSettled }
            .sumOf { (it.amount - it.paidAmount).coerceAtLeast(0L) }
    }
    val totalHutangSisa = remember(debts) {
        debts.filter { it.direction == DebtDirection.BORROW && !it.isSettled }
            .sumOf { (it.amount - it.paidAmount).coerceAtLeast(0L) }
    }

    val filteredDebts = remember(debts, filterTab) {
        when (filterTab) {
            "LEND" -> debts.filter { it.direction == DebtDirection.LEND && !it.isSettled }
            "BORROW" -> debts.filter { it.direction == DebtDirection.BORROW && !it.isSettled }
            "SETTLED" -> debts.filter { it.isSettled }
            else -> debts
        }
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
            item(key = "debts_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ObsidianSubScreenHeader(
                        title = "Hutang & Piutang",
                        subtitle = "Catat pinjaman, cicilan & jadwal jatuh tempo",
                        onBack = onBack,
                        backTestTag = "debts_back_btn",
                        trailingAction = {
                            ObsidianCircleIconButton(
                                icon = Icons.Rounded.Add,
                                contentDescription = "Tambah catatan hutang atau piutang",
                                isActive = true,
                                onClick = {
                                    editingDebt = null
                                    showFormDialog = true
                                },
                                modifier = Modifier.testTag("add_debt_btn")
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FintechCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = null,
                                        tint = extended.income,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Piutang Aktif",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = CurrencyFormatter.formatRupiah(totalPiutangSisa),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = extended.income,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        FintechCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = null,
                                        tint = extended.expense,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Hutang Aktif",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = CurrencyFormatter.formatRupiah(totalHutangSisa),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = extended.expense,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item(key = "debts_sheet") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 32.dp, start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "ALL" to "Semua (${debts.size})",
                                "LEND" to "Piutang",
                                "BORROW" to "Hutang",
                                "SETTLED" to "Lunas"
                            ).forEach { (key, lbl) ->
                                FilterChip(
                                    selected = filterTab == key,
                                    onClick = { filterTab = key },
                                    label = { Text(lbl) },
                                    shape = RoundedCornerShape(999.dp)
                                )
                            }
                        }

                        if (filteredDebts.isEmpty()) {
                            FintechCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(extended.periwinkleContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                            contentDescription = null,
                                            tint = if (extended.isDark) extended.periwinkle else Color(0xFF4338CA),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Text(
                                        text = "Tidak ada catatan di tab ini",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Catat uang yang dipinjam teman atau cicilan agar mudah ditagih & dilunasi.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            filteredDebts.forEach { debt ->
                                DebtRecordCard(
                                    debt = debt,
                                    onPay = { payingDebt = debt },
                                    onEdit = {
                                        editingDebt = debt
                                        showFormDialog = true
                                    },
                                    onDelete = { onDeleteDebt(debt) }
                                )
                            }
                        }
                }
            }
        }
    }

    if (showFormDialog) {
        DebtFormDialog(
            initialDebt = editingDebt,
            wallets = wallets,
            onDismiss = { showFormDialog = false },
            onConfirm = { name, amount, direction, walletId, dueMillis, note ->
                onSaveDebt(editingDebt, name, amount, direction, walletId, dueMillis, note)
                showFormDialog = false
            }
        )
    }

    payingDebt?.let { debt ->
        DebtPaymentDialog(
            debt = debt,
            wallets = wallets,
            onDismiss = { payingDebt = null },
            onConfirm = { amount, walletId ->
                onRecordPayment(debt, amount, walletId)
                payingDebt = null
            }
        )
    }
}

@Composable
private fun DebtRecordCard(
    debt: DebtRecordEntity,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extended = LocalSakuColors.current
    val isPiutang = debt.direction == DebtDirection.LEND
    val remaining = (debt.amount - debt.paidAmount).coerceAtLeast(0L)
    val ratio = if (debt.amount > 0L) (debt.paidAmount.toFloat() / debt.amount.toFloat()).coerceIn(0f, 1f) else 0f
    val accentTint = if (isPiutang) extended.income else extended.expense
    val isDueSoon = !debt.isSettled && debt.dueDateMillis != null &&
        debt.dueDateMillis <= System.currentTimeMillis() + 3L * 86_400_000L

    FintechCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_card_${debt.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (isPiutang) extended.incomeContainer else extended.expenseContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPiutang) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (debt.isSettled) {
                            extended.incomeContainer
                        } else if (isPiutang) {
                            extended.limeContainer
                        } else {
                            extended.expenseContainer
                        }
                    ) {
                        Text(
                            text = when {
                                debt.isSettled -> "Lunas"
                                isPiutang -> "Piutang"
                                else -> "Hutang"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                debt.isSettled -> extended.onIncomeContainer
                                isPiutang -> extended.onLimeContainer
                                else -> extended.onExpenseContainer
                            },
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                val subtitleParts = buildList {
                    if (debt.note.isNotBlank()) add(debt.note)
                    debt.dueDateMillis?.let { add("Jatuh tempo ${DateFormatter.formatShortDate(it)}") }
                }
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        text = subtitleParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDueSoon) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit catatan", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus catatan", tint = extended.expense, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (debt.isSettled) "Sudah lunas" else "Sisa ${CurrencyFormatter.formatRupiah(remaining)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (debt.isSettled) extended.income else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Terbayar ${CurrencyFormatter.formatRupiah(debt.paidAmount)} dari ${CurrencyFormatter.formatRupiah(debt.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (debt.isSettled) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Lunas",
                    tint = extended.income,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = accentTint,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )

        if (!debt.isSettled) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onPay,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("debt_pay_btn_${debt.id}")
            ) {
                Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPiutang) "Catat Pelunasan Diterima" else "Catat Bayar / Cicilan",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DebtFormDialog(
    initialDebt: DebtRecordEntity?,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        personName: String,
        amount: Long,
        direction: String,
        walletId: Long?,
        dueDateMillis: Long?,
        note: String
    ) -> Unit
) {
    val extended = LocalSakuColors.current
    var personName by remember { mutableStateOf(initialDebt?.personName.orEmpty()) }
    var amountText by remember { mutableStateOf(initialDebt?.amount?.toString().orEmpty()) }
    var direction by remember { mutableStateOf(initialDebt?.direction ?: DebtDirection.LEND) }
    var note by remember { mutableStateOf(initialDebt?.note.orEmpty()) }
    var selectedWalletId by remember { mutableStateOf<Long?>(initialDebt?.walletId ?: wallets.firstOrNull()?.id) }
    var dueDaysOffset by remember { mutableStateOf<Int?>(null) }

    val parsedAmount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L

    SakuBottomDrawer(
        title = if (initialDebt == null) "Catat Hutang / Piutang" else "Edit Catatan",
        subtitle = "Pantau pinjaman dan jadwal pelunasannya secara rapi",
        onDismiss = onDismiss
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(DebtDirection.LEND to "Piutang", DebtDirection.BORROW to "Hutang")
            options.forEachIndexed { idx, (dir, label) ->
                SegmentedButton(
                    selected = direction == dir,
                    onClick = { direction = dir },
                    shape = SegmentedButtonDefaults.itemShape(idx, options.size)
                ) {
                    Text(label)
                }
            }
        }

        OutlinedTextField(
            value = personName,
            onValueChange = { personName = it.take(60) },
            label = { Text(if (direction == DebtDirection.LEND) "Nama Peminjam" else "Nama Pemberi Pinjaman") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Nominal (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(150) },
            label = { Text("Catatan (opsional)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (initialDebt == null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Jatuh Tempo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null to "Tanpa batas", 7 to "7 Hari", 14 to "14 Hari", 30 to "30 Hari").forEach { (days, lbl) ->
                        FilterChip(
                            selected = dueDaysOffset == days,
                            onClick = { dueDaysOffset = days },
                            label = { Text(lbl) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, extended.cardBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Batal", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    val due = dueDaysOffset?.let { System.currentTimeMillis() + it * 86_400_000L }
                        ?: initialDebt?.dueDateMillis
                    onConfirm(personName, parsedAmount, direction, selectedWalletId, due, note)
                },
                enabled = personName.isNotBlank() && parsedAmount > 0L,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DebtPaymentDialog(
    debt: DebtRecordEntity,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (paymentAmount: Long, walletId: Long?) -> Unit
) {
    val extended = LocalSakuColors.current
    val remaining = (debt.amount - debt.paidAmount).coerceAtLeast(0L)
    var amountText by remember { mutableStateOf(remaining.toString()) }
    var selectedWalletId by remember { mutableStateOf<Long?>(debt.walletId ?: wallets.firstOrNull()?.id) }
    val parsedAmount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L

    SakuBottomDrawer(
        title = if (debt.direction == DebtDirection.LEND) {
            "Terima dari ${debt.personName}"
        } else {
            "Bayar ke ${debt.personName}"
        },
        subtitle = "Sisa tagihan: ${CurrencyFormatter.formatRupiah(remaining)}",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Nominal Pembayaran (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = parsedAmount == remaining,
                onClick = { amountText = remaining.toString() },
                label = { Text("Lunasi (${CurrencyFormatter.formatCompactRupiah(remaining)})") }
            )
            if (remaining >= 200_000L) {
                val half = remaining / 2L
                FilterChip(
                    selected = parsedAmount == half,
                    onClick = { amountText = half.toString() },
                    label = { Text("50%") }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Dompet Terkait (otomatis catat mutasi)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
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
                    label = { Text("Tanpa dompet") }
                )
                wallets.forEach { w ->
                    FilterChip(
                        selected = selectedWalletId == w.id,
                        onClick = { selectedWalletId = w.id },
                        label = { Text(w.name) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, extended.cardBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Batal", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onConfirm(parsedAmount.coerceAtMost(remaining), selectedWalletId) },
                enabled = parsedAmount > 0L,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Konfirmasi", fontWeight = FontWeight.Bold)
            }
        }
    }
}
