package com.example.util

import com.example.data.local.TransactionType
import com.example.ui.viewmodel.RecurringUiItem
import com.example.ui.viewmodel.TransactionUiItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDaySummary(
    val dayOfMonth: Int,
    val dayStartMillis: Long,
    val incomeTotal: Long,
    val expenseTotal: Long,
    val transferTotal: Long,
    val transactions: List<TransactionUiItem>,
    val dueRecurring: List<RecurringUiItem>,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isNoSpendDay: Boolean,
    val expenseIntensity: Int
) {
    val netCashflow: Long get() = incomeTotal - expenseTotal
    val hasActivity: Boolean get() = transactions.isNotEmpty() || dueRecurring.isNotEmpty()
}

data class CashflowCalendarMonthState(
    val year: Int,
    val month: Int,
    val monthTitle: String,
    val leadingEmptySlots: Int,
    val days: List<CalendarDaySummary>,
    val totalIncome: Long,
    val totalExpense: Long,
    val netCashflow: Long,
    val noSpendDaysCount: Int,
    val currentNoSpendStreak: Int,
    val bestNoSpendStreak: Int,
    val highestSpendDay: CalendarDaySummary?,
    val averageDailyExpense: Long,
    val upcomingRecurringCount: Int,
    val upcomingRecurringTotal: Long
)

object CashflowCalendarCalculator {
    private val indonesiaLocale = Locale.Builder().setLanguage("in").setRegion("ID").build()

