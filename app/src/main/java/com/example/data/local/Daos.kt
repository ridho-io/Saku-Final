package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE walletId = :walletId OR targetWalletId = :walletId")
    suspend fun getByWallet(walletId: Long): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE transactions SET isSynced = 1, updatedAt = :updatedAt, cloudCreatedAtSeconds = COALESCE(:createdSec, cloudCreatedAtSeconds), cloudCreatedAtNanos = COALESCE(:createdNano, cloudCreatedAtNanos) WHERE id = :id")
    suspend fun markSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?)

    @Query("UPDATE transactions SET isSynced = 0")
    suspend fun markAllUnsynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :walletId OR targetWalletId = :walletId")
    suspend fun countByWallet(walletId: Long): Int

    @Query("DELETE FROM transactions WHERE walletId = :walletId OR targetWalletId = :walletId")
    suspend fun deleteByWallet(walletId: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY id ASC")
    fun observeAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets ORDER BY id ASC")
    suspend fun getAllWalletsOnce(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Query("SELECT * FROM wallets WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): WalletEntity?

    @Query("SELECT * FROM wallets WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getWalletByName(name: String): WalletEntity?

    @Query("SELECT * FROM wallets WHERE isSynced = 0")
    suspend fun getUnsynced(): List<WalletEntity>

    @Query("SELECT COUNT(*) FROM wallets WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE wallets SET isSynced = 1, updatedAt = :updatedAt, cloudCreatedAtSeconds = COALESCE(:createdSec, cloudCreatedAtSeconds), cloudCreatedAtNanos = COALESCE(:createdNano, cloudCreatedAtNanos) WHERE id = :id")
    suspend fun markSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?)

    @Query("UPDATE wallets SET isSynced = 0")
    suspend fun markAllUnsynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallets: List<WalletEntity>): List<Long>

    @Update
    suspend fun update(wallet: WalletEntity)

    @Delete
    suspend fun delete(wallet: WalletEntity)

    @Query("DELETE FROM wallets WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("UPDATE wallets SET balance = balance + :delta, updatedAt = :updatedAt, isSynced = 0 WHERE id = :walletId")
    suspend fun adjustBalance(walletId: Long, delta: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun count(): Int
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey ORDER BY category ASC")
    fun observeBudgetsForMonth(monthKey: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey ORDER BY category ASC")
    suspend fun getBudgetsForMonthOnce(monthKey: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsOnce(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE category = :category AND monthKey = :monthKey LIMIT 1")
    suspend fun getBudgetByCategoryAndMonth(category: String, monthKey: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE isSynced = 0")
    suspend fun getUnsynced(): List<BudgetEntity>

    @Query("SELECT COUNT(*) FROM budgets WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE budgets SET isSynced = 1, updatedAt = :updatedAt, cloudCreatedAtSeconds = COALESCE(:createdSec, cloudCreatedAtSeconds), cloudCreatedAtNanos = COALESCE(:createdNano, cloudCreatedAtNanos) WHERE id = :id")
    suspend fun markSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?)

    @Query("UPDATE budgets SET isSynced = 0")
    suspend fun markAllUnsynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun count(): Int
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY isActive DESC, nextDueDate ASC")
    fun observeAllRecurring(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllOnce(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextDueDate ASC")
    suspend fun getActiveRecurringOnce(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE walletId = :walletId")
    suspend fun getByWallet(walletId: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE isSynced = 0")
    suspend fun getUnsynced(): List<RecurringTransactionEntity>

    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE recurring_transactions SET isSynced = 1, updatedAt = :updatedAt, cloudCreatedAtSeconds = COALESCE(:createdSec, cloudCreatedAtSeconds), cloudCreatedAtNanos = COALESCE(:createdNano, cloudCreatedAtNanos) WHERE id = :id")
    suspend fun markSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?)

    @Query("UPDATE recurring_transactions SET isSynced = 0")
    suspend fun markAllUnsynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RecurringTransactionEntity>)

    @Update
    suspend fun update(rule: RecurringTransactionEntity)

    @Delete
    suspend fun delete(rule: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("DELETE FROM recurring_transactions WHERE walletId = :walletId")
    suspend fun deleteByWallet(walletId: Long)

    @Query("SELECT COUNT(*) FROM recurring_transactions")
    suspend fun count(): Int
}

@Dao
interface SyncTombstoneDao {
    @Query("SELECT * FROM sync_tombstones ORDER BY deletedAt ASC")
    suspend fun getAllOnce(): List<SyncTombstoneEntity>

    @Query("SELECT COUNT(*) FROM sync_tombstones")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tombstone: SyncTombstoneEntity): Long

    @Delete
    suspend fun delete(tombstone: SyncTombstoneEntity)

    @Query("DELETE FROM sync_tombstones WHERE collectionName = :collectionName AND syncId = :syncId")
    suspend fun deleteByKey(collectionName: String, syncId: String)

    @Query("DELETE FROM sync_tombstones")
    suspend fun clearAll()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, updatedAt DESC")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, updatedAt DESC")
    suspend fun getAllOnce(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals WHERE isSynced = 0")
    suspend fun getUnsynced(): List<SavingsGoalEntity>

    @Query("SELECT COUNT(*) FROM savings_goals WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE savings_goals SET isSynced = 1, updatedAt = :updatedAt, cloudCreatedAtSeconds = COALESCE(:createdSec, cloudCreatedAtSeconds), cloudCreatedAtNanos = COALESCE(:createdNano, cloudCreatedAtNanos) WHERE id = :id")
    suspend fun markSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?)

    @Query("UPDATE savings_goals SET isSynced = 0")
    suspend fun markAllUnsynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}

@Dao
interface DebtRecordDao {
    @Query("SELECT * FROM debts ORDER BY isSettled ASC, COALESCE(dueDateMillis, 9223372036854775807) ASC, createdAtMillis DESC")
    fun observeAll(): Flow<List<DebtRecordEntity>>

    @Query("SELECT * FROM debts ORDER BY isSettled ASC, createdAtMillis DESC")
    suspend fun getAllOnce(): List<DebtRecordEntity>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DebtRecordEntity?

    @Query("SELECT * FROM debts WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): DebtRecordEntity?

    @Query("SELECT * FROM debts WHERE isSynced = 0")
    suspend fun getUnsynced(): List<DebtRecordEntity>

    @Query("SELECT COUNT(*) FROM debts WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE debts SET isSynced = 1, updatedAt = :updatedAt, cloudCreatedAtSeconds = COALESCE(:createdSec, cloudCreatedAtSeconds), cloudCreatedAtNanos = COALESCE(:createdNano, cloudCreatedAtNanos) WHERE id = :id")
    suspend fun markSynced(id: Long, updatedAt: Long, createdSec: Long?, createdNano: Int?)

    @Query("UPDATE debts SET isSynced = 0")
    suspend fun markAllUnsynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(debts: List<DebtRecordEntity>)

    @Update
    suspend fun update(debt: DebtRecordEntity)

    @Delete
    suspend fun delete(debt: DebtRecordEntity)

    @Query("DELETE FROM debts WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}

