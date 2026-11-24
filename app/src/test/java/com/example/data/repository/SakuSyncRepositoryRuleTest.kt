package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.BudgetEntity
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SyncCollectionNames
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.data.remote.FirestoreSyncRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SakuSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedOwner_upsertsAndUpdatesAllSyncCollections() = runBlocking {
        val uid = signInTestUser("alice@saku.test")
        val syncRepo = FirestoreSyncRepository(firestore, auth)
        val now = System.currentTimeMillis()

        val walletSyncId = UUID.randomUUID().toString()
        val wallet = WalletEntity(
            id = 1L,
            name = "BCA",
            walletType = "Bank",
            balance = 5_000_000L,
            accentHex = 0xFF1A1A1AL,
            accountNumberHint = "•••• 4829",
            syncId = walletSyncId,
            updatedAt = now
        )
        val walletCreated = withTimeout(DEFAULT_TIMEOUT_MS) { syncRepo.upsertWallet(wallet) }
        assertNotNull(walletCreated)

        // Update wallet while preserving createdAt
        val updatedWallet = wallet.copy(
            balance = 4_975_000L,
            updatedAt = now + 1_000L,
            cloudCreatedAtSeconds = walletCreated?.seconds,
            cloudCreatedAtNanos = walletCreated?.nanoseconds
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { syncRepo.upsertWallet(updatedWallet) }

        val budgetSyncId = UUID.randomUUID().toString()
        val budget = BudgetEntity(
            id = 1L,
            category = "Kopi & Jajan",
            monthlyLimit = 300_000L,
            monthKey = "2026-11",
            syncId = budgetSyncId,
            updatedAt = now
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { syncRepo.upsertBudget(budget) }

        val ruleSyncId = UUID.randomUUID().toString()
        val rule = RecurringTransactionEntity(
            id = 1L,
            merchant = "Kos & Internet",
            amount = 1_850_000L,
            type = TransactionType.EXPENSE,
            category = "Tagihan & Kos",
            walletId = 1L,
            note = "Bulanan",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = now,
            nextDueDate = now + 86_400_000L,
            isActive = true,
            syncId = ruleSyncId,
            updatedAt = now
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { syncRepo.upsertRecurring(rule, walletSyncId) }

        val txSyncId = UUID.randomUUID().toString()
        val tx = TransactionEntity(
            id = 1L,
            merchant = "Kopi Kenangan",
            amount = 25_000L,
            type = TransactionType.EXPENSE,
            category = "Kopi & Jajan",
            walletId = 1L,
            note = "Kopi pagi",
            timestamp = now,
            isAiGenerated = false,
            syncId = txSyncId,
            updatedAt = now
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { syncRepo.upsertTransaction(tx, walletSyncId, null, null) }

        val fetchedWallets = withTimeout(DEFAULT_TIMEOUT_MS) { syncRepo.fetchWalletsOnce() }
        assertTrue(fetchedWallets.any { it.id == walletSyncId && it.balance == 4_975_000L })

        val observedTx = withTimeout(FLOW_TIMEOUT_MS) {
            syncRepo.observeTransactions(uid).first { list -> list.any { it.id == txSyncId } }
        }
        assertEquals("Kopi Kenangan", observedTx.first { it.id == txSyncId }.merchant)

        // Cleanup
        withTimeout(DEFAULT_TIMEOUT_MS) {
            syncRepo.deleteDocument(SyncCollectionNames.TRANSACTIONS, txSyncId)
            syncRepo.deleteDocument(SyncCollectionNames.RECURRING, ruleSyncId)
            syncRepo.deleteDocument(SyncCollectionNames.BUDGETS, budgetSyncId)
            syncRepo.deleteDocument(SyncCollectionNames.WALLETS, walletSyncId)
        }
    }

    @Test
    fun crossUserAccess_isDeniedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso@saku.test")
        val aliceRepo = FirestoreSyncRepository(firestore, auth)
        val walletSyncId = UUID.randomUUID().toString()
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertWallet(
                WalletEntity(
                    id = 1L,
                    name = "Dompet Alice",
                    walletType = "Cash",
                    balance = 100_000L,
                    accentHex = 0xFF10B981L,
                    accountNumberHint = "Tunai",
                    syncId = walletSyncId,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // Sign in as Bob and attempt to read Alice's user-scoped collection directly
        signInTestUser("bob_iso@saku.test")
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection(SyncCollectionNames.WALLETS)
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED for cross-user wallet query")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun unauthenticatedAccess_isDeniedWithPermissionDenied() = runBlocking {
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("users").document("some_user")
                    .collection(SyncCollectionNames.TRANSACTIONS)
                    .snapshots()
                    .first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
