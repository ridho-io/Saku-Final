package com.example.nfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.NfcF
import java.io.IOException
import java.util.Locale

enum class EmoneyIssuer(
    val displayName: String,
    val badgeText: String,
    val accentHex: Long
) {
    MANDIRI_EMONEY("Mandiri e-Money", "MANDIRI", 0xFF003D79L),
    BNI_TAPCASH("BNI TapCash", "BNI", 0xFFF15A22L),
    BRI_BRIZZI("BRI Brizzi", "BRI", 0xFF00529CL),
    BCA_FLAZZ("BCA Flazz", "BCA", 0xFF0060AFL),
    KMT_COMMUTER("KMT Commuter", "KMT", 0xFFE11D48L),
    UNKNOWN_NFC("Kartu NFC", "NFC", 0xFF334155L)
}

data class EmoneyCardSnapshot(
    val uidHex: String,
    val issuer: EmoneyIssuer,
    val cardNumber: String,
    val balance: Long,
    val techLabel: String,
    val scannedAtMillis: Long = System.currentTimeMillis()
)

sealed interface EmoneyScanOutcome {
    data class BalanceRead(val card: EmoneyCardSnapshot) : EmoneyScanOutcome
    data class ProtectedCardDetected(
        val issuer: EmoneyIssuer,
        val uidHex: String,
        val cardNumber: String?,
        val techSummary: String,
        val explanation: String
    ) : EmoneyScanOutcome
    data class ReadError(val title: String, val message: String) : EmoneyScanOutcome
}

object EmoneyProtocolParser {

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(Locale.US, it.toInt() and 0xFF) }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace(" ", "").trim()
        require(clean.length % 2 == 0) { "Hex length must be even" }
        return ByteArray(clean.length / 2) { idx ->
            clean.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
        }
    }

    fun isIsoSuccess(response: ByteArray?): Boolean {
        if (response == null || response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
    }

    private const val MAX_EMONEY_BALANCE_RUPIAH = 20_000_000L

    fun formatCardNumber(rawDigitsOrHex: String): String {
        val clean = rawDigitsOrHex.filter { it.isLetterOrDigit() }.uppercase(Locale.US)
        if (clean.length <= 4) return clean
        return clean.chunked(4).joinToString(" ")
    }

    fun maskCardNumber(cardNumber: String): String {
        val clean = cardNumber.filter { it.isLetterOrDigit() }
        if (clean.length <= 6) return "•••• ${clean.takeLast(minOf(4, clean.length))}".trim()
        val first = clean.take(4)
        val last = clean.takeLast(4)
        return "$first •••• •••• $last"
    }

    /**
     * Parses Mandiri e-Money balance APDU (`00 B5 00 00 0A` or `00 B5 00 00 04`) response.
     * Payload bytes 0..3 hold a 32-bit unsigned little-endian Rupiah balance followed by SW1 SW2 = 90 00.
     * Caps at Indonesia e-money regulatory limit (Rp 20.000.000) to reject corrupted/garbage reads.
     */
    fun parseMandiriBalanceResponse(response: ByteArray): Long? {
        if (!isIsoSuccess(response) || response.size < 6) return null
        val b0 = response[0].toLong() and 0xFFL
        val b1 = response[1].toLong() and 0xFFL
        val b2 = response[2].toLong() and 0xFFL
        val b3 = response[3].toLong() and 0xFFL
        val value = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        return value.takeIf { it in 0L..MAX_EMONEY_BALANCE_RUPIAH }
    }

    /**
     * Parses Mandiri e-Money card number APDU (`00 B3 00 00 3F`) response.
     * Supports both 16-byte ASCII digits and 8-byte BCD encoding.
     */
    fun parseMandiriCardNumberResponse(response: ByteArray): String? {
        if (!isIsoSuccess(response) || response.size < 10) return null
        val payloadLen = response.size - 2
        val payload = response.copyOfRange(0, payloadLen)

        val asciiDigits = payload
            .take(16)
            .map { (it.toInt() and 0xFF).toChar() }
            .filter { it.isDigit() }
            .joinToString("")
        if (asciiDigits.length >= 12) {
            return formatCardNumber(asciiDigits.take(16))
        }

        val bcdHex = bytesToHex(payload.copyOfRange(0, minOf(8, payload.size)))
        return if (bcdHex.all { it.isDigit() } && bcdHex.length >= 12) {
            formatCardNumber(bcdHex)
        } else {
            null
        }
    }

    /**
     * Parses BNI TapCash CEPAS Purse APDU (`90 32 03 00 00`) response.
     * Bytes 2..4 hold a 24-bit signed big-endian Rupiah balance; bytes 8..15 hold the 8-byte CAN.
     */
    fun parseBniTapCashPurseResponse(response: ByteArray): Pair<Long, String>? {
        if (!isIsoSuccess(response) || response.size < 18) return null
        val b2 = response[2].toInt()
        val b3 = response[3].toInt() and 0xFF
        val b4 = response[4].toInt() and 0xFF
        val balance = (((b2 shl 24) shr 8) or (b3 shl 8) or b4).toLong()
        if (balance !in 0L..MAX_EMONEY_BALANCE_RUPIAH) return null
        val canBytes = response.copyOfRange(8, 16)
        val canFormatted = formatCardNumber(bytesToHex(canBytes))
        return balance to canFormatted
    }

    /**
     * Parses a 16-byte FeliCa unencrypted commuter purse block (4-byte little-endian balance at offset 0..3).
     */
    fun parseFelicaReadResponseBalance(response: ByteArray): Long? {
        // Standard FeliCa Read Without Encryption response:
        // [0]=len, [1]=0x07, [2..9]=IDm, [10]=status1, [11]=status2, [12]=blockCount, [13..28]=block0
        if (response.size < 29) return null
        if ((response[1].toInt() and 0xFF) != 0x07) return null
        val status1 = response[10].toInt() and 0xFF
        val status2 = response[11].toInt() and 0xFF
        if (status1 != 0x00 || status2 != 0x00) return null
        val b0 = response[13].toLong() and 0xFFL
        val b1 = response[14].toLong() and 0xFFL
        val b2 = response[15].toLong() and 0xFFL
        val b3 = response[16].toLong() and 0xFFL
        val le = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        return le.takeIf { it in 0L..MAX_EMONEY_BALANCE_RUPIAH }
    }
}

