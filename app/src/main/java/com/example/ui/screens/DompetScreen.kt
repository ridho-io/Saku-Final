package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WalletEntity
import com.example.data.remote.CloudSyncStatus
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.ObsidianCircleIconButton
import com.example.ui.components.SakuBottomDrawer
import com.example.ui.components.SakuBrandMark
import com.example.ui.components.SakuDebitStyleWalletCard
import com.example.ui.theme.LocalSakuBottomPadding
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DompetScreen(
    wallets: List<WalletEntity>,
    activeRecurringCount: Int,
    syncStatus: CloudSyncStatus,
    onGoogleSignIn: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEmoneyNfc: () -> Unit,
    onTransfer: (Long, Long, Long, String) -> Unit,
    onAddWallet: (String, String, Long, Long, String) -> Unit,
    onUpdateWallet: (WalletEntity) -> Unit,
    onCountWalletTransactions: suspend (Long) -> Int,
    onDeleteWallet: (WalletEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<WalletEntity?>(null) }
    var deletingWallet by remember { mutableStateOf<WalletEntity?>(null) }
    var deleteTxCount by remember { mutableIntStateOf(0) }

    val totalAssets = remember(wallets) { wallets.sumOf { it.balance } }
    val carouselState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = carouselState)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        FreshAtmosphericBackdrop(modifier = Modifier.height(210.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(top = LocalSakuTopPadding.current, bottom = LocalSakuBottomPadding.current),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Clean Top Header Bar (Avatar + "Dompet Saya" + Add & Settings Circle Buttons)
            item(key = "dompet_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SakuBrandMark(size = 40.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Dompet Saya",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Total aset ${CurrencyFormatter.formatRupiah(totalAssets)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ObsidianCircleIconButton(
                            icon = Icons.Rounded.Add,
                            contentDescription = "Tambah dompet",
                            onClick = { showAddWalletDialog = true },
                            isActive = true,
                            modifier = Modifier.testTag("add_wallet_btn")
                        )
                        ObsidianCircleIconButton(
                            icon = Icons.Rounded.Settings,
                            contentDescription = "Pengaturan",
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("dompet_open_settings_btn")
                        )
                    }
                }
            }

            // 2. Swipeable Crisp White Main Wallet Card Deck (Matching Reference Screen 3)
            item(key = "dompet_card_carousel") {
                LazyRow(
                    state = carouselState,
                    flingBehavior = snapBehavior,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(wallets, key = { _, w -> w.id }) { idx, wallet ->
                        SakuDebitStyleWalletCard(
                            wallet = wallet,
                            cardIndex = idx,
                            compact = false,
                            onClick = { editingWallet = wallet },
                            trailingAction = {
                                Surface(
                                    onClick = { editingWallet = wallet },
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.20f),
                                    border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.28f))
                                ) {
                                    Box(
                                        modifier = Modifier.size(34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit dompet ${wallet.name}",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.width(if (wallets.size == 1) 324.dp else 298.dp)
                        )
                    }
                }
            }

            // 3. 4-Column Pastel Wallet Quick Actions (Matching Screen 3's Add/Withdraw/Transfer/Request Strip)
            item(key = "dompet_quick_actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    DompetActionTile(
                        icon = Icons.Rounded.Add,
                        label = "Tambah",
                        containerColor = extended.lime,
                        iconTint = extended.onLime,
                        onClick = { showAddWalletDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    DompetActionTile(
                        icon = Icons.Rounded.SwapHoriz,
                        label = "Transfer",
                        containerColor = extended.limeContainer,
                        iconTint = extended.onLimeContainer,
                        enabled = wallets.size >= 2,
                        onClick = { if (wallets.size >= 2) showTransferDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transfer_wallets_btn")
                    )
                    DompetActionTile(
                        icon = Icons.Rounded.EventRepeat,
                        label = if (activeRecurringCount > 0) "Rutin ($activeRecurringCount)" else "Berulang",
                        containerColor = extended.periwinkleContainer,
                        iconTint = extended.periwinkle,
                        onClick = onOpenRecurring,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dompet_recurring_card")
                    )
                    DompetActionTile(
                        icon = Icons.Rounded.Contactless,
                        label = "Cek NFC",
                        containerColor = extended.incomeContainer,
                        iconTint = extended.income,
                        onClick = onOpenEmoneyNfc,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dompet_emoney_nfc_card")
                    )
                }
            }

            // 4. Allocation & Wallet List Card
            if (wallets.isNotEmpty()) {
                item(key = "dompet_allocation") {
                    WalletAllocationAndListSection(
                        wallets = wallets,
                        totalAssets = totalAssets,
                        onWalletClick = { editingWallet = it },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            // 5. Cloud Sync Account Card
            item(key = "dompet_cloud_sync") {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    CloudSyncAccountCard(
                        syncStatus = syncStatus,
                        onGoogleSignIn = onGoogleSignIn,
                        onSyncNow = onSyncNow,
                        onSignOut = onSignOut
                    )
                }
            }

            // 6. Clean link to SettingsScreen
            item(key = "dompet_settings_link") {
                FintechCard(
                    onClick = onOpenSettings,
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengaturan Tampilan & Keamanan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tema, biometrik, pengingat & personalisasi",
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
            }
        }
    }

    if (showTransferDialog && wallets.size >= 2) {
        TransferDialog(
            wallets = wallets,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amount, note ->
                onTransfer(fromId, toId, amount, note)
                showTransferDialog = false
            }
        )
    }

    if (showAddWalletDialog) {
        WalletFormDialog(
            editing = null,
            canDelete = false,
            onDismiss = { showAddWalletDialog = false },
            onDeleteRequest = {},
            onSave = { name, type, balance, hint, accentHex ->
                onAddWallet(name, type, balance, accentHex, hint)
                showAddWalletDialog = false
            }
        )
    }

    editingWallet?.let { wallet ->
        WalletFormDialog(
            editing = wallet,
            canDelete = wallets.size > 1,
            onDismiss = { editingWallet = null },
            onDeleteRequest = {
                editingWallet = null
                deletingWallet = wallet
            },
            onSave = { name, type, balance, hint, accentHex ->
                onUpdateWallet(
                    wallet.copy(
                        name = name,
                        walletType = type,
                        balance = balance,
                        accentHex = accentHex,
                        accountNumberHint = hint
                    )
                )
                editingWallet = null
            }
        )
    }

    deletingWallet?.let { wallet ->
        LaunchedEffect(wallet.id) {
            deleteTxCount = onCountWalletTransactions(wallet.id)
        }
        SakuBottomDrawer(
            title = "Hapus Dompet ${wallet.name}?",
            onDismiss = { deletingWallet = null }
        ) {
            Text(
                text = if (deleteTxCount > 0) {
                    "Menghapus dompet ini juga akan menghapus $deleteTxCount transaksi terkait secara permanen."
                } else {
                    "Dompet \"${wallet.name}\" akan dihapus secara permanen."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { deletingWallet = null },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                ) {
                    Text("Batal", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        onDeleteWallet(wallet)
                        deletingWallet = null
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extended.expense,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                ) {
                    Text("Hapus Dompet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DompetActionTile(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val extended = LocalSakuColors.current
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            border = BorderStroke(
                1.dp,
                if (extended.isDark) iconTint.copy(alpha = 0.28f) else extended.cardBorder
            ),
            modifier = Modifier.size(56.dp),
            shadowElevation = if (extended.isDark) 0.dp else 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WalletAllocationAndListSection(
    wallets: List<WalletEntity>,
    totalAssets: Long,
    onWalletClick: (WalletEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val positiveTotal = wallets.filter { it.balance > 0L }.sumOf { it.balance }
    val paletteColors = listOf(
        extended.lime,
        extended.periwinkle,
        extended.income,
        extended.warning
    )

    FintechCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alokasi & Rincian (${wallets.size} Dompet)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = CurrencyFormatter.formatCompactRupiah(totalAssets),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = extended.lime,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (positiveTotal > 0L) {
                wallets.filter { it.balance > 0L }.forEachIndexed { idx, wallet ->
                    val fraction = (wallet.balance.toFloat() / positiveTotal.toFloat()).coerceAtLeast(0.03f)
                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .height(8.dp)
                            .background(paletteColors[idx % paletteColors.size])
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            wallets.forEachIndexed { idx, wallet ->
                val badgeColor = paletteColors[idx % paletteColors.size]
                val pct = if (positiveTotal > 0L && wallet.balance > 0L) {
                    ((wallet.balance.toFloat() / positiveTotal.toFloat()) * 100f).roundToInt()
                } else 0
                val typeIcon = when (wallet.walletType) {
                    "Bank" -> Icons.Rounded.AccountBalance
                    "E-Wallet" -> Icons.Rounded.AccountBalanceWallet
                    else -> Icons.Rounded.Payments
                }

                if (idx > 0) {
                    HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onWalletClick(wallet) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = if (extended.isDark) 0.22f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = wallet.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val cleanHint = wallet.accountNumberHint.trim()
                        val subtitleLabel = if (cleanHint.isBlank() || cleanHint.equals(wallet.walletType, ignoreCase = true)) {
                            "${wallet.walletType} • $pct%"
                        } else {
                            "${wallet.walletType} • ${cleanHint.takeLast(6)} • $pct%"
                        }
                        Text(
                            text = subtitleLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = CurrencyFormatter.formatRupiah(wallet.balance),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudSyncAccountCard(
    syncStatus: CloudSyncStatus,
    onGoogleSignIn: () -> Unit,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit
) {
    val extended = LocalSakuColors.current
    val lastSyncLabel = remember(syncStatus.lastSyncAt) {
        val ts = syncStatus.lastSyncAt ?: return@remember "Belum pernah sinkron"
        val locale = Locale.Builder().setLanguage("in").setRegion("ID").build()
        val fmt = SimpleDateFormat("dd MMM, HH:mm", locale)
        "Terakhir sinkron: ${fmt.format(Date(ts))}"
    }

    val statusTint = when {
        syncStatus.isSyncing -> extended.lime
        syncStatus.isSignedIn && syncStatus.errorMessage != null -> extended.expense
        syncStatus.isSignedIn -> extended.income
        else -> extended.periwinkle
    }
    val statusBg = when {
        syncStatus.isSignedIn && syncStatus.errorMessage != null -> extended.expenseContainer
        syncStatus.isSignedIn -> extended.incomeContainer
        else -> extended.periwinkleContainer
    }

    FintechCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dompet_cloud_sync_card"),
        cornerRadius = 22.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusBg),
                contentAlignment = Alignment.Center
            ) {
                if (syncStatus.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
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
                        tint = statusTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sinkronisasi Cloud",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (syncStatus.isSignedIn) {
                        syncStatus.email ?: syncStatus.displayName ?: "Akun Google terhubung"
                    } else {
                        "Tersimpan lokal offline • Backup dengan Google"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
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

        Spacer(modifier = Modifier.height(12.dp))

        if (!syncStatus.isSignedIn) {
            if (syncStatus.pendingChangesCount > 0) {
                Text(
                    text = "${syncStatus.pendingChangesCount} data lokal siap disinkronkan setelah masuk.",
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.income
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
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
                    .testTag("google_sign_in_btn")
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
                        .testTag("sync_now_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (syncStatus.isSyncing) "Menyinkronkan..." else "Sinkronkan", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSignOut,
                    enabled = !syncStatus.isSyncing,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("google_sign_out_btn")
                ) {
                    Text("Keluar")
                }
            }
        }
    }
}

@Composable
private fun TransferDialog(
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Long, String) -> Unit
) {
    val extended = LocalSakuColors.current
    var fromId by remember { mutableStateOf(wallets.first().id) }
    var toId by remember { mutableStateOf(wallets.getOrNull(1)?.id ?: wallets.first().id) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amount = amountText.toLongOrNull() ?: 0L

    SakuBottomDrawer(
        title = "Transfer Antar Dompet",
        subtitle = "Pindahkan saldo antar rekening atau e-wallet secara instan",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Dari Dompet",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wallets.forEach { w ->
                    FilterChip(
                        selected = fromId == w.id,
                        onClick = {
                            fromId = w.id
                            if (toId == fromId) {
                                toId = wallets.firstOrNull { it.id != fromId }?.id ?: fromId
                            }
                        },
                        label = { Text(w.name) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Ke Dompet",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wallets.filter { it.id != fromId }.forEach { w ->
                    FilterChip(
                        selected = toId == w.id,
                        onClick = { toId = w.id },
                        label = { Text(w.name) }
                    )
                }
            }
        }

        val sourceWallet = wallets.firstOrNull { it.id == fromId }
        if (sourceWallet != null) {
            Text(
                text = "Saldo ${sourceWallet.name}: ${CurrencyFormatter.formatRupiah(sourceWallet.balance)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { raw -> amountText = raw.filter { it.isDigit() } },
            label = { Text("Nominal Transfer (Rp)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (sourceWallet != null && amount > sourceWallet.balance) {
            Text(
                text = "Melebihi saldo ${sourceWallet.name} saat ini.",
                style = MaterialTheme.typography.bodySmall,
                color = extended.expense
            )
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Catatan (opsional)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, extended.cardBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Batal", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onConfirm(fromId, toId, amount, note) },
                enabled = amount > 0L && fromId != toId,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Transfer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val walletCardPaletteOptions = listOf(
    0xFF1D56F5L to "Cobalt",
    0xFF7C5DFAL to "Violet",
    0xFF10B981L to "Emerald",
    0xFFF59E0BL to "Amber",
    0xFFEF4444L to "Coral",
    0xFF1F232CL to "Navy"
)

@Composable
private fun WalletFormDialog(
    editing: WalletEntity?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onDeleteRequest: () -> Unit,
    onSave: (String, String, Long, String, Long) -> Unit
) {
    val extended = LocalSakuColors.current
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var walletType by remember { mutableStateOf(editing?.walletType ?: "Bank") }
    var balanceText by remember { mutableStateOf(editing?.balance?.toString() ?: "0") }
    var hint by remember { mutableStateOf(editing?.accountNumberHint ?: "") }
    var selectedAccentHex by remember {
        mutableStateOf(
            editing?.accentHex?.takeIf { hex -> walletCardPaletteOptions.any { it.first == hex } }
                ?: when (walletType) {
                    "E-Wallet" -> 0xFF7C5DFAL
                    "Bank" -> 0xFF1D56F5L
                    else -> 0xFF10B981L
                }
        )
    }
    val balance = balanceText.toLongOrNull() ?: 0L

    SakuBottomDrawer(
        title = if (editing != null) "Edit Dompet" else "Tambah Dompet Baru",
        subtitle = if (editing != null) {
            "Perbarui nama, saldo, atau warna kartu dompetmu"
        } else {
            "Catat rekening bank, e-wallet, atau uang tunai"
        },
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama Dompet (cth: Mandiri, OVO)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Jenis Dompet",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Bank", "E-Wallet", "Cash").forEach { type ->
                    FilterChip(
                        selected = walletType == type,
                        onClick = { walletType = type },
                        label = { Text(type) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Warna Aksen Dompet",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                walletCardPaletteOptions.forEach { (hex, label) ->
                    val isSelected = selectedAccentHex == hex
                    val swatchColor = Color(hex)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 2.5.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier.border(
                                        width = 0.8.dp,
                                        color = extended.cardBorder,
                                        shape = CircleShape
                                    )
                                }
                            )
                            .clickable { selectedAccentHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = label,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = balanceText,
            onValueChange = { raw -> balanceText = raw.filter { it.isDigit() } },
            label = { Text("Saldo (Rp)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = hint,
            onValueChange = { hint = it },
            label = { Text("Keterangan / 4 Digit Terakhir") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (editing != null && canDelete) {
            TextButton(
                onClick = onDeleteRequest,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = extended.expense
                ),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hapus Dompet Ini", fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, extended.cardBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Batal", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onSave(name.trim(), walletType, balance, hint.trim(), selectedAccentHex) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

