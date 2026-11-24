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
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.util.CurrencyFormatter

class SakuQuickActionWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSakuWidgetSnapshot(context)
        provideContent {
            GlanceTheme {
                SakuQuickActionWidgetContent(context = context, snapshot = snapshot)
            }
        }
    }
}

@Composable
private fun SakuQuickActionWidgetContent(
    context: Context,
    snapshot: SakuWidgetSnapshot
) {
    val bgColor = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF0F172A))
    val titleColor = ColorProvider(day = Color(0xFF0F172A), night = Color.White)
    val mutedColor = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))

    val manualBg = ColorProvider(day = Color(0xFFEEF3FF), night = Color(0xFF17274F))
    val manualTint = ColorProvider(day = Color(0xFF1D56F5), night = Color(0xFF60A5FA))

    val aiBg = ColorProvider(day = Color(0xFFF3EEFF), night = Color(0xFF261E47))
    val aiTint = ColorProvider(day = Color(0xFF7C3AED), night = Color(0xFFA78BFA))

    val voiceBg = ColorProvider(day = Color(0xFFECFDF5), night = Color(0xFF113327))
    val voiceTint = ColorProvider(day = Color(0xFF059669), night = Color(0xFF34D399))

    val scanBg = ColorProvider(day = Color(0xFFFFF7ED), night = Color(0xFF3B240C))
    val scanTint = ColorProvider(day = Color(0xFFD97706), night = Color(0xFFFBBF24))

    val openHome = buildWidgetLaunchIntent(context)
    val openManual = buildWidgetLaunchIntent(context, openTab = "MANUAL")
    val openAi = buildWidgetLaunchIntent(context, openTab = "AI_MAGIC")
    val openVoice = buildWidgetLaunchIntent(context, openTab = "VOICE")
    val openScan = buildWidgetLaunchIntent(context, openTab = "SCAN")

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
                    .size(22.dp)
                    .cornerRadius(6.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "Catat Cepat",
                style = TextStyle(color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = if (snapshot.hideBalance) {
                    "Hari ini •••••"
                } else {
                    "Hari ini ${CurrencyFormatter.formatCompactRupiah(snapshot.todayExpense)}"
                },
                style = TextStyle(color = mutedColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickWidgetActionTile(
                iconRes = R.drawable.ic_widget_add,
                label = "Manual",
                bg = manualBg,
                tint = manualTint,
                intent = openManual,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(7.dp))
            QuickWidgetActionTile(
                iconRes = R.drawable.ic_widget_ai,
                label = "Tanya AI",
                bg = aiBg,
                tint = aiTint,
                intent = openAi,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(7.dp))
            QuickWidgetActionTile(
                iconRes = R.drawable.ic_widget_mic,
                label = "Suara",
                bg = voiceBg,
                tint = voiceTint,
                intent = openVoice,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(7.dp))
            QuickWidgetActionTile(
                iconRes = R.drawable.ic_widget_scan,
                label = "Struk",
                bg = scanBg,
                tint = scanTint,
                intent = openScan,
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

@Composable
private fun QuickWidgetActionTile(
    iconRes: Int,
    label: String,
    bg: ColorProvider,
    tint: ColorProvider,
    intent: android.content.Intent,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier
            .background(bg)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(intent))
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = label,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(20.dp)
        )
        Spacer(modifier = GlanceModifier.height(5.dp))
        Text(
            text = label,
            style = TextStyle(color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

class SakuQuickActionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SakuQuickActionWidget()
}
