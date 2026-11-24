package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LocalSakuColors
import com.example.ui.viewmodel.AnggaranUiState
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.widget.SakuWidgetType
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakuWidgetGallerySheet(
    netBalance: Long,
    monthlyIncome: Long,
    monthlyExpense: Long,
    walletCount: Int,
    primaryWalletName: String,
    anggaranState: AnggaranUiState,
    hideBalance: Boolean,
    billingCycleStartDay: Int,
    onDismiss: () -> Unit,
    onPinWidget: (SakuWidgetType) -> Unit
) {
    val extended = LocalSakuColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val periodLabel = DateFormatter.cycleLabel(System.currentTimeMillis(), billingCycleStartDay)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = extended.sheetRadius, topEnd = extended.sheetRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(extended.limeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Widgets,
                            contentDescription = null,
                            tint = extended.onLimeContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Galeri Widget Saku",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Pilih gaya widget favorit untuk layar utama HP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup galeri widget"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SakuWidgetType.entries.forEach { widgetType ->
                    item(key = widgetType.name) {
                        WidgetGalleryCard(
                            type = widgetType,
                            onPin = { onPinWidget(widgetType) }
                        ) {
                            when (widgetType) {
                                SakuWidgetType.BALANCE_HERO -> BalanceWidgetPreview(
                                    netBalance = netBalance,
                                    income = monthlyIncome,
                                    expense = monthlyExpense,
                                    walletCount = walletCount,
                                    periodLabel = periodLabel,
                                    hideBalance = hideBalance
                                )
                                SakuWidgetType.QUICK_CAPTURE -> QuickCaptureWidgetPreview(
                                    todayExpense = monthlyExpense / 30L,
                                    hideBalance = hideBalance
                                )
                                SakuWidgetType.BUDGET_PULSE -> BudgetPulseWidgetPreview(
                                    anggaranState = anggaranState,
                                    hideBalance = hideBalance
                                )
                                SakuWidgetType.COMPACT_COBALT -> CompactMiniWidgetPreview(
                                    netBalance = netBalance,
                                    primaryWalletName = primaryWalletName,
                                    walletCount = walletCount,
                                    hideBalance = hideBalance
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetGalleryCard(
    type: SakuWidgetType,
    onPin: () -> Unit,
    previewContent: @Composable () -> Unit
) {
    val extended = LocalSakuColors.current
    FintechCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = extended.cardRadius,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = type.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = type.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = extended.limeContainer
            ) {
                Text(
                    text = type.sizeBadge,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = extended.onLimeContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (extended.isDark) Color(0xFF070B14) else Color(0xFFE3EAF6)
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            previewContent()
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onPin,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = extended.lime,
                contentColor = extended.onLime
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pin_widget_${type.name.lowercase()}")
        ) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Pasang ${type.title}",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BalanceWidgetPreview(
    netBalance: Long,
    income: Long,
    expense: Long,
    walletCount: Int,
    periodLabel: String,
    hideBalance: Boolean
) {
    val extended = LocalSakuColors.current
    val surfaceBg = if (extended.isDark) Color(0xFF0F172A) else Color.White
    val tileBg = if (extended.isDark) Color(0xFF1A2438) else Color(0xFFF3F6FC)
    val titleCol = if (extended.isDark) Color.White else Color(0xFF0F172A)
    val mutedCol = if (extended.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cobalt = if (extended.isDark) Color(0xFF3B82F6) else Color(0xFF1D56F5)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surfaceBg,
        border = BorderStroke(0.8.dp, extended.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SakuBrandMark(size = 24.dp, cornerRadius = 7.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Saku • $periodLabel",
                    fontSize = 11.sp,
                    color = mutedCol,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(999.dp), color = cobalt) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text("Catat", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hideBalance) "Rp ••••••••" else CurrencyFormatter.formatRupiah(netBalance),
                    color = titleCol,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(999.dp), color = tileBg) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.AccountBalanceWallet, null, tint = mutedCol, modifier = Modifier.size(11.dp))
                        Text("$walletCount Dompet", color = mutedCol, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = tileBg, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Rounded.SouthWest, null, tint = IncomeEmerald, modifier = Modifier.size(11.dp))
                            Text("Masuk", color = mutedCol, fontSize = 10.sp)
                        }
                        Text(
                            text = if (hideBalance) "Rp ••••" else CurrencyFormatter.formatCompactRupiah(income),
                            color = IncomeEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(14.dp), color = tileBg, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Rounded.NorthEast, null, tint = ExpenseCoral, modifier = Modifier.size(11.dp))
                            Text("Keluar", color = mutedCol, fontSize = 10.sp)
                        }
                        Text(
                            text = if (hideBalance) "Rp ••••" else CurrencyFormatter.formatCompactRupiah(expense),
                            color = ExpenseCoral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (extended.isDark) Color(0xFF1E3A8A) else Color(0xFFEEF3FF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = cobalt, modifier = Modifier.size(14.dp))
                        Text("AI", color = cobalt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCaptureWidgetPreview(
    todayExpense: Long,
    hideBalance: Boolean
) {
    val extended = LocalSakuColors.current
    val surfaceBg = if (extended.isDark) Color(0xFF0F172A) else Color.White
    val titleCol = if (extended.isDark) Color.White else Color(0xFF0F172A)
    val mutedCol = if (extended.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val items = listOf(
        Triple("Manual", Icons.Rounded.Add, Color(0xFF1D56F5) to Color(0xFFEEF3FF)),
        Triple("Tanya AI", Icons.Rounded.AutoAwesome, Color(0xFF7C3AED) to Color(0xFFF3EEFF)),
        Triple("Suara", Icons.Rounded.Mic, Color(0xFF059669) to Color(0xFFECFDF5)),
        Triple("Struk", Icons.Rounded.CameraAlt, Color(0xFFD97706) to Color(0xFFFFF7ED))
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surfaceBg,
        border = BorderStroke(0.8.dp, extended.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SakuBrandMark(size = 22.dp, cornerRadius = 6.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Catat Cepat",
                    color = titleCol,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (hideBalance) "Hari ini •••••" else "Hari ini ${CurrencyFormatter.formatCompactRupiah(todayExpense)}",
                    color = mutedCol,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items.forEach { (label, icon, colors) ->
                    val fg = colors.first
                    val bg = if (extended.isDark) fg.copy(alpha = 0.18f) else colors.second
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = bg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetPulseWidgetPreview(
    anggaranState: AnggaranUiState,
    hideBalance: Boolean
) {
    val extended = LocalSakuColors.current
    val surfaceBg = if (extended.isDark) Color(0xFF0F172A) else Color.White
    val tileBg = if (extended.isDark) Color(0xFF1A2438) else Color(0xFFF3F6FC)
    val trackCol = if (extended.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val titleCol = if (extended.isDark) Color.White else Color(0xFF0F172A)
    val mutedCol = if (extended.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val hasBudget = anggaranState.totalLimit > 0L
    val pct = if (hasBudget) {
        ((anggaranState.totalSpent.toDouble() / anggaranState.totalLimit) * 100.0).roundToInt().coerceAtLeast(0)
    } else 64
    val accent = when {
        pct >= 100 -> ExpenseCoral
        pct >= 75 -> Color(0xFFD97706)
        else -> IncomeEmerald
    }
    val filledSegs = ((pct.coerceIn(0, 100) / 100f) * 8f).roundToInt().coerceIn(1, 8)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = surfaceBg,
        border = BorderStroke(0.8.dp, extended.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SakuBrandMark(size = 22.dp, cornerRadius = 6.dp)
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Anggaran ${anggaranState.monthLabel}",
                    color = mutedCol,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(999.dp), color = accent.copy(alpha = 0.14f)) {
                    Text(
                        text = if (hasBudget) "${if (pct >= 100) "Melebihi" else if (pct >= 75) "Waspada" else "Aman"} $pct%" else "Contoh 64%",
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val rem = if (hasBudget) anggaranState.totalLimit - anggaranState.totalSpent else 1_450_000L
                Text(
                    text = if (hideBalance) "Sisa Rp •••••" else "Sisa ${CurrencyFormatter.formatCompactRupiah(rem.coerceAtLeast(0L))}",
                    color = titleCol,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasBudget && !hideBalance) {
                        "${CurrencyFormatter.formatCompactRupiah(anggaranState.totalSpent)} / ${CurrencyFormatter.formatCompactRupiah(anggaranState.totalLimit)}"
                    } else if (hideBalance) "•••• / ••••" else "Rp 2,5 jt / Rp 4,0 jt",
                    color = mutedCol,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until 8) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(if (i < filledSegs) accent else trackCol)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                val previewCats = if (anggaranState.items.isNotEmpty()) {
                    anggaranState.items.take(2).map {
                        it.budget.category to (it.ratio * 100f).roundToInt()
                    }
                } else {
                    listOf("Makanan" to 68, "Transport" to 42)
                }
                previewCats.forEach { (cat, catPct) ->
                    Surface(shape = RoundedCornerShape(12.dp), color = tileBg, modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat, color = titleCol, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text("$catPct%", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMiniWidgetPreview(
    netBalance: Long,
    primaryWalletName: String,
    walletCount: Int,
    hideBalance: Boolean
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        modifier = Modifier.width(172.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF172554))
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SakuBrandMark(size = 22.dp, cornerRadius = 6.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Saku", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.20f)) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(5.dp).size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Total Saldo", color = Color(0xFFDBEAFE), fontSize = 10.sp)
            Text(
                text = if (hideBalance) "Rp ••••••" else CurrencyFormatter.formatCompactRupiah(netBalance),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "$primaryWalletName • $walletCount dompet",
                color = Color(0xFFDBEAFE),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(shape = RoundedCornerShape(999.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Color(0xFF1D56F5), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Catat", color = Color(0xFF1D56F5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
