package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BudgetEntity
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.SakuBottomDrawer
import com.example.ui.components.SakuBrandMark
import com.example.ui.theme.LocalSakuBottomPadding
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.viewmodel.AnggaranUiState
import com.example.ui.viewmodel.BudgetAlertLevel
import com.example.ui.viewmodel.BudgetProgressItem
import com.example.util.CurrencyFormatter
import com.example.util.FinanceCategories
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun AnggaranScreen(
    uiState: AnggaranUiState,
    onUpsertBudget: (String, Long) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }

    val monthTiming = remember {
        val cal = Calendar.getInstance()
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val today = cal.get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDay)
        val remaining = (maxDay - today + 1).coerceAtLeast(1)
        val elapsedFraction = (today.toFloat() / maxDay.toFloat()).coerceIn(0.05f, 1f)
        Triple(today, remaining, elapsedFraction)
    }
    val todayDay = monthTiming.first
    val remainingDaysInMonth = monthTiming.second
    val monthElapsedFraction = monthTiming.third

    val totalRatio = if (uiState.totalLimit > 0L) {
        uiState.totalSpent.toFloat() / uiState.totalLimit.toFloat()
    } else 0f
    val animatedTotalRatio by animateFloatAsState(
        targetValue = totalRatio.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "total_budget_hero_ring"
    )
    val totalRemaining = uiState.totalLimit - uiState.totalSpent
    val totalPct = (totalRatio * 100f).roundToInt()
    val isAheadOfPace = uiState.totalLimit > 0L && totalRatio > (monthElapsedFraction + 0.08f)

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
            // 1. Clean Top Header Bar
            item(key = "anggaran_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SakuBrandMark(size = 40.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Anggaran Bulanan",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.monthLabel} • $remainingDaysInMonth hari tersisa",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            editingBudget = null
                            showBudgetDialog = true
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = extended.lime,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_budget_btn")
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
                                text = "Atur",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = extended.onLime,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 2. Crisp White Budget Ring Hero Card
            item(key = "anggaran_hero_card") {
                val ringColorStops = when {
                    totalRatio >= 0.90f -> listOf(Color(0xFFFB7185), extended.expense)
                    totalRatio >= 0.75f -> listOf(Color(0xFFFBBF24), extended.warning)
                    else -> listOf(extended.lime, extended.periwinkle)
                }
                val ringTrackColor = if (extended.isDark) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color(0xFFEEF2F8)
                }

                FintechCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(156.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = 14.dp.toPx()
                                drawArc(
                                    color = ringTrackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )
                                if (animatedTotalRatio > 0f) {
                                    drawArc(
                                        brush = Brush.sweepGradient(ringColorStops),
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedTotalRatio,
                                        useCenter = false,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (uiState.totalLimit > 0L) "$totalPct%" else "0%",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "terpakai",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (uiState.totalLimit > 0L) {
                                    Text(
                                        text = "/ ${CurrencyFormatter.formatCompactRupiah(uiState.totalLimit)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = extended.lime
                                    )
                                }
                            }
                        }

                        HorizontalDivider(thickness = 0.7.dp, color = extended.subtleDivider)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Terpakai",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(uiState.totalSpent),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val (badgeBg, badgeText) = when {
                                uiState.totalLimit == 0L -> extended.periwinkleContainer to extended.periwinkle
                                isAheadOfPace -> extended.warningContainer to extended.warning
                                else -> extended.incomeContainer to extended.income
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = when {
                                        uiState.totalLimit == 0L -> "Belum diatur"
                                        isAheadOfPace -> "Laju cepat • H-$todayDay"
                                        else -> "Laju aman • H-$todayDay"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = badgeText,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Batas ${CurrencyFormatter.formatRupiah(uiState.totalLimit)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (totalRemaining >= 0L) {
                                    "Sisa ${CurrencyFormatter.formatCompactRupiah(totalRemaining)}"
                                } else {
                                    "Lebih ${CurrencyFormatter.formatCompactRupiah(-totalRemaining)}"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = if (totalRemaining >= 0L) extended.income else extended.expense,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 3. Pos Anggaran Section
            if (uiState.items.isNotEmpty()) {
                item(key = "anggaran_list_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pos Anggaran",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${uiState.items.size} kategori",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(uiState.items, key = { it.budget.id }) { item ->
                    BudgetProgressCard(
                        item = item,
                        remainingDaysInMonth = remainingDaysInMonth,
                        monthElapsedFraction = monthElapsedFraction,
                        onEdit = {
                            editingBudget = item.budget
                            showBudgetDialog = true
                        },
                        onDelete = { onDeleteBudget(item.budget) }
                    )
                }
            } else {
                item(key = "anggaran_empty") {
                    FintechCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(extended.limeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TrackChanges,
                                    contentDescription = null,
                                    tint = extended.lime,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = "Belum ada anggaran diatur",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tekan tombol Atur di kanan atas untuk menetapkan batas belanja bulanan per kategori.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    if (showBudgetDialog) {
        BudgetEditDialog(
            initialBudget = editingBudget,
            onDismiss = { showBudgetDialog = false },
            onSave = { category, limit ->
                onUpsertBudget(category, limit)
                showBudgetDialog = false
            }
        )
    }
}

@Composable
private fun BudgetProgressCard(
    item: BudgetProgressItem,
    remainingDaysInMonth: Int,
    monthElapsedFraction: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extended = LocalSakuColors.current
    val meta = FinanceCategories.getMeta(item.budget.category)
    val pct = (item.ratio * 100f).roundToInt()

    val statusColor = when (item.alertLevel) {
        BudgetAlertLevel.CRITICAL_90 -> extended.expense
        BudgetAlertLevel.WARNING_75 -> extended.warning
        BudgetAlertLevel.SAFE -> extended.income
    }
    val statusBadgeText = when (item.alertLevel) {
        BudgetAlertLevel.CRITICAL_90 -> if (item.ratio >= 1f) "Melebihi Batas ($pct%)" else "Kritis ($pct%)"
        BudgetAlertLevel.WARNING_75 -> "Waspada ($pct%)"
        BudgetAlertLevel.SAFE -> "Aman ($pct%)"
    }

    val animatedProgress by animateFloatAsState(
        targetValue = item.ratio.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "budget_item_${item.budget.id}"
    )

    val remaining = item.budget.monthlyLimit - item.spent
    val dailySafe = if (remaining > 0L) remaining / remainingDaysInMonth else 0L

    FintechCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(meta.color.copy(alpha = if (extended.isDark) 0.20f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(meta.icon, contentDescription = item.budget.category, tint = meta.color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.budget.category,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${CurrencyFormatter.formatRupiah(item.spent)} / ${CurrencyFormatter.formatRupiah(item.budget.monthlyLimit)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit anggaran", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus anggaran", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val markerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            val barHeight = 7.dp.toPx()
            val barTop = (size.height - barHeight) / 2f
            val corner = CornerRadius(barHeight / 2f, barHeight / 2f)

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, barTop),
                size = Size(size.width, barHeight),
                cornerRadius = corner
            )
            if (animatedProgress > 0f) {
                drawRoundRect(
                    color = statusColor,
                    topLeft = Offset(0f, barTop),
                    size = Size((size.width * animatedProgress).coerceAtLeast(barHeight), barHeight),
                    cornerRadius = corner
                )
            }
            val paceX = (size.width * monthElapsedFraction).coerceIn(2.dp.toPx(), size.width - 2.dp.toPx())
            drawLine(
                color = markerColor,
                start = Offset(paceX, 0f),
                end = Offset(paceX, size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = statusColor.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.alertLevel != BudgetAlertLevel.SAFE) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (remaining >= 0L) {
                        "Sisa ${CurrencyFormatter.formatRupiah(remaining)}"
                    } else {
                        "Lebih ${CurrencyFormatter.formatRupiah(-remaining)}"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (remaining >= 0L) MaterialTheme.colorScheme.onSurface else extended.expense,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (dailySafe > 0L) {
                    Text(
                        text = "~${CurrencyFormatter.formatCompactRupiah(dailySafe)}/hari",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetEditDialog(
    initialBudget: BudgetEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    val extended = LocalSakuColors.current
    var selectedCategory by remember {
        mutableStateOf(initialBudget?.category ?: FinanceCategories.expenseCategoryNames.first())
    }
    var limitInput by remember {
        mutableStateOf(initialBudget?.monthlyLimit?.toString() ?: "500000")
    }
    val amount = limitInput.toLongOrNull() ?: 0L

    SakuBottomDrawer(
        title = if (initialBudget != null) "Edit Anggaran" else "Tambah Anggaran",
        subtitle = "Tetapkan batas belanja bulanan per kategori agar tetap terkontrol",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Pilih Kategori",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceCategories.expenseCategoryNames.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { if (initialBudget == null) selectedCategory = cat },
                        enabled = initialBudget == null || selectedCategory == cat,
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = limitInput,
            onValueChange = { raw -> limitInput = raw.filter { it.isDigit() } },
            label = { Text("Batas Bulanan (Rp)") },
            supportingText = {
                if (amount > 0L) {
                    Text("Batas: ${CurrencyFormatter.formatRupiah(amount)}")
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

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
                onClick = { onSave(selectedCategory, amount) },
                enabled = amount > 0L,
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
