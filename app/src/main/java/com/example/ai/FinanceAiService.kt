package com.example.ai

import android.graphics.Bitmap
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.util.FinanceCategories
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Collections
import java.util.Locale
import java.util.UUID

data class ParsedTransactionDraft(
    val localId: String = UUID.randomUUID().toString(),
    val merchant: String,
    val amount: Long,
    val type: String, // EXPENSE or INCOME
    val category: String,
    val walletName: String,
    val note: String
)

class FinanceAiService {

    companion object {
        // Fast & economical Gemini 3.8 Flash (billing not required, no gemini-3.5)
        const val FAST_MODEL_NAME = "gemini-3.8-flash"
    }

    private val transactionItemSchema = Schema.obj(
        properties = mapOf(
            "merchant" to Schema.string(description = "Nama merchant/toko singkat"),
            "amount" to Schema.integer(description = "Nominal Rupiah bulat positif"),
            "type" to Schema.enumeration(
                values = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
                description = "EXPENSE atau INCOME"
            ),
            "category" to Schema.enumeration(
                values = FinanceCategories.aiCategoryNames,
                description = "Kategori paling sesuai"
            ),
            "walletName" to Schema.string(description = "Nama dompet/bank"),
            "note" to Schema.string(description = "Catatan singkat")
        ),
        optionalProperties = listOf("note")
    )

    private val batchTransactionSchema = Schema.obj(
        properties = mapOf(
            "transactions" to Schema.array(
                items = transactionItemSchema,
                description = "Daftar transaksi yang diekstrak"
            )
        )
    )

    private val autoTagSchema = Schema.obj(
        properties = mapOf(
            "category" to Schema.enumeration(
                values = FinanceCategories.aiCategoryNames,
                description = "Kategori transaksi yang paling tepat"
            )
        )
    )