object EmoneyNfcReader {
    // Mandiri e-Money APDUs (primary + fallback AID & Le=04)
    private val MANDIRI_SELECT_8 = EmoneyProtocolParser.hexToBytes("00A40400080000000000000001")
    private val MANDIRI_SELECT_7 = EmoneyProtocolParser.hexToBytes("00A404000700000000000001")
    private val MANDIRI_SELECT_ALT = EmoneyProtocolParser.hexToBytes("00A4040008D000000000000001")
    private val MANDIRI_GET_BALANCE = EmoneyProtocolParser.hexToBytes("00B500000A")
    private val MANDIRI_GET_BALANCE_SHORT = EmoneyProtocolParser.hexToBytes("00B5000004")
    private val MANDIRI_GET_CARD_NO = EmoneyProtocolParser.hexToBytes("00B300003F")

    // BNI TapCash CEPAS APDUs
    private val BNI_SELECT = EmoneyProtocolParser.hexToBytes("00A4040008A000424E49100001")
    private val BNI_SELECT_ALT = EmoneyProtocolParser.hexToBytes("00A4040007A000424E499900")
    private val BNI_GET_PURSE = EmoneyProtocolParser.hexToBytes("9032030000")

    // BRI Brizzi detection APDUs
    private val BRI_DESFIRE_SELECT_APP1 = EmoneyProtocolParser.hexToBytes("905A00000301000000")
    private val BRI_ISO_SELECT = EmoneyProtocolParser.hexToBytes("00A4040008D000000001000001")

    // BCA Flazz detection APDUs
    private val BCA_FLAZZ_SELECT_1 = EmoneyProtocolParser.hexToBytes("00A4040008A0000004220001")
    private val BCA_FLAZZ_SELECT_2 = EmoneyProtocolParser.hexToBytes("00A404000AA0000000030000000100")

