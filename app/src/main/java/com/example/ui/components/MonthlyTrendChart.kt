package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalSakuColors
import com.example.ui.viewmodel.MonthlyBarPoint
import com.example.util.CurrencyFormatter

/**
 * Smooth interactive dual-spline wave chart (Electric Lime for Income, Soft Periwinkle for Expense)
 * with a glowing vertical column highlight and floating lime tooltip capsule, inspired by Screen 2.
 */
@Composable
fun MonthlyIncomeExpenseChartCard(
    months: List<MonthlyBarPoint>,
    modifier: Modifier = Modifier,
    onOpenDetailClick: (() -> Unit)? = null,
    showIncomeCurve: Boolean = true,
    showExpenseCurve: Boolean = true,
    onObsidianCanvas: Boolean = false
) {
    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current
    var selectedIdx by remember(months.size) {
        mutableIntStateOf((months.lastIndex).coerceAtLeast(0))
    }

    val maxVal = remember(months, showIncomeCurve, showExpenseCurve) {
        months.maxOfOrNull {
            maxOf(
                if (showIncomeCurve) it.income else 0L,
                if (showExpenseCurve) it.expense else 0L
            )
        }?.coerceAtLeast(1L) ?: 1L
    }
    val selectedMonth = months.getOrNull(selectedIdx)

    val containerBg = if (onObsidianCanvas) Color.Transparent else MaterialTheme.colorScheme.surface
    val primaryInk = MaterialTheme.colorScheme.onSurface
    val mutedInk = MaterialTheme.colorScheme.onSurfaceVariant
    val incomeCurveColor = extended.lime
    val expenseCurveColor = extended.periwinkle
    val baselineColor = extended.subtleDivider

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (onObsidianCanvas) 0.dp else extended.cardRadius),
        color = containerBg,
        border = if (onObsidianCanvas) null else BorderStroke(0.9.dp, extended.cardBorder),
        shadowElevation = if (onObsidianCanvas || extended.isDark) 0.dp else 1.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (onObsidianCanvas) 0.dp else 18.dp,
                    vertical = if (onObsidianCanvas) 4.dp else 16.dp
                )
        ) {
            if (!onObsidianCanvas) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pemasukan vs Pengeluaran (6 Bln)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = primaryInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Ketuk bulan untuk melihat detail titik kurva",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedInk
                        )
                    }

                    if (onOpenDetailClick != null) {
                        Surface(
                            onClick = onOpenDetailClick,
                            shape = RoundedCornerShape(999.dp),
                            color = extended.limeContainer,
                            modifier = Modifier.testTag("analisis_open_monthly_overview_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Detail",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = extended.lime
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Buka Ringkasan 6 Bulan",
                                    tint = extended.lime,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (selectedMonth != null) {
                val tooltipAmount = when {
                    showIncomeCurve && !showExpenseCurve -> selectedMonth.income
                    !showIncomeCurve && showExpenseCurve -> selectedMonth.expense
                    else -> maxOf(selectedMonth.income, selectedMonth.expense)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showIncomeCurve) {
                            ChartLegendDot(color = incomeCurveColor, label = "Masuk")
                        }
                        if (showExpenseCurve) {
                            ChartLegendDot(color = expenseCurveColor, label = "Keluar")
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = incomeCurveColor
                    ) {
                        Text(
                            text = "${selectedMonth.shortLabel}: ${CurrencyFormatter.formatCompactRupiah(tooltipAmount)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = extended.onLime,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                ) {
                    val count = months.size
                    if (count == 0) return@Canvas
                    val stepX = if (count > 1) size.width / count.toFloat() else size.width
                    val topPad = 14.dp.toPx()
                    val bottomPad = 14.dp.toPx()
                    val usableH = (size.height - topPad - bottomPad).coerceAtLeast(10f)

                    fun pointOffset(index: Int, value: Long): Offset {
                        val x = stepX * index + stepX / 2f
                        val ratio = (value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                        val y = topPad + usableH * (1f - ratio)
                        return Offset(x, y)
                    }

                    val incPoints = months.mapIndexed { idx, p -> pointOffset(idx, p.income) }
                    val expPoints = months.mapIndexed { idx, p -> pointOffset(idx, p.expense) }

                    fun buildSmoothPath(pts: List<Offset>): Path {
                        val path = Path()
                        if (pts.isEmpty()) return path
                        path.moveTo(0f, pts.first().y)
                        path.lineTo(pts.first().x, pts.first().y)
                        for (i in 0 until pts.lastIndex) {
                            val p0 = pts[i]
                            val p1 = pts[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                        path.lineTo(size.width, pts.last().y)
                        return path
                    }

                    drawLine(
                        color = baselineColor,
                        start = Offset(0f, size.height - bottomPad),
                        end = Offset(size.width, size.height - bottomPad),
                        strokeWidth = 1.dp.toPx()
                    )

                    val selIdxClamped = selectedIdx.coerceIn(0, months.lastIndex)
                    val activeX = stepX * selIdxClamped + stepX / 2f
                    val beamWidth = 34.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                incomeCurveColor.copy(alpha = if (extended.isDark) 0.24f else 0.14f),
                                incomeCurveColor.copy(alpha = 0.03f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(activeX - beamWidth / 2f, 0f),
                        size = androidx.compose.ui.geometry.Size(beamWidth, size.height - bottomPad)
                    )

                    if (showExpenseCurve) {
                        val expPath = buildSmoothPath(expPoints)
                        drawPath(
                            path = expPath,
                            color = expenseCurveColor.copy(alpha = 0.90f),
                            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    if (showIncomeCurve) {
                        val incPath = buildSmoothPath(incPoints)
                        drawPath(
                            path = incPath,
                            color = incomeCurveColor,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    val focusPt = when {
                        showIncomeCurve && !showExpenseCurve -> incPoints[selIdxClamped]
                        !showIncomeCurve && showExpenseCurve -> expPoints[selIdxClamped]
                        else -> if (months[selIdxClamped].income >= months[selIdxClamped].expense) {
                            incPoints[selIdxClamped]
                        } else {
                            expPoints[selIdxClamped]
                        }
                    }
                    val dotColor = if (!showIncomeCurve && showExpenseCurve) expenseCurveColor else incomeCurveColor
                    drawCircle(
                        color = dotColor.copy(alpha = 0.24f),
                        radius = 10.dp.toPx(),
                        center = focusPt
                    )
                    drawCircle(
                        color = dotColor,
                        radius = 5.dp.toPx(),
                        center = focusPt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.4.dp.toPx(),
                        center = focusPt
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                months.forEachIndexed { index, point ->
                    val isSelected = index == selectedIdx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) extended.limeContainer else Color.Transparent
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedIdx = index
                            }
                            .padding(vertical = 6.dp)
                            .testTag("monthly_bar_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = point.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) incomeCurveColor else mutedInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (selectedMonth != null && !onObsidianCanvas) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.7.dp, color = extended.subtleDivider)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pemasukan (${selectedMonth.fullLabel})",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(selectedMonth.income),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = extended.income,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Pengeluaran",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedInk,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(selectedMonth.expense),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = expenseCurveColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
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
