package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.data.local.WalletEntity
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.ObsidianSubScreenHeader
import com.example.ui.theme.AccentPalettePreset
import com.example.ui.theme.CardCornerStyle
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.SheetInputTab
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizationScreen(
    prefs: PersonalizationPrefs,
    themeMode: ThemeMode,
    wallets: List<WalletEntity>,
    sampleNetBalance: Long,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onSetAccentPreset: (AccentPalettePreset) -> Unit,
    onSetCardCornerStyle: (CardCornerStyle) -> Unit,
    onSetShowWalletsOnHome: (Boolean) -> Unit,
    onSetShowBudgetOnHome: (Boolean) -> Unit,
    onSetCompactCurrencyOnHome: (Boolean) -> Unit,
    onSetHomeShortcuts: (List<HomeShortcutOption>) -> Unit,
    onSetDefaultWalletId: (Long?) -> Unit,
    onSetDefaultFabTab: (SheetInputTab) -> Unit,
    onSetBudgetAlertPercent: (Int) -> Unit,
    onSetBillingCycleStartDay: (Int) -> Unit,
    onResetPersonalization: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current
    var nameDraft by remember(prefs.displayName) { mutableStateOf(prefs.displayName) }

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
            contentPadding = PaddingValues(top = LocalSakuTopPadding.current, bottom = 32.dp)
        ) {
            // 1. Clean Header + Live Cobalt Hero Preview
            item(key = "personalization_hero_preview") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ObsidianSubScreenHeader(
                        title = "Kustomisasi & Personalisasi",
                        subtitle = "Sesuaikan tampilan, warna aksen & kebiasaanmu",
                        onBack = onBack,
                        backTestTag = "personalization_back_btn"
                    )

                    // Live Mini-Preview matching Beranda Cobalt Gradient Hero
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("personalization_live_preview"),
                        shape = RoundedCornerShape(prefs.cardCornerStyle.cardRadius),
                        color = Color.Transparent,
                        border = BorderStroke(0.8.dp, ObsidianCardBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(extended.heroStart, extended.heroMid, extended.heroEnd)
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pratinjau Beranda",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color.White.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "Siklus tgl ${prefs.billingCycleStartDay}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val cleanName = nameDraft.trim()
                                    Text(
                                        text = if (cleanName.isNotEmpty()) "Selamat Pagi, $cleanName" else "Selamat Pagi,",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                    Text(
                                        text = if (prefs.compactCurrencyOnHome) {
                                            CurrencyFormatter.formatCompactRupiah(sampleNetBalance)
                                        } else {
                                            CurrencyFormatter.formatRupiah(sampleNetBalance)
                                        },
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    prefs.homeShortcuts.forEachIndexed { idx, shortcut ->
                                        val isPrimaryPill = idx == 0
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (isPrimaryPill) Color.White else Color.White.copy(alpha = 0.16f),
                                            border = if (isPrimaryPill) null else BorderStroke(0.7.dp, Color.White.copy(alpha = 0.25f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = shortcutIcon(shortcut),
                                                    contentDescription = null,
                                                    tint = if (isPrimaryPill) extended.heroMid else Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = shortcut.label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isPrimaryPill) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isPrimaryPill) extended.heroMid else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Crisp Unified Personalization Sections
            item(key = "personalization_rising_sheet") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                        // Section A: Profil & Sapaan
                        FintechCard(modifier = Modifier.fillMaxWidth()) {
                            SectionHeaderRow(
                                icon = Icons.Rounded.Person,
                                title = "Profil & Sapaan",
                                subtitle = "Nama panggilan muncul di header Beranda"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { raw ->
                                    val capped = raw.take(24)
                                    nameDraft = capped
                                    onUpdateDisplayName(capped)
                                },
                                label = { Text("Nama Panggilan (opsional)") },
                                placeholder = { Text("Contoh: Ridho, Budi, Sinta") },
                                singleLine = true,
                                trailingIcon = {
                                    if (nameDraft.isNotEmpty()) {
                                        IconButton(onClick = {
                                            nameDraft = ""
                                            onUpdateDisplayName("")
                                        }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Hapus nama")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("personalization_name_input")
                            )
                        }

                        // Section B: Tema, Warna Aksen & Sudut Kartu
                        FintechCard(modifier = Modifier.fillMaxWidth()) {
                            SectionHeaderRow(
                                icon = Icons.Rounded.ColorLens,
                                title = "Tema & Palet Aksen",
                                subtitle = "Ubah nuansa warna tombol, grafik & penanda aktif"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("personalization_theme_picker")
                            ) {
                                ThemeMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = themeMode == mode,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSetThemeMode(mode)
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = ThemeMode.entries.size
                                        )
                                    ) {
                                        Text(mode.label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Warna Aksen Utama",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AccentPalettePreset.entries.forEach { preset ->
                                    val selected = prefs.accentPreset == preset
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSetAccentPreset(preset)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                            .testTag("accent_preset_${preset.name.lowercase()}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(preset.primaryAccent)
                                                .then(
                                                    if (selected) {
                                                        Modifier.border(
                                                            width = 2.5.dp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            shape = CircleShape
                                                        )
                                                    } else {
                                                        Modifier.border(
                                                            width = 1.dp,
                                                            color = extended.cardBorder,
                                                            shape = CircleShape
                                                        )
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Terpilih",
                                                    tint = preset.onPrimaryAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RoundedCorner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "Gaya Sudut Kartu",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("personalization_corner_picker")
                            ) {
                                CardCornerStyle.entries.forEachIndexed { index, style ->
                                    SegmentedButton(
                                        selected = prefs.cardCornerStyle == style,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSetCardCornerStyle(style)
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = CardCornerStyle.entries.size
                                        )
                                    ) {
                                        Text(style.label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        // Section C: Tata Letak Beranda & 3 Pintasan Cepat
                        FintechCard(modifier = Modifier.fillMaxWidth()) {
                            SectionHeaderRow(
                                icon = Icons.Rounded.DashboardCustomize,
                                title = "Tata Letak & Pintasan Beranda",
                                subtitle = "Atur komponen yang tampil di halaman utama"
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            PersonalizationSwitchRow(
                                title = "Kartu \"Dompet Saya\" di Beranda",
                                subtitle = "Carousel kartu dompet untuk filter saldo cepat",
                                checked = prefs.showWalletsOnHome,
                                onCheckedChange = onSetShowWalletsOnHome,
                                modifier = Modifier.testTag("toggle_home_wallets")
                            )
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            PersonalizationSwitchRow(
                                title = "Ringkasan Anggaran & Arus Kas",
                                subtitle = "Bar progres anggaran di bagian Aktivitas Terbaru",
                                checked = prefs.showBudgetOnHome,
                                onCheckedChange = onSetShowBudgetOnHome,
                                modifier = Modifier.testTag("toggle_home_budget")
                            )
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            PersonalizationSwitchRow(
                                title = "Format Nominal Ringkas",
                                subtitle = "Tampilkan saldo utama sebagai jt / rb (mis. Rp 12,4 jt)",
                                checked = prefs.compactCurrencyOnHome,
                                onCheckedChange = onSetCompactCurrencyOnHome,
                                modifier = Modifier.testTag("toggle_home_compact_currency")
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pilih 3 Pintasan Utama",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = prefs.homeShortcuts.joinToString(" • ") { it.label },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ketuk opsi untuk mengganti slot pintasan di bawah saldo (selalu 3 tombol aktif).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HomeShortcutOption.entries.forEach { option ->
                                    val activeIndex = prefs.homeShortcuts.indexOf(option)
                                    val isSelected = activeIndex >= 0
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (!isSelected) {
                                                // Rotate out oldest non-first or push onto 3-slot queue
                                                val updated = (prefs.homeShortcuts.drop(1) + option)
                                                onSetHomeShortcuts(updated)
                                            } else if (activeIndex > 0) {
                                                // Move selected item to primary slot #1
                                                val reordered = listOf(option) + prefs.homeShortcuts.filter { it != option }
                                                onSetHomeShortcuts(reordered)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = shortcutIcon(option),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (isSelected) "${activeIndex + 1}. ${option.label}" else option.label,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = extended.limeContainer,
                                            selectedLabelColor = extended.onLimeContainer,
                                            selectedLeadingIconColor = extended.onLimeContainer
                                        ),
                                        modifier = Modifier.testTag("shortcut_choice_${option.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Section D: Kebiasaan Pencatatan & Siklus Gajian
                        FintechCard(modifier = Modifier.fillMaxWidth()) {
                            SectionHeaderRow(
                                icon = Icons.Rounded.Tune,
                                title = "Pencatatan & Siklus Bulanan",
                                subtitle = "Atur dompet bawaan, tombol + dan tanggal gajian"
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Aksi Awal Tombol +",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SheetInputTab.entries.forEach { tab ->
                                    val selected = prefs.defaultFabTabName == tab.name
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSetDefaultFabTab(tab) },
                                        label = { Text(tab.title, style = MaterialTheme.typography.labelMedium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = extended.limeContainer,
                                            selectedLabelColor = extended.onLimeContainer
                                        ),
                                        modifier = Modifier.testTag("default_fab_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }

                            if (wallets.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Dompet Utama Bawaan",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = prefs.defaultWalletId == null,
                                        onClick = { onSetDefaultWalletId(null) },
                                        label = { Text("Otomatis", style = MaterialTheme.typography.labelMedium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = extended.limeContainer,
                                            selectedLabelColor = extended.onLimeContainer
                                        )
                                    )
                                    wallets.forEach { wallet ->
                                        FilterChip(
                                            selected = prefs.defaultWalletId == wallet.id,
                                            onClick = { onSetDefaultWalletId(wallet.id) },
                                            label = { Text(wallet.name, style = MaterialTheme.typography.labelMedium) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = extended.limeContainer,
                                                selectedLabelColor = extended.onLimeContainer
                                            ),
                                            modifier = Modifier.testTag("default_wallet_${wallet.id}")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(thickness = 0.6.dp, color = extended.subtleDivider)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Tanggal Mulai Siklus Bulanan",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    Text(
                                        text = if (prefs.billingCycleStartDay == 1) {
                                            "Standar kalender (tanggal 1 tiap bulan)"
                                        } else {
                                            "Siklus gajian mulai tanggal ${prefs.billingCycleStartDay}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            val next = (prefs.billingCycleStartDay - 1).coerceAtLeast(1)
                                            onSetBillingCycleStartDay(next)
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Remove, contentDescription = "Kurangi tanggal", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(
                                        text = "${prefs.billingCycleStartDay}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    Surface(
                                        onClick = {
                                            val next = (prefs.billingCycleStartDay + 1).coerceAtMost(28)
                                            onSetBillingCycleStartDay(next)
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Add, contentDescription = "Tambah tanggal", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                listOf(1, 5, 10, 20, 25, 28).forEach { day ->
                                    FilterChip(
                                        selected = prefs.billingCycleStartDay == day,
                                        onClick = { onSetBillingCycleStartDay(day) },
                                        label = { Text("Tgl $day", style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = extended.limeContainer,
                                            selectedLabelColor = extended.onLimeContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Ambang Peringatan Anggaran (${prefs.budgetAlertPercent}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(70, 75, 80, 85).forEach { pct ->
                                    FilterChip(
                                        selected = prefs.budgetAlertPercent == pct,
                                        onClick = { onSetBudgetAlertPercent(pct) },
                                        label = { Text("$pct%", style = MaterialTheme.typography.labelMedium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = extended.limeContainer,
                                            selectedLabelColor = extended.onLimeContainer
                                        )
                                    )
                                }
                            }
                        }

                        // Reset Personalization Button
                        OutlinedButton(
                            onClick = {
                                nameDraft = ""
                                onResetPersonalization()
                            },
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("reset_personalization_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kembalikan Personalisasi Awal")
                        }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val extended = LocalSakuColors.current
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
                imageVector = icon,
                contentDescription = null,
                tint = extended.onLimeContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PersonalizationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = extended.onLime,
                checkedTrackColor = extended.lime
            )
        )
    }
}

private fun shortcutIcon(option: HomeShortcutOption): ImageVector = when (option) {
    HomeShortcutOption.AI -> Icons.Rounded.AutoAwesome
    HomeShortcutOption.VOICE -> Icons.Rounded.Mic
    HomeShortcutOption.SCAN -> Icons.Rounded.CameraAlt
    HomeShortcutOption.MANUAL -> Icons.Rounded.Calculate
    HomeShortcutOption.HISTORY -> Icons.AutoMirrored.Rounded.ReceiptLong
    HomeShortcutOption.BUDGET -> Icons.Rounded.PieChart
    HomeShortcutOption.WALLET -> Icons.Rounded.AccountBalanceWallet
    HomeShortcutOption.SAVINGS -> Icons.Rounded.Savings
    HomeShortcutOption.DEBTS -> Icons.Rounded.Handshake
    HomeShortcutOption.OVERVIEW -> Icons.Rounded.BarChart
    HomeShortcutOption.NFC_EMONEY -> Icons.Rounded.Contactless
    HomeShortcutOption.CALENDAR -> Icons.Rounded.CalendarMonth
}