    // Gemini 3.8 Flash with MINIMAL thinking + tight output budget for fast, quota-frugal extraction
    private val parserModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.1f
                maxOutputTokens = 384
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
                responseMimeType = "application/json"
                responseSchema = batchTransactionSchema
            }
        )
    }

    // Ultra-lean classifier model (tiny 64-token budget) for unknown merchants only
    private val autoTagModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.0f
                maxOutputTokens = 64
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
                responseMimeType = "application/json"
                responseSchema = autoTagSchema
            }
        )
    }

    // Fast streaming model for Q&A with minimal thinking latency
    private val qaModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 384
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
            }
        )
    }

    // Fast advisor model with MINIMAL thinking & compact summary to keep token usage lean
    private val advisorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.3f
                maxOutputTokens = 512
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
            }
        )
    }

    // Bounded LRU cache to prevent duplicate API calls for identical descriptions
    private val autoTagCache: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 64
        }
    )

    // Cache last monthly insights by compact summary hash to save API quota
    @Volatile
    private var lastInsightsKey: Int? = null
    @Volatile
    private var lastInsightsResult: String? = null

    /**
     * Hybrid Auto-Tagging:
     * 1) Personalized history + Indonesian keyword matching first (0ms, 0 API quota).
     * 2) In-memory LRU cache lookup.
     * 3) Gemini 3.8 Flash structured classification fallback for unfamiliar descriptions.
     */
    suspend fun suggestCategoryWithAi(
        description: String,
        txType: String,
        history: List<TransactionEntity>,
        allowCloudFallback: Boolean = true
    ): AutoTagSuggestion? {
        val clean = description.trim()
        if (clean.length < 2) return null

        val localHit = CategoryAutoTagger.suggestLocally(clean, txType, history)
        if (localHit != null) {
            return localHit
        }

        if (!allowCloudFallback || clean.length < 3) return null

        val cacheKey = "${txType}_${clean.lowercase(Locale.ROOT)}"
        autoTagCache[cacheKey]?.let { cachedCat ->
            return AutoTagSuggestion(
                category = cachedCat,
                confidence = 0.90f,
                sourceLabel = "AI Auto-Tag (cache)",
                isAiPowered = true
            )
        }

        val validCategories = if (txType == TransactionType.INCOME) {
            FinanceCategories.incomeCategoryNames
        } else {
            FinanceCategories.expenseCategoryNames
        }
        val historyHints = CategoryAutoTagger.buildHistoryHintsForPrompt(
            history.filter { it.type == txType },
            maxHints = 6
        )

        val prompt = buildString {
            append("Kategorikan transaksi ${if (txType == TransactionType.INCOME) "pemasukan" else "pengeluaran"} ini ke 1 kategori valid.\n")
            append("Kategori valid: ${validCategories.joinToString(", ")}.\n")
            if (historyHints.isNotBlank()) {
                append("Riwayat user: $historyHints\n")
            }
            append("Deskripsi: \"$clean\"")
        }

        val response = withTimeout(30_000L) {
            autoTagModel.generateContent(prompt)
        }
        val jsonText = response.text ?: return null
        val rawCategory = JSONObject(jsonText).optString("category", "Lainnya")
        val normalizedCategory = validCategories.firstOrNull {
            it.equals(rawCategory, ignoreCase = true)
        } ?: FinanceCategories.aiCategoryNames.firstOrNull {
            it.equals(rawCategory, ignoreCase = true)
        } ?: "Lainnya"

        autoTagCache[cacheKey] = normalizedCategory
        return AutoTagSuggestion(
            category = normalizedCategory,
            confidence = 0.90f,
            sourceLabel = "AI Auto-Tag • Gemini 3.8",
            isAiPowered = true
        )
    }

    suspend fun parseNaturalLanguage(
        input: String,
        availableWallets: List<String>,
        history: List<TransactionEntity> = emptyList()
    ): List<ParsedTransactionDraft> {
        val walletListStr = availableWallets.joinToString(", ").ifBlank { "BCA, SeaBank, GoPay, Tunai" }
        val defaultWallet = availableWallets.firstOrNull() ?: "Tunai"
        val historyHints = CategoryAutoTagger.buildHistoryHintsForPrompt(history, maxHints = 6)
        val prompt = buildString {
            append("Ekstrak transaksi ID->JSON. Slang: gopek=500,goceng=5rb,ceban=10rb,noban=20rb,goban=50rb,rb/k=x1000,jt=x1jt.\n")
            append("Dompet: $walletListStr (def:$defaultWallet).\n")
            append("Kategori: ${FinanceCategories.aiCategoryNames.joinToString(", ")}.\n")
            if (historyHints.isNotBlank()) {
                append("Riwayat user: $historyHints\n")
            }
            append("Input: \"$input\"")
        }

        val response = withTimeout(45_000L) {
            parserModel.generateContent(prompt)
        }
        val jsonText = response.text ?: throw IllegalStateException("AI tidak mengembalikan hasil.")
        return parseDraftsFromJson(jsonText, defaultWallet, history)
    }

    suspend fun parseAudioTransaction(
        audioBytes: ByteArray,
        mimeType: String,
        availableWallets: List<String>,
        history: List<TransactionEntity> = emptyList()
    ): List<ParsedTransactionDraft> {
        val walletListStr = availableWallets.joinToString(", ").ifBlank { "BCA, SeaBank, GoPay, Tunai" }
        val defaultWallet = availableWallets.firstOrNull() ?: "Tunai"
        val historyHints = CategoryAutoTagger.buildHistoryHintsForPrompt(history, maxHints = 6)
        val prompt = buildString {
            append("Ekstrak rekaman suara transaksi ID ke JSON. Slang: gopek=500,goceng=5rb,ceban=10rb,noban=20rb,goban=50rb.\n")
            append("Dompet: $walletListStr (def:$defaultWallet).\n")
            append("Kategori: ${FinanceCategories.aiCategoryNames.joinToString(", ")}.")
            if (historyHints.isNotBlank()) {
                append("\nRiwayat user: $historyHints")
            }
        }

        val response = withTimeout(45_000L) {
            parserModel.generateContent(
                content {
                    inlineData(audioBytes, mimeType)
                    text(prompt)
                }
            )
        }
        val jsonText = response.text ?: throw IllegalStateException("AI gagal memahami rekaman suara.")
        return parseDraftsFromJson(jsonText, defaultWallet, history)
    }

    suspend fun parseReceiptImage(
        bitmap: Bitmap,
        availableWallets: List<String>,
        history: List<TransactionEntity> = emptyList()
    ): List<ParsedTransactionDraft> {
        val scaledBitmap = withContext(Dispatchers.Default) {
            scaleBitmapDown(bitmap, maxDimension = 840)
        }
        val walletListStr = availableWallets.joinToString(", ").ifBlank { "BCA, SeaBank, GoPay, Tunai" }
        val defaultWallet = availableWallets.firstOrNull() ?: "BCA"
        val prompt = """
            Ekstrak Grand Total, merchant, jenis (EXPENSE/INCOME), kategori (${FinanceCategories.aiCategoryNames.joinToString(", ")}), dompet ($walletListStr), dan note ringkas dari struk ini (1 transaksi total).
        """.trimIndent()

        val response = withTimeout(45_000L) {
            parserModel.generateContent(
                content {
                    image(scaledBitmap)
                    text(prompt)
                }
            )
        }
        val jsonText = response.text ?: throw IllegalStateException("AI gagal membaca gambar struk.")
        return parseDraftsFromJson(jsonText, defaultWallet, history)
    }

    fun streamMonthlyInsights(
        financialContextSummary: String,
        forceRefresh: Boolean = false
    ): Flow<String> = flow {
        val summaryHash = financialContextSummary.hashCode()
        val cached = lastInsightsResult
        if (!forceRefresh && lastInsightsKey == summaryHash && !cached.isNullOrBlank()) {
            emit(cached)
            return@flow
        }

        val prompt = """
            Penasihat keuangan Saku (ID). Ringkas, padat, tanpa tabel:
            1. Kesehatan Arus Kas
            2. Anomali & Anggaran
            3. 2 Aksi Hemat minggu ini

            Data:
            $financialContextSummary
        """.trimIndent()

        val acc = StringBuilder()
        withTimeout(45_000L) {
            advisorModel.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    acc.append(text)
                    emit(acc.toString())
                }
            }
        }
        val finalText = acc.toString()
        if (finalText.isBlank()) {
            throw IllegalStateException("Respons analisis AI kosong.")
        }
        lastInsightsKey = summaryHash
        lastInsightsResult = finalText
    }.flowOn(Dispatchers.IO)

    fun streamFinanceQa(question: String, financialContextSummary: String): Flow<String> = flow {
        val prompt = """
            Asisten Saku. Jawab ringkas HANYA dari data (Rupiah: Rp xx.xxx, Bahasa Indonesia):
            $financialContextSummary

            Tanya: "$question"
        """.trimIndent()

        val acc = StringBuilder()
        withTimeout(45_000L) {
            qaModel.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    acc.append(text)
                    emit(acc.toString())
                }
            }
        }
        if (acc.isBlank()) {
            throw IllegalStateException("AI tidak memberikan jawaban.")
        }
    }.flowOn(Dispatchers.IO)

    private fun parseDraftsFromJson(
        jsonString: String,
        defaultWallet: String,
        history: List<TransactionEntity>
    ): List<ParsedTransactionDraft> {
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("transactions")
        val results = mutableListOf<ParsedTransactionDraft>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val amount = obj.optLong("amount", 0L).coerceAtLeast(0L)
            if (amount <= 0L) continue
            val merchant = obj.optString("merchant", "Transaksi").ifBlank { "Transaksi" }
            val rawType = obj.optString("type", TransactionType.EXPENSE)
            val type = if (rawType == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
            val rawCat = obj.optString("category", "Lainnya")
            val validCat = FinanceCategories.aiCategoryNames.firstOrNull {
                it.equals(rawCat, ignoreCase = true)
            } ?: "Lainnya"
            val note = obj.optString("note", "")

            // Refine fallback "Lainnya" using personalized user history + local rules at zero cost
            val refinedCategory = if (validCat == "Lainnya" && history.isNotEmpty()) {
                CategoryAutoTagger.suggestLocally("$merchant $note", type, history)?.category ?: validCat
            } else {
                validCat
            }

            val wallet = obj.optString("walletName", defaultWallet).ifBlank { defaultWallet }

            results.add(
                ParsedTransactionDraft(
                    merchant = merchant,
                    amount = amount,
                    type = type,
                    category = refinedCategory,
                    walletName = wallet,
                    note = note
                )
            )
        }
        if (results.isEmpty()) {
            throw IllegalStateException("Tidak ada transaksi dengan nominal valid yang ditemukan.")
        }
        return results
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val newWidth = (width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
