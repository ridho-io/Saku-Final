package com.example.data.remote

import android.util.Log
import com.example.data.local.SyncCollectionNames
import com.example.data.local.UserPreferences
import com.example.data.repository.FinanceRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "SakuSyncManager"

data class CloudSyncStatus(
    val uid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val isSignedIn: Boolean = false,
    val isSyncing: Boolean = false,
    val pendingChangesCount: Int = 0,
    val lastSyncAt: Long? = null,
    val errorMessage: String? = null
)

class SakuSyncManager(
    private val localRepository: FinanceRepository,
    private val remoteRepository: FirestoreSyncRepository,
    private val userPreferences: UserPreferences,
    private val scope: CoroutineScope,
    authProvider: () -> FirebaseAuth = { Firebase.auth }
) {
    private val auth: FirebaseAuth by lazy(LazyThreadSafetyMode.SYNCHRONIZED, authProvider)
    private val syncMutex = Mutex()
    private val _status = MutableStateFlow(CloudSyncStatus())
    val status: StateFlow<CloudSyncStatus> = _status.asStateFlow()

    private var activeSessionJob: Job? = null
    private var pendingObserverJob: Job? = null
    private var currentUid: String? = null

    fun start(authFlow: kotlinx.coroutines.flow.Flow<com.google.firebase.auth.FirebaseUser?>) {
        scope.launch {
            localRepository.observePendingSyncCount().collect { count ->
                _status.update { it.copy(pendingChangesCount = count) }
            }
        }
        scope.launch {
            userPreferences.lastSyncAt.collect { ts ->
                _status.update { it.copy(lastSyncAt = ts) }
            }
        }
        scope.launch {
            authFlow.collect { user ->
                if (user == null) {
                    stopCloudListeners()
                    currentUid = null
                    _status.update {
                        it.copy(
                            uid = null,
                            email = null,
                            displayName = null,
                            isSignedIn = false,
                            isSyncing = false,
                            errorMessage = null
                        )
                    }
                } else {
                    val userChanged = currentUid != user.uid
                    currentUid = user.uid
                    _status.update {
                        it.copy(
                            uid = user.uid,
                            email = user.email,
                            displayName = user.displayName ?: user.email,
                            isSignedIn = true,
                            errorMessage = null
                        )
                    }
                    if (userChanged || activeSessionJob?.isActive != true) {
                        startCloudSession(user.uid)
                    }
                }
            }
        }
    }

    private fun stopCloudListeners() {
        activeSessionJob?.cancel()
        activeSessionJob = null
        pendingObserverJob?.cancel()
        pendingObserverJob = null
    }

    private fun startCloudSession(uid: String) {
        stopCloudListeners()
        activeSessionJob = scope.launch {
            // 1. Initial pull + push merge before attaching real-time listeners
            syncOnceInternal(uid, isInitialConnect = true)

            // 2. Real-time listeners for remote changes from other devices
            launch {
                try {
                    remoteRepository.observeWallets(uid).collect { remoteWallets ->
                        syncMutex.withLock {
                            localRepository.applyRemoteWallets(remoteWallets, pruneMissingSynced = true)
                            recordSyncSuccess(uid)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Wallet listener error", e)
                    _status.update { it.copy(errorMessage = e.localizedMessage ?: "Sinkronisasi dompet gagal.") }
                }
            }

            launch {
                try {
                    remoteRepository.observeBudgets(uid).collect { remoteBudgets ->
                        syncMutex.withLock {
                            localRepository.applyRemoteBudgets(remoteBudgets, pruneMissingSynced = true)
                            recordSyncSuccess(uid)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Budget listener error", e)
                    _status.update { it.copy(errorMessage = e.localizedMessage ?: "Sinkronisasi anggaran gagal.") }
                }
            }

            launch {
                try {
                    remoteRepository.observeRecurring(uid).collect { remoteRules ->
                        syncMutex.withLock {
                            localRepository.applyRemoteRecurring(remoteRules, pruneMissingSynced = true)
                            recordSyncSuccess(uid)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Recurring listener error", e)
                    _status.update { it.copy(errorMessage = e.localizedMessage ?: "Sinkronisasi transaksi berulang gagal.") }
                }
            }

            launch {
                try {
                    remoteRepository.observeTransactions(uid).collect { remoteTx ->
                        syncMutex.withLock {
                            localRepository.applyRemoteTransactions(remoteTx, pruneMissingSynced = true)
                            recordSyncSuccess(uid)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Transaction listener error", e)
                    _status.update { it.copy(errorMessage = e.localizedMessage ?: "Sinkronisasi transaksi gagal.") }
                }
            }

            launch {
                try {
                    remoteRepository.observeSavingsGoals(uid).collect { remoteGoals ->
                        syncMutex.withLock {
                            localRepository.applyRemoteSavingsGoals(remoteGoals, pruneMissingSynced = true)
                            recordSyncSuccess(uid)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Savings goal listener error", e)
                    _status.update { it.copy(errorMessage = e.localizedMessage ?: "Sinkronisasi target tabungan gagal.") }
                }
            }

            launch {
                try {
                    remoteRepository.observeDebts(uid).collect { remoteDebts ->
                        syncMutex.withLock {
                            localRepository.applyRemoteDebts(remoteDebts, pruneMissingSynced = true)
                            recordSyncSuccess(uid)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Debt listener error", e)
                    _status.update { it.copy(errorMessage = e.localizedMessage ?: "Sinkronisasi hutang piutang gagal.") }
                }
            }
        }

        // 3. Auto-push local Room writes to Firestore whenever pending count > 0
        pendingObserverJob = scope.launch {
            localRepository.observePendingSyncCount().collectLatest { count ->
                if (count > 0 && auth.currentUser?.uid == uid) {
                    delay(450)
                    pushPendingInternal(uid)
                }
            }
        }
    }

    suspend fun syncNow(): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Masuk dengan akun Google terlebih dahulu."))
        return syncOnceInternal(uid, isInitialConnect = false)
    }

    suspend fun forcePushAllLocal(): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Masuk dengan akun Google terlebih dahulu."))
        localRepository.markAllLocalUnsynced()
        return syncOnceInternal(uid, isInitialConnect = false)
    }

    private suspend fun syncOnceInternal(uid: String, isInitialConnect: Boolean): Result<Unit> {
        return syncMutex.withLock {
            if (auth.currentUser?.uid != uid) {
                return@withLock Result.failure(IllegalStateException("Sesi Google berakhir."))
            }
            _status.update { it.copy(isSyncing = true, errorMessage = null) }
            try {
                // Pull remote state first
                val remoteWallets = remoteRepository.fetchWalletsOnce()
                val remoteBudgets = remoteRepository.fetchBudgetsOnce()
                val remoteRecurring = remoteRepository.fetchRecurringOnce()
                val remoteTransactions = remoteRepository.fetchTransactionsOnce()
                val remoteGoals = remoteRepository.fetchSavingsGoalsOnce()
                val remoteDebts = remoteRepository.fetchDebtsOnce()

                // If signing in on a fresh device with only untouched sample data and the cloud
                // already has wallets for this account, replace untouched sample rows cleanly.
                if (isInitialConnect && remoteWallets.isNotEmpty()) {
                    val hasCustomLocal = userPreferences.hasCustomLocalData()
                    val lastUid = userPreferences.getLastSyncedUid()
                    if (!hasCustomLocal && lastUid == null) {
                        localRepository.clearUntouchedLocalDataForInitialCloudRestore()
                    }
                }

                // Apply remote changes into Room in dependency order
                localRepository.applyRemoteWallets(remoteWallets, pruneMissingSynced = !isInitialConnect)
                localRepository.applyRemoteBudgets(remoteBudgets, pruneMissingSynced = !isInitialConnect)
                localRepository.applyRemoteRecurring(remoteRecurring, pruneMissingSynced = !isInitialConnect)
                localRepository.applyRemoteTransactions(remoteTransactions, pruneMissingSynced = !isInitialConnect)
                localRepository.applyRemoteSavingsGoals(remoteGoals, pruneMissingSynced = !isInitialConnect)
                localRepository.applyRemoteDebts(remoteDebts, pruneMissingSynced = !isInitialConnect)

                // Push any local pending changes (including offline creations/edits/deletions)
                pushBatchLocked(uid)

                recordSyncSuccess(uid)
                _status.update { it.copy(isSyncing = false, errorMessage = null) }
                Result.success(Unit)
            } catch (e: CancellationException) {
                _status.update { it.copy(isSyncing = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Cloud sync failed", e)
                val msg = friendlySyncError(e)
                _status.update { it.copy(isSyncing = false, errorMessage = msg) }
                Result.failure(e)
            }
        }
    }

    private suspend fun pushPendingInternal(uid: String) {
        syncMutex.withLock {
            if (auth.currentUser?.uid != uid) return@withLock
            _status.update { it.copy(isSyncing = true, errorMessage = null) }
            try {
                pushBatchLocked(uid)
                recordSyncSuccess(uid)
                _status.update { it.copy(isSyncing = false, errorMessage = null) }
            } catch (e: CancellationException) {
                _status.update { it.copy(isSyncing = false) }
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Background push failed (will retry when online)", e)
                _status.update { it.copy(isSyncing = false, errorMessage = friendlySyncError(e)) }
            }
        }
    }

    private suspend fun pushBatchLocked(uid: String) {
        if (auth.currentUser?.uid != uid) return
        val batch = localRepository.getPendingPushBatch()
        if (batch.isEmpty) return

        // 1. Delete tombstones first (in safe dependency order: transactions -> recurring -> budgets -> wallets)
        val sortedTombstones = batch.tombstones.sortedBy {
            when (it.collectionName) {
                SyncCollectionNames.TRANSACTIONS -> 0
                SyncCollectionNames.RECURRING -> 1
                SyncCollectionNames.BUDGETS -> 2
                else -> 3
            }
        }
        for (tomb in sortedTombstones) {
            remoteRepository.deleteDocument(tomb.collectionName, tomb.syncId)
            localRepository.deleteTombstone(tomb)
        }

        // 2. Push wallets first so foreign walletSyncId exists
        for (wallet in batch.wallets) {
            val createdTs = remoteRepository.upsertWallet(wallet)
            localRepository.markWalletSynced(
                id = wallet.id,
                updatedAt = wallet.updatedAt,
                createdSec = createdTs?.seconds,
                createdNano = createdTs?.nanoseconds
            )
        }

        // 3. Push budgets
        for (budget in batch.budgets) {
            val createdTs = remoteRepository.upsertBudget(budget)
            localRepository.markBudgetSynced(
                id = budget.id,
                updatedAt = budget.updatedAt,
                createdSec = createdTs?.seconds,
                createdNano = createdTs?.nanoseconds
            )
        }

        // 4. Push recurring rules
        for (rule in batch.recurringRules) {
            val walletSyncId = batch.walletSyncIdByLocalId[rule.walletId] ?: continue
            val createdTs = remoteRepository.upsertRecurring(rule, walletSyncId)
            localRepository.markRecurringSynced(
                id = rule.id,
                updatedAt = rule.updatedAt,
                createdSec = createdTs?.seconds,
                createdNano = createdTs?.nanoseconds
            )
        }

        // 5. Push transactions
        for (tx in batch.transactions) {
            val walletSyncId = batch.walletSyncIdByLocalId[tx.walletId] ?: continue
            val targetSyncId = tx.targetWalletId?.let { batch.walletSyncIdByLocalId[it] }
            val ruleSyncId = tx.recurringRuleId?.let { batch.recurringSyncIdByLocalId[it] }
            val createdTs = remoteRepository.upsertTransaction(tx, walletSyncId, targetSyncId, ruleSyncId)
            localRepository.markTransactionSynced(
                id = tx.id,
                updatedAt = tx.updatedAt,
                createdSec = createdTs?.seconds,
                createdNano = createdTs?.nanoseconds
            )
        }

        // 6. Push savings goals
        for (goal in batch.savingsGoals) {
            val createdTs = remoteRepository.upsertSavingsGoal(goal)
            localRepository.markSavingsGoalSynced(
                id = goal.id,
                updatedAt = goal.updatedAt,
                createdSec = createdTs?.seconds,
                createdNano = createdTs?.nanoseconds
            )
        }

        // 7. Push debts
        for (debt in batch.debts) {
            val walletSyncId = debt.walletId?.let { batch.walletSyncIdByLocalId[it] }
            val createdTs = remoteRepository.upsertDebt(debt, walletSyncId)
            localRepository.markDebtSynced(
                id = debt.id,
                updatedAt = debt.updatedAt,
                createdSec = createdTs?.seconds,
                createdNano = createdTs?.nanoseconds
            )
        }
    }

    private suspend fun recordSyncSuccess(uid: String) {
        val now = System.currentTimeMillis()
        userPreferences.setLastSyncInfo(uid, now)
    }

    private fun friendlySyncError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("Quota exceeded", ignoreCase = true) ->
                "Kuota sinkronisasi harian tercapai. Data tetap aman tersimpan lokal."
            msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("offline", ignoreCase = true) ->
                "Sedang offline. Perubahan disimpan lokal dan akan tersinkronisasi otomatis saat online."
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Izin akses cloud ditolak. Silakan masuk ulang dengan Google."
            else -> e.localizedMessage ?: "Sinkronisasi tertunda. Data tetap tersimpan lokal."
        }
    }
}
