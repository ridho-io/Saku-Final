package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

object CurrencyFormatter {
    private val indonesiaLocale = Locale.Builder().setLanguage("in").setRegion("ID").build()

    private val rupiahFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(indonesiaLocale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    fun formatRupiah(amount: Long, showSign: Boolean = false, isPositive: Boolean = true): String {
        val absAmount = kotlin.math.abs(amount)
        val raw = rupiahFormat.format(absAmount)
            .replace("IDR", "Rp")
            .replace("Rp", "Rp ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return when {
            showSign && isPositive -> "+$raw"
            showSign && !isPositive -> "-$raw"
            amount < 0 -> "-$raw"
            else -> raw
        }
    }

    fun formatCompactRupiah(amount: Long): String {
        val abs = kotlin.math.abs(amount)
        val prefix = if (amount < 0) "-Rp " else "Rp "
        return when {
            abs >= 1_000_000_000L -> String.format(indonesiaLocale, "%s%.1f M", prefix, abs / 1_000_000_000.0)
            abs >= 1_000_000L -> String.format(indonesiaLocale, "%s%.1f jt", prefix, abs / 1_000_000.0)
            abs >= 1_000L -> String.format(indonesiaLocale, "%s%d rb", prefix, abs / 1_000L)
            else -> formatRupiah(amount)
        }
    }
}

object DateFormatter {
    private val indonesiaLocale = Locale.Builder().setLanguage("in").setRegion("ID").build()
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val monthLabelFormat = SimpleDateFormat("MMMM yyyy", indonesiaLocale)
    private val fullDateFormat = SimpleDateFormat("d MMM yyyy", indonesiaLocale)
    private val timeFormat = SimpleDateFormat("HH:mm", indonesiaLocale)

    fun currentMonthKey(timeMillis: Long = System.currentTimeMillis()): String =
        monthKeyFormat.format(Date(timeMillis))

    fun currentMonthLabel(timeMillis: Long = System.currentTimeMillis()): String =
        monthLabelFormat.format(Date(timeMillis))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(indonesiaLocale) else it.toString() }

    fun monthBounds(timeMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    fun cycleBounds(timeMillis: Long = System.currentTimeMillis(), startDay: Int = 1): Pair<Long, Long> {
        val safeDay = startDay.coerceIn(1, 28)
        if (safeDay == 1) return monthBounds(timeMillis)

        val nowCal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val startCal = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (currentDay < safeDay) {
                add(Calendar.MONTH, -1)
            }
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, safeDay.coerceAtMost(maxDay))
        }
        val start = startCal.timeInMillis
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, safeDay.coerceAtMost(maxDay))
        }
        return start to endCal.timeInMillis
    }

    fun cycleLabel(timeMillis: Long = System.currentTimeMillis(), startDay: Int = 1): String {
        val safeDay = startDay.coerceIn(1, 28)
        if (safeDay == 1) return currentMonthLabel(timeMillis)
        val (start, endExclusive) = cycleBounds(timeMillis, safeDay)
        val fmt = SimpleDateFormat("d MMM", indonesiaLocale)
        val endInclusive = endExclusive - 1L
        return "${fmt.format(Date(start))} - ${fmt.format(Date(endInclusive))}"
    }

    fun formatDateHeader(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val todayStart = startOfDay(nowMillis)
        val yesterdayStart = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val targetStart = startOfDay(timestamp)

        return when (targetStart) {
            todayStart -> "Hari Ini"
            yesterdayStart -> "Kemarin"
            else -> fullDateFormat.format(Date(timestamp))
        }
    }

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun formatShortDate(timestamp: Long): String = fullDateFormat.format(Date(timestamp))

    fun startOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun noonOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun advanceByFrequency(timestamp: Long, frequency: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        when (frequency) {
            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            "YEARLY" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    /**
     * Converts a local timestamp to UTC midnight millis expected by Material 3 DatePicker.
     */
    fun localMillisToUtcPickerMillis(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Converts UTC midnight millis returned by Material 3 DatePicker into 09:00 local time on that exact calendar date.
     */
    fun utcPickerMillisToLocalNoon(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return Calendar.getInstance().apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                9,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

/**
 * Safely evaluates arithmetic expressions (+, -, *, /) entered in the manual Calculator Numpad.
 */
object MathEvaluator {
    fun evaluate(expression: String): Long? {
        val clean = expression
            .replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .dropLastWhile { it in charArrayOf('+', '-', '*', '/', '.') }
        if (clean.isBlank()) return null
        return try {
            val result = Parser(clean).parse()
            if (result.isNaN() || result.isInfinite()) null else result.roundToLong().coerceAtLeast(0L)
        } catch (_: Exception) {
            null
        }
    }

    private class Parser(private val str: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw IllegalArgumentException("Unexpected char")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Divide by zero")
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            val startPos = pos
            if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                return str.substring(startPos, pos).toDouble()
            }
            throw IllegalArgumentException("Invalid number")
        }
    }
}
