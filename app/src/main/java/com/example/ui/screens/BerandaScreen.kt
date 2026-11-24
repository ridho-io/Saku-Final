package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.HomeShortcutOption
import com.example.data.local.PersonalizationPrefs
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.remote.CloudSyncStatus
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.GroupedTransactionDayCard
import com.example.ui.components.ObsidianCircleIconButton
import com.example.ui.components.ObsidianHeroCard
import com.example.ui.components.SakuBottomDrawer
import com.example.ui.components.SakuBrandMark
import com.example.ui.components.SakuDebitStyleWalletCard
import com.example.ui.components.SakuProfileAvatar
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.LocalSakuBottomPadding
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BerandaUiState
import com.example.util.CurrencyFormatter
import java.util.Calendar

@Composable
fun BerandaScreen(
    uiState: BerandaUiState,
    personalization: PersonalizationPrefs,
    activeRecurringCount: Int,
    syncStatus: CloudSyncStatus,
    hideBalance: Boolean,
    onToggleHideBalance: () -> Unit,
    onSyncBadgeClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenManual: () -> Unit,
    onOpenAnalisis: () -> Unit,
    onOpenAnggaran: () -> Unit,
    onOpenWallets: () -> Unit,
    onOpenMonthlyOverview: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSavingsGoals: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenEmoneyNfc: () -> Unit,
    onOpenCalendar: () -> Unit,
    onSelectWallet: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (String?) -> Unit,
    onOpenAi: () -> Unit,
    onQuickVoice: () -> Unit,
    onQuickScan: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current
    var searchExpanded by remember { mutableStateOf(false) }
    var moreShortcutsExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val totalDisplayedTxCount = remember(uiState.groupedTransactions) {
        uiState.groupedTransactions.sumOf { it.second.size }
    }
    val topTxId = uiState.groupedTransactions.firstOrNull()?.second?.firstOrNull()?.entity?.id
    var lastSeenTopTxId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(topTxId) {
        if (topTxId != null && lastSeenTopTxId != null && topTxId != lastSeenTopTxId) {
            val maxIdx = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            listState.animateScrollToItem(1.coerceAtMost(maxIdx))
        }
        if (topTxId != null) {
            lastSeenTopTxId = topTxId
        }
    }

    val baseGreeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..10 -> "Selamat Pagi,"
            in 11..14 -> "Selamat Siang,"
            in 15..18 -> "Selamat Sore,"
            else -> "Selamat Malam,"
        }
    }
    val displayUserTitle = remember(personalization.displayName) {
        personalization.displayName.trim().ifBlank { "Saku Keuangan" }
    }

    val isSearchingOrFiltering = uiState.searchQuery.isNotBlank() || uiState.typeFilter != null
    val displayedGroups = if (isSearchingOrFiltering) {
        uiState.groupedTransactions
    } else {
        var remainingBudget = 6
        uiState.groupedTransactions.mapNotNull { (header, dayList) ->
            if (remainingBudget <= 0) {
                null
            } else {
                val sliced = dayList.take(remainingBudget)
                remainingBudget -= sliced.size
                header to sliced
            }
        }
    }
    val shownTxCount = displayedGroups.sumOf { it.second.size }
    val hasMoreHistory = totalDisplayedTxCount > shownTxCount

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        FreshAtmosphericBackdrop(modifier = Modifier.height(240.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(top = LocalSakuTopPadding.current, bottom = LocalSakuBottomPadding.current),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Clean Top Header Bar (Avatar + Greeting + Search/Settings Circle Buttons)
            item(key = "top_header") {
                BerandaTopBar(
                    greeting = baseGreeting,
                    userName = displayUserTitle,
                    syncStatus = syncStatus,
                    searchExpanded = searchExpanded,
                    onAvatarClick = onOpenPersonalization,
                    onSyncBadgeClick = onSyncBadgeClick,
                    onToggleSearch = {
                        if (searchExpanded) {
                            searchExpanded = false
                            onSearchQueryChange("")
                            onTypeFilterChange(null)
                        } else {
                            searchExpanded = true
                        }
                    },
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // 2. Vibrant Cobalt Gradient Hero Card (Matching Reference Screen 1)
            item(key = "cobalt_hero_card") {
                val walletName = if (uiState.selectedWalletId == null) {
                    "Semua Dompet"
                } else {
                    uiState.wallets.firstOrNull { it.id == uiState.selectedWalletId }?.name ?: "Dompet"
                }
                ObsidianHeroCard(
                    netBalance = uiState.netBalance,
                    walletLabel = walletName,
                    monthLabel = uiState.monthLabel,
                    monthlyIncome = null,
                    monthlyExpense = null,
                    hideBalance = hideBalance,
                    onToggleHideBalance = onToggleHideBalance,
                    heroActionsSlot = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            personalization.homeShortcuts.forEachIndexed { idx, shortcut ->
                                val isPrimaryPill = idx == 0
                                val (icon, tag, action) = when (shortcut) {
                                    HomeShortcutOption.AI -> Triple(Icons.Rounded.AutoAwesome, "shortcut_ai", onOpenAi)
                                    HomeShortcutOption.VOICE -> Triple(Icons.Rounded.Mic, "shortcut_voice", onQuickVoice)
                                    HomeShortcutOption.SCAN -> Triple(Icons.Rounded.CameraAlt, "shortcut_scan", onQuickScan)
                                    HomeShortcutOption.MANUAL -> Triple(Icons.Rounded.Calculate, "shortcut_manual", onOpenManual)
                                    HomeShortcutOption.HISTORY -> Triple(Icons.AutoMirrored.Rounded.ReceiptLong, "shortcut_history", onOpenHistory)
                                    HomeShortcutOption.BUDGET -> Triple(Icons.Rounded.PieChart, "shortcut_anggaran", onOpenAnggaran)
                                    HomeShortcutOption.WALLET -> Triple(Icons.Rounded.AccountBalanceWallet, "shortcut_dompet", onOpenWallets)
                                    HomeShortcutOption.SAVINGS -> Triple(Icons.Rounded.Savings, "shortcut_savings", onOpenSavingsGoals)
                                    HomeShortcutOption.DEBTS -> Triple(Icons.Rounded.Handshake, "shortcut_debts", onOpenDebts)
                                    HomeShortcutOption.OVERVIEW -> Triple(Icons.Rounded.BarChart, "shortcut_monthly_overview", onOpenMonthlyOverview)
                                    HomeShortcutOption.NFC_EMONEY -> Triple(Icons.Rounded.Contactless, "shortcut_nfc_emoney", onOpenEmoneyNfc)
                                    HomeShortcutOption.CALENDAR -> Triple(Icons.Rounded.CalendarMonth, "shortcut_calendar", onOpenCalendar)
                                }
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        action()
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isPrimaryPill) Color.White else Color.White.copy(alpha = 0.16f),
                                    border = if (isPrimaryPill) null else BorderStroke(0.9.dp, Color.White.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 42.dp)
                                        .testTag(tag)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isPrimaryPill) extended.heroMid else Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = shortcut.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 12.sp,
                                                letterSpacing = (-0.15).sp,
                                                fontWeight = if (isPrimaryPill) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = if (isPrimaryPill) extended.heroMid else Color.White,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // 3. Pemasukan & Pengeluaran (Two Clean Standalone Cards Side-by-Side)
            item(key = "cashflow_summary_cards") {
                val incomeText = if (hideBalance) "Rp ••••" else CurrencyFormatter.formatRupiah(uiState.monthlyIncome)
                val expenseText = if (hideBalance) "Rp ••••" else CurrencyFormatter.formatRupiah(uiState.monthlyExpense)
                val incomeFontSize = when {
                    incomeText.length >= 16 -> 13.sp
                    incomeText.length >= 12 -> 14.5.sp
                    else -> 16.5.sp
                }
                val expenseFontSize = when {
                    expenseText.length >= 16 -> 13.sp
                    expenseText.length >= 12 -> 14.5.sp
                    else -> 16.5.sp
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("beranda_cashflow_summary"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FintechCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 22.dp,
                        contentPadding = PaddingValues(14.dp),
                        onClick = onOpenAnalisis
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(extended.incomeContainer)
                                    .border(
                                        width = 0.8.dp,
                                        color = extended.income.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SouthWest,
                                    contentDescription = "Pemasukan",
                                    tint = extended.income,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = extended.incomeContainer
                            ) {
                                Text(
                                    text = if (uiState.monthlyIncomeCount > 0) "${uiState.monthlyIncomeCount}x" else "Masuk",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = extended.income,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Pemasukan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = incomeText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = incomeFontSize,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = extended.income,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FintechCard(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 22.dp,
                        contentPadding = PaddingValues(14.dp),
                        onClick = onOpenAnalisis
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(extended.expenseContainer)
                                    .border(
                                        width = 0.8.dp,
                                        color = extended.expense.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NorthEast,
                                    contentDescription = "Pengeluaran",
                                    tint = extended.expense,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = extended.expenseContainer
                            ) {
                                Text(
                                    text = if (uiState.monthlyExpenseCount > 0) "${uiState.monthlyExpenseCount}x" else "Keluar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = extended.expense,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Pengeluaran",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = expenseText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = expenseFontSize,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = extended.expense,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Quick Actions (Cohesive Card + Interactive Header + More Bottom Drawer)
            item(key = "quick_actions") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aksi Cepat",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    moreShortcutsExpanded = true
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Semua Fitur",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Lihat semua fitur",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    FintechCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            BerandaPastelActionTile(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                label = "Dompet",
                                containerColor = extended.limeContainer,
                                iconTint = extended.onLimeContainer,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenWallets()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qa_dompet")
                            )

                            BerandaPastelActionTile(
                                icon = Icons.Rounded.Contactless,
                                label = "Cek NFC",
                                containerColor = extended.periwinkleContainer,
                                iconTint = extended.periwinkle,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenEmoneyNfc()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qa_nfc_emoney")
                            )

                            BerandaPastelActionTile(
                                icon = Icons.Rounded.CalendarMonth,
                                label = "Kalender",
                                containerColor = extended.incomeContainer,
                                iconTint = extended.income,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenCalendar()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qa_calendar")
                            )

                            BerandaPastelActionTile(
                                icon = Icons.Rounded.GridView,
                                label = "Lainnya",
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (extended.isDark) 0.55f else 0.72f),
                                iconTint = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    moreShortcutsExpanded = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("beranda_more_shortcuts_btn")
                            )
                        }
                    }
                }
            }

            // 5. Horizontal Wallet Deck
            if (personalization.showWalletsOnHome && uiState.wallets.isNotEmpty()) {
                item(key = "wallet_deck") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Dompet Saya",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (uiState.selectedWalletId != null) {
                                    Surface(
                                        onClick = { onSelectWallet(null) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = extended.limeContainer,
                                        modifier = Modifier.testTag("wallet_filter_all")
                                    ) {
                                        Text(
                                            text = "Semua ×",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = extended.onLimeContainer,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onOpenWallets()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("beranda_add_wallet_link"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Kelola",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Kelola Dompet",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(uiState.wallets, key = { _, w -> w.id }) { idx, wallet ->
                                val selected = uiState.selectedWalletId == wallet.id
                                SakuDebitStyleWalletCard(
                                    wallet = wallet,
                                    cardIndex = idx,
                                    isSelected = selected,
                                    compact = true,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectWallet(if (selected) null else wallet.id)
                                    },
                                    modifier = Modifier
                                        .width(if (uiState.wallets.size == 1) 316.dp else 238.dp)
                                        .testTag("wallet_filter_${wallet.id}")
                                )
                            }
                        }
                    }
                }
            }

            // 6. Recent Transactions Section (Crisp White Cards Matching Screen 1)
            item(key = "recent_transactions_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(visible = searchExpanded || uiState.searchQuery.isNotBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { Text("Cari merchant, kategori, atau catatan...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        onSearchQueryChange("")
                                        onTypeFilterChange(null)
                                        searchExpanded = false
                                    }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Tutup pencarian")
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = extended.cardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("beranda_search_input")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filterChips = listOf(
                                    null to "Semua",
                                    TransactionType.EXPENSE to "Pengeluaran",
                                    TransactionType.INCOME to "Pemasukan",
                                    TransactionType.TRANSFER to "Transfer"
                                )
                                filterChips.forEach { (typeKey, label) ->
                                    val isSelected = uiState.typeFilter == typeKey
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onTypeFilterChange(typeKey) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(999.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("type_filter_${typeKey ?: "all"}")
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktivitas Terbaru",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenHistory()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("beranda_see_all_history_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Lihat Semua",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Lihat semua aktivitas",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (uiState.groupedTransactions.isEmpty()) {
                        val hasActiveFilter = uiState.searchQuery.isNotBlank() || uiState.typeFilter != null
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(extended.limeContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                        contentDescription = null,
                                        tint = extended.onLimeContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = if (hasActiveFilter) {
                                        "Tidak ada transaksi yang cocok"
                                    } else {
                                        "Belum ada aktivitas"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasActiveFilter) {
                                        "Coba ubah kata kunci pencarian atau filter."
                                    } else {
                                        "Tekan tombol + di kanan bawah atau aksi cepat di atas."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            displayedGroups.forEach { (dateHeader, dayTransactions) ->
                                key(dateHeader) {
                                    GroupedTransactionDayCard(
                                        dateHeader = dateHeader,
                                        transactions = dayTransactions,
                                        flatSheetStyle = false,
                                        onEditTransaction = { item -> onEditTransaction(item.entity) },
                                        onDeleteTransaction = { item -> onDeleteTransaction(item.entity) }
                                    )
                                }
                            }
                        }

                        if (hasMoreHistory || isSearchingOrFiltering) {
                            FintechCard(
                                onClick = onOpenHistory,
                                cornerRadius = 16.dp,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("beranda_view_full_history_card")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (hasMoreHistory) {
                                            "Lihat semua $totalDisplayedTxCount transaksi"
                                        } else {
                                            "Buka Riwayat Lengkap"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "Buka riwayat transaksi",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (moreShortcutsExpanded) {
            SakuBottomDrawer(
                title = "Semua Fitur",
                subtitle = "Akses cepat catatan, perencanaan & alat Saku",
                onDismiss = { moreShortcutsExpanded = false }
            ) {
                BerandaMoreCategoryHeader("Catat & Riwayat")
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (extended.isDark) 0.42f else 0.34f),
                    border = BorderStroke(0.8.dp, extended.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BerandaMoreDrawerRow(
                            icon = Icons.Rounded.Calculate,
                            title = "Catat Manual",
                            subtitle = "Kalkulator & input transaksi cepat",
                            accentTint = extended.onLimeContainer,
                            badgeBg = extended.limeContainer,
                            onClick = {
                                moreShortcutsExpanded = false
                                onOpenManual()
                            },
                            modifier = Modifier.testTag("shortcut_manual")
                        )
                        HorizontalDivider(color = extended.subtleDivider, modifier = Modifier.padding(horizontal = 14.dp))
                        BerandaMoreDrawerRow(
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            title = "Riwayat Transaksi",
                            subtitle = "Cari, filter, ubah, atau hapus catatan",
                            accentTint = extended.periwinkle,
                            badgeBg = extended.periwinkleContainer,
                            onClick = {
                                moreShortcutsExpanded = false
                                onOpenHistory()
                            },
                            modifier = Modifier.testTag("shortcut_history")
                        )
                        HorizontalDivider(color = extended.subtleDivider, modifier = Modifier.padding(horizontal = 14.dp))
                        BerandaMoreDrawerRow(
                            icon = Icons.Rounded.EventRepeat,
                            title = "Transaksi Berulang",
                            subtitle = "Otomasi tagihan & pemasukan rutin",
                            badgeLabel = if (activeRecurringCount > 0) "$activeRecurringCount aktif" else null,
                            accentTint = extended.income,
                            badgeBg = extended.incomeContainer,
                            onClick = {
                                moreShortcutsExpanded = false
                                onOpenRecurring()
                            },
                            modifier = Modifier.testTag("beranda_recurring_btn")
                        )
                    }
                }

                BerandaMoreCategoryHeader("Perencanaan & Analisis")
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (extended.isDark) 0.42f else 0.34f),
                    border = BorderStroke(0.8.dp, extended.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BerandaMoreDrawerRow(
                            icon = Icons.Rounded.BarChart,
                            title = "Ringkasan 6 Bulan",
                            subtitle = "Grafik tren pemasukan & pengeluaran",
                            accentTint = extended.periwinkle,
                            badgeBg = extended.periwinkleContainer,
                            onClick = {
                                moreShortcutsExpanded = false
                                onOpenMonthlyOverview()
                            },
                            modifier = Modifier.testTag("shortcut_monthly_overview")
                        )
                        HorizontalDivider(color = extended.subtleDivider, modifier = Modifier.padding(horizontal = 14.dp))
                        BerandaMoreDrawerRow(
                            icon = Icons.Rounded.PieChart,
                            title = "Anggaran Bulanan",
                            subtitle = "Pantau batas belanja tiap kategori",
                            accentTint = extended.onLimeContainer,
                            badgeBg = extended.limeContainer,
                            onClick = {
                                moreShortcutsExpanded = false
                                onOpenAnggaran()
                            },
                            modifier = Modifier.testTag("shortcut_anggaran")
                        )
                        HorizontalDivider(color = extended.subtleDivider, modifier = Modifier.padding(horizontal = 14.dp))
                        BerandaMoreDrawerRow(
                            icon = Icons.Rounded.Savings,
                            title = "Target Tabungan",
                            subtitle = "Catat progres dana impianmu",
                            accentTint = extended.income,
                            badgeBg = extended.incomeContainer,
                            onClick = {
                                moreShortcutsExpanded = false
                                onOpenSavingsGoals()
                            },
                            modifier = Modifier.testTag("shortcut_savings")
                        )
                        HorizontalDivider(color = extended.subtleDivider, modifier = Modifier.padding(horizontal = 14.dp))
                        BerandaMoreDrawerRow(
                            icon = Icons.Rounded.Handshake,
                            title = "Hutang & Piutang",
                            subtitle = "Lacak pinjaman, cicilan & jatuh tempo",
                            accentTint = extended.warning,
                            badgeBg = extended.warningContainer,
                            onClick = {
                                moreShortcutsExpanded = false
                                onOpenDebts()
                            },
                            modifier = Modifier.testTag("shortcut_debts")
                        )
                    }
                }

                BerandaMoreCategoryHeader("Data & Tampilan")
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (extended.isDark) 0.42f else 0.34f),
                    border = BorderStroke(0.8.dp, extended.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BerandaMoreDrawerRow(
                            icon = Icons.Rounded.FileDownload,
                            title = "Ekspor Laporan",
                            subtitle = "Unduh laporan transaksi CSV / PDF",
                            accentTint = extended.onLimeContainer,
                            badgeBg = extended.limeContainer,
                            onClick = {
                                moreShortcutsExpanded = false
                                onOpenExport()
                            },
                            modifier = Modifier.testTag("shortcut_export")
                        )
                        HorizontalDivider(color = extended.subtleDivider, modifier = Modifier.padding(horizontal = 14.dp))
                        BerandaMoreDrawerRow(
                            icon = Icons.Rounded.Tune,
                            title = "Kustomisasi Tampilan",
                            subtitle = "Atur tema, warna aksen & pintasan utama",
                            accentTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            badgeBg = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                moreShortcutsExpanded = false
                                onOpenPersonalization()
                            },
                            modifier = Modifier.testTag("beranda_personalization_btn")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BerandaTopBar(
    greeting: String,
    userName: String,
    syncStatus: CloudSyncStatus,
    searchExpanded: Boolean,
    onAvatarClick: () -> Unit,
    onSyncBadgeClick: () -> Unit,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val statusDotColor = when {
        syncStatus.isSyncing -> extended.lime
        syncStatus.isSignedIn && syncStatus.errorMessage != null -> ExpenseCoral
        syncStatus.isSignedIn && syncStatus.pendingChangesCount > 0 -> WarningAmber
        syncStatus.isSignedIn -> extended.income
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = when {
        syncStatus.isSyncing -> "Sinkron"
        syncStatus.isSignedIn && syncStatus.pendingChangesCount > 0 -> "${syncStatus.pendingChangesCount} tertunda"
        syncStatus.isSignedIn && syncStatus.errorMessage != null -> "Tertunda"
        syncStatus.isSignedIn -> "Cloud"
        else -> "Lokal"
    }
    val showPendingDot = syncStatus.isSignedIn &&
        (syncStatus.pendingChangesCount > 0 || syncStatus.errorMessage != null)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isDefaultSakuUser = userName.isBlank() ||
                userName.equals("Pengguna Saku", ignoreCase = true) ||
                userName.equals("Saku", ignoreCase = true) ||
                userName.equals("Saku Keuangan", ignoreCase = true)
            if (isDefaultSakuUser) {
                SakuBrandMark(
                    size = 44.dp,
                    cornerRadius = 14.dp,
                    onClick = onAvatarClick
                )
            } else {
                SakuProfileAvatar(
                    displayName = userName,
                    size = 44.dp,
                    onClick = onAvatarClick
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.8.dp, extended.cardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onSyncBadgeClick)
                            .testTag("beranda_sync_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (syncStatus.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(8.dp),
                                    strokeWidth = 1.4.dp,
                                    color = extended.lime
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusDotColor)
                                )
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObsidianCircleIconButton(
                icon = if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = if (searchExpanded) "Tutup pencarian" else "Cari transaksi",
                isActive = searchExpanded,
                onClick = onToggleSearch,
                modifier = Modifier.testTag("beranda_search_toggle_btn")
            )

            Box {
                ObsidianCircleIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = "Buka Pengaturan",
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("beranda_settings_btn")
                )
                if (showPendingDot) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-1).dp, y = 1.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (syncStatus.errorMessage != null) ExpenseCoral else WarningAmber)
                    )
                }
            }
        }
    }
}

@Composable
private fun BerandaPastelActionTile(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val tileShape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = tileShape,
            color = containerColor,
            border = BorderStroke(0.8.dp, iconTint.copy(alpha = if (extended.isDark) 0.28f else 0.16f)),
            shadowElevation = 0.dp,
            modifier = Modifier.size(48.dp)
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
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
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
private fun BerandaMoreCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun BerandaMoreDrawerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentTint: Color,
    badgeBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeLabel: String? = null
) {
    val extended = LocalSakuColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!badgeLabel.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = extended.incomeContainer
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = extended.income,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

