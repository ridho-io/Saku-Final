package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.R
import com.example.ROUTE_ANGGARAN
import com.example.util.CurrencyFormatter
import kotlin.math.roundToInt

class SakuBudgetPulseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSakuWidgetSnapshot(context)
        provideContent {
            GlanceTheme {
                SakuBudgetPulseWidgetContent(context = context, snapshot = snapshot)
            }
        }
    }
}

@Composable
private fun SakuBudgetPulseWidgetContent(
    context: Context,
    snapshot: SakuWidgetSnapshot
) {
    val bgColor = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF0F172A))
    val trackColor = ColorProvider(day = Color(0xFFE2E8F0), night = Color(0xFF1E293B))
    val chipBg = ColorProvider(day = Color(0xFFF3F6FC), night = Color(0xFF1A2438))
    val titleColor = ColorProvider(day = Color(0xFF0F172A), night = Color.White)
    val mutedColor = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))

    val hasBudget = snapshot.totalBudgetLimit > 0L
    val pct = if (hasBudget) {
        ((snapshot.totalBudgetSpent.toDouble() / snapshot.totalBudgetLimit) * 100.0).roundToInt().coerceAtLeast(0)
    } else 0

    val (statusLabel, statusColor, statusBg) = when {
        !hasBudget -> Triple(
            "Belum diatur",
            ColorProvider(day = Color(0xFF1D56F5), night = Color(0xFF60A5FA)),
            ColorProvider(day = Color(0xFFEEF3FF), night = Color(0xFF17274F))
        )
        pct >= 100 -> Triple(
            "Melebihi $pct%",
            ColorProvider(day = Color(0xFFE11D48), night = Color(0xFFFB7185)),
            ColorProvider(day = Color(0xFFFFE4E6), night = Color(0xFF3E1420))
        )
        pct >= 75 -> Triple(
            "Waspada $pct%",
            ColorProvider(day = Color(0xFFD97706), night = Color(0xFFFBBF24)),
            ColorProvider(day = Color(0xFFFEF3C7), night = Color(0xFF3B2808))
        )
        else -> Triple(
            "Aman $pct%",
            ColorProvider(day = Color(0xFF059669), night = Color(0xFF34D399)),
            ColorProvider(day = Color(0xFFD1FAE5), night = Color(0xFF113327))
        )
    }

    val openBudget = buildWidgetLaunchIntent(context, openRoute = ROUTE_ANGGARAN)
    val segmentCount = 8
    val filledSegments = ((pct.coerceIn(0, 100) / 100f) * segmentCount).roundToInt()
        .coerceIn(if (pct > 0) 1 else 0, segmentCount)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity(openBudget))
            .padding(14.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.saku_brand_icon),
                contentDescription = "Logo Saku",
                modifier = GlanceModifier
                    .size(22.dp)
                    .cornerRadius(6.dp)
            )
            Spacer(modifier = GlanceModifier.width(7.dp))
            Text(
                text = "Anggaran ${snapshot.monthLabel}",
                style = TextStyle(color = mutedColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .background(statusBg)
                    .cornerRadius(999.dp)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = TextStyle(color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (hasBudget) {
            val remaining = snapshot.totalBudgetLimit - snapshot.totalBudgetSpent
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (snapshot.hideBalance) {
                        "Sisa Rp •••••"
                    } else if (remaining >= 0) {
                        "Sisa ${CurrencyFormatter.formatCompactRupiah(remaining)}"
                    } else {
                        "Lebih ${CurrencyFormatter.formatCompactRupiah(-remaining)}"
                    },
                    style = TextStyle(color = titleColor, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = if (snapshot.hideBalance) {
                        "•••• / ••••"
                    } else {
                        "${CurrencyFormatter.formatCompactRupiah(snapshot.totalBudgetSpent)} / ${CurrencyFormatter.formatCompactRupiah(snapshot.totalBudgetLimit)}"
                    },
                    style = TextStyle(color = mutedColor, fontSize = 11.sp),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until segmentCount) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(end = if (i < segmentCount - 1) 4.dp else 0.dp)
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .background(if (i < filledSegments) statusColor else trackColor)
                                .cornerRadius(999.dp)
                        ) {}
                    }
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            if (snapshot.topBudgets.isNotEmpty()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    snapshot.topBudgets.take(2).forEachIndexed { idx, item ->
                        if (idx > 0) Spacer(modifier = GlanceModifier.width(7.dp))
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(chipBg)
                                .cornerRadius(12.dp)
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.category,
                                    style = TextStyle(color = titleColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                    maxLines = 1
                                )
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Text(
                                    text = "${item.percent}%",
                                    style = TextStyle(color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(chipBg)
                    .cornerRadius(14.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_budget),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(statusColor),
                    modifier = GlanceModifier.size(18.dp)
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column {
                    Text(
                        text = "Atur Anggaran Bulan Ini",
                        style = TextStyle(color = titleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (snapshot.topGoalTitle != null) {
                            "Target ${snapshot.topGoalTitle} • ${snapshot.topGoalPercent}% tercapai"
                        } else {
                            "Ketuk untuk pasang batas pengeluaran"
                        },
                        style = TextStyle(color = mutedColor, fontSize = 10.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

class SakuBudgetPulseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SakuBudgetPulseWidget()
}
