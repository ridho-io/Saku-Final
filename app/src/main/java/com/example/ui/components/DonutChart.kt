package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalSakuColors
import com.example.ui.viewmodel.CategorySlice
import com.example.util.CurrencyFormatter
import java.util.Locale

@Composable
fun ExpenseDonutChart(
    slices: List<CategorySlice>,
    totalExpense: Long,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    var animationPlayed by remember { mutableStateOf(false) }
    val sweepProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "donut_sweep"
    )

    LaunchedEffect(slices) {
        animationPlayed = true
    }

    val selectedSlice = slices.firstOrNull { it.category == selectedCategory }
    val indonesianLocale = remember { Locale.Builder().setLanguage("in").setRegion("ID").build() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.size(204.dp)) {
                val baseStroke = 26.dp.toPx()
                val diameter = size.minDimension - baseStroke - 8.dp.toPx()
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                // Subtle backdrop ring
                drawArc(
                    color = trackColor.copy(alpha = 0.45f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = baseStroke)
                )

                if (slices.isNotEmpty() && totalExpense > 0L) {
                    var startAngle = -90f
                    val gapDegrees = if (slices.size > 1) 4.5f else 0f
                    slices.forEach { slice ->
                        val fullSweep = (slice.percentage / 100f) * 360f
                        val sweep = ((fullSweep - gapDegrees) * sweepProgress).coerceAtLeast(1.5f)
                        val isHighlighted = selectedCategory == null || selectedCategory == slice.category
                        val activeStroke = if (selectedCategory == slice.category) baseStroke + 8.dp.toPx() else baseStroke
                        drawArc(
                            color = if (isHighlighted) slice.color else slice.color.copy(alpha = 0.22f),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = activeStroke, cap = StrokeCap.Round)
                        )
                        startAngle += fullSweep
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .clickable(enabled = selectedCategory != null) { onSelectCategory(null) }
            ) {
                Text(
                    text = selectedSlice?.category ?: "Total Pengeluaran",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = CurrencyFormatter.formatCompactRupiah(selectedSlice?.amount ?: totalExpense),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedSlice != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = selectedSlice.color.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = String.format(indonesianLocale, "%.1f%%", selectedSlice.percentage),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = selectedSlice.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (slices.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEach { slice ->
                    val isSelected = selectedCategory == slice.category
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onSelectCategory(if (isSelected) null else slice.category)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            slice.color.copy(alpha = if (extended.isDark) 0.16f else 0.10f)
                        } else {
                            extended.elevatedCard
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) slice.color.copy(alpha = 0.55f) else extended.cardBorder.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = slice.category,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = String.format(indonesianLocale, "%.0f%%", slice.percentage),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = CurrencyFormatter.formatRupiah(slice.amount),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Proportional visual share track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = (slice.percentage / 100f).coerceIn(0.03f, 1f))
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(slice.color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
