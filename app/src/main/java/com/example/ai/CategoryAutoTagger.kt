package com.example.ai

import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import java.util.Locale

data class AutoTagSuggestion(
    val category: String,
    val confidence: Float,
    val sourceLabel: String,
    val isAiPowered: Boolean = false
)

/**
 * Fast, zero-token local auto-tagging engine that predicts a transaction category
 * from the user's personal transaction history first, then Indonesian merchant/keyword rules.
 * Cloud Gemini calls are reserved strictly for unfamiliar/ambiguous descriptions.
 */
object CategoryAutoTagger {

    private val stopWords = setOf(
        "di", "ke", "dari", "dan", "atau", "yang", "untuk", "buat", "bayar",
        "beli", "pembelian", "pembayaran", "transaksi", "via", "pakai", "rp",
        "idr", "rb", "ribu", "jt", "juta", "k", "pts", "pt", "tbk", "id"
    )

    private data class KeywordRule(
        val category: String,
        val isIncome: Boolean = false,
        val keywords: List<String>
    )

    private val keywordRules = listOf(
        KeywordRule(
            category = "Gaji",
            isIncome = true,
            keywords = listOf("gaji", "payroll", "salary", "upah", "honor", "thr", "insentif", "tunjangan")
        ),
        KeywordRule(
            category = "Bonus & Investasi",
            isIncome = true,
            keywords = listOf(
                "bonus", "dividen", "kupon", "bunga", "cashback", "refund", "reksadana",
                "saham", "bibit", "bareksa", "deposito", "sbn", "freelance", "proyek", "komisi"
            )
        ),
        KeywordRule(
            category = "Kopi & Jajan",
            keywords = listOf(
                "kopi", "coffee", "kenangan", "fore", "janji jiwa", "starbucks", "tuku",
                "point coffee", "tomoro", "flash coffee", "boba", "chatime", "mixue",
                "haus", "matcha", "latte", "es teh", "jajan", "cemilan", "snack",
                "martabak", "bakery", "roti", "donut", "jco", "krispy", "gelato", "biscoff"
            )
        ),
        KeywordRule(
            category = "Makanan & Minuman",
            keywords = listOf(
                "makan", "makanan", "sarapan", "lunch", "dinner", "warteg", "padang",
                "nasi", "bakso", "mie", "soto", "sate", "ayam", "bebek", "geprek",
                "ramen", "sushi", "kfc", "mcd", "burger", "pizza", "hokben", "solaria",
                "gofood", "grabfood", "shopeefood", "restoran", "warung", "kantin",
                "superindo", "sayurbox", "buah", "sembako", "galon", "aqua"
            )
        ),
        KeywordRule(
            category = "Transportasi",
            keywords = listOf(
                "gojek", "goride", "gocar", "grab", "grabbike", "grabcar", "maxim",
                "indrive", "bluebird", "taksi", "bensin", "pertamina", "pertamax",
                "pertalite", "shell", "spbu", "solar", "parkir", "tol", "etoll",
                "flazz", "emoney", "brizzi", "krl", "commuter", "mrt", "lrt",
                "transjakarta", "tj", "kai", "kereta", "tiket pesawat", "garuda",
                "lion", "citilink", "servis motor", "servis mobil", "tambal ban", "bengkel"
            )
        ),
        KeywordRule(
            category = "Tagihan & Kos",
            keywords = listOf(
                "listrik", "pln", "token", "air", "pdam", "kos", "kost", "kontrakan",
                "sewa", "ipl", "internet", "wifi", "indihome", "biznet", "myrepublic",
                "firstmedia", "oxygen", "telkomsel", "xl", "indosat", "tri", "smartfren",
                "pulsa", "paket data", "kuota", "bpjs", "pajak", "cicilan", "tagihan", "pascabayar"
            )
        ),
        KeywordRule(
            category = "Hiburan",
            keywords = listOf(
                "bioskop", "xxi", "cgv", "cinepolis", "nonton", "film", "netflix",
                "spotify", "youtube", "disney", "vidio", "viu", "steam", "playstation",
                "nintendo", "game", "topup game", "mlbb", "valorant", "konser", "tiket.com",
                "traveloka", "karaoke", "billiard", "bowling", "gym", "fitness"
            )
        ),
        KeywordRule(
            category = "Kesehatan",
            keywords = listOf(
                "apotek", "kimia farma", "k24", "guardian", "watson", "obat", "vitamin",
                "suplemen", "dokter", "klinik", "rumah sakit", "rs", "halodoc", "alodokter",
                "lab", "prodia", "cek darah", "gigi", "dentist", "kacamata", "optik"
            )
        ),
        KeywordRule(
            category = "Belanja",
            keywords = listOf(
                "indomaret", "alfamart", "alfamidi", "lawson", "familymart", "tokopedia",
                "shopee", "lazada", "blibli", "tiktok shop", "uniqlo", "zara", "hm",
                "erigo", "ikea", "ace hardware", "azko", "mr diy", "miniso", "sociolla",
                "sephora", "skincare", "sabun", "shampoo", "baju", "celana", "sepatu",
                "belanja", "mall", "buka lapak", "elektronik"
            )
        )
    )

