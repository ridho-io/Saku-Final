package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        WalletEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        SavingsGoalEntity::class,
        DebtRecordEntity::class,
        SyncTombstoneEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class DanaKuDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun debtRecordDao(): DebtRecordDao
    abstract fun syncTombstoneDao(): SyncTombstoneDao

    companion object {
        @Volatile
        private var INSTANCE: DanaKuDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurringRuleId INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        merchant TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        walletId INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER,
                        nextDueDate INTEGER NOT NULL,
                        lastProcessedDate INTEGER,
                        isActive INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = listOf("transactions", "wallets", "budgets", "recurring_transactions")
                for (table in tables) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE $table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE $table ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE $table ADD COLUMN cloudCreatedAtSeconds INTEGER")
                    db.execSQL("ALTER TABLE $table ADD COLUMN cloudCreatedAtNanos INTEGER")
                    db.execSQL(
                        "UPDATE $table SET syncId = lower(hex(randomblob(16))), updatedAt = CAST((strftime('%s','now') * 1000) AS INTEGER) WHERE syncId = ''"
                    )
                }

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_tombstones (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        collectionName TEXT NOT NULL,
                        syncId TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS savings_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        targetAmount INTEGER NOT NULL,
                        savedAmount INTEGER NOT NULL,
                        deadlineMillis INTEGER,
                        accentHex INTEGER NOT NULL,
                        iconName TEXT NOT NULL,
                        note TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        syncId TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL,
                        cloudCreatedAtSeconds INTEGER,
                        cloudCreatedAtNanos INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS debts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        personName TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        paidAmount INTEGER NOT NULL,
                        direction TEXT NOT NULL,
                        walletId INTEGER,
                        dueDateMillis INTEGER,
                        note TEXT NOT NULL,
                        isSettled INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        syncId TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL,
                        cloudCreatedAtSeconds INTEGER,
                        cloudCreatedAtNanos INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): DanaKuDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DanaKuDatabase::class.java,
                    "danaku_finance.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

