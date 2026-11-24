package com.example.data.local

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nfc.EmoneyCardSnapshot
import com.example.ui.theme.AccentPalettePreset
import com.example.ui.theme.CardCornerStyle
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.sakuDataStore: DataStore<Preferences> by preferencesDataStore(name = "saku_prefs")

enum class HomeShortcutOption(val label: String) {
    AI("AI"),
    VOICE("Suara"),
    SCAN("Scan"),
    MANUAL("Manual"),
    HISTORY("Riwayat"),
    BUDGET("Anggaran"),
    WALLET("Dompet"),
    SAVINGS("Tabungan"),
    DEBTS("Hutang"),
    OVERVIEW("Tren 6 Bln"),
    NFC_EMONEY("Cek NFC"),
    CALENDAR("Kalender");

    companion object {
        val defaultSelection = listOf(AI, VOICE, SCAN)
    }
}

data class PersonalizationPrefs(
    val displayName: String = "",
    val accentPreset: AccentPalettePreset = AccentPalettePreset.COBALT_BLUE,
    val cardCornerStyle: CardCornerStyle = CardCornerStyle.ROUNDED,
    val showWalletsOnHome: Boolean = true,
    val showBudgetOnHome: Boolean = true,
    val compactCurrencyOnHome: Boolean = false,
    val homeShortcuts: List<HomeShortcutOption> = HomeShortcutOption.defaultSelection,
    val defaultWalletId: Long? = null,
    val defaultFabTabName: String = "MANUAL",
    val budgetAlertPercent: Int = 75,
    val billingCycleStartDay: Int = 1
)

data class AppSecurityPrefs(
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = true,
    val hasPin: Boolean = false,
    val lockOnBackground: Boolean = true
)

data class NotificationPrefs(
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val budgetAlertNotifEnabled: Boolean = true
)

data class EmoneyCardRecord(
    val uidHex: String,
    val issuerName: String,
    val issuerBadge: String,
    val cardNumber: String,
    val balance: Long,
    val previousBalance: Long?,
    val accentHex: Long,
    val scannedAtMillis: Long,
    val sourceLabel: String = "Chip NFC"
) {
    val balanceDelta: Long?
        get() = previousBalance?.let { balance - it }?.takeIf { it != 0L }
}

