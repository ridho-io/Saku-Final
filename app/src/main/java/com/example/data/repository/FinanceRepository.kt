package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.BudgetEntity
import com.example.data.local.DanaKuDatabase
import com.example.data.local.DebtDirection
import com.example.data.local.DebtRecordEntity
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.SyncCollectionNames
import com.example.data.local.SyncTombstoneEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.data.remote.BudgetSyncDoc
import com.example.data.remote.DebtSyncDoc
import com.example.data.remote.RecurringSyncDoc
import com.example.data.remote.SavingsGoalSyncDoc
import com.example.data.remote.TransactionSyncDoc
import com.example.data.remote.WalletSyncDoc
import com.example.util.DateFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.UUID

data class PendingSyncBatch(
    val wallets: List<WalletEntity>,
    val budgets: List<BudgetEntity>,
    val recurringRules: List<RecurringTransactionEntity>,
    val transactions: List<TransactionEntity>,
    val savingsGoals: List<SavingsGoalEntity>,
    val debts: List<DebtRecordEntity>,
    val tombstones: List<SyncTombstoneEntity>,
    val walletSyncIdByLocalId: Map<Long, String>,
    val recurringSyncIdByLocalId: Map<Long, String>
) {
    val isEmpty: Boolean
        get() = wallets.isEmpty() &&
            budgets.isEmpty() &&
            recurringRules.isEmpty() &&
            transactions.isEmpty() &&
            savingsGoals.isEmpty() &&
            debts.isEmpty() &&
            tombstones.isEmpty()
}

class FinanceRepository(private val db: DanaKuDatabase) {
    private val transactionDao = db.transactionDao()
    private val walletDao = db.walletDao()
    private val budgetDao = db.budgetDao()
    private val recurringDao = db.recurringTransactionDao()
    private val savingsGoalDao = db.savingsGoalDao()
    private val debtRecordDao = db.debtRecordDao()
    private val tombstoneDao = db.syncTombstoneDao()
    private val seedMutex = Mutex()
    private val recurringMutex = Mutex()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.observeAllTransactions()
    val allWallets: Flow<List<WalletEntity>> = walletDao.observeAllWallets()
    val allRecurringTransactions: Flow<List<RecurringTransactionEntity>> = recurringDao.observeAllRecurring()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.observeAll()
    val allDebts: Flow<List<DebtRecordEntity>> = debtRecordDao.observeAll()

    fun observePendingSyncCount(): Flow<Int> = combine(
        combine(
            walletDao.observeUnsyncedCount(),
            transactionDao.observeUnsyncedCount(),
            budgetDao.observeUnsyncedCount()
        ) { w, t, b -> w + t + b },
        combine(
            recurringDao.observeUnsyncedCount(),
            savingsGoalDao.observeUnsyncedCount(),
            debtRecordDao.observeUnsyncedCount(),
            tombstoneDao.observeCount()
        ) { r, s, d, tomb -> r + s + d + tomb }
    ) { a, b -> a + b }

    fun observeBudgets(monthKey: String = DateFormatter.currentMonthKey()): Flow<List<BudgetEntity>> =
        budgetDao.observeBudgetsForMonth(monthKey)

