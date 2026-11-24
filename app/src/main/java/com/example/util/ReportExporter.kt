package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StatementEntry(
    val transaction: TransactionEntity,
    val walletLabel: String,
    val creditAmount: Long,
    val debitAmount: Long,
    val runningBalance: Long
)

data class StatementReport(
    val periodLabel: String,
    val scopeLabel: String,
    val openingBalance: Long,
    val totalCredit: Long,
    val totalDebit: Long,
    val closingBalance: Long,
    val entries: List<StatementEntry>,
    val generatedAtMillis: Long = System.currentTimeMillis()
)

object ReportExporter {
    private val indonesiaLocale = Locale.Builder().setLanguage("in").setRegion("ID").build()

    fun buildStatementReport(
        allTransactions: List<TransactionEntity>,
        wallets: List<WalletEntity>,
        selectedWalletId: Long?,
        periodStartMillis: Long?,
        periodEndMillis: Long?,
        periodLabel: String,
        generatedAtMillis: Long = System.currentTimeMillis()
    ): StatementReport {
        val walletMap = wallets.associateBy { it.id }
        val scopeLabel = if (selectedWalletId == null) {
            "Semua Dompet (${wallets.size} akun)"
        } else {
            walletMap[selectedWalletId]?.let { "${it.name} (${it.walletType})" } ?: "Dompet"
        }

        val currentBalance = if (selectedWalletId == null) {
            wallets.sumOf { it.balance }
        } else {
            walletMap[selectedWalletId]?.balance ?: 0L
        }

        fun deltaForScope(tx: TransactionEntity): Long = when {
            selectedWalletId == null -> when (tx.type) {
                TransactionType.INCOME -> tx.amount
                TransactionType.EXPENSE -> -tx.amount
                else -> 0L
            }
            tx.type == TransactionType.INCOME && tx.walletId == selectedWalletId -> tx.amount
            tx.type == TransactionType.EXPENSE && tx.walletId == selectedWalletId -> -tx.amount
            tx.type == TransactionType.TRANSFER && tx.walletId == selectedWalletId -> -tx.amount
            tx.type == TransactionType.TRANSFER && tx.targetWalletId == selectedWalletId -> tx.amount
            else -> 0L
        }

        val relevantSorted = allTransactions
            .filter { tx ->
                selectedWalletId == null ||
                    tx.walletId == selectedWalletId ||
                    tx.targetWalletId == selectedWalletId
            }
            .sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id })

        val postPeriodDelta = relevantSorted
            .filter { periodEndMillis != null && it.timestamp >= periodEndMillis }
            .sumOf { deltaForScope(it) }

        val closingBalance = currentBalance - postPeriodDelta

        val periodTx = relevantSorted.filter { tx ->
            (periodStartMillis == null || tx.timestamp >= periodStartMillis) &&
                (periodEndMillis == null || tx.timestamp < periodEndMillis)
        }

        val totalCredit = periodTx.sumOf { tx ->
            val d = deltaForScope(tx)
            if (d > 0L) d else 0L
        }
        val totalDebit = periodTx.sumOf { tx ->
            val d = deltaForScope(tx)
            if (d < 0L) -d else 0L
        }

        val openingBalance = closingBalance - (totalCredit - totalDebit)
        var running = openingBalance

        val entries = periodTx.map { tx ->
            val delta = deltaForScope(tx)
            running += delta
            val sourceName = walletMap[tx.walletId]?.name ?: "Dompet"
            val targetName = tx.targetWalletId?.let { walletMap[it]?.name }
            val walletDisplay = if (tx.type == TransactionType.TRANSFER && targetName != null) {
                "$sourceName -> $targetName"
            } else {
                sourceName
            }
            StatementEntry(
                transaction = tx,
                walletLabel = walletDisplay,
                creditAmount = if (delta > 0L) delta else 0L,
                debitAmount = if (delta < 0L) -delta else 0L,
                runningBalance = running
            )
        }

        return StatementReport(
            periodLabel = periodLabel,
            scopeLabel = scopeLabel,
            openingBalance = openingBalance,
            totalCredit = totalCredit,
            totalDebit = totalDebit,
            closingBalance = closingBalance,
            entries = entries,
            generatedAtMillis = generatedAtMillis
        )
    }

    fun buildCsvContent(report: StatementReport): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("Periode,${csvCell(report.periodLabel)}")
        sb.appendLine("Cakupan Dompet,${csvCell(report.scopeLabel)}")
        sb.appendLine("Saldo Awal,${report.openingBalance}")
        sb.appendLine("Total Kredit (CR),${report.totalCredit}")
        sb.appendLine("Total Debet (DB),${report.totalDebit}")
        sb.appendLine("Saldo Akhir,${report.closingBalance}")
        sb.appendLine()
        sb.appendLine("Tanggal,Waktu,Merchant/Keterangan,Kategori,Tipe,Dompet,Kredit (CR),Debet (DB),Saldo Berjalan,Catatan")

        report.entries.forEach { entry ->
            val tx = entry.transaction
            val dateStr = dateFmt.format(Date(tx.timestamp))
            val timeStr = timeFmt.format(Date(tx.timestamp))
            sb.appendLine(
                listOf(
                    csvCell(dateStr),
                    csvCell(timeStr),
                    csvCell(tx.merchant),
                    csvCell(tx.category),
                    csvCell(tx.type),
                    csvCell(entry.walletLabel),
                    entry.creditAmount.toString(),
                    entry.debitAmount.toString(),
                    entry.runningBalance.toString(),
                    csvCell(tx.note)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    internal fun csvCell(raw: String): String {
        val normalized = raw.replace("\r", " ").replace("\n", " ").trim()
        val safe = if (normalized.isNotEmpty() && normalized[0] in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
            "'$normalized"
        } else {
            normalized
        }
        return "\"${safe.replace("\"", "\"\"")}\""
    }

    fun exportCsvToFileUri(
        context: Context,
        report: StatementReport
    ): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(report.generatedAtMillis))
        val outFile = File(dir, "Mutasi_Saku_$stamp.csv")
        outFile.writeText(buildCsvContent(report), Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    fun exportPdfToFileUri(
        context: Context,
        report: StatementReport,
        accountHolderName: String
    ): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(report.generatedAtMillis))
        val outFile = File(dir, "eStatement_Saku_$stamp.pdf")

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f
        val contentRight = pageWidth - margin
        val bottomLimit = pageHeight - 48f

        val dateFmt = SimpleDateFormat("dd/MM/yy", indonesiaLocale)
        val printedFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesiaLocale)

        val entriesPerFirstPageEstimate = 24
        val remainingAfterFirst = (report.entries.size - entriesPerFirstPageEstimate).coerceAtLeast(0)
        val totalPages = if (report.entries.isEmpty()) 1 else 1 + ((remainingAfterFirst + 31) / 32)

        val obsidianPaint = Paint().apply { color = Color.rgb(19, 22, 26) }
        val limePaint = Paint().apply { color = Color.rgb(181, 238, 90) }
        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val fillCardPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
        val zebraPaint = Paint().apply { color = Color.rgb(249, 250, 251) }
        val headerBandPaint = Paint().apply { color = Color.rgb(241, 245, 249) }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 8.8f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 7.8f
        }
        val valueBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 8.2f
        }
        val cellSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 7.0f
        }
        val creditPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 128, 61)
            textSize = 8.2f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val debitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(190, 18, 60)
            textSize = 8.2f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val neutralRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.2f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val balanceRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.2f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        fun drawTableHeader(canvas: Canvas, yTop: Float): Float {
            canvas.drawRect(margin, yTop, contentRight, yTop + 20f, headerBandPaint)
            canvas.drawLine(margin, yTop + 20f, contentRight, yTop + 20f, borderPaint)
            val textY = yTop + 13f
            canvas.drawText("TGL", margin + 6f, textY, tableHeaderPaint)
            canvas.drawText("KETERANGAN", margin + 58f, textY, tableHeaderPaint)
            canvas.drawText("DOMPET", margin + 235f, textY, tableHeaderPaint)
            val rightHeader = Paint(tableHeaderPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("MUTASI (CR/DB)", margin + 422f, textY, rightHeader)
            canvas.drawText("SALDO", contentRight - 6f, textY, rightHeader)
            return yTop + 20f
        }

        fun drawFooter(canvas: Canvas, pageIndex: Int) {
            canvas.drawLine(margin, pageHeight - 36f, contentRight, pageHeight - 36f, borderPaint)
            canvas.drawText(
                "Dokumen e-Statement digital dihasilkan otomatis oleh Saku • Mata Uang: IDR",
                margin,
                pageHeight - 22f,
                labelPaint
            )
            val pageTextPaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("Hal $pageIndex / $totalPages", contentRight, pageHeight - 22f, pageTextPaint)
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        // Matte Obsidian Bank Header Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 94f, obsidianPaint)
        canvas.drawRect(0f, 94f, pageWidth.toFloat(), 98f, limePaint)
        canvas.drawText("REKENING KORAN / e-STATEMENT SAKU", margin, 32f, titlePaint)
        val holder = accountHolderName.trim().ifBlank { "Pengguna Saku" }
        canvas.drawText("Pemilik: $holder  •  Cakupan: ${report.scopeLabel}", margin, 52f, subHeaderPaint)
        canvas.drawText("Periode: ${report.periodLabel}  •  Dicetak: ${printedFmt.format(Date(report.generatedAtMillis))}", margin, 68f, subHeaderPaint)

        // 4 Summary Cards
        var curY = 114f
        val boxGap = 8f
        val boxWidth = (pageWidth - margin * 2 - boxGap * 3) / 4f
        val boxHeight = 46f
        val summaryItems = listOf(
            Triple("SALDO AWAL", CurrencyFormatter.formatRupiah(report.openingBalance), Color.rgb(15, 23, 42)),
            Triple("TOTAL KREDIT (CR)", "+${CurrencyFormatter.formatRupiah(report.totalCredit)}", Color.rgb(21, 128, 61)),
            Triple("TOTAL DEBET (DB)", "-${CurrencyFormatter.formatRupiah(report.totalDebit)}", Color.rgb(190, 18, 60)),
            Triple("SALDO AKHIR", CurrencyFormatter.formatRupiah(report.closingBalance), Color.rgb(15, 23, 42))
        )
        summaryItems.forEachIndexed { idx, (lbl, valText, tint) ->
            val left = margin + idx * (boxWidth + boxGap)
            canvas.drawRoundRect(left, curY, left + boxWidth, curY + boxHeight, 6f, 6f, fillCardPaint)
            canvas.drawRoundRect(left, curY, left + boxWidth, curY + boxHeight, 6f, 6f, borderPaint)
            canvas.drawText(lbl, left + 8f, curY + 16f, labelPaint)
            val valPaint = Paint(valueBoldPaint).apply { color = tint }
            canvas.drawText(truncateToFit(valText, valPaint, boxWidth - 14f), left + 8f, curY + 34f, valPaint)
        }

        curY += boxHeight + 16f
        curY = drawTableHeader(canvas, curY)

        if (report.entries.isEmpty()) {
            canvas.drawText("Tidak ada transaksi pada periode & dompet ini.", margin + 8f, curY + 24f, cellPaint)
        } else {
            val rowHeight = 26f
            report.entries.forEachIndexed { idx, entry ->
                if (curY + rowHeight > bottomLimit) {
                    drawFooter(canvas, pageNumber)
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    canvas.drawText("MUTASI SAKU (LANJUTAN) • ${report.periodLabel}", margin, 30f, valueBoldPaint)
                    curY = drawTableHeader(canvas, 42f)
                }

                if (idx % 2 == 1) {
                    canvas.drawRect(margin, curY, contentRight, curY + rowHeight, zebraPaint)
                }

                val tx = entry.transaction
                canvas.drawText(dateFmt.format(Date(tx.timestamp)), margin + 6f, curY + 12f, cellPaint)
                canvas.drawText(truncateToFit(tx.merchant, cellPaint, 168f), margin + 58f, curY + 11f, cellPaint)
                val catNote = if (tx.note.isNotBlank()) "${tx.category} • ${tx.note}" else tx.category
                canvas.drawText(truncateToFit(catNote, cellSubPaint, 168f), margin + 58f, curY + 21f, cellSubPaint)
                canvas.drawText(truncateToFit(entry.walletLabel, cellPaint, 95f), margin + 235f, curY + 14f, cellPaint)

                when {
                    entry.creditAmount > 0L -> {
                        canvas.drawText("+${CurrencyFormatter.formatRupiah(entry.creditAmount)} CR", margin + 422f, curY + 14f, creditPaint)
                    }
                    entry.debitAmount > 0L -> {
                        canvas.drawText("-${CurrencyFormatter.formatRupiah(entry.debitAmount)} DB", margin + 422f, curY + 14f, debitPaint)
                    }
                    else -> {
                        canvas.drawText("${CurrencyFormatter.formatRupiah(tx.amount)} TRF", margin + 422f, curY + 14f, neutralRightPaint)
                    }
                }

                canvas.drawText(CurrencyFormatter.formatRupiah(entry.runningBalance), contentRight - 6f, curY + 14f, balanceRightPaint)
                canvas.drawLine(margin, curY + rowHeight, contentRight, curY + rowHeight, borderPaint)
                curY += rowHeight
            }
        }

        drawFooter(canvas, pageNumber)
        document.finishPage(page)

        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    private fun truncateToFit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }
}
