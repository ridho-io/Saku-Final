package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.WalletEntity
import com.example.ui.theme.LocalSakuColors
import com.example.util.DateFormatter
import kotlinx.coroutines.launch
import java.util.Calendar

private enum class ExportPeriodPreset(val label: String) {
    CURRENT_CYCLE("Siklus Aktif"),
    THIS_MONTH("Bulan Ini"),
    LAST_MONTH("Bulan Lalu"),
    LAST_30_DAYS("30 Hari"),
    ALL_TIME("Semua Data")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportSheet(
    wallets: List<WalletEntity>,
    billingCycleStartDay: Int,
    initialWalletId: Long? = null,
    onDismiss: () -> Unit,
    onExportPdf: suspend (walletId: Long?, startMillis: Long?, endMillis: Long?, periodLabel: String) -> Result<Uri>,
    onExportCsv: suspend (walletId: Long?, startMillis: Long?, endMillis: Long?, periodLabel: String) -> Result<Uri>,
    onError: (String) -> Unit
) {
    val extended = LocalSakuColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedWalletId by remember(initialWalletId) { mutableStateOf(initialWalletId) }
    var selectedPeriod by remember { mutableStateOf(ExportPeriodPreset.CURRENT_CYCLE) }
    var exportingFormat by remember { mutableStateOf<String?>(null) }

    fun computePeriodBounds(): Triple<Long?, Long?, String> {
        val now = System.currentTimeMillis()
        return when (selectedPeriod) {
            ExportPeriodPreset.CURRENT_CYCLE -> {
                val (s, e) = DateFormatter.cycleBounds(now, billingCycleStartDay)
                Triple(s, e, DateFormatter.cycleLabel(now, billingCycleStartDay))
            }
            ExportPeriodPreset.THIS_MONTH -> {
                val (s, e) = DateFormatter.monthBounds(now)
                Triple(s, e, DateFormatter.currentMonthLabel(now))
            }
            ExportPeriodPreset.LAST_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.MONTH, -1)
                }
                val (s, e) = DateFormatter.monthBounds(cal.timeInMillis)
                Triple(s, e, DateFormatter.currentMonthLabel(cal.timeInMillis))
            }
            ExportPeriodPreset.LAST_30_DAYS -> {
                val start = now - 30L * 86_400_000L
                Triple(start, now + 1L, "30 Hari Terakhir")
            }
            ExportPeriodPreset.ALL_TIME -> Triple(null, null, "Seluruh Riwayat")
        }
    }

    fun launchShareIntent(uri: Uri, mimeType: String, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(extended.limeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = extended.lime,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Ekspor Laporan Mutasi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Format e-Statement rekening koran PDF atau CSV",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Pilih Periode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportPeriodPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedPeriod == preset,
                            onClick = { selectedPeriod = preset },
                            label = { Text(preset.label) },
                            shape = RoundedCornerShape(999.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Cakupan Dompet",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
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
                        label = { Text("Semua Dompet") },
                        shape = RoundedCornerShape(999.dp)
                    )
                    wallets.forEach { wallet ->
                        FilterChip(
                            selected = selectedWalletId == wallet.id,
                            onClick = { selectedWalletId = wallet.id },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(wallet.accentHex))
                                )
                            },
                            label = { Text(wallet.name) },
                            shape = RoundedCornerShape(999.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PDF Mutasi Bank menyertakan ringkasan Saldo Awal, Total Kredit (CR), Total Debet (DB), Saldo Akhir, serta kolom mutasi dan saldo berjalan berurutan kronologis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Button(
                onClick = {
                    if (exportingFormat != null) return@Button
                    val (start, end, label) = computePeriodBounds()
                    exportingFormat = "PDF"
                    scope.launch {
                        val result = onExportPdf(selectedWalletId, start, end, label)
                        exportingFormat = null
                        result.onSuccess { uri ->
                            launchShareIntent(uri, "application/pdf", "Bagikan e-Statement Saku ($label)")
                            onDismiss()
                        }.onFailure { err ->
                            onError(err.localizedMessage ?: "Gagal membuat file PDF")
                        }
                    }
                },
                enabled = exportingFormat == null,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_pdf_btn")
            ) {
                if (exportingFormat == "PDF") {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = extended.onLime)
                } else {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(19.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ekspor PDF Mutasi Bank (e-Statement)", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    if (exportingFormat != null) return@OutlinedButton
                    val (start, end, label) = computePeriodBounds()
                    exportingFormat = "CSV"
                    scope.launch {
                        val result = onExportCsv(selectedWalletId, start, end, label)
                        exportingFormat = null
                        result.onSuccess { uri ->
                            launchShareIntent(uri, "text/csv", "Bagikan Mutasi CSV Saku ($label)")
                            onDismiss()
                        }.onFailure { err ->
                            onError(err.localizedMessage ?: "Gagal membuat file CSV")
                        }
                    }
                },
                enabled = exportingFormat == null,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_csv_btn")
            ) {
                if (exportingFormat == "CSV") {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ekspor CSV (Excel / Sheets)", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
