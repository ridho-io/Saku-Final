package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.BudgetEntity
import com.example.data.local.DebtRecordEntity
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.SyncCollectionNames
import com.example.data.local.TransactionEntity
import com.example.data.local.WalletEntity
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(
    dbProvider: () -> FirebaseFirestore,
    authProvider: () -> FirebaseAuth = { Firebase.auth }
) {
    private val db: FirebaseFirestore by lazy(LazyThreadSafetyMode.SYNCHRONIZED, dbProvider)
    private val auth: FirebaseAuth by lazy(LazyThreadSafetyMode.SYNCHRONIZED, authProvider)

    constructor(db: FirebaseFirestore, auth: FirebaseAuth = Firebase.auth) : this(
        dbProvider = { db },
        authProvider = { auth }
    )

    constructor(context: Context) : this(
        dbProvider = {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        },
        authProvider = { Firebase.auth }
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userRoot(uid: String) = db.collection("users").document(uid)

    fun observeWallets(userId: String): Flow<List<WalletSyncDoc>> =
        observeCollection(userId, SyncCollectionNames.WALLETS) { snap ->
            snap.toObject(WalletSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeTransactions(userId: String): Flow<List<TransactionSyncDoc>> =
        observeCollection(userId, SyncCollectionNames.TRANSACTIONS) { snap ->
            snap.toObject(TransactionSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeBudgets(userId: String): Flow<List<BudgetSyncDoc>> =
        observeCollection(userId, SyncCollectionNames.BUDGETS) { snap ->
            snap.toObject(BudgetSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeRecurring(userId: String): Flow<List<RecurringSyncDoc>> =
        observeCollection(userId, SyncCollectionNames.RECURRING) { snap ->
            snap.toObject(RecurringSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeSavingsGoals(userId: String): Flow<List<SavingsGoalSyncDoc>> =
        observeCollection(userId, SyncCollectionNames.SAVINGS_GOALS) { snap ->
            snap.toObject(SavingsGoalSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeDebts(userId: String): Flow<List<DebtSyncDoc>> =
        observeCollection(userId, SyncCollectionNames.DEBTS) { snap ->
            snap.toObject(DebtSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    private fun <T> observeCollection(
        userId: String,
        collectionName: String,
        mapper: (DocumentSnapshot) -> T?
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Authenticated user mismatch for sync observation." }
        val path = "users/$uid/$collectionName"
        emitAll(
            userRoot(uid).collection(collectionName)
                .snapshots()
                .map { querySnapshot ->
                    querySnapshot.documents.mapNotNull { doc -> mapper(doc) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun fetchWalletsOnce(): List<WalletSyncDoc> = fetchOnce(SyncCollectionNames.WALLETS) {
        it.toObject(WalletSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    suspend fun fetchBudgetsOnce(): List<BudgetSyncDoc> = fetchOnce(SyncCollectionNames.BUDGETS) {
        it.toObject(BudgetSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    suspend fun fetchRecurringOnce(): List<RecurringSyncDoc> = fetchOnce(SyncCollectionNames.RECURRING) {
        it.toObject(RecurringSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    suspend fun fetchTransactionsOnce(): List<TransactionSyncDoc> = fetchOnce(SyncCollectionNames.TRANSACTIONS) {
        it.toObject(TransactionSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    suspend fun fetchSavingsGoalsOnce(): List<SavingsGoalSyncDoc> = fetchOnce(SyncCollectionNames.SAVINGS_GOALS) {
        it.toObject(SavingsGoalSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    suspend fun fetchDebtsOnce(): List<DebtSyncDoc> = fetchOnce(SyncCollectionNames.DEBTS) {
        it.toObject(DebtSyncDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    private suspend fun <T> fetchOnce(
        collectionName: String,
        mapper: (DocumentSnapshot) -> T?
    ): List<T> {
        val uid = requireUserId()
        val path = "users/$uid/$collectionName"
        return try {
            val snap = userRoot(uid).collection(collectionName).get().await()
            snap.documents.mapNotNull(mapper)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun upsertWallet(wallet: WalletEntity): Timestamp? {
        val uid = requireUserId()
        val docRef = userRoot(uid).collection(SyncCollectionNames.WALLETS).document(wallet.syncId)
        val cachedCreated = cachedTimestamp(wallet.cloudCreatedAtSeconds, wallet.cloudCreatedAtNanos)
        return upsertWithCreatedAtPreservation(
            path = docRef.path,
            cachedCreated = cachedCreated,
            docRefGet = { docRef.get().await() },
            writeFn = { existingCreated ->
                val model = WalletSyncDoc(
                    id = wallet.syncId,
                    userId = uid,
                    name = wallet.name,
                    walletType = wallet.walletType,
                    balance = wallet.balance,
                    accentHex = wallet.accentHex,
                    accountNumberHint = wallet.accountNumberHint,
                    clientUpdatedAt = wallet.updatedAt
                )
                docRef.set(model.toFirestoreMap(existingCreated)).await()
            },
            readCreatedFn = { docRef.get().await().getTimestamp("createdAt") }
        )
    }

    suspend fun upsertBudget(budget: BudgetEntity): Timestamp? {
        val uid = requireUserId()
        val docRef = userRoot(uid).collection(SyncCollectionNames.BUDGETS).document(budget.syncId)
        val cachedCreated = cachedTimestamp(budget.cloudCreatedAtSeconds, budget.cloudCreatedAtNanos)
        return upsertWithCreatedAtPreservation(
            path = docRef.path,
            cachedCreated = cachedCreated,
            docRefGet = { docRef.get().await() },
            writeFn = { existingCreated ->
                val model = BudgetSyncDoc(
                    id = budget.syncId,
                    userId = uid,
                    category = budget.category,
                    monthlyLimit = budget.monthlyLimit,
                    monthKey = budget.monthKey,
                    clientUpdatedAt = budget.updatedAt
                )
                docRef.set(model.toFirestoreMap(existingCreated)).await()
            },
            readCreatedFn = { docRef.get().await().getTimestamp("createdAt") }
        )
    }

    suspend fun upsertRecurring(rule: RecurringTransactionEntity, walletSyncId: String): Timestamp? {
        val uid = requireUserId()
        val docRef = userRoot(uid).collection(SyncCollectionNames.RECURRING).document(rule.syncId)
        val cachedCreated = cachedTimestamp(rule.cloudCreatedAtSeconds, rule.cloudCreatedAtNanos)
        return upsertWithCreatedAtPreservation(
            path = docRef.path,
            cachedCreated = cachedCreated,
            docRefGet = { docRef.get().await() },
            writeFn = { existingCreated ->
                val model = RecurringSyncDoc(
                    id = rule.syncId,
                    userId = uid,
                    merchant = rule.merchant,
                    amount = rule.amount,
                    type = rule.type,
                    category = rule.category,
                    walletSyncId = walletSyncId,
                    note = rule.note,
                    frequency = rule.frequency,
                    startDate = rule.startDate,
                    endDate = rule.endDate,
                    nextDueDate = rule.nextDueDate,
                    lastProcessedDate = rule.lastProcessedDate,
                    isActive = rule.isActive,
                    clientUpdatedAt = rule.updatedAt
                )
                docRef.set(model.toFirestoreMap(existingCreated)).await()
            },
            readCreatedFn = { docRef.get().await().getTimestamp("createdAt") }
        )
    }

    suspend fun upsertTransaction(
        tx: TransactionEntity,
        walletSyncId: String,
        targetWalletSyncId: String?,
        recurringRuleSyncId: String?
    ): Timestamp? {
        val uid = requireUserId()
        val docRef = userRoot(uid).collection(SyncCollectionNames.TRANSACTIONS).document(tx.syncId)
        val cachedCreated = cachedTimestamp(tx.cloudCreatedAtSeconds, tx.cloudCreatedAtNanos)
        return upsertWithCreatedAtPreservation(
            path = docRef.path,
            cachedCreated = cachedCreated,
            docRefGet = { docRef.get().await() },
            writeFn = { existingCreated ->
                val model = TransactionSyncDoc(
                    id = tx.syncId,
                    userId = uid,
                    merchant = tx.merchant,
                    amount = tx.amount,
                    type = tx.type,
                    category = tx.category,
                    walletSyncId = walletSyncId,
                    targetWalletSyncId = targetWalletSyncId,
                    note = tx.note,
                    timestamp = tx.timestamp,
                    isAiGenerated = tx.isAiGenerated,
                    recurringRuleSyncId = recurringRuleSyncId,
                    clientUpdatedAt = tx.updatedAt
                )
                docRef.set(model.toFirestoreMap(existingCreated)).await()
            },
            readCreatedFn = { docRef.get().await().getTimestamp("createdAt") }
        )
    }

    suspend fun upsertSavingsGoal(goal: SavingsGoalEntity): Timestamp? {
        val uid = requireUserId()
        val docRef = userRoot(uid).collection(SyncCollectionNames.SAVINGS_GOALS).document(goal.syncId)
        val cachedCreated = cachedTimestamp(goal.cloudCreatedAtSeconds, goal.cloudCreatedAtNanos)
        return upsertWithCreatedAtPreservation(
            path = docRef.path,
            cachedCreated = cachedCreated,
            docRefGet = { docRef.get().await() },
            writeFn = { existingCreated ->
                val model = SavingsGoalSyncDoc(
                    id = goal.syncId,
                    userId = uid,
                    title = goal.title,
                    targetAmount = goal.targetAmount,
                    savedAmount = goal.savedAmount,
                    deadlineMillis = goal.deadlineMillis,
                    accentHex = goal.accentHex,
                    iconName = goal.iconName,
                    note = goal.note,
                    completed = goal.isCompleted,
                    clientUpdatedAt = goal.updatedAt
                )
                docRef.set(model.toFirestoreMap(existingCreated)).await()
            },
            readCreatedFn = { docRef.get().await().getTimestamp("createdAt") }
        )
    }

    suspend fun upsertDebt(debt: DebtRecordEntity, walletSyncId: String?): Timestamp? {
        val uid = requireUserId()
        val docRef = userRoot(uid).collection(SyncCollectionNames.DEBTS).document(debt.syncId)
        val cachedCreated = cachedTimestamp(debt.cloudCreatedAtSeconds, debt.cloudCreatedAtNanos)
        return upsertWithCreatedAtPreservation(
            path = docRef.path,
            cachedCreated = cachedCreated,
            docRefGet = { docRef.get().await() },
            writeFn = { existingCreated ->
                val model = DebtSyncDoc(
                    id = debt.syncId,
                    userId = uid,
                    personName = debt.personName,
                    amount = debt.amount,
                    paidAmount = debt.paidAmount,
                    direction = debt.direction,
                    walletSyncId = walletSyncId,
                    dueDateMillis = debt.dueDateMillis,
                    note = debt.note,
                    settled = debt.isSettled,
                    createdAtMillis = debt.createdAtMillis,
                    clientUpdatedAt = debt.updatedAt
                )
                docRef.set(model.toFirestoreMap(existingCreated)).await()
            },
            readCreatedFn = { docRef.get().await().getTimestamp("createdAt") }
        )
    }

    suspend fun deleteDocument(collectionName: String, syncId: String) {
        val uid = requireUserId()
        val docRef = userRoot(uid).collection(collectionName).document(syncId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    private suspend fun upsertWithCreatedAtPreservation(
        path: String,
        cachedCreated: Timestamp?,
        docRefGet: suspend () -> DocumentSnapshot,
        writeFn: suspend (Timestamp?) -> Unit,
        readCreatedFn: suspend () -> Timestamp?
    ): Timestamp? {
        var resolvedCreated = cachedCreated
        val op = if (cachedCreated == null) OperationType.CREATE else OperationType.UPDATE
        try {
            if (resolvedCreated == null) {
                val snap = docRefGet()
                if (snap.exists()) {
                    resolvedCreated = snap.getTimestamp("createdAt")
                }
            }
            writeFn(resolvedCreated)
            return resolvedCreated ?: readCreatedFn()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, path)
            throw e
        }
    }

    private fun cachedTimestamp(seconds: Long?, nanos: Int?): Timestamp? {
        return if (seconds != null && nanos != null) Timestamp(seconds, nanos) else null
    }
}
