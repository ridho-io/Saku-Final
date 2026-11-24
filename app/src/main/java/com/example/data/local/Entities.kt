package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

object TransactionType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
    const val TRANSFER = "TRANSFER"
}

object RecurrenceFrequency {
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"

    val all = listOf(DAILY, WEEKLY, MONTHLY, YEARLY)

    fun label(frequency: String): String = when (frequency) {
        DAILY -> "Harian"
        WEEKLY -> "Mingguan"
        MONTHLY -> "Bulanan"
        YEARLY -> "Tahunan"
        else -> "Bulanan"
    }
}

object SyncCollectionNames {
    const val WALLETS = "wallets"
    const val TRANSACTIONS = "transactions"
    const val BUDGETS = "budgets"
    const val RECURRING = "recurring_transactions"
    const val SAVINGS_GOALS = "savings_goals"
    const val DEBTS = "debts"
}

object DebtDirection {
    const val LEND = "LEND"     // Piutang (orang berhutang ke kita)
    const val BORROW = "BORROW" // Hutang (kita berhutang ke orang lain)
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amount: Long,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val category: String,
    val walletId: Long,
    val targetWalletId: Long? = null,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isAiGenerated: Boolean = false,
    val recurringRuleId: Long? = null,
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val cloudCreatedAtSeconds: Long? = null,
    val cloudCreatedAtNanos: Int? = null
)

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val walletType: String, // Bank, E-Wallet, Cash
    val balance: Long,
    val accentHex: Long,
    val accountNumberHint: String,
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val cloudCreatedAtSeconds: Long? = null,
    val cloudCreatedAtNanos: Int? = null
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val monthlyLimit: Long,
    val monthKey: String, // yyyy-MM
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val cloudCreatedAtSeconds: Long? = null,
    val cloudCreatedAtNanos: Int? = null
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amount: Long,
    val type: String, // EXPENSE or INCOME
    val category: String,
    val walletId: Long,
    val note: String = "",
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val startDate: Long,
    val endDate: Long? = null,
    val nextDueDate: Long,
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val cloudCreatedAtSeconds: Long? = null,
    val cloudCreatedAtNanos: Int? = null
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Long,
    val savedAmount: Long = 0L,
    val deadlineMillis: Long? = null,
    val accentHex: Long = 0xFFB5EE5AL,
    val iconName: String = "SAVINGS",
    val note: String = "",
    val isCompleted: Boolean = false,
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val cloudCreatedAtSeconds: Long? = null,
    val cloudCreatedAtNanos: Int? = null
)

@Entity(tableName = "debts")
data class DebtRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amount: Long,
    val paidAmount: Long = 0L,
    val direction: String, // LEND (Piutang) or BORROW (Hutang)
    val walletId: Long? = null,
    val dueDateMillis: Long? = null,
    val note: String = "",
    val isSettled: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val cloudCreatedAtSeconds: Long? = null,
    val cloudCreatedAtNanos: Int? = null
)

@Entity(tableName = "sync_tombstones")
data class SyncTombstoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionName: String,
    val syncId: String,
    val deletedAt: Long = System.currentTimeMillis()
)

