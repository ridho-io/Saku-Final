package com.example

import com.example.ai.CategoryAutoTagger
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.util.MathEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ExampleUnitTest {
    @Test
    fun formatRupiah_formatsWithZeroDecimalsAndCleanSpacing() {
        assertEquals("Rp 25.000", CurrencyFormatter.formatRupiah(25_000L))
        assertEquals("+Rp 1.500.000", CurrencyFormatter.formatRupiah(1_500_000L, showSign = true, isPositive = true))
        assertEquals("-Rp 34.000", CurrencyFormatter.formatRupiah(34_000L, showSign = true, isPositive = false))
    }

    @Test
    fun mathEvaluator_evaluatesLiveCalculatorExpressions() {
        assertEquals(28_000L, MathEvaluator.evaluate("25000+3000"))
        assertEquals(25_000L, MathEvaluator.evaluate("25000+"))
        assertEquals(25_000L, MathEvaluator.evaluate("25000×"))
        assertEquals(90_000L, MathEvaluator.evaluate("50000*2-10000"))
        assertEquals(25_000L, MathEvaluator.evaluate("100000/4"))
        assertNull(MathEvaluator.evaluate("10000/0"))
        assertNull(MathEvaluator.evaluate(""))
    }

    @Test
    fun dateFormatter_groupsTodayYesterdayAndYearBoundary() {
        val now = 1_763_200_000_000L
        assertEquals("Hari Ini", DateFormatter.formatDateHeader(now, now))
        assertEquals("Kemarin", DateFormatter.formatDateHeader(now - TimeUnit.DAYS.toMillis(1), now))

        val jan1 = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dec31 = Calendar.getInstance().apply {
            set(2025, Calendar.DECEMBER, 31, 22, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals("Hari Ini", DateFormatter.formatDateHeader(jan1, jan1))
        assertEquals("Kemarin", DateFormatter.formatDateHeader(dec31, jan1))
    }

    @Test
    fun dateFormatter_utcPickerRoundTripPreservesCalendarDate() {
        val localDate = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 22, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val utcPicker = DateFormatter.localMillisToUtcPickerMillis(localDate)
        val restoredLocal = DateFormatter.utcPickerMillisToLocalNoon(utcPicker)

        val cal = Calendar.getInstance().apply { timeInMillis = restoredLocal }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun categoryAutoTagger_prioritizesUserHistoryOverGenericRules() {
        val history = listOf(
            TransactionEntity(
                merchant = "Kedai Pak Budi",
                amount = 45_000L,
                type = TransactionType.EXPENSE,
                category = "Hiburan",
                walletId = 1L,
                note = "Billiard & minum",
                timestamp = System.currentTimeMillis()
            )
        )
        val suggestion = CategoryAutoTagger.suggestLocally(
            description = "Kedai Pak Budi malam",
            txType = TransactionType.EXPENSE,
            history = history
        )
        assertNotNull(suggestion)
        assertEquals("Hiburan", suggestion?.category)
        assertTrue(suggestion?.sourceLabel?.contains("Riwayat") == true)
    }

    @Test
    fun categoryAutoTagger_classifiesCommonFoodAndTransportKeywords() {
        val food = CategoryAutoTagger.suggestLocally("Makan siang Warteg", TransactionType.EXPENSE, emptyList())
        assertEquals("Makanan & Minuman", food?.category)

        val transport = CategoryAutoTagger.suggestLocally("GoRide ke Stasiun", TransactionType.EXPENSE, emptyList())
        assertEquals("Transportasi", transport?.category)

        val salary = CategoryAutoTagger.suggestLocally("Transfer Payroll Gaji", TransactionType.INCOME, emptyList())
        assertEquals("Gaji", salary?.category)
    }

    @Test
    fun dateFormatter_cycleBoundsAndLabelRespectPaydayStartDay() {
        val nov10 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Start day 25 on Nov 10 -> cycle is Oct 25 to Nov 25 (exclusive)
        val (start, endExclusive) = DateFormatter.cycleBounds(nov10, startDay = 25)
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = endExclusive }
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(25, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.NOVEMBER, endCal.get(Calendar.MONTH))
        assertEquals(25, endCal.get(Calendar.DAY_OF_MONTH))

        val label = DateFormatter.cycleLabel(nov10, startDay = 25)
        assertTrue(label.contains("25"))
        assertTrue(label.contains("24"))
    }
}


