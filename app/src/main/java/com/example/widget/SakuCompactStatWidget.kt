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

class SakuCompactStatWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSakuWidgetSnapshot(context)
        provideContent {
            GlanceTheme {
                SakuCompactStatWidgetContent(context = context, snapshot = snapshot)
            }
        }
    }
}

@Composable
private fun SakuCompactStatWidgetContent(
    context: Context,
    snapshot: SakuWidgetSnapshot
) {
    val bgCobalt = ColorProvider(day = Color(0xFF1D56F5), night = Color(0xFF172554))
    val glassCard = ColorProvider(day = Color(0xFF3468F7), night = Color(0xFF1E3A8A))
    val whiteColor = ColorProvider(day = Color.White, night = Color.White)
    val softBlueText = ColorProvider(day = Color(0xFFDBEAFE), night = Color(0xFFBFDBFE))

    val openHome = buildWidgetLaunchIntent(context)
    val openManual = buildWidgetLaunchIntent(context, openTab = "MANUAL")
    val openAi = buildWidgetLaunchIntent(context, openTab = "AI_MAGIC")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgCobalt)
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
                    .size(22.dp)
                    .cornerRadius(6.dp)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "Saku",
                style = TextStyle(color = whiteColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .background(glassCard)
                    .cornerRadius(999.dp)
                    .clickable(actionStartActivity(openAi))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_ai),
                    contentDescription = "AI Saku",
                    colorFilter = ColorFilter.tint(whiteColor),
                    modifier = GlanceModifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Text(
            text = "Total Saldo",
            style = TextStyle(color = softBlueText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = if (snapshot.hideBalance) "Rp ••••••" else CurrencyFormatter.formatCompactRupiah(snapshot.netBalance),
            style = TextStyle(color = whiteColor, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Text(
            text = "${snapshot.primaryWalletName} • ${snapshot.walletCount} dompet",
            style = TextStyle(color = softBlueText, fontSize = 10.sp),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(whiteColor)
                .cornerRadius(999.dp)
                .clickable(actionStartActivity(openManual))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(bgCobalt),
                    modifier = GlanceModifier.size(13.dp)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "Catat",
                    style = TextStyle(color = bgCobalt, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

class SakuCompactStatWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SakuCompactStatWidget()
}
