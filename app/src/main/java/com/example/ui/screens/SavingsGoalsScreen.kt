package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.WalletEntity
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.ObsidianCircleIconButton
import com.example.ui.components.ObsidianSubScreenHeader
import com.example.ui.components.SakuBottomDrawer
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.theme.ObsidianCardBorder
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import kotlin.math.roundToInt

private val goalAccentOptions = listOf(
    0xFF1D56F5L,
    0xFF7C5DFAL,
    0xFF10B981L,
    0xFFF59E0BL,
    0xFFEF4444L
)

private val goalIconOptions = listOf(
    "SAVINGS" to "Tabungan",
    "SHIELD" to "Darurat",
    "FLIGHT" to "Liburan",
    "HOME" to "Rumah",
    "LAPTOP" to "Gadget",
    "CAR" to "Kendaraan",
    "SCHOOL" to "Pendidikan"
)

private fun goalIconVector(name: String): ImageVector = when (name) {
    "SHIELD" -> Icons.Rounded.Shield
    "FLIGHT" -> Icons.Rounded.Flight
    "HOME" -> Icons.Rounded.Home
    "LAPTOP" -> Icons.Rounded.LaptopMac
    "CAR" -> Icons.Rounded.DirectionsCar
    "SCHOOL" -> Icons.Rounded.School
    else -> Icons.Rounded.Savings
}