    fun buildMonth(
        year: Int,
        month: Int,
        transactions: List<TransactionUiItem>,
        recurringItems: List<RecurringUiItem>,
        nowMillis: Long = System.currentTimeMillis()
    ): CashflowCalendarMonthState {
        val monthStartCal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = monthStartCal.get(Calendar.DAY_OF_WEEK)
        // Monday-first grid: Monday=0, Tuesday=1, ..., Sunday=6
        val leadingEmptySlots = when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val monthStartMillis = monthStartCal.timeInMillis
        val nextMonthCal = (monthStartCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val monthEndMillis = nextMonthCal.timeInMillis

        val todayStartMillis = DateFormatter.startOfDay(nowMillis)
        val todayCal = Calendar.getInstance().apply { timeInMillis = todayStartMillis }
        val isCurrentMonth = todayCal.get(Calendar.YEAR) == year && todayCal.get(Calendar.MONTH) == month
        val todayDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

        val monthFormat = SimpleDateFormat("MMMM yyyy", indonesiaLocale)
        val monthTitle = monthFormat.format(Date(monthStartMillis))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(indonesiaLocale) else it.toString() }

        // Group month transactions by day-of-month
        val txByDay = mutableMapOf<Int, MutableList<TransactionUiItem>>()
        val dayCal = Calendar.getInstance()
        transactions.forEach { item ->
            val ts = item.entity.timestamp
            if (ts in monthStartMillis until monthEndMillis) {
                dayCal.timeInMillis = ts
                val dom = dayCal.get(Calendar.DAY_OF_MONTH)
                txByDay.getOrPut(dom) { mutableListOf() }.add(item)
            }
        }

        // Map active recurring rules whose nextDueDate falls in this month (or project within this month)
        val recurringByDay = mutableMapOf<Int, MutableList<RecurringUiItem>>()
        recurringItems.filter { it.rule.isActive }.forEach { item ->
            var cursor = item.rule.nextDueDate
            val endLimit = item.rule.endDate
            var safety = 0
            while (cursor < monthStartMillis && safety < 64) {
                cursor = DateFormatter.advanceByFrequency(cursor, item.rule.frequency)
                safety++
            }
            while (cursor in monthStartMillis until monthEndMillis &&
                (endLimit == null || cursor <= endLimit) &&
                safety < 64
            ) {
                dayCal.timeInMillis = cursor
                val dom = dayCal.get(Calendar.DAY_OF_MONTH)
                recurringByDay.getOrPut(dom) { mutableListOf() }.add(item)
                cursor = DateFormatter.advanceByFrequency(cursor, item.rule.frequency)
                safety++
            }
        }

        val rawIncome = LongArray(daysInMonth + 1)
        val rawExpense = LongArray(daysInMonth + 1)
        val rawTransfer = LongArray(daysInMonth + 1)
        var maxDailyExpense = 0L

        for (day in 1..daysInMonth) {
            val dayTx = txByDay[day].orEmpty()
            var inc = 0L
            var exp = 0L
            var trf = 0L
            dayTx.forEach { tx ->
                when (tx.entity.type) {
                    TransactionType.INCOME -> inc += tx.entity.amount
                    TransactionType.EXPENSE -> exp += tx.entity.amount
                    TransactionType.TRANSFER -> trf += tx.entity.amount
                }
            }
            rawIncome[day] = inc
            rawExpense[day] = exp
            rawTransfer[day] = trf
            if (exp > maxDailyExpense) {
                maxDailyExpense = exp
            }
        }

        val daySummaries = (1..daysInMonth).map { day ->
            val cal = (monthStartCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
            val dayStart = cal.timeInMillis
            val isToday = isCurrentMonth && day == todayDayOfMonth
            val isFuture = dayStart > todayStartMillis
            val inc = rawIncome[day]
            val exp = rawExpense[day]
            val trf = rawTransfer[day]
            val dayTx = txByDay[day].orEmpty().sortedByDescending { it.entity.timestamp }
            val dueRec = recurringByDay[day].orEmpty()
            val intensity = when {
                exp <= 0L -> 0
                maxDailyExpense <= 0L -> 1
                else -> {
                    val ratio = exp.toDouble() / maxDailyExpense.toDouble()
                    when {
                        ratio >= 0.65 -> 3
                        ratio >= 0.30 -> 2
                        else -> 1
                    }
                }
            }
            CalendarDaySummary(
                dayOfMonth = day,
                dayStartMillis = dayStart,
                incomeTotal = inc,
                expenseTotal = exp,
                transferTotal = trf,
                transactions = dayTx,
                dueRecurring = dueRec,
                isToday = isToday,
                isFuture = isFuture,
                isNoSpendDay = !isFuture && exp == 0L,
                expenseIntensity = intensity
            )
        }

        val totalIncome = daySummaries.sumOf { it.incomeTotal }
        val totalExpense = daySummaries.sumOf { it.expenseTotal }
        val elapsedDays = daySummaries.filter { !it.isFuture }
        val noSpendDaysCount = elapsedDays.count { it.isNoSpendDay }

        var bestStreak = 0
        var runningStreak = 0
        elapsedDays.forEach { day ->
            if (day.isNoSpendDay) {
                runningStreak++
                if (runningStreak > bestStreak) bestStreak = runningStreak
            } else {
                runningStreak = 0
            }
        }
        val currentStreak = runningStreak // consecutive no-spend days ending on the latest elapsed day

        val highestSpendDay = daySummaries
            .filter { it.expenseTotal > 0L }
            .maxByOrNull { it.expenseTotal }

        val divisor = elapsedDays.size.coerceAtLeast(1)
        val averageDailyExpense = totalExpense / divisor

        val upcomingRec = daySummaries
            .filter { !it.dayStartMillis.let { ts -> ts < todayStartMillis } }
            .flatMap { it.dueRecurring }

        return CashflowCalendarMonthState(
            year = year,
            month = month,
            monthTitle = monthTitle,
            leadingEmptySlots = leadingEmptySlots,
            days = daySummaries,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashflow = totalIncome - totalExpense,
            noSpendDaysCount = noSpendDaysCount,
            currentNoSpendStreak = currentStreak,
            bestNoSpendStreak = bestStreak,
            highestSpendDay = highestSpendDay,
            averageDailyExpense = averageDailyExpense,
            upcomingRecurringCount = upcomingRec.size,
            upcomingRecurringTotal = upcomingRec
                .filter { it.rule.type == TransactionType.EXPENSE }
                .sumOf { it.rule.amount }
        )
    }
}
