package com.example

import com.example.data.local.HomeShortcutOption
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.viewmodel.RecurringUiItem
import com.example.ui.viewmodel.TransactionUiItem
import com.example.util.CashflowCalendarCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CashflowCalendarTest {

    @Test
    fun cashflowCalendarCalculator_computesHeatmapStreaksAndRecurringDueDates() {
        assertTrue(HomeShortcutOption.entries.contains(HomeShortcutOption.CALENDAR))

        fun dayMillis(day: Int, hour: Int = 10): Long {
            return Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.NOVEMBER, day, hour, 0, 0)
            }.timeInMillis
        }

        // Nov 1, 2026 is a Sunday -> Monday-first leadingEmptySlots = 6
        val now = dayMillis(6, 15)
        val txs = listOf(
            TransactionUiItem(
                entity = TransactionEntity(
                    id = 1,
                    merchant = "Gaji",
                    amount = 5_000_000L,
                    type = TransactionType.INCOME,
                    category = "Gaji",
                    walletId = 1L,
                    note = "",
                    timestamp = dayMillis(1)
                ),
                walletName = "BCA",
                targetWalletName = null
            ),
            TransactionUiItem(
                entity = TransactionEntity(
                    id = 2,
                    merchant = "Belanja",
                    amount = 400_000L,
                    type = TransactionType.EXPENSE,
                    category = "Belanja",
                    walletId = 1L,
                    note = "",
                    timestamp = dayMillis(3)
                ),
                walletName = "BCA",
                targetWalletName = null
            ),
            TransactionUiItem(
                entity = TransactionEntity(
                    id = 3,
                    merchant = "Kopi",
                    amount = 50_000L,
                    type = TransactionType.EXPENSE,
                    category = "Kopi & Jajan",
                    walletId = 1L,
                    note = "",
                    timestamp = dayMillis(4)
                ),
                walletName = "GoPay",
                targetWalletName = null
            )
            // Nov 1 (income only), Nov 2 (empty), Nov 5 (empty), Nov 6 (today, empty) => no-spend days among Nov 1..6 = 4
            // Current streak ending Nov 6 = 2 days (Nov 5 & Nov 6); best streak = 2 days (Nov 1..2 and Nov 5..6)
        )
        val recurring = listOf(
            RecurringUiItem(
                rule = RecurringTransactionEntity(
                    id = 10L,
                    merchant = "Internet WiFi",
                    amount = 350_000L,
                    type = TransactionType.EXPENSE,
                    category = "Tagihan",
                    walletId = 1L,
                    frequency = "MONTHLY",
                    startDate = dayMillis(10, 9),
                    nextDueDate = dayMillis(10, 9),
                    isActive = true
                ),
                walletName = "BCA"
            )
        )

        val state = CashflowCalendarCalculator.buildMonth(
            year = 2026,
            month = Calendar.NOVEMBER,
            transactions = txs,
            recurringItems = recurring,
            nowMillis = now
        )

        assertEquals(6, state.leadingEmptySlots)
        assertEquals(30, state.days.size)
        assertEquals(5_000_000L, state.totalIncome)
        assertEquals(450_000L, state.totalExpense)
        assertEquals(4_550_000L, state.netCashflow)
        assertEquals(4, state.noSpendDaysCount)
        assertEquals(2, state.currentNoSpendStreak)
        assertEquals(2, state.bestNoSpendStreak)
        assertEquals(3, state.highestSpendDay?.dayOfMonth)
        assertEquals(3, state.days[2].expenseIntensity)
        assertEquals(1, state.days[9].dueRecurring.size)
        assertEquals(350_000L, state.upcomingRecurringTotal)
    }
}
