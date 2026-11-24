package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DanaKuDatabase
import com.example.data.local.DebtDirection
import com.example.data.local.DebtRecordEntity
import com.example.data.local.HomeShortcutOption
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.UserPreferences
import com.example.data.local.WalletEntity
import com.example.data.repository.FinanceRepository
import com.example.nfc.EmoneyCardSnapshot
import com.example.nfc.EmoneyIssuer
import com.example.nfc.EmoneyProtocolParser
import com.example.ui.viewmodel.RecurringUiItem
import com.example.ui.viewmodel.TransactionUiItem
import com.example.util.CashflowCalendarCalculator
import com.example.util.ReportExporter
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SakuNewFeaturesTest {

    private lateinit var context: Context
    private lateinit var db: DanaKuDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var prefs: UserPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DanaKuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db)
        prefs = UserPreferences(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun savingsGoal_depositAndWithdraw_updateGoalProgressAndWalletBalance() = runBlocking {
        repository.addWallet("BCA", "Bank", 5_000_000L, 0xFF1A1A1AL, "1234")
        val wallet = repository.allWallets.first().single()

        repository.upsertSavingsGoal(
            SavingsGoalEntity(
                title = "Dana Darurat",
                targetAmount = 2_000_000L,
                savedAmount = 500_000L
            )
        )
        val goal = repository.allSavingsGoals.first().single()
        assertFalse(goal.isCompleted)

        // Deposit 1.500.000 from BCA -> goal hits target 2.000.000, wallet drops to 3.500.000
        repository.contributeToSavingsGoal(goal, 1_500_000L, wallet.id, isWithdraw = false)
        val completedGoal = repository.allSavingsGoals.first().single()
        assertEquals(2_000_000L, completedGoal.savedAmount)
        assertTrue(completedGoal.isCompleted)
        assertEquals(3_500_000L, db.walletDao().getWalletById(wallet.id)?.balance)

        // Withdraw 250.000 back to BCA -> goal is 1.750.000 (not completed), wallet becomes 3.750.000
        repository.contributeToSavingsGoal(completedGoal, 250_000L, wallet.id, isWithdraw = true)
        val afterWithdraw = repository.allSavingsGoals.first().single()
        assertEquals(1_750_000L, afterWithdraw.savedAmount)
        assertFalse(afterWithdraw.isCompleted)
        assertEquals(3_750_000L, db.walletDao().getWalletById(wallet.id)?.balance)
    }

    @Test
    fun debtRecord_partialAndFullSettlement_updateWalletForLendAndBorrow() = runBlocking {
        repository.addWallet("GoPay", "E-Wallet", 1_000_000L, 0xFF00AA5BL, "0812")
        val wallet = repository.allWallets.first().single()

        // Piutang (LEND): teman pinjam 300rb, bayar 100rb -> saldo GoPay bertambah 100rb
        repository.upsertDebtRecord(
            DebtRecordEntity(
                personName = "Budi",
                amount = 300_000L,
                direction = DebtDirection.LEND,
                walletId = wallet.id
            )
        )
        val lend = repository.allDebts.first().single { it.direction == DebtDirection.LEND }
        repository.recordDebtPayment(lend, 100_000L, wallet.id)
        val lendAfterPartial = repository.allDebts.first().single { it.id == lend.id }
        assertEquals(100_000L, lendAfterPartial.paidAmount)
        assertFalse(lendAfterPartial.isSettled)
        assertEquals(1_100_000L, db.walletDao().getWalletById(wallet.id)?.balance)

        // Hutang (BORROW): pinjam 200rb, bayar lunas 200rb -> saldo GoPay berkurang 200rb
        repository.upsertDebtRecord(
            DebtRecordEntity(
                personName = "Rina",
                amount = 200_000L,
                direction = DebtDirection.BORROW,
                walletId = wallet.id
            )
        )
        val borrow = repository.allDebts.first().single { it.direction == DebtDirection.BORROW }
        repository.recordDebtPayment(borrow, 200_000L, wallet.id)
        val borrowSettled = repository.allDebts.first().single { it.id == borrow.id }
        assertEquals(200_000L, borrowSettled.paidAmount)
        assertTrue(borrowSettled.isSettled)
        assertEquals(900_000L, db.walletDao().getWalletById(wallet.id)?.balance)
    }

    @Test
    fun reportExporter_runningBalancesAndCsvFormulaSafety_areExact() {
        val walletBca = WalletEntity(
            id = 1L,
            name = "BCA",
            walletType = "Bank",
            balance = 1_450_000L,
            accentHex = 0xFF1A1A1AL,
            accountNumberHint = "1234"
        )
        val walletCash = WalletEntity(
            id = 2L,
            name = "Tunai",
            walletType = "Cash",
            balance = 200_000L,
            accentHex = 0xFF3F3F46L,
            accountNumberHint = "Dompet"
        )
        val t1 = 1_763_000_000_000L
        val t2 = t1 + 3_600_000L
        val t3 = t2 + 3_600_000L
        val tPost = t3 + 86_400_000L // after period end

        val txList = listOf(
            TransactionEntity(id = 1, merchant = "Gaji", amount = 1_000_000L, type = TransactionType.INCOME, category = "Gaji", walletId = 1L, note = "", timestamp = t1),
            TransactionEntity(id = 2, merchant = "=CMD|'/C calc'!A0", amount = 150_000L, type = TransactionType.EXPENSE, category = "Belanja", walletId = 1L, note = "+formula", timestamp = t2),
            TransactionEntity(id = 3, merchant = "Transfer Tunai", amount = 100_000L, type = TransactionType.TRANSFER, category = "Transfer", walletId = 1L, targetWalletId = 2L, note = "Tarik ATM", timestamp = t3),
            TransactionEntity(id = 4, merchant = "Kopi Setelah Periode", amount = 50_000L, type = TransactionType.EXPENSE, category = "Kopi & Jajan", walletId = 1L, note = "", timestamp = tPost)
        )

        // Scope: BCA only, period covers t1..t3 (excludes tPost)
        val report = ReportExporter.buildStatementReport(
            allTransactions = txList,
            wallets = listOf(walletBca, walletCash),
            selectedWalletId = 1L,
            periodStartMillis = t1,
            periodEndMillis = tPost,
            periodLabel = "Periode Uji"
        )

        // Current BCA balance = 1,450,000; tPost spent -50,000 -> Closing at tPost = 1,500,000
        assertEquals(1_500_000L, report.closingBalance)
        assertEquals(1_000_000L, report.totalCredit)
        assertEquals(250_000L, report.totalDebit) // 150k expense + 100k transfer out
        // Opening = 1,500,000 - (1,000,000 - 250,000) = 750,000
        assertEquals(750_000L, report.openingBalance)
        assertEquals(3, report.entries.size)
        assertEquals(1_750_000L, report.entries[0].runningBalance)
        assertEquals(1_600_000L, report.entries[1].runningBalance)
        assertEquals(1_500_000L, report.entries[2].runningBalance)
        assertEquals(report.closingBalance, report.entries.last().runningBalance)

        // CSV starts with UTF-8 BOM and neutralizes '=' and '+' formula cells
        val csv = ReportExporter.buildCsvContent(report)
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("\"'=CMD|'/C calc'!A0\""))
        assertTrue(csv.contains("\"'+formula\""))
    }

    @Test
    fun userPreferences_pinPbkdf2HashAndNotificationsAndOnboarding_workSafely() = runBlocking {
        prefs.setPinAndEnableLock("2580", biometricEnabled = true)
        val sec = prefs.securityPrefs.first()
        assertTrue(sec.appLockEnabled)
        assertTrue(sec.hasPin)
        assertTrue(sec.biometricEnabled)

        assertTrue(prefs.verifyPin("2580"))
        assertFalse(prefs.verifyPin("0000"))

        prefs.clearAppLock()
        val cleared = prefs.securityPrefs.first()
        assertFalse(cleared.appLockEnabled)
        assertFalse(cleared.hasPin)

        prefs.setDailyReminder(enabled = true, hour = 19, minute = 45)
        prefs.setBudgetAlertNotifEnabled(false)
        val notif = prefs.notificationPrefs.first()
        assertTrue(notif.dailyReminderEnabled)
        assertEquals(19, notif.reminderHour)
        assertEquals(45, notif.reminderMinute)
        assertFalse(notif.budgetAlertNotifEnabled)

        assertFalse(prefs.getHasCompletedOnboarding())
        prefs.setHasCompletedOnboarding(true)
        assertTrue(prefs.getHasCompletedOnboarding())

        // Reset workspace to 1 clean wallet
        repository.ensureSeeded()
        assertTrue(repository.walletCountOnce() >= 3)
        repository.resetAndCreateSingleWallet("Dompet Utama", "Bank", 1_250_000L, 0xFF1A1A1AL)
        assertEquals(1, repository.walletCountOnce())
        assertEquals(0, repository.allTransactions.first().size)
        assertEquals(1_250_000L, repository.allWallets.first().single().balance)
    }

    @Test
    fun homeShortcutOptions_includeSavingsAndDebts() {
        assertTrue(HomeShortcutOption.entries.contains(HomeShortcutOption.SAVINGS))
        assertTrue(HomeShortcutOption.entries.contains(HomeShortcutOption.DEBTS))
    }

    @Test
    fun userPreferences_biometricOnlyLockAndBackgroundRelock_workSafely() = runBlocking {
        prefs.enableBiometricLock()
        val bioOnly = prefs.securityPrefs.first()
        assertTrue(bioOnly.appLockEnabled)
        assertTrue(bioOnly.biometricEnabled)
        assertFalse(bioOnly.hasPin)
        assertTrue(bioOnly.lockOnBackground)

        prefs.setLockOnBackground(false)
        assertFalse(prefs.securityPrefs.first().lockOnBackground)

        prefs.setBiometricEnabled(false)
        val disabled = prefs.securityPrefs.first()
        assertFalse(disabled.biometricEnabled)
        assertFalse(disabled.appLockEnabled)
    }

    @Test
    fun emoneyProtocolParser_mandiriBniFelicaAndInvalidSw_parseAccurately() {
        // Mandiri 200,000 (0x00030D40) little-endian + trailing bytes + 90 00
        val mandiriBalResp = EmoneyProtocolParser.hexToBytes("400D03000000000000009000")
        assertEquals(200_000L, EmoneyProtocolParser.parseMandiriBalanceResponse(mandiriBalResp))

        // Short 6-byte Mandiri Le=04 fallback response (50,000 = 0x0000C350)
        val mandiriShortResp = EmoneyProtocolParser.hexToBytes("50C300009000")
        assertEquals(50_000L, EmoneyProtocolParser.parseMandiriBalanceResponse(mandiriShortResp))

        // Out-of-range corrupted APDU balance (> Rp 20jt) is safely rejected
        val corruptBalResp = EmoneyProtocolParser.hexToBytes("FFFFFF0F0000000000009000")
        assertNull(EmoneyProtocolParser.parseMandiriBalanceResponse(corruptBalResp))

        // Failed APDU SW (6A 82)
        val failedBalResp = EmoneyProtocolParser.hexToBytes("400D03000000000000006A82")
        assertNull(EmoneyProtocolParser.parseMandiriBalanceResponse(failedBalResp))

        // Mandiri ASCII card number: "6032981234567890" + 90 00 & privacy masking
        val asciiCardBytes = "6032981234567890".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x90.toByte(), 0x00)
        val parsedCardNo = EmoneyProtocolParser.parseMandiriCardNumberResponse(asciiCardBytes)
        assertEquals("6032 9812 3456 7890", parsedCardNo)
        assertEquals("6032 •••• •••• 7890", EmoneyProtocolParser.maskCardNumber(parsedCardNo!!))

        // BNI TapCash CEPAS 24-bit BE 125,000 (0x01E848) at [2..4] + 8-byte CAN at [8..15] + 90 00
        val bniResp = EmoneyProtocolParser.hexToBytes("000001E84800000075461234567890129000")
        val parsedBni = EmoneyProtocolParser.parseBniTapCashPurseResponse(bniResp)
        assertEquals(125_000L, parsedBni?.first)
        assertEquals("7546 1234 5678 9012", parsedBni?.second)

        // FeliCa Read Without Encryption (0x07): len=29, cmd=07, IDm(8B), status=00 00, blocks=01, block0 LE 50,000 (0x0000C350 -> 50 C3 00 00 ...)
        val felicaResp = EmoneyProtocolParser.hexToBytes(
            "1D" + "07" + "0102030405060708" + "0000" + "01" + "50C30000" + "000000000000000000000000"
        )
        assertEquals(50_000L, EmoneyProtocolParser.parseFelicaReadResponseBalance(felicaResp))
    }

    @Test
    fun userPreferences_emoneyScanHistory_tracksBalanceDeltaAndPersists() = runBlocking {
        assertTrue(HomeShortcutOption.entries.contains(HomeShortcutOption.NFC_EMONEY))

        val firstScan = prefs.saveEmoneyScan(
            EmoneyCardSnapshot(
                uidHex = "04AABBCC112233",
                issuer = EmoneyIssuer.MANDIRI_EMONEY,
                cardNumber = "6032 9812 3456 7890",
                balance = 150_000L,
                techLabel = "IsoDep • NfcA",
                scannedAtMillis = 1_000_000L
            )
        )
        assertNull(firstScan.previousBalance)
        assertNull(firstScan.balanceDelta)
        assertEquals("IsoDep • NfcA", firstScan.sourceLabel)

        // Second scan after paying toll Rp 18.000
        val secondScan = prefs.saveEmoneyScan(
            EmoneyCardSnapshot(
                uidHex = "04AABBCC112233",
                issuer = EmoneyIssuer.MANDIRI_EMONEY,
                cardNumber = "6032 9812 3456 7890",
                balance = 132_000L,
                techLabel = "IsoDep • NfcA",
                scannedAtMillis = 2_000_000L
            )
        )
        assertEquals(150_000L, secondScan.previousBalance)
        assertEquals(-18_000L, secondScan.balanceDelta)

        // Manual fallback update for encrypted card (e.g., BCA Flazz UID detected)
        val flazzFirst = prefs.saveEmoneyScan(
            EmoneyCardSnapshot(
                uidHex = "08112233445566",
                issuer = EmoneyIssuer.BCA_FLAZZ,
                cardNumber = "0811 2233 4455 66",
                balance = 100_000L,
                techLabel = "Input Manual",
                scannedAtMillis = 2_500_000L
            )
        )
        assertEquals("Input Manual", flazzFirst.sourceLabel)

        val flazzUpdated = prefs.saveEmoneyScan(
            EmoneyCardSnapshot(
                uidHex = "08112233445566",
                issuer = EmoneyIssuer.BCA_FLAZZ,
                cardNumber = "0811 2233 4455 66",
                balance = 75_000L,
                techLabel = "Input Manual",
                scannedAtMillis = 2_600_000L
            )
        )
        assertEquals(-25_000L, flazzUpdated.balanceDelta)

        // Repeated scan with identical balance preserves previousBalance delta reference
        val repeatedScan = prefs.saveEmoneyScan(
            EmoneyCardSnapshot(
                uidHex = "04AABBCC112233",
                issuer = EmoneyIssuer.MANDIRI_EMONEY,
                cardNumber = "6032 9812 3456 7890",
                balance = 132_000L,
                techLabel = "IsoDep • NfcA",
                scannedAtMillis = 3_000_000L
            )
        )
        assertEquals(150_000L, repeatedScan.previousBalance)

        // Top-up scan Rp 100.000
        val topUpScan = prefs.saveEmoneyScan(
            EmoneyCardSnapshot(
                uidHex = "04AABBCC112233",
                issuer = EmoneyIssuer.MANDIRI_EMONEY,
                cardNumber = "6032 9812 3456 7890",
                balance = 232_000L,
                techLabel = "IsoDep • NfcA",
                scannedAtMillis = 4_000_000L
            )
        )
        assertEquals(132_000L, topUpScan.previousBalance)
        assertEquals(100_000L, topUpScan.balanceDelta)
        assertEquals(2, prefs.emoneyHistory.first().size)

        prefs.deleteEmoneyRecord("04AABBCC112233")
        prefs.deleteEmoneyRecord("08112233445566")
        assertTrue(prefs.emoneyHistory.first().isEmpty())
    }

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
