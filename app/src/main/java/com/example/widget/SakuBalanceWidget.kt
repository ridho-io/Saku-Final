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
import com.example.util.CurrencyFormatter

class SakuBalanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSakuWidgetSnapshot(context)
        provideContent {
            GlanceTheme {
                SakuBalanceWidgetContent(context = context, snapshot = snapshot)
            }
        }
    }
}

@Composable
private fun SakuBalanceWidgetContent(
    context: Context,
    snapshot: SakuWidgetSnapshot
) {
    val bgColor = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF0F172A))
    val tileColor = ColorProvider(day = Color(0xFFF3F6FC), night = Color(0xFF1A2438))
    val cobaltColor = ColorProvider(day = Color(0xFF1D56F5), night = Color(0xFF3B82F6))
    val onCobaltColor = ColorProvider(day = Color.White, night = Color.White)
    val titleColor = ColorProvider(day = Color(0xFF0F172A), night = Color.White)
    val mutedColor = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
    val incomeColor = ColorProvider(day = Color(0xFF059669), night = Color(0xFF34D399))
    val expenseColor = ColorProvider(day = Color(0xFFE11D48), night = Color(0xFFFB7185))
    val aiBg = ColorProvider(day = Color(0xFFEEF3FF), night = Color(0xFF1E3A8A))
    val aiText = ColorProvider(day = Color(0xFF1D56F5), night = Color(0xFF93C5FD))

    val openHome = buildWidgetLaunchIntent(context)
    val openManual = buildWidgetLaunchIntent(context, openTab = "MANUAL")
    val openAi = buildWidgetLaunchIntent(context, openTab = "AI_MAGIC")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity(openHome))
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
                    .size(24.dp)
                    .cornerRadius(7.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column {
                Text(
                    text = "Saku • ${snapshot.periodLabel}",
                    style = TextStyle(color = mutedColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .background(cobaltColor)
                    .cornerRadius(999.dp)
                    .clickable(actionStartActivity(openManual))
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_add),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(onCobaltColor),
                        modifier = GlanceModifier.size(13.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Catat",
                        style = TextStyle(color = onCobaltColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (snapshot.hideBalance) "Rp ••••••••" else CurrencyFormatter.formatRupiah(snapshot.netBalance),
                style = TextStyle(color = titleColor, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .background(tileColor)
                    .cornerRadius(999.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_wallet),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(mutedColor),
                        modifier = GlanceModifier.size(11.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "${snapshot.walletCount} Dompet",
                        style = TextStyle(color = mutedColor, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(tileColor)
                    .cornerRadius(14.dp)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_arrow_down),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(incomeColor),
                            modifier = GlanceModifier.size(11.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(3.dp))
                        Text(
                            text = "Masuk",
                            style = TextStyle(color = mutedColor, fontSize = 10.sp)
                        )
                    }
                    Text(
                        text = if (snapshot.hideBalance) "Rp ••••" else CurrencyFormatter.formatCompactRupiah(snapshot.income),
                        style = TextStyle(color = incomeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(7.dp))

            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(tileColor)
                    .cornerRadius(14.dp)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_arrow_up),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(expenseColor),
                            modifier = GlanceModifier.size(11.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(3.dp))
                        Text(
                            text = "Keluar",
                            style = TextStyle(color = mutedColor, fontSize = 10.sp)
                        )
                    }
                    Text(
                        text = if (snapshot.hideBalance) "Rp ••••" else CurrencyFormatter.formatCompactRupiah(snapshot.expense),
                        style = TextStyle(color = expenseColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(7.dp))

            Box(
                modifier = GlanceModifier
                    .background(aiBg)
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(openAi))
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_ai),
                        contentDescription = "Tanya AI Saku",
                        colorFilter = ColorFilter.tint(aiText),
                        modifier = GlanceModifier.size(14.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "AI",
                        style = TextStyle(color = aiText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

class SakuBalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SakuBalanceWidget()
}