    suspend fun ensureSeeded() {
        seedMutex.withLock {
            if (walletDao.count() > 0) return

            db.withTransaction {
                if (walletDao.count() > 0) return@withTransaction

                val now = System.currentTimeMillis()
                val walletIds = walletDao.insertAll(
                    listOf(
                        WalletEntity(
                            name = "BCA",
                            walletType = "Bank",
                            balance = 14_850_000L,
                            accentHex = 0xFF1A1A1AL,
                            accountNumberHint = "•••• 4829",
                            updatedAt = now,
                            isSynced = false
                        ),
                        WalletEntity(
                            name = "SeaBank",
                            walletType = "Bank",
                            balance = 6_240_000L,
                            accentHex = 0xFFEA580CL,
                            accountNumberHint = "•••• 9012",
                            updatedAt = now,
                            isSynced = false
                        ),
                        WalletEntity(
                            name = "GoPay",
                            walletType = "E-Wallet",
                            balance = 845_000L,
                            accentHex = 0xFF10B981L,
                            accountNumberHint = "0812-••••-7741",
                            updatedAt = now,
                            isSynced = false
                        ),
                        WalletEntity(
                            name = "Tunai",
                            walletType = "Cash",
                            balance = 420_000L,
                            accentHex = 0xFF3F3F46L,
                            accountNumberHint = "Dompet Fisik",
                            updatedAt = now,
                            isSynced = false
                        )
                    )
                )

                val bcaId = walletIds[0]
                val seaBankId = walletIds[1]
                val gopayId = walletIds[2]
                val tunaiId = walletIds[3]

                fun timeAgo(daysAgo: Int, hour: Int, minute: Int): Long {
                    return Calendar.getInstance().apply {
                        timeInMillis = now
                        add(Calendar.DAY_OF_YEAR, -daysAgo)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                }

                val todayStart = DateFormatter.startOfDay(now)
                fun todayPastMillis(minutesAgo: Long): Long {
                    val candidate = now - minutesAgo * 60_000L
                    return candidate.coerceAtLeast(todayStart + 60_000L).coerceAtMost(now - 60_000L)
                }

                val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                fun safeDaysAgo(preferredDays: Int): Int = preferredDays.coerceAtMost((dayOfMonth - 1).coerceAtLeast(0))

                val sampleTransactions = listOf(
                    TransactionEntity(
                        merchant = "Kopi Kenangan",
                        amount = 28_000L,
                        type = TransactionType.EXPENSE,
                        category = "Kopi & Jajan",
                        walletId = gopayId,
                        note = "Kopi Kenangan Mantan Regular",
                        timestamp = todayPastMillis(25),
                        isAiGenerated = true,
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Warteg Bahari",
                        amount = 34_000L,
                        type = TransactionType.EXPENSE,
                        category = "Makanan & Minuman",
                        walletId = tunaiId,
                        note = "Makan siang nasi ayam + es teh",
                        timestamp = todayPastMillis(75),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "GoRide Kantor",
                        amount = 22_000L,
                        type = TransactionType.EXPENSE,
                        category = "Transportasi",
                        walletId = gopayId,
                        note = "Stasiun Sudirman ke Kantor",
                        timestamp = todayPastMillis(130),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Indomaret Point",
                        amount = 67_500L,
                        type = TransactionType.EXPENSE,
                        category = "Belanja",
                        walletId = seaBankId,
                        note = "Air mineral, roti, dan tisu",
                        timestamp = timeAgo(1, 19, 45),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Fore Coffee",
                        amount = 39_000L,
                        type = TransactionType.EXPENSE,
                        category = "Kopi & Jajan",
                        walletId = gopayId,
                        note = "Butterscotch Sea Salt Latte",
                        timestamp = timeAgo(1, 15, 10),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "RamenYA!",
                        amount = 118_000L,
                        type = TransactionType.EXPENSE,
                        category = "Makanan & Minuman",
                        walletId = bcaId,
                        note = "Dinner berdua + ocha",
                        timestamp = timeAgo(1, 20, 15),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Token Listrik PLN",
                        amount = 252_500L,
                        type = TransactionType.EXPENSE,
                        category = "Tagihan & Kos",
                        walletId = bcaId,
                        note = "Token listrik bulanan apartemen",
                        timestamp = timeAgo(safeDaysAgo(2), 10, 5),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Kos & Internet",
                        amount = 1_850_000L,
                        type = TransactionType.EXPENSE,
                        category = "Tagihan & Kos",
                        walletId = bcaId,
                        note = "Pembayaran sewa bulanan",
                        timestamp = timeAgo(safeDaysAgo(3), 11, 0),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Uniqlo Indonesia",
                        amount = 499_000L,
                        type = TransactionType.EXPENSE,
                        category = "Belanja",
                        walletId = seaBankId,
                        note = "Kemeja linen & kaus polos",
                        timestamp = timeAgo(safeDaysAgo(4), 16, 40),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Cinema XXI",
                        amount = 135_000L,
                        type = TransactionType.EXPENSE,
                        category = "Hiburan",
                        walletId = gopayId,
                        note = "Tiket nonton & caramel popcorn",
                        timestamp = timeAgo(safeDaysAgo(5), 19, 0),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Gaji Bulanan",
                        amount = 11_500_000L,
                        type = TransactionType.INCOME,
                        category = "Gaji",
                        walletId = bcaId,
                        note = "Payroll PT Teknologi Nusantara",
                        timestamp = timeAgo(safeDaysAgo(6), 9, 0),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Apotek Kimia Farma",
                        amount = 145_000L,
                        type = TransactionType.EXPENSE,
                        category = "Kesehatan",
                        walletId = seaBankId,
                        note = "Vitamin C & suplemen harian",
                        timestamp = timeAgo(safeDaysAgo(7), 14, 20),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Proyek Desain Freelance",
                        amount = 2_400_000L,
                        type = TransactionType.INCOME,
                        category = "Bonus & Investasi",
                        walletId = seaBankId,
                        note = "Pelunasan UI/UX Sprint",
                        timestamp = timeAgo(safeDaysAgo(8), 17, 30),
                        updatedAt = now,
                        isSynced = false
                    ),
                    TransactionEntity(
                        merchant = "Superindo",
                        amount = 385_000L,
                        type = TransactionType.EXPENSE,
                        category = "Makanan & Minuman",
                        walletId = bcaId,
                        note = "Belanja bahan masak mingguan",
                        timestamp = timeAgo(safeDaysAgo(9), 18, 15),
                        updatedAt = now,
                        isSynced = false
                    )
                )

                fun monthDayMillis(monthsBack: Int, day: Int, hour: Int): Long {
                    return Calendar.getInstance().apply {
                        timeInMillis = now
                        add(Calendar.MONTH, -monthsBack)
                        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, maxDay))
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis.coerceAtMost(now - 60_000L)
                }

                val multiMonthSeeds = listOf(
                    // Bulan -1
                    TransactionEntity(merchant = "Gaji Bulanan", amount = 11_500_000L, type = TransactionType.INCOME, category = "Gaji", walletId = bcaId, note = "Payroll bulanan", timestamp = monthDayMillis(1, 25, 9), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Kos & Internet", amount = 1_850_000L, type = TransactionType.EXPENSE, category = "Tagihan & Kos", walletId = bcaId, note = "Sewa bulanan", timestamp = monthDayMillis(1, 5, 10), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Superindo", amount = 1_420_000L, type = TransactionType.EXPENSE, category = "Makanan & Minuman", walletId = bcaId, note = "Kebutuhan dapur", timestamp = monthDayMillis(1, 12, 18), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "GoRide & Tol", amount = 640_000L, type = TransactionType.EXPENSE, category = "Transportasi", walletId = gopayId, note = "Komuter harian", timestamp = monthDayMillis(1, 18, 8), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Kopi Kenangan", amount = 310_000L, type = TransactionType.EXPENSE, category = "Kopi & Jajan", walletId = gopayId, note = "Ngopi kerja", timestamp = monthDayMillis(1, 20, 15), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Tokopedia", amount = 890_000L, type = TransactionType.EXPENSE, category = "Belanja", walletId = seaBankId, note = "Perlengkapan rumah", timestamp = monthDayMillis(1, 22, 19), updatedAt = now, isSynced = false),

                    // Bulan -2
                    TransactionEntity(merchant = "Gaji Bulanan", amount = 11_500_000L, type = TransactionType.INCOME, category = "Gaji", walletId = bcaId, note = "Payroll bulanan", timestamp = monthDayMillis(2, 25, 9), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Bonus Proyek Q3", amount = 1_650_000L, type = TransactionType.INCOME, category = "Bonus & Investasi", walletId = seaBankId, note = "Komisi desain", timestamp = monthDayMillis(2, 19, 16), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Kos & Internet", amount = 1_850_000L, type = TransactionType.EXPENSE, category = "Tagihan & Kos", walletId = bcaId, note = "Sewa bulanan", timestamp = monthDayMillis(2, 5, 10), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "RamenYA & Makan", amount = 1_680_000L, type = TransactionType.EXPENSE, category = "Makanan & Minuman", walletId = bcaId, note = "Makan bulanan", timestamp = monthDayMillis(2, 14, 19), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Shopee Mall", amount = 1_350_000L, type = TransactionType.EXPENSE, category = "Belanja", walletId = seaBankId, note = "Sepatu lari", timestamp = monthDayMillis(2, 16, 20), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Cinema XXI & Konser", amount = 780_000L, type = TransactionType.EXPENSE, category = "Hiburan", walletId = gopayId, note = "Akhir pekan", timestamp = monthDayMillis(2, 21, 19), updatedAt = now, isSynced = false),

                    // Bulan -3
                    TransactionEntity(merchant = "Gaji Bulanan", amount = 11_000_000L, type = TransactionType.INCOME, category = "Gaji", walletId = bcaId, note = "Payroll bulanan", timestamp = monthDayMillis(3, 25, 9), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Kos & Internet", amount = 1_850_000L, type = TransactionType.EXPENSE, category = "Tagihan & Kos", walletId = bcaId, note = "Sewa bulanan", timestamp = monthDayMillis(3, 4, 11), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Warteg & Groceries", amount = 1_510_000L, type = TransactionType.EXPENSE, category = "Makanan & Minuman", walletId = tunaiId, note = "Konsumsi harian", timestamp = monthDayMillis(3, 15, 13), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Apotek Kimia Farma", amount = 420_000L, type = TransactionType.EXPENSE, category = "Kesehatan", walletId = seaBankId, note = "Cek lab & vitamin", timestamp = monthDayMillis(3, 18, 10), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Pertamina & GoCar", amount = 710_000L, type = TransactionType.EXPENSE, category = "Transportasi", walletId = gopayId, note = "Transportasi", timestamp = monthDayMillis(3, 22, 17), updatedAt = now, isSynced = false),

                    // Bulan -4
                    TransactionEntity(merchant = "Gaji Bulanan", amount = 11_000_000L, type = TransactionType.INCOME, category = "Gaji", walletId = bcaId, note = "Payroll bulanan", timestamp = monthDayMillis(4, 25, 9), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Dividen Reksadana", amount = 950_000L, type = TransactionType.INCOME, category = "Bonus & Investasi", walletId = seaBankId, note = "Imbal hasil", timestamp = monthDayMillis(4, 28, 14), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Kos & Internet", amount = 1_850_000L, type = TransactionType.EXPENSE, category = "Tagihan & Kos", walletId = bcaId, note = "Sewa bulanan", timestamp = monthDayMillis(4, 5, 10), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Konsumsi & Makan", amount = 1_890_000L, type = TransactionType.EXPENSE, category = "Makanan & Minuman", walletId = bcaId, note = "Makan & groceries", timestamp = monthDayMillis(4, 14, 18), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Uniqlo & Mall", amount = 1_240_000L, type = TransactionType.EXPENSE, category = "Belanja", walletId = seaBankId, note = "Pakaian kerja", timestamp = monthDayMillis(4, 19, 17), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Fore Coffee & Jajan", amount = 430_000L, type = TransactionType.EXPENSE, category = "Kopi & Jajan", walletId = gopayId, note = "Kopi bulanan", timestamp = monthDayMillis(4, 23, 15), updatedAt = now, isSynced = false),

                    // Bulan -5
                    TransactionEntity(merchant = "Gaji Bulanan", amount = 11_000_000L, type = TransactionType.INCOME, category = "Gaji", walletId = bcaId, note = "Payroll bulanan", timestamp = monthDayMillis(5, 25, 9), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Kos & Internet", amount = 1_850_000L, type = TransactionType.EXPENSE, category = "Tagihan & Kos", walletId = bcaId, note = "Sewa bulanan", timestamp = monthDayMillis(5, 6, 10), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Superindo & Makan", amount = 1_380_000L, type = TransactionType.EXPENSE, category = "Makanan & Minuman", walletId = bcaId, note = "Belanja & makan", timestamp = monthDayMillis(5, 12, 19), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "KRL & GoRide", amount = 590_000L, type = TransactionType.EXPENSE, category = "Transportasi", walletId = gopayId, note = "Transportasi kerja", timestamp = monthDayMillis(5, 20, 8), updatedAt = now, isSynced = false),
                    TransactionEntity(merchant = "Netflix & Hiburan", amount = 460_000L, type = TransactionType.EXPENSE, category = "Hiburan", walletId = seaBankId, note = "Langganan & bioskop", timestamp = monthDayMillis(5, 24, 20), updatedAt = now, isSynced = false)
                )

                transactionDao.insertAll(sampleTransactions + multiMonthSeeds)

                val monthKey = DateFormatter.currentMonthKey(now)
                budgetDao.insertAll(
                    listOf(
                        BudgetEntity(category = "Makanan & Minuman", monthlyLimit = 700_000L, monthKey = monthKey, updatedAt = now, isSynced = false),
                        BudgetEntity(category = "Kopi & Jajan", monthlyLimit = 250_000L, monthKey = monthKey, updatedAt = now, isSynced = false),
                        BudgetEntity(category = "Transportasi", monthlyLimit = 400_000L, monthKey = monthKey, updatedAt = now, isSynced = false),
                        BudgetEntity(category = "Belanja", monthlyLimit = 600_000L, monthKey = monthKey, updatedAt = now, isSynced = false),
                        BudgetEntity(category = "Tagihan & Kos", monthlyLimit = 2_500_000L, monthKey = monthKey, updatedAt = now, isSynced = false),
                        BudgetEntity(category = "Hiburan", monthlyLimit = 300_000L, monthKey = monthKey, updatedAt = now, isSynced = false)
                    )
                )

                val nextMonthStart = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.MONTH, 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                recurringDao.insertAll(
                    listOf(
                        RecurringTransactionEntity(
                            merchant = "Kos & Internet",
                            amount = 1_850_000L,
                            type = TransactionType.EXPENSE,
                            category = "Tagihan & Kos",
                            walletId = bcaId,
                            note = "Pembayaran sewa bulanan otomatis",
                            frequency = RecurrenceFrequency.MONTHLY,
                            startDate = DateFormatter.noonOfDay(now),
                            endDate = null,
                            nextDueDate = nextMonthStart,
                            lastProcessedDate = DateFormatter.noonOfDay(now),
                            isActive = true,
                            updatedAt = now,
                            isSynced = false
                        ),
                        RecurringTransactionEntity(
                            merchant = "Gaji Bulanan",
                            amount = 11_500_000L,
                            type = TransactionType.INCOME,
                            category = "Gaji",
                            walletId = bcaId,
                            note = "Payroll PT Teknologi Nusantara",
                            frequency = RecurrenceFrequency.MONTHLY,
                            startDate = DateFormatter.noonOfDay(now),
                            endDate = null,
                            nextDueDate = nextMonthStart,
                            lastProcessedDate = DateFormatter.noonOfDay(now),
                            isActive = true,
                            updatedAt = now,
                            isSynced = false
                        )
                    )
                )

                savingsGoalDao.insertAll(
                    listOf(
                        SavingsGoalEntity(
                            title = "Dana Darurat",
                            targetAmount = 25_000_000L,
                            savedAmount = 14_850_000L,
                            deadlineMillis = now + 180L * 86_400_000L,
                            accentHex = 0xFFB5EE5AL,
                            iconName = "SHIELD",
                            note = "Target 6 bulan biaya hidup aman",
                            updatedAt = now,
                            isSynced = false
                        ),
                        SavingsGoalEntity(
                            title = "Liburan Akhir Tahun",
                            targetAmount = 8_000_000L,
                            savedAmount = 3_200_000L,
                            deadlineMillis = now + 90L * 86_400_000L,
                            accentHex = 0xFFAEB8FFL,
                            iconName = "FLIGHT",
                            note = "Tiket & penginapan Bali",
                            updatedAt = now,
                            isSynced = false
                        )
                    )
                )

                debtRecordDao.insertAll(
                    listOf(
                        DebtRecordEntity(
                            personName = "Rama Wijaya",
                            amount = 350_000L,
                            paidAmount = 100_000L,
                            direction = DebtDirection.LEND,
                            walletId = gopayId,
                            dueDateMillis = now + 5L * 86_400_000L,
                            note = "Talangan makan & tiket konser",
                            createdAtMillis = now - 3L * 86_400_000L,
                            updatedAt = now,
                            isSynced = false
                        ),
                        DebtRecordEntity(
                            personName = "Cicilan Laptop",
                            amount = 2_400_000L,
                            paidAmount = 1_200_000L,
                            direction = DebtDirection.BORROW,
                            walletId = bcaId,
                            dueDateMillis = now + 10L * 86_400_000L,
                            note = "Cicilan bulanan ke-2",
                            createdAtMillis = now - 30L * 86_400_000L,
                            updatedAt = now,
                            isSynced = false
                        )
                    )
                )
            }
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        val now = System.currentTimeMillis()
        val stamped = transaction.copy(
            syncId = transaction.syncId.ifBlank { UUID.randomUUID().toString() },
            updatedAt = now,
            isSynced = false
        )
        db.withTransaction {
            transactionDao.insert(stamped)
            applyBalanceEffect(stamped, multiplier = 1, updatedAt = now)
        }
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            transactions.forEach { tx ->
                val stamped = tx.copy(
                    syncId = tx.syncId.ifBlank { UUID.randomUUID().toString() },
                    updatedAt = now,
                    isSynced = false
                )
                transactionDao.insert(stamped)
                applyBalanceEffect(stamped, multiplier = 1, updatedAt = now)
            }
        }
    }

    suspend fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        val now = System.currentTimeMillis()
        val stamped = newTx.copy(
            syncId = oldTx.syncId.ifBlank { UUID.randomUUID().toString() },
            updatedAt = now,
            isSynced = false,
            cloudCreatedAtSeconds = oldTx.cloudCreatedAtSeconds,
            cloudCreatedAtNanos = oldTx.cloudCreatedAtNanos
        )
        db.withTransaction {
            applyBalanceEffect(oldTx, multiplier = -1, updatedAt = now)
            transactionDao.update(stamped)
            applyBalanceEffect(stamped, multiplier = 1, updatedAt = now)
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            transactionDao.delete(transaction)
            applyBalanceEffect(transaction, multiplier = -1, updatedAt = now)
            if (transaction.syncId.isNotBlank()) {
                tombstoneDao.insert(
                    SyncTombstoneEntity(
                        collectionName = SyncCollectionNames.TRANSACTIONS,
                        syncId = transaction.syncId,
                        deletedAt = now
                    )
                )
            }
        }
    }