    fun suggestLocally(
        description: String,
        txType: String,
        history: List<TransactionEntity>
    ): AutoTagSuggestion? {
        val cleanDesc = normalizeText(description)
        if (cleanDesc.length < 2) return null

        // 1. Prioritize user history match (personalized tagging!)
        val historySuggestion = matchFromHistory(cleanDesc, txType, history)
        if (historySuggestion != null) {
            return historySuggestion
        }

        // 2. Fall back to deterministic Indonesian keyword patterns
        return matchFromKeywords(cleanDesc, txType)
    }

    private fun matchFromHistory(
        cleanDesc: String,
        txType: String,
        history: List<TransactionEntity>
    ): AutoTagSuggestion? {
        val descTokens = tokenize(cleanDesc)
        val filteredHistory = history.asSequence()
            .filter { it.type == txType && it.type != TransactionType.TRANSFER && it.category != "Transfer" }
            .take(150)
            .toList()

        if (filteredHistory.isEmpty()) return null

        val categoryScores = mutableMapOf<String, Double>()
        val bestMatchedMerchant = mutableMapOf<String, String>()

        filteredHistory.forEachIndexed { index, tx ->
            val recencyWeight = 1.0 + (150 - index).coerceAtLeast(0) / 300.0
            val cleanMerchant = normalizeText(tx.merchant)
            val cleanNote = normalizeText(tx.note)

            var matchScore = 0.0
            when {
                cleanMerchant == cleanDesc -> matchScore += 12.0
                cleanMerchant.length >= 3 && (cleanDesc.contains(cleanMerchant) || cleanMerchant.contains(cleanDesc)) -> {
                    matchScore += 7.5
                }
                else -> {
                    val txTokens = tokenize("$cleanMerchant $cleanNote")
                    val overlap = descTokens.intersect(txTokens)
                    if (overlap.isNotEmpty()) {
                        matchScore += overlap.size * 4.0
                    }
                }
            }

            if (matchScore > 0.0) {
                val weighted = matchScore * recencyWeight
                categoryScores[tx.category] = (categoryScores[tx.category] ?: 0.0) + weighted
                bestMatchedMerchant.putIfAbsent(tx.category, tx.merchant)
            }
        }

        val top = categoryScores.maxByOrNull { it.value } ?: return null
        if (top.value < 3.8) return null

        val matchedName = bestMatchedMerchant[top.key].orEmpty()
        return AutoTagSuggestion(
            category = top.key,
            confidence = if (top.value >= 10.0) 0.96f else 0.88f,
            sourceLabel = if (matchedName.isNotBlank()) "Riwayat • $matchedName" else "Dari riwayatmu",
            isAiPowered = false
        )
    }

    private fun matchFromKeywords(cleanDesc: String, txType: String): AutoTagSuggestion? {
        val wantIncome = txType == TransactionType.INCOME
        val tokens = tokenize(cleanDesc)

        var bestCategory: String? = null
        var bestScore = 0

        for (rule in keywordRules) {
            if (rule.isIncome != wantIncome) continue
            var score = 0
            for (kw in rule.keywords) {
                if (kw.contains(' ')) {
                    if (cleanDesc.contains(kw)) score += 3
                } else {
                    if (kw in tokens || (kw.length >= 4 && cleanDesc.contains(kw))) {
                        score += 2
                    }
                }
            }
            if (score > bestScore) {
                bestScore = score
                bestCategory = rule.category
            }
        }

        val matched = bestCategory ?: return null
        if (bestScore < 2) return null

        return AutoTagSuggestion(
            category = matched,
            confidence = if (bestScore >= 4) 0.90f else 0.82f,
            sourceLabel = "Pola otomatis",
            isAiPowered = false
        )
    }

    fun buildHistoryHintsForPrompt(
        history: List<TransactionEntity>,
        maxHints: Int = 8
    ): String {
        if (history.isEmpty()) return ""
        return history.asSequence()
            .filter { it.type != TransactionType.TRANSFER && it.merchant.isNotBlank() && it.category.isNotBlank() }
            .groupBy { it.merchant.trim().lowercase(Locale.ROOT) }
            .entries
            .sortedByDescending { it.value.size }
            .take(maxHints)
            .joinToString("; ") { (_, list) ->
                val sample = list.first()
                "${sample.merchant}=>${sample.category}"
            }
    }

    private fun normalizeText(raw: String): String {
        return raw.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun tokenize(normalized: String): Set<String> {
        return normalized.split(' ')
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 2 && it !in stopWords && !it.all(Char::isDigit) }
            .toSet()
    }
}