class UserPreferences(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val lastSyncAtKey = longPreferencesKey("last_sync_at")
    private val lastSyncedUidKey = stringPreferencesKey("last_synced_uid")
    private val hasCustomLocalDataKey = booleanPreferencesKey("has_custom_local_data")
    private val hideBalanceKey = booleanPreferencesKey("hide_balance_default")

    // Onboarding
    private val hasCompletedOnboardingKey = booleanPreferencesKey("has_completed_onboarding")

    // App Lock (hashed PIN + biometric)
    private val appLockEnabledKey = booleanPreferencesKey("security_app_lock_enabled")
    private val biometricEnabledKey = booleanPreferencesKey("security_biometric_enabled")
    private val pinHashKey = stringPreferencesKey("security_pin_hash")
    private val pinSaltKey = stringPreferencesKey("security_pin_salt")
    private val lockOnBackgroundKey = booleanPreferencesKey("security_lock_on_background")

    // Notifications
    private val dailyReminderEnabledKey = booleanPreferencesKey("notif_daily_reminder_enabled")
    private val reminderHourKey = intPreferencesKey("notif_reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("notif_reminder_minute")
    private val budgetAlertNotifEnabledKey = booleanPreferencesKey("notif_budget_alert_enabled")

    // NFC E-Money history
    private val emoneyHistoryJsonKey = stringPreferencesKey("nfc_emoney_history_json")

    // Personalization keys
    private val displayNameKey = stringPreferencesKey("personal_display_name")
    private val accentPresetKey = stringPreferencesKey("personal_accent_preset")
    private val cardCornerStyleKey = stringPreferencesKey("personal_card_corner_style")
    private val showWalletsOnHomeKey = booleanPreferencesKey("personal_show_wallets_home")
    private val showBudgetOnHomeKey = booleanPreferencesKey("personal_show_budget_home")
    private val compactCurrencyOnHomeKey = booleanPreferencesKey("personal_compact_currency_home")
    private val homeShortcutsKey = stringPreferencesKey("personal_home_shortcuts")
    private val defaultWalletIdKey = longPreferencesKey("personal_default_wallet_id")
    private val defaultFabTabKey = stringPreferencesKey("personal_default_fab_tab")
    private val budgetAlertPercentKey = intPreferencesKey("personal_budget_alert_pct")
    private val billingCycleStartDayKey = intPreferencesKey("personal_billing_cycle_start_day")

    val themeMode: Flow<ThemeMode> = context.sakuDataStore.data.map { prefs ->
        val raw = prefs[themeModeKey]
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.LIGHT
    }

    val hideBalance: Flow<Boolean> = context.sakuDataStore.data.map { prefs ->
        prefs[hideBalanceKey] ?: false
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.sakuDataStore.data.map { prefs ->
        prefs[hasCompletedOnboardingKey] ?: false
    }

    val securityPrefs: Flow<AppSecurityPrefs> = context.sakuDataStore.data.map { prefs ->
        val hasPin = !prefs[pinHashKey].isNullOrBlank() && !prefs[pinSaltKey].isNullOrBlank()
        val bioEnabled = prefs[biometricEnabledKey] ?: true
        val hasAuthFactor = hasPin || bioEnabled
        AppSecurityPrefs(
            appLockEnabled = (prefs[appLockEnabledKey] ?: false) && hasAuthFactor,
            biometricEnabled = bioEnabled,
            hasPin = hasPin,
            lockOnBackground = prefs[lockOnBackgroundKey] ?: true
        )
    }

    val notificationPrefs: Flow<NotificationPrefs> = context.sakuDataStore.data.map { prefs ->
        NotificationPrefs(
            dailyReminderEnabled = prefs[dailyReminderEnabledKey] ?: false,
            reminderHour = (prefs[reminderHourKey] ?: 20).coerceIn(0, 23),
            reminderMinute = (prefs[reminderMinuteKey] ?: 0).coerceIn(0, 59),
            budgetAlertNotifEnabled = prefs[budgetAlertNotifEnabledKey] ?: true
        )
    }

    val emoneyHistory: Flow<List<EmoneyCardRecord>> = context.sakuDataStore.data.map { prefs ->
        parseEmoneyHistory(prefs[emoneyHistoryJsonKey])
    }

    val personalization: Flow<PersonalizationPrefs> = context.sakuDataStore.data.map { prefs ->
        val accent = AccentPalettePreset.entries.firstOrNull { it.name == prefs[accentPresetKey] }
            ?: AccentPalettePreset.COBALT_BLUE
        val corner = CardCornerStyle.entries.firstOrNull { it.name == prefs[cardCornerStyleKey] }
            ?: CardCornerStyle.ROUNDED
        val parsedShortcuts = prefs[homeShortcutsKey]
            ?.split(",")
            ?.mapNotNull { token -> HomeShortcutOption.entries.firstOrNull { it.name == token.trim() } }
            ?.distinct()
            ?.take(3)
            ?.takeIf { it.size == 3 }
            ?: HomeShortcutOption.defaultSelection
        val rawDefaultWallet = prefs[defaultWalletIdKey]

        PersonalizationPrefs(
            displayName = prefs[displayNameKey].orEmpty(),
            accentPreset = accent,
            cardCornerStyle = corner,
            showWalletsOnHome = prefs[showWalletsOnHomeKey] ?: true,
            showBudgetOnHome = prefs[showBudgetOnHomeKey] ?: true,
            compactCurrencyOnHome = prefs[compactCurrencyOnHomeKey] ?: false,
            homeShortcuts = parsedShortcuts,
            defaultWalletId = rawDefaultWallet?.takeIf { it > 0L },
            defaultFabTabName = prefs[defaultFabTabKey] ?: "MANUAL",
            budgetAlertPercent = (prefs[budgetAlertPercentKey] ?: 75).coerceIn(50, 95),
            billingCycleStartDay = (prefs[billingCycleStartDayKey] ?: 1).coerceIn(1, 28)
        )
    }

    val lastSyncAt: Flow<Long?> = context.sakuDataStore.data.map { prefs ->
        prefs[lastSyncAtKey]
    }

    val lastSyncedUid: Flow<String?> = context.sakuDataStore.data.map { prefs ->
        prefs[lastSyncedUidKey]
    }

    suspend fun getHasCompletedOnboarding(): Boolean = hasCompletedOnboarding.first()

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.sakuDataStore.edit { prefs ->
            prefs[hasCompletedOnboardingKey] = completed
        }
    }

    suspend fun hasCustomLocalData(): Boolean =
        context.sakuDataStore.data.map { prefs -> prefs[hasCustomLocalDataKey] ?: false }.first()

    suspend fun getLastSyncedUid(): String? = lastSyncedUid.first()

    suspend fun markCustomLocalData() {
        context.sakuDataStore.edit { prefs ->
            prefs[hasCustomLocalDataKey] = true
        }
    }

    suspend fun clearCustomLocalDataFlag() {
        context.sakuDataStore.edit { prefs ->
            prefs[hasCustomLocalDataKey] = false
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.sakuDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun setHideBalance(hidden: Boolean) {
        context.sakuDataStore.edit { prefs ->
            prefs[hideBalanceKey] = hidden
        }
    }

    suspend fun setDisplayName(name: String) {
        context.sakuDataStore.edit { prefs ->
            prefs[displayNameKey] = name.trim().take(24)
        }
    }

    suspend fun setAccentPreset(preset: AccentPalettePreset) {
        context.sakuDataStore.edit { prefs ->
            prefs[accentPresetKey] = preset.name
        }
    }

    suspend fun setCardCornerStyle(style: CardCornerStyle) {
        context.sakuDataStore.edit { prefs ->
            prefs[cardCornerStyleKey] = style.name
        }
    }

    suspend fun setShowWalletsOnHome(show: Boolean) {
        context.sakuDataStore.edit { prefs ->
            prefs[showWalletsOnHomeKey] = show
        }
    }

    suspend fun setShowBudgetOnHome(show: Boolean) {
        context.sakuDataStore.edit { prefs ->
            prefs[showBudgetOnHomeKey] = show
        }
    }

    suspend fun setCompactCurrencyOnHome(compact: Boolean) {
        context.sakuDataStore.edit { prefs ->
            prefs[compactCurrencyOnHomeKey] = compact
        }
    }

    suspend fun setHomeShortcuts(shortcuts: List<HomeShortcutOption>) {
        val valid = shortcuts.distinct().take(3)
        if (valid.size != 3) return
        context.sakuDataStore.edit { prefs ->
            prefs[homeShortcutsKey] = valid.joinToString(",") { it.name }
        }
    }

    suspend fun setDefaultWalletId(walletId: Long?) {
        context.sakuDataStore.edit { prefs ->
            if (walletId == null || walletId <= 0L) {
                prefs.remove(defaultWalletIdKey)
            } else {
                prefs[defaultWalletIdKey] = walletId
            }
        }
    }

    suspend fun setDefaultFabTab(tabName: String) {
        context.sakuDataStore.edit { prefs ->
            prefs[defaultFabTabKey] = tabName
        }
    }

    suspend fun setBudgetAlertPercent(percent: Int) {
        context.sakuDataStore.edit { prefs ->
            prefs[budgetAlertPercentKey] = percent.coerceIn(50, 95)
        }
    }

    suspend fun setBillingCycleStartDay(day: Int) {
        context.sakuDataStore.edit { prefs ->
            prefs[billingCycleStartDayKey] = day.coerceIn(1, 28)
        }
    }

    suspend fun resetPersonalization() {
        context.sakuDataStore.edit { prefs ->
            prefs.remove(displayNameKey)
            prefs.remove(accentPresetKey)
            prefs.remove(cardCornerStyleKey)
            prefs.remove(showWalletsOnHomeKey)
            prefs.remove(showBudgetOnHomeKey)
            prefs.remove(compactCurrencyOnHomeKey)
            prefs.remove(homeShortcutsKey)
            prefs.remove(defaultWalletIdKey)
            prefs.remove(defaultFabTabKey)
            prefs.remove(budgetAlertPercentKey)
            prefs.remove(billingCycleStartDayKey)
        }
    }

    // --- App Lock Security (PBKDF2-HMAC-SHA256 Salted Hash) ---

    suspend fun setPinAndEnableLock(rawPin: String, biometricEnabled: Boolean = true) {
        val clean = rawPin.filter { it.isDigit() }
        require(clean.length in 4..6) { "PIN harus 4-6 digit" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPinBytes(clean, salt)
        context.sakuDataStore.edit { prefs ->
            prefs[pinSaltKey] = Base64.encodeToString(salt, Base64.NO_WRAP)
            prefs[pinHashKey] = Base64.encodeToString(hash, Base64.NO_WRAP)
            prefs[appLockEnabledKey] = true
            prefs[biometricEnabledKey] = biometricEnabled
        }
    }

    suspend fun verifyPin(rawPin: String): Boolean {
        val prefs = context.sakuDataStore.data.first()
        val saltB64 = prefs[pinSaltKey] ?: return false
        val hashB64 = prefs[pinHashKey] ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val candidate = hashPinBytes(rawPin.filter { it.isDigit() }, salt)
        return MessageDigest.isEqual(expected, candidate)
    }

    suspend fun enableBiometricLock() {
        context.sakuDataStore.edit { prefs ->
            prefs[biometricEnabledKey] = true
            prefs[appLockEnabledKey] = true
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.sakuDataStore.edit { prefs ->
            val hasPin = !prefs[pinHashKey].isNullOrBlank()
            val bioEnabled = prefs[biometricEnabledKey] ?: true
            prefs[appLockEnabledKey] = enabled && (hasPin || bioEnabled)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.sakuDataStore.edit { prefs ->
            prefs[biometricEnabledKey] = enabled
            val hasPin = !prefs[pinHashKey].isNullOrBlank()
            if (!enabled && !hasPin) {
                prefs[appLockEnabledKey] = false
            }
        }
    }

    suspend fun setLockOnBackground(enabled: Boolean) {
        context.sakuDataStore.edit { prefs ->
            prefs[lockOnBackgroundKey] = enabled
        }
    }

    suspend fun clearAppLock() {
        context.sakuDataStore.edit { prefs ->
            prefs[appLockEnabledKey] = false
            prefs.remove(pinHashKey)
            prefs.remove(pinSaltKey)
        }
    }

    private fun hashPinBytes(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    // --- Notifications ---

    suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.sakuDataStore.edit { prefs ->
            prefs[dailyReminderEnabledKey] = enabled
            prefs[reminderHourKey] = hour.coerceIn(0, 23)
            prefs[reminderMinuteKey] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setBudgetAlertNotifEnabled(enabled: Boolean) {
        context.sakuDataStore.edit { prefs ->
            prefs[budgetAlertNotifEnabledKey] = enabled
        }
    }

    suspend fun setLastSyncInfo(uid: String, timestamp: Long) {
        context.sakuDataStore.edit { prefs ->
            prefs[lastSyncedUidKey] = uid
            prefs[lastSyncAtKey] = timestamp
        }
    }

    suspend fun clearSyncInfo() {
        context.sakuDataStore.edit { prefs ->
            prefs.remove(lastSyncAtKey)
            prefs.remove(lastSyncedUidKey)
        }
    }

    // --- NFC E-Money Scan History ---

    suspend fun saveEmoneyScan(snapshot: EmoneyCardSnapshot): EmoneyCardRecord {
        var savedRecord: EmoneyCardRecord? = null
        context.sakuDataStore.edit { prefs ->
            val current = parseEmoneyHistory(prefs[emoneyHistoryJsonKey]).toMutableList()
            val existing = current.firstOrNull { it.uidHex == snapshot.uidHex }
            val previousBalance = when {
                existing == null -> null
                existing.balance != snapshot.balance -> existing.balance
                else -> existing.previousBalance
            }
            val updated = EmoneyCardRecord(
                uidHex = snapshot.uidHex,
                issuerName = snapshot.issuer.displayName,
                issuerBadge = snapshot.issuer.badgeText,
                cardNumber = snapshot.cardNumber,
                balance = snapshot.balance,
                previousBalance = previousBalance,
                accentHex = snapshot.issuer.accentHex,
                scannedAtMillis = snapshot.scannedAtMillis,
                sourceLabel = snapshot.techLabel.ifBlank { "Chip NFC" }
            )
            current.removeAll { it.uidHex == snapshot.uidHex }
            current.add(0, updated)
            prefs[emoneyHistoryJsonKey] = serializeEmoneyHistory(current.take(10))
            savedRecord = updated
        }
        return savedRecord!!
    }

    suspend fun deleteEmoneyRecord(uidHex: String) {
        context.sakuDataStore.edit { prefs ->
            val current = parseEmoneyHistory(prefs[emoneyHistoryJsonKey])
                .filterNot { it.uidHex == uidHex }
            prefs[emoneyHistoryJsonKey] = serializeEmoneyHistory(current)
        }
    }

    suspend fun clearEmoneyHistory() {
        context.sakuDataStore.edit { prefs ->
            prefs.remove(emoneyHistoryJsonKey)
        }
    }

    private fun parseEmoneyHistory(raw: String?): List<EmoneyCardRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        EmoneyCardRecord(
                            uidHex = obj.getString("uidHex"),
                            issuerName = obj.getString("issuerName"),
                            issuerBadge = obj.getString("issuerBadge"),
                            cardNumber = obj.getString("cardNumber"),
                            balance = obj.getLong("balance"),
                            previousBalance = if (obj.has("previousBalance") && !obj.isNull("previousBalance")) {
                                obj.getLong("previousBalance")
                            } else {
                                null
                            },
                            accentHex = obj.optLong("accentHex", 0xFF003D79L),
                            scannedAtMillis = obj.getLong("scannedAtMillis"),
                            sourceLabel = obj.optString("sourceLabel", "Chip NFC")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeEmoneyHistory(records: List<EmoneyCardRecord>): String {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("uidHex", r.uidHex)
                    put("issuerName", r.issuerName)
                    put("issuerBadge", r.issuerBadge)
                    put("cardNumber", r.cardNumber)
                    put("balance", r.balance)
                    if (r.previousBalance != null) {
                        put("previousBalance", r.previousBalance)
                    }
                    put("accentHex", r.accentHex)
                    put("scannedAtMillis", r.scannedAtMillis)
                    put("sourceLabel", r.sourceLabel)
                }
            )
        }
        return arr.toString()
    }
}
