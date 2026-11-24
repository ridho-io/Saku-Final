package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.configureAppCheck
import com.example.data.local.DanaKuDatabase
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.RecurringTransactionEntity
import com.example.data.local.SyncCollectionNames
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.HomeShortcutOption
import com.example.data.local.UserPreferences
import com.example.data.local.WalletEntity
import com.example.data.remote.TransactionSyncDoc
import com.example.data.remote.WalletSyncDoc
import com.example.data.repository.FinanceRepository
import com.example.ui.theme.AccentPalettePreset
import com.example.ui.theme.CardCornerStyle
import com.example.ui.theme.ThemeMode
import com.example.util.DateFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: DanaKuDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DanaKuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Saku", appName)
    }

    @Test
    fun `recurring transactions auto-create due occurrences up to endDate and update wallet balance`() = runBlocking {
        val walletId = db.walletDao().insert(
            WalletEntity(
                name = "BCA",
                walletType = "Bank",
                balance = 1_000_000L,
                accentHex = 0xFF1A1A1AL,
                accountNumberHint = "•••• 1234"
            )
        )

        val now = DateFormatter.noonOfDay(System.currentTimeMillis())
        val twoDaysAgo = now - TimeUnit.DAYS.toMillis(2)
        val yesterday = now - TimeUnit.DAYS.toMillis(1)

        val rule = RecurringTransactionEntity(
            merchant = "Kopi Harian",
            amount = 25_000L,
            type = TransactionType.EXPENSE,
            category = "Kopi & Jajan",
            walletId = walletId,
            note = "Jadwal rutin",
            frequency = RecurrenceFrequency.DAILY,
            startDate = twoDaysAgo,
            endDate = DateFormatter.endOfDay(yesterday),
            nextDueDate = twoDaysAgo,
            isActive = true
        )

        val created = repository.upsertRecurringRule(rule, nowMillis = now)
        // Should create 2 occurrences: twoDaysAgo and yesterday, then stop and deactivate after endDate
        assertEquals(2, created)

        val txList = repository.allTransactions.first()
        assertEquals(2, txList.size)
        assertEquals(950_000L, db.walletDao().getWalletById(walletId)?.balance)

        val savedRule = repository.allRecurringTransactions.first().single()
        assertFalse(savedRule.isActive)
    }

    @Test
    fun `seeded transactions stay in the past and new manual transaction appears first in history`() = runBlocking {
        repository.ensureSeeded()
        val now = System.currentTimeMillis()
        val seeded = repository.allTransactions.first()
        assertEquals(true, seeded.isNotEmpty())
        assertEquals(true, seeded.all { it.timestamp <= now })

        val wallet = repository.allWallets.first().first()
        val initialBalance = wallet.balance
        val newTx = com.example.data.local.TransactionEntity(
            merchant = "Bakso Solo",
            amount = 20_000L,
            type = TransactionType.EXPENSE,
            category = "Makanan & Minuman",
            walletId = wallet.id,
            note = "Makan malam",
            timestamp = System.currentTimeMillis() + 100L
        )
        repository.insertTransaction(newTx)

        val updatedList = repository.allTransactions.first()
        assertEquals("Bakso Solo", updatedList.first().merchant)
        assertEquals(initialBalance - 20_000L, db.walletDao().getWalletById(wallet.id)?.balance)
    }

    @Test
    fun `deleteWalletSafely deletes wallet and its transactions but blocks deleting last wallet`() = runBlocking {
        repository.ensureSeeded()
        val wallets = repository.allWallets.first()
        val targetWallet = wallets.last()
        val txCountBefore = repository.countTransactionsForWallet(targetWallet.id)
        assertEquals(true, txCountBefore >= 0)

        val deleted = repository.deleteWalletSafely(targetWallet)
        assertEquals(true, deleted)
        assertEquals(0, repository.countTransactionsForWallet(targetWallet.id))

        // Delete until only 1 wallet remains
        var remaining = repository.allWallets.first()
        while (remaining.size > 1) {
            assertEquals(true, repository.deleteWalletSafely(remaining.last()))
            remaining = repository.allWallets.first()
        }

        // Last wallet cannot be deleted
        assertEquals(false, repository.deleteWalletSafely(remaining.single()))
        assertEquals(1, repository.allWallets.first().size)
    }

    @Test
    fun `offline delete records tombstone and pending push batch includes unsynced items`() = runBlocking {
        repository.addWallet("BCA", "Bank", 500_000L, 0xFF1A1A1AL, "1234")
        val wallet = repository.allWallets.first().single()
        assertFalse(wallet.isSynced)
        assertTrue(wallet.syncId.isNotBlank())

        val tx = TransactionEntity(
            merchant = "Warung Padang",
            amount = 35_000L,
            type = TransactionType.EXPENSE,
            category = "Makanan & Minuman",
            walletId = wallet.id,
            note = "Offline entry",
            timestamp = System.currentTimeMillis()
        )
        repository.insertTransaction(tx)
        val savedTx = repository.allTransactions.first().single()
        assertFalse(savedTx.isSynced)

        val pendingBeforeDelete = repository.getPendingPushBatch()
        assertEquals(1, pendingBeforeDelete.wallets.size)
        assertEquals(1, pendingBeforeDelete.transactions.size)
        assertEquals(0, pendingBeforeDelete.tombstones.size)

        repository.deleteTransaction(savedTx)
        assertEquals(0, repository.allTransactions.first().size)
        assertEquals(500_000L, db.walletDao().getWalletById(wallet.id)?.balance)

        val pendingAfterDelete = repository.getPendingPushBatch()
        assertEquals(0, pendingAfterDelete.transactions.size)
        assertEquals(1, pendingAfterDelete.tombstones.size)
        assertEquals(SyncCollectionNames.TRANSACTIONS, pendingAfterDelete.tombstones.single().collectionName)
        assertEquals(savedTx.syncId, pendingAfterDelete.tombstones.single().syncId)
    }

    @Test
    fun `applyRemoteWallets and applyRemoteTransactions merge cloud changes while keeping unsynced local writes`() = runBlocking {
        // Local wallet created offline (unsynced)
        repository.addWallet("Tunai Lokal", "Cash", 100_000L, 0xFF3F3F46L, "Dompet")
        val localUnsyncedWallet = repository.allWallets.first().single()

        // Remote wallet from another device
        val remoteWalletDoc = WalletSyncDoc(
            id = "remote-wallet-bca",
            userId = "user-1",
            name = "BCA Cloud",
            walletType = "Bank",
            balance = 2_000_000L,
            accentHex = 0xFF1A1A1AL,
            accountNumberHint = "9988",
            clientUpdatedAt = System.currentTimeMillis()
        )

        repository.applyRemoteWallets(listOf(remoteWalletDoc), pruneMissingSynced = true)

        val walletsAfterMerge = repository.allWallets.first()
        assertEquals(2, walletsAfterMerge.size)
        assertTrue(walletsAfterMerge.any { it.syncId == localUnsyncedWallet.syncId && !it.isSynced })
        assertTrue(walletsAfterMerge.any { it.syncId == "remote-wallet-bca" && it.isSynced && it.balance == 2_000_000L })

        val remoteTxDoc = TransactionSyncDoc(
            id = "remote-tx-1",
            userId = "user-1",
            merchant = "Kopi Cloud",
            amount = 25_000L,
            type = TransactionType.EXPENSE,
            category = "Kopi & Jajan",
            walletSyncId = "remote-wallet-bca",
            note = "Dari perangkat lain",
            timestamp = System.currentTimeMillis(),
            clientUpdatedAt = System.currentTimeMillis()
        )
        repository.applyRemoteTransactions(listOf(remoteTxDoc), pruneMissingSynced = true)

        val txsAfterMerge = repository.allTransactions.first()
        assertEquals(1, txsAfterMerge.size)
        assertEquals("Kopi Cloud", txsAfterMerge.single().merchant)
        assertTrue(txsAfterMerge.single().isSynced)
    }

    @Test
    fun `userPreferences personalization defaults, updates, and reset work reliably`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)

        assertEquals(ThemeMode.LIGHT, prefs.themeMode.first())
        val initial = prefs.personalization.first()
        assertEquals(AccentPalettePreset.COBALT_BLUE, initial.accentPreset)
        assertEquals(CardCornerStyle.ROUNDED, initial.cardCornerStyle)
        assertEquals(HomeShortcutOption.defaultSelection, initial.homeShortcuts)
        assertEquals(1, initial.billingCycleStartDay)
        assertEquals(75, initial.budgetAlertPercent)

        prefs.setDisplayName("Raka")
        prefs.setAccentPreset(AccentPalettePreset.COBALT_INDIGO)
        prefs.setCardCornerStyle(CardCornerStyle.SOFT)
        prefs.setHomeShortcuts(listOf(HomeShortcutOption.MANUAL, HomeShortcutOption.HISTORY, HomeShortcutOption.BUDGET))
        prefs.setBillingCycleStartDay(25)
        prefs.setBudgetAlertPercent(85)
        prefs.setShowBudgetOnHome(false)

        val updated = prefs.personalization.first()
        assertEquals("Raka", updated.displayName)
        assertEquals(AccentPalettePreset.COBALT_INDIGO, updated.accentPreset)
        assertEquals(CardCornerStyle.SOFT, updated.cardCornerStyle)
        assertEquals(listOf(HomeShortcutOption.MANUAL, HomeShortcutOption.HISTORY, HomeShortcutOption.BUDGET), updated.homeShortcuts)
        assertEquals(25, updated.billingCycleStartDay)
        assertEquals(85, updated.budgetAlertPercent)
        assertFalse(updated.showBudgetOnHome)

        prefs.resetPersonalization()
        val reset = prefs.personalization.first()
        assertEquals("", reset.displayName)
        assertEquals(AccentPalettePreset.COBALT_BLUE, reset.accentPreset)
        assertEquals(HomeShortcutOption.defaultSelection, reset.homeShortcuts)
        assertEquals(1, reset.billingCycleStartDay)
    }

    @Test
    fun `configureAppCheck uses reCAPTCHA key without intent token and stores debug token synchronously when provided`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val metaPrefs = context.getSharedPreferences("saku_appcheck_meta", Context.MODE_PRIVATE)
        metaPrefs.edit().clear().commit()

        val recaptchaResId = context.resources.getIdentifier("recaptcha_site_key", "string", context.packageName)
        assertTrue(recaptchaResId != 0)
        assertTrue(context.getString(recaptchaResId).isNotBlank())

        // Standalone launch (e.g. GitHub Debug APK without FIREBASE_APPCHECK_DEBUG_TOKEN extra)
        configureAppCheck(context, Intent())
        assertFalse(metaPrefs.getBoolean("debug_token_injected", false))

        // Harness launch with FIREBASE_APPCHECK_DEBUG_TOKEN extra
        val tokenIntent = Intent().apply {
            putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", "test-debug-token-123")
        }
        configureAppCheck(context, tokenIntent)
        assertTrue(metaPrefs.getBoolean("debug_token_injected", false))
    }
}