@Composable
fun SavingsGoalsScreen(
    goals: List<SavingsGoalEntity>,
    wallets: List<WalletEntity>,
    onBack: () -> Unit,
    onSaveGoal: (
        editingGoal: SavingsGoalEntity?,
        title: String,
        targetAmount: Long,
        savedAmount: Long,
        deadlineMillis: Long?,
        accentHex: Long,
        iconName: String,
        note: String
    ) -> Unit,
    onContributeGoal: (goal: SavingsGoalEntity, amount: Long, walletId: Long?, isWithdraw: Boolean) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val extended = LocalSakuColors.current
    var showEditDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var contributingGoal by remember { mutableStateOf<Pair<SavingsGoalEntity, Boolean>?>(null) }

    val totalSaved = remember(goals) { goals.sumOf { it.savedAmount } }
    val totalTarget = remember(goals) { goals.sumOf { it.targetAmount } }
    val overallProgress = if (totalTarget > 0L) (totalSaved.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f) else 0f

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
            item(key = "savings_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ObsidianSubScreenHeader(
                        title = "Target Tabungan",
                        subtitle = "${goals.count { !it.isCompleted }} target aktif • ${goals.count { it.isCompleted }} tercapai",
                        onBack = onBack,
                        backTestTag = "savings_back_btn",
                        trailingAction = {
                            ObsidianCircleIconButton(
                                icon = Icons.Rounded.Add,
                                contentDescription = "Tambah target tabungan",
                                isActive = true,
                                onClick = {
                                    editingGoal = null
                                    showEditDialog = true
                                },
                                modifier = Modifier.testTag("add_savings_goal_btn")
                            )
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(extended.cardRadius),
                        color = Color.Transparent,
                        border = BorderStroke(0.8.dp, ObsidianCardBorder),
                        shadowElevation = if (extended.isDark) 0.dp else 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(extended.heroStart, extended.heroMid, extended.heroEnd)
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Total Dana Terkumpul",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.80f)
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatRupiah(totalSaved),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color.White
                                    ) {
                                        Text(
                                            text = "${(overallProgress * 100f).roundToInt()}%",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = extended.heroMid,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { overallProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.22f),
                                    strokeCap = StrokeCap.Round
                                )

                                Text(
                                    text = "Dari total target ${CurrencyFormatter.formatRupiah(totalTarget)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.80f)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "savings_sheet") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 32.dp, start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        if (goals.isEmpty()) {
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
                                            .background(extended.limeContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Savings,
                                            contentDescription = null,
                                            tint = extended.onLimeContainer,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Text(
                                        text = "Belum ada target tabungan",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Buat target dana darurat, liburan, atau barang impianmu dan sisihkan sedikit demi sedikit.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = {
                                            editingGoal = null
                                            showEditDialog = true
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = extended.lime,
                                            contentColor = extended.onLime
                                        )
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Buat Target Pertama", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            goals.forEach { goal ->
                                SavingsGoalCard(
                                    goal = goal,
                                    onContribute = { contributingGoal = goal to false },
                                    onWithdraw = { contributingGoal = goal to true },
                                    onEdit = {
                                        editingGoal = goal
                                        showEditDialog = true
                                    },
                                    onDelete = { onDeleteGoal(goal) }
                                )
                            }
                        }
                }
            }
        }
    }

    if (showEditDialog) {
        SavingsGoalFormDialog(
            initialGoal = editingGoal,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, target, saved, deadline, accent, iconName, note ->
                onSaveGoal(editingGoal, title, target, saved, deadline, accent, iconName, note)
                showEditDialog = false
            }
        )
    }

    contributingGoal?.let { (goal, isWithdraw) ->
        SavingsContributeDialog(
            goal = goal,
            wallets = wallets,
            isWithdraw = isWithdraw,
            onDismiss = { contributingGoal = null },
            onConfirm = { amount, walletId ->
                onContributeGoal(goal, amount, walletId, isWithdraw)
                contributingGoal = null
            }
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extended = LocalSakuColors.current
    val accentColor = Color(goal.accentHex)
    val ratio = if (goal.targetAmount > 0L) {
        (goal.savedAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val pct = (ratio * 100f).roundToInt()
    val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0L)

    FintechCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("savings_goal_card_${goal.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = goalIconVector(goal.iconName),
                    contentDescription = null,
                    tint = if (accentColor.luminance() > 0.55f) Color(0xFF0F172A) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (goal.isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Tercapai",
                            tint = extended.income,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val deadlineLabel = goal.deadlineMillis?.let { "Target ${DateFormatter.formatShortDate(it)}" }
                    ?: goal.note.ifBlank { "Tabungan fleksibel" }
                Text(
                    text = deadlineLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit target", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Hapus target",
                    tint = extended.expense,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CurrencyFormatter.formatRupiah(goal.savedAmount),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (remaining > 0L) {
                        "Kurang ${CurrencyFormatter.formatRupiah(remaining)} dari ${CurrencyFormatter.formatRupiah(goal.targetAmount)}"
                    } else {
                        "Target ${CurrencyFormatter.formatRupiah(goal.targetAmount)} tercapai!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$pct%",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (goal.isCompleted) extended.income else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = if (goal.isCompleted) extended.income else accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onContribute,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .testTag("goal_contribute_btn_${goal.id}")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sisihkan", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            if (goal.savedAmount > 0L) {
                OutlinedButton(
                    onClick = onWithdraw,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .heightIn(min = 46.dp)
                        .testTag("goal_withdraw_btn_${goal.id}")
                ) {
                    Text("Tarik", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalFormDialog(
    initialGoal: SavingsGoalEntity?,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        targetAmount: Long,
        savedAmount: Long,
        deadlineMillis: Long?,
        accentHex: Long,
        iconName: String,
        note: String
    ) -> Unit
) {
    val extended = LocalSakuColors.current
    var title by remember { mutableStateOf(initialGoal?.title.orEmpty()) }
    var targetText by remember { mutableStateOf(initialGoal?.targetAmount?.toString().orEmpty()) }
    var savedText by remember { mutableStateOf(initialGoal?.savedAmount?.takeIf { it > 0L }?.toString().orEmpty()) }
    var note by remember { mutableStateOf(initialGoal?.note.orEmpty()) }
    var selectedAccent by remember { mutableLongStateOf(initialGoal?.accentHex ?: goalAccentOptions.first()) }
    var selectedIcon by remember { mutableStateOf(initialGoal?.iconName ?: "SAVINGS") }
    var deadlineOffsetMonths by remember { mutableStateOf<Int?>(null) }

    val parsedTarget = targetText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val parsedSaved = savedText.filter { it.isDigit() }.toLongOrNull() ?: 0L

    SakuBottomDrawer(
        title = if (initialGoal == null) "Target Tabungan Baru" else "Edit Target Tabungan",
        subtitle = "Sisihkan dana secara bertahap untuk tujuan finansialmu",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(60) },
            label = { Text("Nama Target") },
            placeholder = { Text("Contoh: Dana Darurat, Laptop Baru") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = targetText,
            onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Nominal Target (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = savedText,
            onValueChange = { savedText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Sudah Terkumpul Awal (opsional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(160) },
            label = { Text("Catatan") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Ikon", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                goalIconOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedIcon == key,
                        onClick = { selectedIcon = key },
                        leadingIcon = { Icon(goalIconVector(key), contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(label) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Warna Kartu", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                goalAccentOptions.forEach { hex ->
                    val isSel = selectedAccent == hex
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .then(
                                if (isSel) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier.border(0.8.dp, extended.cardBorder, CircleShape)
                            )
                            .clickable { selectedAccent = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSel) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (initialGoal == null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tenggat Waktu", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null to "Fleksibel", 3 to "3 Bln", 6 to "6 Bln", 12 to "1 Thn").forEach { (m, lbl) ->
                        FilterChip(
                            selected = deadlineOffsetMonths == m,
                            onClick = { deadlineOffsetMonths = m },
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
                    val computedDeadline = if (deadlineOffsetMonths != null) {
                        System.currentTimeMillis() + deadlineOffsetMonths!!.toLong() * 30L * 86_400_000L
                    } else {
                        initialGoal?.deadlineMillis
                    }
                    onConfirm(title, parsedTarget, parsedSaved, computedDeadline, selectedAccent, selectedIcon, note)
                },
                enabled = title.isNotBlank() && parsedTarget > 0L,
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
private fun SavingsContributeDialog(
    goal: SavingsGoalEntity,
    wallets: List<WalletEntity>,
    isWithdraw: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, walletId: Long?) -> Unit
) {
    val extended = LocalSakuColors.current
    var amountText by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf<Long?>(wallets.firstOrNull()?.id) }
    val parsedAmount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L

    SakuBottomDrawer(
        title = if (isWithdraw) "Tarik dari ${goal.title}" else "Sisihkan ke ${goal.title}",
        subtitle = "Terkumpul saat ini: ${CurrencyFormatter.formatRupiah(goal.savedAmount)}",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Nominal (Rp)") },
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
            listOf(100_000L to "100 rb", 250_000L to "250 rb", 500_000L to "500 rb").forEach { (amt, lbl) ->
                FilterChip(
                    selected = parsedAmount == amt,
                    onClick = { amountText = amt.toString() },
                    label = { Text(lbl) }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isWithdraw) "Masukkan ke dompet (catat riwayat)" else "Ambil dari dompet (catat pengeluaran)",
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
                    label = { Text("Catat progres saja") }
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
                onClick = { onConfirm(parsedAmount, selectedWalletId) },
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
                Text(
                    text = if (isWithdraw) "Tarik Dana" else "Simpan Setoran",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