    suspend fun transferBetweenWallets(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Long,
        note: String
    ) {
        if (fromWalletId == toWalletId || amount <= 0L) return
        val now = System.currentTimeMillis()
        db.withTransaction {
            val fromWallet = walletDao.getWalletById(fromWalletId)
            val toWallet = walletDao.getWalletById(toWalletId)
            val merchantLabel = "Transfer ${fromWallet?.name.orEmpty()} → ${toWallet?.name.orEmpty()}"
            val tx = TransactionEntity(
                merchant = merchantLabel,
                amount = amount,
                type = TransactionType.TRANSFER,
                category = "Transfer",
                walletId = fromWalletId,
                targetWalletId = toWalletId,
                note = note.ifBlank { "Pemindahan saldo antar dompet" },
                timestamp = now,
                updatedAt = now,
                isSynced = false
            )
            transactionDao.insert(tx)
            applyBalanceEffect(tx, multiplier = 1, updatedAt = now)
        }
    }

    suspend fun upsertBudget(category: String, monthlyLimit: Long, monthKey: String = DateFormatter.currentMonthKey()) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val existing = budgetDao.getBudgetByCategoryAndMonth(category, monthKey)
            if (existing != null) {
                budgetDao.update(
                    existing.copy(
                        monthlyLimit = monthlyLimit,
                        updatedAt = now,
                        isSynced = false
                    )
                )
            } else {
                budgetDao.insert(
                    BudgetEntity(
                        category = category,
                        monthlyLimit = monthlyLimit,
                        monthKey = monthKey,
                        updatedAt = now,
                        isSynced = false
                    )
                )
            }
        }
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            budgetDao.delete(budget)
            if (budget.syncId.isNotBlank()) {
                tombstoneDao.insert(
                    SyncTombstoneEntity(
                        collectionName = SyncCollectionNames.BUDGETS,
                        syncId = budget.syncId,
                        deletedAt = now
                    )
                )
            }
        }
    }

    suspend fun addWallet(name: String, walletType: String, initialBalance: Long, accentHex: Long, hint: String) {
        val now = System.currentTimeMillis()
        walletDao.insert(
            WalletEntity(
                name = name.trim(),
                walletType = walletType,
                balance = initialBalance,
                accentHex = accentHex,
                accountNumberHint = hint.ifBlank { walletType },
                updatedAt = now,
                isSynced = false
            )
        )
    }

    suspend fun upsertRecurringRule(rule: RecurringTransactionEntity, nowMillis: Long = System.currentTimeMillis()): Int {
        val now = System.currentTimeMillis()
        val stamped = rule.copy(
            syncId = rule.syncId.ifBlank { UUID.randomUUID().toString() },
            updatedAt = now,
            isSynced = false
        )
        if (stamped.id == 0L) {
            recurringDao.insert(stamped)
        } else {
            recurringDao.update(stamped)
        }
        return processDueRecurringTransactions(nowMillis)
    }

    suspend fun toggleRecurringActive(rule: RecurringTransactionEntity, nowMillis: Long = System.currentTimeMillis()): Int {
        val now = System.currentTimeMillis()
        val updated = rule.copy(
            isActive = !rule.isActive,
            updatedAt = now,
            isSynced = false
        )
        recurringDao.update(updated)
        return if (updated.isActive) processDueRecurringTransactions(nowMillis) else 0
    }

    suspend fun deleteRecurringRule(rule: RecurringTransactionEntity) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            recurringDao.delete(rule)
            if (rule.syncId.isNotBlank()) {
                tombstoneDao.insert(
                    SyncTombstoneEntity(
                        collectionName = SyncCollectionNames.RECURRING,
                        syncId = rule.syncId,
                        deletedAt = now
                    )
                )
            }
        }
    }

    suspend fun processDueRecurringTransactions(nowMillis: Long = System.currentTimeMillis()): Int {
        return recurringMutex.withLock {
            var createdCount = 0
            val todayEnd = DateFormatter.endOfDay(nowMillis)
            val writeTime = System.currentTimeMillis()

            db.withTransaction {
                val activeRules = recurringDao.getActiveRecurringOnce()
                for (rule in activeRules) {
                    val endLimit = rule.endDate?.let { DateFormatter.endOfDay(it) }
                    var nextDue = rule.nextDueDate
                    var lastProcessed = rule.lastProcessedDate
                    var stillActive = rule.isActive
                    var guard = 0

                    while (stillActive && nextDue <= todayEnd && (endLimit == null || nextDue <= endLimit) && guard < 366) {
                        val generatedTx = TransactionEntity(
                            merchant = rule.merchant,
                            amount = rule.amount,
                            type = rule.type,
                            category = rule.category,
                            walletId = rule.walletId,
                            note = rule.note.ifBlank { "Transaksi berulang (${RecurrenceFrequency.label(rule.frequency)})" },
                            timestamp = nextDue,
                            isAiGenerated = false,
                            recurringRuleId = rule.id,
                            updatedAt = writeTime,
                            isSynced = false
                        )
                        transactionDao.insert(generatedTx)
                        applyBalanceEffect(generatedTx, multiplier = 1, updatedAt = writeTime)
                        lastProcessed = nextDue
                        nextDue = DateFormatter.advanceByFrequency(nextDue, rule.frequency)
                        createdCount++
                        guard++

                        if (endLimit != null && nextDue > endLimit) {
                            stillActive = false
                        }
                    }

                    if (endLimit != null && nextDue > endLimit && stillActive) {
                        stillActive = false
                    }

                    if (nextDue != rule.nextDueDate || lastProcessed != rule.lastProcessedDate || stillActive != rule.isActive) {
                        recurringDao.update(
                            rule.copy(
                                nextDueDate = nextDue,
                                lastProcessedDate = lastProcessed,
                                isActive = stillActive,
                                updatedAt = writeTime,
                                isSynced = false
                            )
                        )
                    }
                }
            }
            createdCount
        }
    }

    suspend fun updateWallet(wallet: WalletEntity) {
        val now = System.currentTimeMillis()
        walletDao.update(
            wallet.copy(
                updatedAt = now,
                isSynced = false
            )
        )
    }

    suspend fun countTransactionsForWallet(walletId: Long): Int =
        transactionDao.countByWallet(walletId)

    suspend fun deleteWalletSafely(wallet: WalletEntity): Boolean {
        if (walletDao.count() <= 1) return false
        val now = System.currentTimeMillis()
        db.withTransaction {
            if (walletDao.count() <= 1) return@withTransaction
            val relatedTx = transactionDao.getByWallet(wallet.id)
            relatedTx.forEach { tx ->
                if (tx.syncId.isNotBlank()) {
                    tombstoneDao.insert(
                        SyncTombstoneEntity(
                            collectionName = SyncCollectionNames.TRANSACTIONS,
                            syncId = tx.syncId,
                            deletedAt = now
                        )
                    )
                }
            }
            val relatedRules = recurringDao.getByWallet(wallet.id)
            relatedRules.forEach { rule ->
                if (rule.syncId.isNotBlank()) {
                    tombstoneDao.insert(
                        SyncTombstoneEntity(
                            collectionName = SyncCollectionNames.RECURRING,
                            syncId = rule.syncId,
                            deletedAt = now
                        )
                    )
                }
            }
            transactionDao.deleteByWallet(wallet.id)
            recurringDao.deleteByWallet(wallet.id)
            walletDao.delete(wallet)
            if (wallet.syncId.isNotBlank()) {
                tombstoneDao.insert(
                    SyncTombstoneEntity(
                        collectionName = SyncCollectionNames.WALLETS,
                        syncId = wallet.syncId,
                        deletedAt = now
                    )
                )
            }
        }
        return true
    }

    suspend fun resolveWalletByName(name: String): WalletEntity? {
        val all = walletDao.getAllWalletsOnce()
        return all.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            ?: all.firstOrNull { it.name.contains(name.trim(), ignoreCase = true) }
            ?: all.firstOrNull()
    }

    // --- Savings Goals ---

    suspend fun upsertSavingsGoal(goal: SavingsGoalEntity) {
        val now = System.currentTimeMillis()
        val stamped = goal.copy(
            syncId = goal.syncId.ifBlank { UUID.randomUUID().toString() },
            isCompleted = goal.savedAmount >= goal.targetAmount && goal.targetAmount > 0L,
            updatedAt = now,
            isSynced = false
        )
        if (stamped.id == 0L) {
            savingsGoalDao.insert(stamped)
        } else {
            savingsGoalDao.update(stamped)
        }
    }

    suspend fun contributeToSavingsGoal(goal: SavingsGoalEntity, amount: Long, sourceWalletId: Long?, isWithdraw: Boolean = false) {
        if (amount <= 0L) return
        val now = System.currentTimeMillis()
        db.withTransaction {
            val delta = if (isWithdraw) -amount else amount
            val newSaved = (goal.savedAmount + delta).coerceAtLeast(0L)
            val completed = newSaved >= goal.targetAmount && goal.targetAmount > 0L
            savingsGoalDao.update(
                goal.copy(
                    savedAmount = newSaved,
                    isCompleted = completed,
                    updatedAt = now,
                    isSynced = false
                )
            )
            if (sourceWalletId != null) {
                val tx = TransactionEntity(
                    merchant = if (isWithdraw) "Tarik Target: ${goal.title}" else "Tabung: ${goal.title}",
                    amount = amount,
                    type = if (isWithdraw) TransactionType.INCOME else TransactionType.EXPENSE,
                    category = if (isWithdraw) "Bonus & Investasi" else "Tagihan & Kos",
                    walletId = sourceWalletId,
                    note = if (isWithdraw) "Penarikan dari target tabungan" else "Alokasi dana ke target tabungan",
                    timestamp = now,
                    updatedAt = now,
                    isSynced = false
                )
                transactionDao.insert(tx)
                applyBalanceEffect(tx, multiplier = 1, updatedAt = now)
            }
        }
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            savingsGoalDao.delete(goal)
            if (goal.syncId.isNotBlank()) {
                tombstoneDao.insert(
                    SyncTombstoneEntity(
                        collectionName = SyncCollectionNames.SAVINGS_GOALS,
                        syncId = goal.syncId,
                        deletedAt = now
                    )
                )
            }
        }
    }

    // --- Debts & Receivables ---

    suspend fun upsertDebtRecord(debt: DebtRecordEntity) {
        val now = System.currentTimeMillis()
        val settled = debt.paidAmount >= debt.amount && debt.amount > 0L
        val stamped = debt.copy(
            syncId = debt.syncId.ifBlank { UUID.randomUUID().toString() },
            isSettled = settled,
            updatedAt = now,
            isSynced = false
        )
        if (stamped.id == 0L) {
            debtRecordDao.insert(stamped)
        } else {
            debtRecordDao.update(stamped)
        }
    }

    suspend fun recordDebtPayment(debt: DebtRecordEntity, paymentAmount: Long, walletId: Long?) {
        if (paymentAmount <= 0L) return
        val now = System.currentTimeMillis()
        db.withTransaction {
            val newPaid = (debt.paidAmount + paymentAmount).coerceAtMost(debt.amount)
            val settled = newPaid >= debt.amount
            debtRecordDao.update(
                debt.copy(
                    paidAmount = newPaid,
                    isSettled = settled,
                    walletId = walletId ?: debt.walletId,
                    updatedAt = now,
                    isSynced = false
                )
            )
            if (walletId != null) {
                val isPiutang = debt.direction == DebtDirection.LEND
                val tx = TransactionEntity(
                    merchant = if (isPiutang) "Pelunasan dari ${debt.personName}" else "Bayar ke ${debt.personName}",
                    amount = paymentAmount,
                    type = if (isPiutang) TransactionType.INCOME else TransactionType.EXPENSE,
                    category = if (isPiutang) "Bonus & Investasi" else "Lainnya",
                    walletId = walletId,
                    note = if (isPiutang) "Cicilan/pelunasan piutang" else "Cicilan/pelunasan hutang",
                    timestamp = now,
                    updatedAt = now,
                    isSynced = false
                )
                transactionDao.insert(tx)
                applyBalanceEffect(tx, multiplier = 1, updatedAt = now)
            }
        }
    }

    suspend fun deleteDebtRecord(debt: DebtRecordEntity) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            debtRecordDao.delete(debt)
            if (debt.syncId.isNotBlank()) {
                tombstoneDao.insert(
                    SyncTombstoneEntity(
                        collectionName = SyncCollectionNames.DEBTS,
                        syncId = debt.syncId,
                        deletedAt = now
                    )
                )
            }
        }
    }

    // --- Onboarding / Empty Workspace ---

    suspend fun walletCountOnce(): Int = walletDao.count()

    suspend fun resetAndCreateSingleWallet(
        walletName: String,
        walletType: String,
        initialBalance: Long,
        accentHex: Long
    ) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            // Queue tombstones for any previously synced items before wiping local data
            transactionDao.getAllTransactionsOnce().filter { it.isSynced && it.syncId.isNotBlank() }.forEach {
                tombstoneDao.insert(SyncTombstoneEntity(collectionName = SyncCollectionNames.TRANSACTIONS, syncId = it.syncId, deletedAt = now))
            }
            recurringDao.getAllOnce().filter { it.isSynced && it.syncId.isNotBlank() }.forEach {
                tombstoneDao.insert(SyncTombstoneEntity(collectionName = SyncCollectionNames.RECURRING, syncId = it.syncId, deletedAt = now))
            }
            budgetDao.getAllBudgetsOnce().filter { it.isSynced && it.syncId.isNotBlank() }.forEach {
                tombstoneDao.insert(SyncTombstoneEntity(collectionName = SyncCollectionNames.BUDGETS, syncId = it.syncId, deletedAt = now))
            }
            savingsGoalDao.getAllOnce().filter { it.isSynced && it.syncId.isNotBlank() }.forEach {
                tombstoneDao.insert(SyncTombstoneEntity(collectionName = SyncCollectionNames.SAVINGS_GOALS, syncId = it.syncId, deletedAt = now))
            }
            debtRecordDao.getAllOnce().filter { it.isSynced && it.syncId.isNotBlank() }.forEach {
                tombstoneDao.insert(SyncTombstoneEntity(collectionName = SyncCollectionNames.DEBTS, syncId = it.syncId, deletedAt = now))
            }
            walletDao.getAllWalletsOnce().filter { it.isSynced && it.syncId.isNotBlank() }.forEach {
                tombstoneDao.insert(SyncTombstoneEntity(collectionName = SyncCollectionNames.WALLETS, syncId = it.syncId, deletedAt = now))
            }

            transactionDao.getAllTransactionsOnce().forEach { transactionDao.delete(it) }
            recurringDao.getAllOnce().forEach { recurringDao.delete(it) }
            budgetDao.getAllBudgetsOnce().forEach { budgetDao.delete(it) }
            savingsGoalDao.getAllOnce().forEach { savingsGoalDao.delete(it) }
            debtRecordDao.getAllOnce().forEach { debtRecordDao.delete(it) }
            walletDao.getAllWalletsOnce().forEach { walletDao.delete(it) }

            walletDao.insert(
                WalletEntity(
                    name = walletName.trim().ifBlank { "Dompet Utama" },
                    walletType = walletType,
                    balance = initialBalance.coerceAtLeast(0L),
                    accentHex = accentHex,
                    accountNumberHint = walletType,
                    updatedAt = now,
                    isSynced = false
                )
            )
        }
    }

    // --- Cloud Sync Bridge Methods ---

    suspend fun getPendingPushBatch(): PendingSyncBatch {
        val allWallets = walletDao.getAllWalletsOnce()
        val allRules = recurringDao.getAllOnce()
        val walletSyncIdByLocalId = allWallets.associate { it.id to it.syncId }
        val recurringSyncIdByLocalId = allRules.associate { it.id to it.syncId }

        return PendingSyncBatch(
            wallets = walletDao.getUnsynced(),
            budgets = budgetDao.getUnsynced(),
            recurringRules = allRules.filter { !it.isSynced },
            transactions = transactionDao.getUnsynced(),
            savingsGoals = savingsGoalDao.getUnsynced(),
            debts = debtRecordDao.getUnsynced(),
            tombstones = tombstoneDao.getAllOnce(),
            walletSyncIdByLocalId = walletSyncIdByLocalId,
            recurringSyncIdByLocalId = recurringSyncIdByLocalId
        )
    }

    suspend fun markWalletSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?) {
        walletDao.markSynced(id, updatedAt, createdSec, createdNano)
    }

    suspend fun markBudgetSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?) {
        budgetDao.markSynced(id, updatedAt, createdSec, createdNano)
    }

    suspend fun markRecurringSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?) {
        recurringDao.markSynced(id, updatedAt, createdSec, createdNano)
    }

    suspend fun markTransactionSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?) {
        transactionDao.markSynced(id, updatedAt, createdSec, createdNano)
    }

    suspend fun markSavingsGoalSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?) {
        savingsGoalDao.markSynced(id, updatedAt, createdSec, createdNano)
    }

    suspend fun markDebtSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?) {
        debtRecordDao.markSynced(id, updatedAt, createdSec, createdNano)
    }

    suspend fun deleteTombstone(tombstone: SyncTombstoneEntity) {
        tombstoneDao.delete(tombstone)
    }

    suspend fun clearAllTombstones() {
        tombstoneDao.clearAll()
    }

    suspend fun markAllLocalUnsynced() {
        db.withTransaction {
            walletDao.markAllUnsynced()
            budgetDao.markAllUnsynced()
            recurringDao.markAllUnsynced()
            transactionDao.markAllUnsynced()
            savingsGoalDao.markAllUnsynced()
            debtRecordDao.markAllUnsynced()
        }
    }

    /**
     * Replaces untouched initial sample data with remote cloud data when signing into an
     * existing cloud account on a fresh device so sample transactions aren't duplicated.
     */
    suspend fun clearUntouchedLocalDataForInitialCloudRestore() {
        db.withTransaction {
            transactionDao.getAllTransactionsOnce().forEach { transactionDao.delete(it) }
            recurringDao.getAllOnce().forEach { recurringDao.delete(it) }
            budgetDao.getAllBudgetsOnce().forEach { budgetDao.delete(it) }
            savingsGoalDao.getAllOnce().forEach { savingsGoalDao.delete(it) }
            debtRecordDao.getAllOnce().forEach { debtRecordDao.delete(it) }
            walletDao.getAllWalletsOnce().forEach { walletDao.delete(it) }
            tombstoneDao.clearAll()
        }
    }

    suspend fun applyRemoteWallets(docs: List<WalletSyncDoc>, pruneMissingSynced: Boolean = true) {
        val remoteIds = docs.map { it.id }.toSet()
        db.withTransaction {
            val pendingTombstones = tombstoneDao.getAllOnce()
                .filter { it.collectionName == SyncCollectionNames.WALLETS }
                .map { it.syncId }
                .toSet()

            for (doc in docs) {
                if (doc.id.isBlank() || doc.id in pendingTombstones) continue
                val local = walletDao.getBySyncId(doc.id)
                val sec = doc.createdAt?.seconds
                val nano = doc.createdAt?.nanoseconds
                if (local == null) {
                    walletDao.insert(
                        WalletEntity(
                            name = doc.name,
                            walletType = doc.walletType,
                            balance = doc.balance,
                            accentHex = doc.accentHex,
                            accountNumberHint = doc.accountNumberHint,
                            syncId = doc.id,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec,
                            cloudCreatedAtNanos = nano
                        )
                    )
                } else if (doc.clientUpdatedAt > local.updatedAt || (local.isSynced && doc.clientUpdatedAt != local.updatedAt)) {
                    walletDao.update(
                        local.copy(
                            name = doc.name,
                            walletType = doc.walletType,
                            balance = doc.balance,
                            accentHex = doc.accentHex,
                            accountNumberHint = doc.accountNumberHint,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec ?: local.cloudCreatedAtSeconds,
                            cloudCreatedAtNanos = nano ?: local.cloudCreatedAtNanos
                        )
                    )
                } else if (local.isSynced && local.cloudCreatedAtSeconds == null && sec != null) {
                    walletDao.markSynced(local.id, local.updatedAt, sec, nano)
                }
            }

            if (pruneMissingSynced) {
                val allLocal = walletDao.getAllWalletsOnce()
                val remainingCount = allLocal.count { !it.isSynced || it.syncId in remoteIds }
                if (remainingCount >= 1) {
                    for (local in allLocal) {
                        if (local.isSynced && local.syncId !in remoteIds) {
                            transactionDao.deleteByWallet(local.id)
                            recurringDao.deleteByWallet(local.id)
                            walletDao.delete(local)
                        }
                    }
                }
            }
        }
    }

    suspend fun applyRemoteBudgets(docs: List<BudgetSyncDoc>, pruneMissingSynced: Boolean = true) {
        val remoteIds = docs.map { it.id }.toSet()
        db.withTransaction {
            val pendingTombstones = tombstoneDao.getAllOnce()
                .filter { it.collectionName == SyncCollectionNames.BUDGETS }
                .map { it.syncId }
                .toSet()

            for (doc in docs) {
                if (doc.id.isBlank() || doc.id in pendingTombstones) continue
                val local = budgetDao.getBySyncId(doc.id)
                    ?: budgetDao.getBudgetByCategoryAndMonth(doc.category, doc.monthKey)
                val sec = doc.createdAt?.seconds
                val nano = doc.createdAt?.nanoseconds
                if (local == null) {
                    budgetDao.insert(
                        BudgetEntity(
                            category = doc.category,
                            monthlyLimit = doc.monthlyLimit,
                            monthKey = doc.monthKey,
                            syncId = doc.id,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec,
                            cloudCreatedAtNanos = nano
                        )
                    )
                } else if (doc.clientUpdatedAt > local.updatedAt || (local.isSynced && doc.clientUpdatedAt != local.updatedAt)) {
                    budgetDao.update(
                        local.copy(
                            category = doc.category,
                            monthlyLimit = doc.monthlyLimit,
                            monthKey = doc.monthKey,
                            syncId = doc.id,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec ?: local.cloudCreatedAtSeconds,
                            cloudCreatedAtNanos = nano ?: local.cloudCreatedAtNanos
                        )
                    )
                }
            }

            if (pruneMissingSynced) {
                for (local in budgetDao.getAllBudgetsOnce()) {
                    if (local.isSynced && local.syncId !in remoteIds) {
                        budgetDao.delete(local)
                    }
                }
            }
        }
    }

    suspend fun applyRemoteRecurring(docs: List<RecurringSyncDoc>, pruneMissingSynced: Boolean = true) {
        val remoteIds = docs.map { it.id }.toSet()
        db.withTransaction {
            val pendingTombstones = tombstoneDao.getAllOnce()
                .filter { it.collectionName == SyncCollectionNames.RECURRING }
                .map { it.syncId }
                .toSet()

            for (doc in docs) {
                if (doc.id.isBlank() || doc.id in pendingTombstones) continue
                val wallet = walletDao.getBySyncId(doc.walletSyncId) ?: continue
                val local = recurringDao.getBySyncId(doc.id)
                val sec = doc.createdAt?.seconds
                val nano = doc.createdAt?.nanoseconds
                if (local == null) {
                    recurringDao.insert(
                        RecurringTransactionEntity(
                            merchant = doc.merchant,
                            amount = doc.amount,
                            type = doc.type,
                            category = doc.category,
                            walletId = wallet.id,
                            note = doc.note,
                            frequency = doc.frequency,
                            startDate = doc.startDate,
                            endDate = doc.endDate,
                            nextDueDate = doc.nextDueDate,
                            lastProcessedDate = doc.lastProcessedDate,
                            isActive = doc.isActive,
                            syncId = doc.id,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec,
                            cloudCreatedAtNanos = nano
                        )
                    )
                } else if (doc.clientUpdatedAt > local.updatedAt || (local.isSynced && doc.clientUpdatedAt != local.updatedAt)) {
                    recurringDao.update(
                        local.copy(
                            merchant = doc.merchant,
                            amount = doc.amount,
                            type = doc.type,
                            category = doc.category,
                            walletId = wallet.id,
                            note = doc.note,
                            frequency = doc.frequency,
                            startDate = doc.startDate,
                            endDate = doc.endDate,
                            nextDueDate = doc.nextDueDate,
                            lastProcessedDate = doc.lastProcessedDate,
                            isActive = doc.isActive,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec ?: local.cloudCreatedAtSeconds,
                            cloudCreatedAtNanos = nano ?: local.cloudCreatedAtNanos
                        )
                    )
                }
            }

            if (pruneMissingSynced) {
                for (local in recurringDao.getAllOnce()) {
                    if (local.isSynced && local.syncId !in remoteIds) {
                        recurringDao.delete(local)
                    }
                }
            }
        }
    }

    suspend fun applyRemoteTransactions(docs: List<TransactionSyncDoc>, pruneMissingSynced: Boolean = true) {
        val remoteIds = docs.map { it.id }.toSet()
        db.withTransaction {
            val pendingTombstones = tombstoneDao.getAllOnce()
                .filter { it.collectionName == SyncCollectionNames.TRANSACTIONS }
                .map { it.syncId }
                .toSet()

            for (doc in docs) {
                if (doc.id.isBlank() || doc.id in pendingTombstones) continue
                val wallet = walletDao.getBySyncId(doc.walletSyncId) ?: continue
                val targetWalletId = doc.targetWalletSyncId?.let { walletDao.getBySyncId(it)?.id }
                val recurringRuleId = doc.recurringRuleSyncId?.let { recurringDao.getBySyncId(it)?.id }
                val local = transactionDao.getBySyncId(doc.id)
                val sec = doc.createdAt?.seconds
                val nano = doc.createdAt?.nanoseconds
                if (local == null) {
                    transactionDao.insert(
                        TransactionEntity(
                            merchant = doc.merchant,
                            amount = doc.amount,
                            type = doc.type,
                            category = doc.category,
                            walletId = wallet.id,
                            targetWalletId = targetWalletId,
                            note = doc.note,
                            timestamp = doc.timestamp,
                            isAiGenerated = doc.isAiGenerated,
                            recurringRuleId = recurringRuleId,
                            syncId = doc.id,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec,
                            cloudCreatedAtNanos = nano
                        )
                    )
                } else if (doc.clientUpdatedAt > local.updatedAt || (local.isSynced && doc.clientUpdatedAt != local.updatedAt)) {
                    transactionDao.update(
                        local.copy(
                            merchant = doc.merchant,
                            amount = doc.amount,
                            type = doc.type,
                            category = doc.category,
                            walletId = wallet.id,
                            targetWalletId = targetWalletId,
                            note = doc.note,
                            timestamp = doc.timestamp,
                            isAiGenerated = doc.isAiGenerated,
                            recurringRuleId = recurringRuleId,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec ?: local.cloudCreatedAtSeconds,
                            cloudCreatedAtNanos = nano ?: local.cloudCreatedAtNanos
                        )
                    )
                }
            }

            if (pruneMissingSynced) {
                for (local in transactionDao.getAllTransactionsOnce()) {
                    if (local.isSynced && local.syncId !in remoteIds) {
                        transactionDao.delete(local)
                    }
                }
            }
        }
    }

    suspend fun applyRemoteSavingsGoals(docs: List<SavingsGoalSyncDoc>, pruneMissingSynced: Boolean = true) {
        val remoteIds = docs.map { it.id }.toSet()
        db.withTransaction {
            val pendingTombstones = tombstoneDao.getAllOnce()
                .filter { it.collectionName == SyncCollectionNames.SAVINGS_GOALS }
                .map { it.syncId }
                .toSet()

            for (doc in docs) {
                if (doc.id.isBlank() || doc.id in pendingTombstones) continue
                val local = savingsGoalDao.getBySyncId(doc.id)
                val sec = doc.createdAt?.seconds
                val nano = doc.createdAt?.nanoseconds
                if (local == null) {
                    savingsGoalDao.insert(
                        SavingsGoalEntity(
                            title = doc.title,
                            targetAmount = doc.targetAmount,
                            savedAmount = doc.savedAmount,
                            deadlineMillis = doc.deadlineMillis,
                            accentHex = doc.accentHex,
                            iconName = doc.iconName,
                            note = doc.note,
                            isCompleted = doc.completed,
                            syncId = doc.id,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec,
                            cloudCreatedAtNanos = nano
                        )
                    )
                } else if (doc.clientUpdatedAt > local.updatedAt || (local.isSynced && doc.clientUpdatedAt != local.updatedAt)) {
                    savingsGoalDao.update(
                        local.copy(
                            title = doc.title,
                            targetAmount = doc.targetAmount,
                            savedAmount = doc.savedAmount,
                            deadlineMillis = doc.deadlineMillis,
                            accentHex = doc.accentHex,
                            iconName = doc.iconName,
                            note = doc.note,
                            isCompleted = doc.completed,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec ?: local.cloudCreatedAtSeconds,
                            cloudCreatedAtNanos = nano ?: local.cloudCreatedAtNanos
                        )
                    )
                }
            }

            if (pruneMissingSynced) {
                for (local in savingsGoalDao.getAllOnce()) {
                    if (local.isSynced && local.syncId !in remoteIds) {
                        savingsGoalDao.delete(local)
                    }
                }
            }
        }
    }

    suspend fun applyRemoteDebts(docs: List<DebtSyncDoc>, pruneMissingSynced: Boolean = true) {
        val remoteIds = docs.map { it.id }.toSet()
        db.withTransaction {
            val pendingTombstones = tombstoneDao.getAllOnce()
                .filter { it.collectionName == SyncCollectionNames.DEBTS }
                .map { it.syncId }
                .toSet()

            for (doc in docs) {
                if (doc.id.isBlank() || doc.id in pendingTombstones) continue
                val walletId = doc.walletSyncId?.let { walletDao.getBySyncId(it)?.id }
                val local = debtRecordDao.getBySyncId(doc.id)
                val sec = doc.createdAt?.seconds
                val nano = doc.createdAt?.nanoseconds
                if (local == null) {
                    debtRecordDao.insert(
                        DebtRecordEntity(
                            personName = doc.personName,
                            amount = doc.amount,
                            paidAmount = doc.paidAmount,
                            direction = doc.direction,
                            walletId = walletId,
                            dueDateMillis = doc.dueDateMillis,
                            note = doc.note,
                            isSettled = doc.settled,
                            createdAtMillis = doc.createdAtMillis,
                            syncId = doc.id,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec,
                            cloudCreatedAtNanos = nano
                        )
                    )
                } else if (doc.clientUpdatedAt > local.updatedAt || (local.isSynced && doc.clientUpdatedAt != local.updatedAt)) {
                    debtRecordDao.update(
                        local.copy(
                            personName = doc.personName,
                            amount = doc.amount,
                            paidAmount = doc.paidAmount,
                            direction = doc.direction,
                            walletId = walletId,
                            dueDateMillis = doc.dueDateMillis,
                            note = doc.note,
                            isSettled = doc.settled,
                            createdAtMillis = doc.createdAtMillis,
                            updatedAt = doc.clientUpdatedAt,
                            isSynced = true,
                            cloudCreatedAtSeconds = sec ?: local.cloudCreatedAtSeconds,
                            cloudCreatedAtNanos = nano ?: local.cloudCreatedAtNanos
                        )
                    )
                }
            }

            if (pruneMissingSynced) {
                for (local in debtRecordDao.getAllOnce()) {
                    if (local.isSynced && local.syncId !in remoteIds) {
                        debtRecordDao.delete(local)
                    }
                }
            }
        }
    }

    private suspend fun applyBalanceEffect(
        tx: TransactionEntity,
        multiplier: Int,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        when (tx.type) {
            TransactionType.EXPENSE -> {
                walletDao.adjustBalance(tx.walletId, -tx.amount * multiplier, updatedAt)
            }
            TransactionType.INCOME -> {
                walletDao.adjustBalance(tx.walletId, tx.amount * multiplier, updatedAt)
            }
            TransactionType.TRANSFER -> {
                walletDao.adjustBalance(tx.walletId, -tx.amount * multiplier, updatedAt)
                tx.targetWalletId?.let { targetId ->
                    walletDao.adjustBalance(targetId, tx.amount * multiplier, updatedAt)
                }
            }
        }
    }
}