    fun readTag(tag: Tag): EmoneyScanOutcome {
        val uidHex = EmoneyProtocolParser.bytesToHex(tag.id ?: ByteArray(0)).ifBlank { "UNKNOWN" }
        val techSummary = tag.techList
            .map { it.substringAfterLast('.') }
            .distinct()
            .joinToString(" • ")

        return try {
            val isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                return readIsoDepCard(isoDep, uidHex, techSummary)
            }

            val nfcF = NfcF.get(tag)
            if (nfcF != null) {
                return readFelicaCard(nfcF, uidHex, techSummary)
            }

            val nfcA = NfcA.get(tag)
            if (nfcA != null) {
                return readNfcAFallback(uidHex, techSummary)
            }

            EmoneyScanOutcome.ProtectedCardDetected(
                issuer = EmoneyIssuer.UNKNOWN_NFC,
                uidHex = uidHex,
                cardNumber = EmoneyProtocolParser.formatCardNumber(uidHex),
                techSummary = techSummary,
                explanation = "Kartu NFC terdeteksi ($techSummary), namun protokol kartu ini tidak mendukung pembacaan saldo offline. Kamu tetap bisa menyimpannya lewat Input Manual."
            )
        } catch (_: TagLostException) {
            EmoneyScanOutcome.ReadError(
                title = "Kartu Terlepas Terlalu Cepat",
                message = "Tempelkan kartu lebih stabil di belakang ponsel selama 1–2 detik hingga pembacaan selesai, atau catat saldonya lewat Input Manual."
            )
        } catch (_: SecurityException) {
            EmoneyScanOutcome.ReadError(
                title = "Sesi NFC Kedaluwarsa",
                message = "Jauhkan kartu sebentar lalu tempelkan kembali ke sensor NFC belakang ponsel."
            )
        } catch (e: IOException) {
            EmoneyScanOutcome.ReadError(
                title = "Koneksi NFC Terganggu",
                message = e.localizedMessage ?: "Pastikan posisi kartu tepat di sensor NFC belakang ponsel."
            )
        } catch (e: Exception) {
            EmoneyScanOutcome.ReadError(
                title = "Gagal Membaca Kartu",
                message = e.localizedMessage ?: "Terjadi kendala saat berkomunikasi dengan chip kartu."
            )
        }
    }

    private fun readIsoDepCard(
        isoDep: IsoDep,
        uidHex: String,
        techSummary: String
    ): EmoneyScanOutcome {
        isoDep.use { dep ->
            dep.timeout = 3000
            dep.connect()

            // 1. Mandiri e-Money (multiple AID + short/long Le fallback)
            val mandiriSel = listOf(MANDIRI_SELECT_8, MANDIRI_SELECT_7, MANDIRI_SELECT_ALT)
                .firstNotNullOfOrNull { cmd ->
                    runCatching { dep.transceive(cmd) }.getOrNull()
                        ?.takeIf { EmoneyProtocolParser.isIsoSuccess(it) }
                }

            if (mandiriSel != null) {
                val balance = listOf(MANDIRI_GET_BALANCE, MANDIRI_GET_BALANCE_SHORT)
                    .firstNotNullOfOrNull { cmd ->
                        runCatching { dep.transceive(cmd) }.getOrNull()
                            ?.let { EmoneyProtocolParser.parseMandiriBalanceResponse(it) }
                    }
                if (balance != null) {
                    val infoResp = runCatching { dep.transceive(MANDIRI_GET_CARD_NO) }.getOrNull()
                    val cardNo = infoResp?.let { EmoneyProtocolParser.parseMandiriCardNumberResponse(it) }
                        ?: EmoneyProtocolParser.formatCardNumber(uidHex)
                    return EmoneyScanOutcome.BalanceRead(
                        EmoneyCardSnapshot(
                            uidHex = uidHex,
                            issuer = EmoneyIssuer.MANDIRI_EMONEY,
                            cardNumber = cardNo,
                            balance = balance,
                            techLabel = techSummary
                        )
                    )
                }
            }

            // 2. BNI TapCash (primary + fallback AID)
            val bniSel = listOf(BNI_SELECT, BNI_SELECT_ALT)
                .firstNotNullOfOrNull { cmd ->
                    runCatching { dep.transceive(cmd) }.getOrNull()
                        ?.takeIf { EmoneyProtocolParser.isIsoSuccess(it) }
                }
            if (bniSel != null) {
                val purseResp = runCatching { dep.transceive(BNI_GET_PURSE) }.getOrNull()
                val parsed = purseResp?.let { EmoneyProtocolParser.parseBniTapCashPurseResponse(it) }
                if (parsed != null) {
                    val (balance, can) = parsed
                    return EmoneyScanOutcome.BalanceRead(
                        EmoneyCardSnapshot(
                            uidHex = uidHex,
                            issuer = EmoneyIssuer.BNI_TAPCASH,
                            cardNumber = can,
                            balance = balance,
                            techLabel = techSummary
                        )
                    )
                }
            }

            // 3. BRI Brizzi detection
            val briDesfire = runCatching { dep.transceive(BRI_DESFIRE_SELECT_APP1) }.getOrNull()
            val briIso = runCatching { dep.transceive(BRI_ISO_SELECT) }.getOrNull()
            val isBriDesfireOk = briDesfire != null && briDesfire.size >= 2 &&
                (briDesfire[briDesfire.size - 2].toInt() and 0xFF) == 0x91 &&
                (briDesfire[briDesfire.size - 1].toInt() and 0xFF) == 0x00
            if (isBriDesfireOk || EmoneyProtocolParser.isIsoSuccess(briIso)) {
                return EmoneyScanOutcome.ProtectedCardDetected(
                    issuer = EmoneyIssuer.BRI_BRIZZI,
                    uidHex = uidHex,
                    cardNumber = EmoneyProtocolParser.formatCardNumber(uidHex),
                    techSummary = techSummary,
                    explanation = "Kartu BRI Brizzi terdeteksi. Sektor saldo kartu Brizzi menggunakan enkripsi kunci SAM/server penerbit sehingga hanya UID chip yang bisa dibaca secara offline."
                )
            }

            // 4. BCA Flazz detection
            val flazz1 = runCatching { dep.transceive(BCA_FLAZZ_SELECT_1) }.getOrNull()
            val flazz2 = runCatching { dep.transceive(BCA_FLAZZ_SELECT_2) }.getOrNull()
            if (EmoneyProtocolParser.isIsoSuccess(flazz1) || EmoneyProtocolParser.isIsoSuccess(flazz2)) {
                return EmoneyScanOutcome.ProtectedCardDetected(
                    issuer = EmoneyIssuer.BCA_FLAZZ,
                    uidHex = uidHex,
                    cardNumber = EmoneyProtocolParser.formatCardNumber(uidHex),
                    techSummary = techSummary,
                    explanation = "Kartu BCA Flazz terdeteksi. Kartu Flazz menggunakan otentikasi kunci terenkripsi BCA untuk membaca saldo."
                )
            }

            return EmoneyScanOutcome.ProtectedCardDetected(
                issuer = EmoneyIssuer.UNKNOWN_NFC,
                uidHex = uidHex,
                cardNumber = EmoneyProtocolParser.formatCardNumber(uidHex),
                techSummary = techSummary,
                explanation = "Chip ISO-DEP terdeteksi, namun tipe kartu ini tidak memakai struktur saldo publik Mandiri e-Money / BNI TapCash."
            )
        }
    }

    private fun readFelicaCard(
        nfcF: NfcF,
        uidHex: String,
        techSummary: String
    ): EmoneyScanOutcome {
        nfcF.use { felica ->
            felica.timeout = 2500
            felica.connect()
            val idm = felica.tag.id ?: ByteArray(8)
            // Attempt unencrypted FeliCa Read Without Encryption (cmd 0x06) on common commuter purse service 0x090F
            if (idm.size == 8) {
                val cmd = ByteArray(16).apply {
                    this[0] = 16.toByte()
                    this[1] = 0x06.toByte()
                    System.arraycopy(idm, 0, this, 2, 8)
                    this[10] = 0x01.toByte() // 1 service
                    this[11] = 0x0F.toByte() // service code low (0x090F LE)
                    this[12] = 0x09.toByte() // service code high
                    this[13] = 0x01.toByte() // 1 block
                    this[14] = 0x80.toByte() // 2-byte block list item, service 0
                    this[15] = 0x00.toByte() // block 0
                }
                val resp = runCatching { felica.transceive(cmd) }.getOrNull()
                val balance = resp?.let { EmoneyProtocolParser.parseFelicaReadResponseBalance(it) }
                if (balance != null) {
                    return EmoneyScanOutcome.BalanceRead(
                        EmoneyCardSnapshot(
                            uidHex = uidHex,
                            issuer = EmoneyIssuer.KMT_COMMUTER,
                            cardNumber = EmoneyProtocolParser.formatCardNumber(uidHex),
                            balance = balance,
                            techLabel = techSummary
                        )
                    )
                }
            }

            return EmoneyScanOutcome.ProtectedCardDetected(
                issuer = EmoneyIssuer.KMT_COMMUTER,
                uidHex = uidHex,
                cardNumber = EmoneyProtocolParser.formatCardNumber(uidHex),
                techSummary = techSummary,
                explanation = "Kartu FeliCa / KMT Commuter terdeteksi (IDm: $uidHex), namun blok saldo memerlukan kunci otentikasi sistem."
            )
        }
    }

    private fun readNfcAFallback(
        uidHex: String,
        techSummary: String
    ): EmoneyScanOutcome {
        val isMifareClassic = techSummary.contains("MifareClassic", ignoreCase = true)
        val detail = if (isMifareClassic) {
            "Kartu MIFARE Classic terdeteksi. Sektor data dikunci oleh penerbit kartu sehingga saldo tidak dapat dibaca secara publik."
        } else {
            "Tag NFC-A terdeteksi ($techSummary). UID chip terbaca, namun tidak berisi applet saldo e-Money publik."
        }
        return EmoneyScanOutcome.ProtectedCardDetected(
            issuer = EmoneyIssuer.UNKNOWN_NFC,
            uidHex = uidHex,
            cardNumber = EmoneyProtocolParser.formatCardNumber(uidHex),
            techSummary = techSummary,
            explanation = detail
        )
    }
}
