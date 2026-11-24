package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSecurityPrefs
import com.example.data.local.NotificationPrefs
import com.example.data.remote.CloudSyncStatus
import com.example.notifications.SakuNotificationHelper
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.ObsidianSubScreenHeader
import com.example.ui.components.SakuBrandMark
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.theme.ThemeMode
import com.example.security.BiometricCapability
import com.example.security.BiometricStatusInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    syncStatus: CloudSyncStatus,
    themeMode: ThemeMode,
    accentLabel: String,
    displayName: String,
    hideBalance: Boolean,
    activeRecurringCount: Int,
    walletCount: Int,
    savingsGoalCount: Int,
    activeDebtCount: Int,
    securityPrefs: AppSecurityPrefs,
    biometricStatus: BiometricStatusInfo,
    notificationPrefs: NotificationPrefs,
    onBack: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetHideBalance: (Boolean) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenWallets: () -> Unit,
    onOpenSavingsGoals: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenExport: () -> Unit,
    onSetupPin: (pin: String, biometricEnabled: Boolean) -> Unit,
    onRequestToggleAppLock: (enable: Boolean, openPinDialogIfUnavailable: () -> Unit) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetLockOnBackground: (Boolean) -> Unit,
    onOpenBiometricEnrollment: () -> Unit,
    onClearAppLock: () -> Unit,
    onSetDailyReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onSetBudgetAlertNotif: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onRequestPinWidget: () -> Unit,
    onReopenOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val extended = LocalSakuColors.current
    val context = LocalContext.current
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pendingNotifAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingNotifAction?.invoke()
        }
        pendingNotifAction = null
    }

    fun runWithNotifPermission(action: () -> Unit) {
        if (SakuNotificationHelper.hasNotificationPermission(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            action()
        } else {
            pendingNotifAction = action
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val lastSyncLabel = remember(syncStatus.lastSyncAt) {
        val ts = syncStatus.lastSyncAt ?: return@remember "Belum pernah sinkron"
        val locale = Locale.Builder().setLanguage("in").setRegion("ID").build()
        val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", locale)
        "Terakhir sinkron: ${fmt.format(Date(ts))}"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        FreshAtmosphericBackdrop(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = LocalSakuTopPadding.current, bottom = 28.dp)
        ) {
            // 1. Clean Adaptive Header + Crisp Cloud Account Card
            item(key = "settings_hero_cloud") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ObsidianSubScreenHeader(
                        title = "Pengaturan",
                        subtitle = "Akun cloud, tampilan & privasi aplikasi",
                        onBack = onBack,
                        backTestTag = "settings_back_btn"
                    )

                    val statusTint = when {
                        syncStatus.isSyncing -> extended.lime
                        syncStatus.isSignedIn && syncStatus.errorMessage != null -> extended.expense
                        syncStatus.isSignedIn -> extended.income
                        else -> extended.periwinkle
                    }
                    val statusBg = when {
                        syncStatus.isSyncing -> extended.limeContainer
                        syncStatus.isSignedIn && syncStatus.errorMessage != null -> extended.expenseContainer
                        syncStatus.isSignedIn -> extended.incomeContainer
                        else -> extended.periwinkleContainer
                    }

                    FintechCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_cloud_card"),
                        cornerRadius = extended.cardRadius,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(statusBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (syncStatus.isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.2.dp,
                                            color = statusTint
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when {
                                                !syncStatus.isSignedIn -> Icons.Rounded.CloudOff
                                                syncStatus.errorMessage != null -> Icons.Rounded.CloudSync
                                                else -> Icons.Rounded.CloudDone
                                            },
                                            contentDescription = null,
                                            tint = statusTint
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (syncStatus.isSignedIn) {
                                            syncStatus.displayName ?: "Akun Google Terhubung"
                                        } else {
                                            "Mode Penyimpanan Lokal"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (syncStatus.isSignedIn) {
                                            syncStatus.email ?: "Sinkronisasi cloud aktif"
                                        } else {
                                            "Masuk Google untuk backup & sinkron antar perangkat"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = statusBg
                                ) {
                                    Text(
                                        text = when {
                                            syncStatus.isSyncing -> "Sinkron..."
                                            syncStatus.isSignedIn && syncStatus.pendingChangesCount > 0 -> "${syncStatus.pendingChangesCount} tertunda"
                                            syncStatus.isSignedIn -> "Aktif"
                                            else -> "Lokal"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = statusTint,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (!syncStatus.isSignedIn) {
                                Button(
                                    onClick = onGoogleSignIn,
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = extended.lime,
                                        contentColor = extended.onLime
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_google_sign_in_btn")
                                ) {
                                    Text("Masuk dengan Google", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "$lastSyncLabel • ${syncStatus.pendingChangesCount} perubahan tertunda",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                syncStatus.errorMessage?.let { err ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = err,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.expense
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onSyncNow,
                                        enabled = !syncStatus.isSyncing,
                                        shape = RoundedCornerShape(999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = extended.lime,
                                            contentColor = extended.onLime
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("settings_sync_now_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (syncStatus.isSyncing) "Menyinkronkan..." else "Sinkronkan",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = onSignOut,
                                        enabled = !syncStatus.isSyncing,
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(1.dp, extended.cardBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("settings_sign_out_btn")
                                    ) {
                                        Text("Keluar", maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Preferences, Links & App Info
            item(key = "settings_rising_sheet") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                        // Personalization Hero Card
                        FintechCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_personalization_btn"),
                            cornerRadius = extended.cardRadius,
                            onClick = onOpenPersonalization
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(extended.lime),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = extended.onLime,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Kustomisasi & Personalisasi",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    val summaryParts = buildList {
                                        if (displayName.isNotBlank()) add("Sapaan $displayName")
                                        add("Aksen $accentLabel")
                                        add("Pintasan & Siklus")
                                    }
                                    Text(
                                        text = summaryParts.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Appearance & Privacy
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = extended.cardRadius
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(extended.limeContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Palette,
                                        contentDescription = null,
                                        tint = extended.onLimeContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Tampilan Aplikasi",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    val activeModeText = if (extended.isDark) "Gelap" else "Terang"
                                    Text(
                                        text = if (themeMode == ThemeMode.SYSTEM) {
                                            "Aktif: $activeModeText (mengikuti sistem)"
                                        } else {
                                            "Mode aktif: $activeModeText"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_theme_picker")
                            ) {
                                ThemeMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = themeMode == mode,
                                        onClick = { onSetThemeMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = ThemeMode.entries.size
                                        )
                                    ) {
                                        Text(mode.label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSetHideBalance(!hideBalance) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sembunyikan Saldo di Beranda",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Samarkan total saldo & kas di layar utama",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = hideBalance,
                                    onCheckedChange = onSetHideBalance,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = extended.onLime,
                                        checkedTrackColor = extended.lime
                                    ),
                                    modifier = Modifier.testTag("settings_hide_balance_switch")
                                )
                            }
                        }

                        // Security & Biometric App Lock
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = extended.cardRadius
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(extended.limeContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = null,
                                        tint = extended.onLimeContainer,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Kunci Biometrik & PIN",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    val lockSummary = when {
                                        securityPrefs.appLockEnabled && securityPrefs.biometricEnabled && securityPrefs.hasPin ->
                                            "Aktif saat buka aplikasi • Biometrik + PIN Saku"
                                        securityPrefs.appLockEnabled && securityPrefs.biometricEnabled ->
                                            "Aktif saat buka aplikasi • Biometrik perangkat"
                                        securityPrefs.appLockEnabled && securityPrefs.hasPin ->
                                            "Aktif saat buka aplikasi • PIN Saku saja"
                                        else ->
                                            "Verifikasi sidik jari/wajah saat Saku dibuka"
                                    }
                                    Text(
                                        text = lockSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = securityPrefs.appLockEnabled,
                                    onCheckedChange = { enable ->
                                        onRequestToggleAppLock(enable) {
                                            showPinSetupDialog = true
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = extended.onLime,
                                        checkedTrackColor = extended.lime
                                    ),
                                    modifier = Modifier.testTag("settings_app_lock_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (biometricStatus.canAuthenticateAtAll) extended.income else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = biometricStatus.statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (biometricStatus.capability == BiometricCapability.NOT_ENROLLED ||
                                        biometricStatus.capability == BiometricCapability.DEVICE_CREDENTIAL_ONLY
                                    ) {
                                        TextButton(
                                            onClick = onOpenBiometricEnrollment,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("Daftarkan", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = securityPrefs.appLockEnabled) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSetLockOnBackground(!securityPrefs.lockOnBackground) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Kunci Otomatis Saat Keluar",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                            )
                                            Text(
                                                text = "Minta verifikasi ulang setelah aplikasi ditutup sementara",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = securityPrefs.lockOnBackground,
                                            onCheckedChange = onSetLockOnBackground,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = extended.onLime,
                                                checkedTrackColor = extended.lime
                                            )
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = securityPrefs.hasPin) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSetBiometricEnabled(!securityPrefs.biometricEnabled) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Fingerprint,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Text(
                                            text = "Buka Cepat dengan Biometrik",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = securityPrefs.biometricEnabled,
                                            onCheckedChange = onSetBiometricEnabled,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = extended.onLime,
                                                checkedTrackColor = extended.lime
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { showPinSetupDialog = true },
                                            shape = RoundedCornerShape(999.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Text("Ubah PIN")
                                        }
                                        TextButton(
                                            onClick = onClearAppLock,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Text("Hapus PIN", color = extended.expense)
                                        }
                                    }
                                }
                            }

                            if (!securityPrefs.hasPin) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { showPinSetupDialog = true },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_setup_pin_btn")
                                ) {
                                    Text("Tambah PIN Cadangan (Opsional)")
                                }
                            }
                        }

                        // Daily Reminder & Smart Notifications
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = extended.cardRadius
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(extended.periwinkleContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (extended.isDark) extended.periwinkle else Color(0xFF4338CA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pengingat Harian & Peringatan",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    val timeStr = String.format(Locale.US, "%02d:%02d", notificationPrefs.reminderHour, notificationPrefs.reminderMinute)
                                    Text(
                                        text = if (notificationPrefs.dailyReminderEnabled) {
                                            "Pengingat aktif setiap pukul $timeStr"
                                        } else {
                                            "Pengingat catat transaksi otomatis setiap hari"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = notificationPrefs.dailyReminderEnabled,
                                    onCheckedChange = { enable ->
                                        if (enable) {
                                            runWithNotifPermission {
                                                onSetDailyReminder(true, notificationPrefs.reminderHour, notificationPrefs.reminderMinute)
                                            }
                                        } else {
                                            onSetDailyReminder(false, notificationPrefs.reminderHour, notificationPrefs.reminderMinute)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = extended.onLime,
                                        checkedTrackColor = extended.lime
                                    ),
                                    modifier = Modifier.testTag("settings_daily_reminder_switch")
                                )
                            }

                            AnimatedVisibility(visible = notificationPrefs.dailyReminderEnabled) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Text(
                                        text = "Jam Pengingat",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            (8 to 0) to "08:00",
                                            (12 to 30) to "12:30",
                                            (20 to 0) to "20:00",
                                            (21 to 30) to "21:30"
                                        ).forEach { (hm, lbl) ->
                                            val sel = notificationPrefs.reminderHour == hm.first && notificationPrefs.reminderMinute == hm.second
                                            FilterChip(
                                                selected = sel,
                                                onClick = { onSetDailyReminder(true, hm.first, hm.second) },
                                                label = { Text(lbl) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val next = !notificationPrefs.budgetAlertNotifEnabled
                                        if (next) {
                                            runWithNotifPermission { onSetBudgetAlertNotif(true) }
                                        } else {
                                            onSetBudgetAlertNotif(false)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Peringatan Anggaran & Jatuh Tempo",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Beri tahu saat anggaran menipis atau hutang jatuh tempo",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = notificationPrefs.budgetAlertNotifEnabled,
                                    onCheckedChange = { next ->
                                        if (next) {
                                            runWithNotifPermission { onSetBudgetAlertNotif(true) }
                                        } else {
                                            onSetBudgetAlertNotif(false)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = extended.onLime,
                                        checkedTrackColor = extended.lime
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { runWithNotifPermission { onSendTestNotification() } },
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_test_notif_btn")
                            ) {
                                Text("Kirim Notifikasi Uji")
                            }
                        }

                        // Quick Management Links
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 14.dp)
                        ) {
                            SettingsNavigationRow(
                                icon = Icons.Rounded.Savings,
                                iconTint = extended.onLimeContainer,
                                iconBg = extended.limeContainer,
                                title = "Target Tabungan",
                                subtitle = "$savingsGoalCount target tabungan dikelola",
                                onClick = onOpenSavingsGoals,
                                modifier = Modifier.testTag("settings_savings_btn")
                            )
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            SettingsNavigationRow(
                                icon = Icons.Rounded.Payments,
                                iconTint = if (extended.isDark) extended.periwinkle else Color(0xFF4338CA),
                                iconBg = extended.periwinkleContainer,
                                title = "Hutang & Piutang",
                                subtitle = "$activeDebtCount catatan belum lunas",
                                onClick = onOpenDebts,
                                modifier = Modifier.testTag("settings_debts_btn")
                            )
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            SettingsNavigationRow(
                                icon = Icons.Rounded.Download,
                                iconTint = extended.onLimeContainer,
                                iconBg = extended.limeContainer,
                                title = "Ekspor Mutasi Bank (PDF / CSV)",
                                subtitle = "Cetak laporan e-Statement dengan saldo berjalan",
                                onClick = onOpenExport,
                                modifier = Modifier.testTag("settings_export_btn")
                            )
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            SettingsNavigationRow(
                                icon = Icons.Rounded.Widgets,
                                iconTint = if (extended.isDark) extended.periwinkle else Color(0xFF4338CA),
                                iconBg = extended.periwinkleContainer,
                                title = "Galeri Widget Layar Utama",
                                subtitle = "4 variasi: Saldo, Catat Cepat, Anggaran & Kartu Mini",
                                onClick = onRequestPinWidget,
                                modifier = Modifier.testTag("settings_widget_btn")
                            )
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            SettingsNavigationRow(
                                icon = Icons.Rounded.Repeat,
                                iconTint = if (extended.isDark) extended.periwinkle else Color(0xFF4338CA),
                                iconBg = extended.periwinkleContainer,
                                title = "Transaksi Berulang",
                                subtitle = "$activeRecurringCount jadwal otomatis aktif",
                                onClick = onOpenRecurring,
                                modifier = Modifier.testTag("settings_recurring_btn")
                            )
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            SettingsNavigationRow(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                iconTint = extended.onLimeContainer,
                                iconBg = extended.limeContainer,
                                title = "Dompet & Rekening",
                                subtitle = "$walletCount dompet terkelola",
                                onClick = onOpenWallets,
                                modifier = Modifier.testTag("settings_wallets_btn")
                            )
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            SettingsNavigationRow(
                                icon = Icons.Rounded.RestartAlt,
                                iconTint = extended.expense,
                                iconBg = extended.expenseContainer,
                                title = "Mulai Ulang / Bersihkan ke Nol",
                                subtitle = "Buka wizard awal untuk bikin dompet baru dari nol",
                                onClick = onReopenOnboarding,
                                modifier = Modifier.testTag("settings_reopen_onboarding_btn")
                            )
                        }

                        // App Info Footer
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SakuBrandMark(
                                    size = 44.dp,
                                    cornerRadius = 13.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Saku",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = extended.limeContainer
                                        ) {
                                            Text(
                                                text = "v1.0",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = extended.onLimeContainer,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Personal Finance • Offline-first & Cloud Sync",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    if (showPinSetupDialog) {
        SetupPinDialog(
            initialBiometricEnabled = securityPrefs.biometricEnabled,
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin, biometric ->
                onSetupPin(pin, biometric)
                showPinSetupDialog = false
            }
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
