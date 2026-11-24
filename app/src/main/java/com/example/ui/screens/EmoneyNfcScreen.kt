package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.EmoneyCardRecord
import com.example.data.local.WalletEntity
import com.example.nfc.EmoneyCardSnapshot
import com.example.nfc.EmoneyIssuer
import com.example.nfc.EmoneyNfcReader
import com.example.nfc.EmoneyProtocolParser
import com.example.nfc.EmoneyScanOutcome
import com.example.ui.components.FintechCard
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.ObsidianCircleIconButton
import com.example.ui.components.ObsidianSubScreenHeader
import com.example.ui.components.SakuBottomDrawer
import com.example.ui.theme.LocalSakuColors
import com.example.ui.theme.LocalSakuTopPadding
import com.example.ui.theme.ObsidianCardBorder
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

private sealed interface NfcHardwareStatus {
    data object Ready : NfcHardwareStatus
    data object Disabled : NfcHardwareStatus
    data object Unsupported : NfcHardwareStatus
}

private data class ManualEmoneyDraft(
    val uidHex: String? = null,
    val issuer: EmoneyIssuer = EmoneyIssuer.MANDIRI_EMONEY,
    val cardNumber: String = "",
    val initialBalance: Long? = null,
    val isUpdate: Boolean = false
)

@Composable
fun EmoneyNfcScreen(
    history: List<EmoneyCardRecord>,
    wallets: List<WalletEntity>,
    hideBalance: Boolean = false,
    onToggleHideBalance: () -> Unit = {},
    onBack: () -> Unit,
    onSaveScan: suspend (EmoneyCardSnapshot) -> EmoneyCardRecord,
    onDeleteRecord: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRecordTransaction: (
        card: EmoneyCardRecord,
        walletId: Long,
        amount: Long,
        isExpense: Boolean,
        note: String
    ) -> Unit,
    onBeforeOpenNfcSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val nfcAdapter = remember(context) { NfcAdapter.getDefaultAdapter(context) }

    fun currentHardwareStatus(): NfcHardwareStatus = when {
        nfcAdapter == null -> NfcHardwareStatus.Unsupported
        nfcAdapter.isEnabled -> NfcHardwareStatus.Ready
        else -> NfcHardwareStatus.Disabled
    }

    var hardwareStatus by remember { mutableStateOf(currentHardwareStatus()) }
    var isReadingTag by remember { mutableStateOf(false) }
    var latestSavedCard by remember { mutableStateOf<EmoneyCardRecord?>(null) }
    var latestOutcome by remember { mutableStateOf<EmoneyScanOutcome?>(null) }
    var txDialogCard by remember { mutableStateOf<EmoneyCardRecord?>(null) }
    var manualDraft by remember { mutableStateOf<ManualEmoneyDraft?>(null) }
    var maskSensitiveInfo by remember(hideBalance) { mutableStateOf(hideBalance) }

    val scanInProgress = remember { AtomicBoolean(false) }
    val lastScanMillis = remember { AtomicLong(0L) }

    BackHandler(onBack = onBack)

    // Register Android NFC ReaderMode safely with concurrency + cooldown debounce
    DisposableEffect(lifecycleOwner, activity, nfcAdapter) {
        val readerCallback = NfcAdapter.ReaderCallback { tag ->
            val now = System.currentTimeMillis()
            if (now - lastScanMillis.get() < 1200L) return@ReaderCallback
            if (!scanInProgress.compareAndSet(false, true)) return@ReaderCallback

            scope.launch {
                try {
                    isReadingTag = true
                    val outcome = withContext(Dispatchers.IO) {
                        EmoneyNfcReader.readTag(tag)
                    }
                    lastScanMillis.set(System.currentTimeMillis())
                    latestOutcome = outcome
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (outcome is EmoneyScanOutcome.BalanceRead) {
                        latestSavedCard = onSaveScan(outcome.card)
                    }
                } finally {
                    isReadingTag = false
                    scanInProgress.set(false)
                }
            }
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        fun enableReaderIfReady() {
            hardwareStatus = currentHardwareStatus()
            if (activity != null && nfcAdapter != null && nfcAdapter.isEnabled) {
                runCatching {
                    nfcAdapter.enableReaderMode(activity, readerCallback, flags, null)
                }
            }
        }

        fun disableReader() {
            if (activity != null && nfcAdapter != null) {
                runCatching { nfcAdapter.disableReaderMode(activity) }
            }
        }

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            enableReaderIfReady()
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> enableReaderIfReady()
                Lifecycle.Event.ON_PAUSE -> disableReader()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            disableReader()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("emoney_nfc_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        FreshAtmosphericBackdrop(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(top = LocalSakuTopPadding.current, bottom = 0.dp)
        ) {
            // 1. Clean Header + Interactive Cobalt NFC Radar Tap Stage
            item(key = "nfc_header_stage") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ObsidianSubScreenHeader(
                        title = "Cek Saldo E-Money",
                        subtitle = "Tempel kartu fisik atau catat manual",
                        onBack = onBack,
                        backTestTag = "emoney_nfc_back_btn",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        trailingAction = {
                            ObsidianCircleIconButton(
                                icon = if (maskSensitiveInfo) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (maskSensitiveInfo) "Tampilkan nomor & saldo" else "Sembunyikan nomor & saldo",
                                isActive = maskSensitiveInfo,
                                onClick = {
                                    maskSensitiveInfo = !maskSensitiveInfo
                                    onToggleHideBalance()
                                },
                                modifier = Modifier.testTag("emoney_nfc_privacy_toggle")
                            )
                        }
                    )

                    NfcTapRadarStage(
                        hardwareStatus = hardwareStatus,
                        isReading = isReadingTag,
                        onOpenNfcSettings = {
                            onBeforeOpenNfcSettings()
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                            }
                        },
                        onOpenManualEntry = { manualDraft = ManualEmoneyDraft() },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            // 2. Scan Result & Card History
            item(key = "nfc_rising_sheet_top") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                        // Latest scan result card
                        AnimatedVisibility(
                            visible = latestOutcome != null,
                            enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 5 }
                        ) {
                            when (val outcome = latestOutcome) {
                                is EmoneyScanOutcome.BalanceRead -> {
                                    val card = latestSavedCard ?: EmoneyCardRecord(
                                        uidHex = outcome.card.uidHex,
                                        issuerName = outcome.card.issuer.displayName,
                                        issuerBadge = outcome.card.issuer.badgeText,
                                        cardNumber = outcome.card.cardNumber,
                                        balance = outcome.card.balance,
                                        previousBalance = null,
                                        accentHex = outcome.card.issuer.accentHex,
                                        scannedAtMillis = outcome.card.scannedAtMillis,
                                        sourceLabel = outcome.card.techLabel.ifBlank { "Chip NFC" }
                                    )
                                    ScannedBalanceCard(
                                        record = card,
                                        maskSensitive = maskSensitiveInfo,
                                        onRecordClick = { txDialogCard = card }
                                    )
                                }

                                is EmoneyScanOutcome.ProtectedCardDetected -> {
                                    ProtectedNfcCardBanner(
                                        outcome = outcome,
                                        onManualFillClick = {
                                            manualDraft = ManualEmoneyDraft(
                                                uidHex = outcome.uidHex,
                                                issuer = outcome.issuer,
                                                cardNumber = outcome.cardNumber.orEmpty()
                                            )
                                        }
                                    )
                                }

                                is EmoneyScanOutcome.ReadError -> {
                                    ReadErrorBanner(
                                        outcome = outcome,
                                        onManualFillClick = { manualDraft = ManualEmoneyDraft() }
                                    )
                                }

                                null -> Unit
                            }
                        }

                        // Supported Cards Info Strip
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = extended.cardRadius
                        ) {
                            Text(
                                text = "Dukungan Pembacaan NFC",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Mandiri e-Money" to true,
                                    "BNI TapCash" to true,
                                    "KMT (FeliCa)" to true,
                                    "BRI Brizzi (UID)" to false,
                                    "BCA Flazz (UID)" to false
                                ).forEach { (label, fullRead) ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (fullRead) {
                                            extended.limeContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (fullRead) {
                                                extended.onLimeContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pembacaan saldo langsung berlaku untuk kartu dengan struktur saldo publik (Mandiri e-Money, BNI TapCash, KMT tertentu). Kartu dengan kunci terenkripsi penerbit tetap terdeteksi UID-nya secara akurat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Riwayat Kartu (${history.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                TextButton(
                                    onClick = { manualDraft = ManualEmoneyDraft() },
                                    modifier = Modifier.testTag("nfc_manual_card_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Input Manual", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                }
                                if (history.isNotEmpty()) {
                                    TextButton(onClick = onClearHistory) {
                                        Text("Hapus", color = extended.expense, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                }
            }

            if (history.isEmpty()) {
                item(key = "nfc_empty_history") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        FintechCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = extended.cardRadius
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Belum ada kartu yang tersimpan",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Tempelkan kartu e-Money di belakang HP atau pakai tombol Input Manual untuk mencatat saldo kartu terenkripsi seperti Flazz/Brizzi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(history, key = { it.uidHex }) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                    ) {
                        EmoneyHistoryRow(
                            record = item,
                            maskSensitive = maskSensitiveInfo,
                            onRecordTx = { txDialogCard = item },
                            onUpdateManual = {
                                val matchedIssuer = EmoneyIssuer.entries.firstOrNull {
                                    it.displayName == item.issuerName || it.badgeText == item.issuerBadge
                                } ?: EmoneyIssuer.UNKNOWN_NFC
                                manualDraft = ManualEmoneyDraft(
                                    uidHex = item.uidHex,
                                    issuer = matchedIssuer,
                                    cardNumber = item.cardNumber,
                                    initialBalance = item.balance,
                                    isUpdate = true
                                )
                            },
                            onDelete = { onDeleteRecord(item.uidHex) }
                        )
                    }
                }
            }

            item(key = "nfc_bottom_spacer") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(28.dp)
                )
            }
        }
    }

    txDialogCard?.let { card ->
        RecordEmoneyTxDialog(
            card = card,
            wallets = wallets,
            onDismiss = { txDialogCard = null },
            onConfirm = { walletId, amount, isExpense, note ->
                onRecordTransaction(card, walletId, amount, isExpense, note)
                txDialogCard = null
            }
        )
    }

    manualDraft?.let { draft ->
        ManualEmoneyEntryDialog(
            draft = draft,
            onDismiss = { manualDraft = null },
            onSave = { issuer, cardNumber, balance ->
                val cleanDigits = cardNumber.filter { it.isLetterOrDigit() }.uppercase()
                val stableUid = draft.uidHex ?: "MANUAL_${issuer.badgeText}_${cleanDigits.takeLast(8).ifBlank { System.currentTimeMillis().toString() }}"
                val formattedNo = EmoneyProtocolParser.formatCardNumber(cleanDigits).ifBlank {
                    EmoneyProtocolParser.formatCardNumber(stableUid)
                }
                val snapshot = EmoneyCardSnapshot(
                    uidHex = stableUid,
                    issuer = issuer,
                    cardNumber = formattedNo,
                    balance = balance,
                    techLabel = "Input Manual"
                )
                scope.launch {
                    val saved = onSaveScan(snapshot)
                    latestSavedCard = saved
                    latestOutcome = EmoneyScanOutcome.BalanceRead(snapshot)
                    manualDraft = null
                }
            }
        )
    }
}

@Composable
private fun NfcTapRadarStage(
    hardwareStatus: NfcHardwareStatus,
    isReading: Boolean,
    onOpenNfcSettings: () -> Unit,
    onOpenManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val inf = rememberInfiniteTransition(label = "nfc_radar")
    val waveProgress by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_wave"
    )
    val pulseScale by inf.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(0.8.dp, ObsidianCardBorder),
        shadowElevation = if (extended.isDark) 0.dp else 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("nfc_radar_stage")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hardwareStatus is NfcHardwareStatus.Ready && !isReading) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxR = size.minDimension / 2f
                            listOf(waveProgress, (waveProgress + 0.5f) % 1f).forEach { p ->
                                val r = maxR * (0.45f + p * 0.55f)
                                drawCircle(
                                    color = Color.White.copy(alpha = (1f - p) * 0.45f),
                                    radius = r,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size((72 * if (hardwareStatus is NfcHardwareStatus.Ready) pulseScale else 1f).dp)
                            .clip(CircleShape)
                            .background(
                                when (hardwareStatus) {
                                    NfcHardwareStatus.Ready -> Color.White
                                    NfcHardwareStatus.Disabled -> Color(0xFFF59E0B)
                                    NfcHardwareStatus.Unsupported -> Color.White.copy(alpha = 0.22f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isReading) {
                            CircularProgressIndicator(
                                color = extended.heroMid,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            Icon(
                                imageVector = when (hardwareStatus) {
                                    NfcHardwareStatus.Ready -> Icons.Rounded.Contactless
                                    NfcHardwareStatus.Disabled -> Icons.Rounded.Nfc
                                    NfcHardwareStatus.Unsupported -> Icons.Rounded.ErrorOutline
                                },
                                contentDescription = null,
                                tint = when (hardwareStatus) {
                                    NfcHardwareStatus.Ready -> extended.heroMid
                                    NfcHardwareStatus.Disabled -> Color(0xFF14161A)
                                    NfcHardwareStatus.Unsupported -> Color.White
                                },
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                when (hardwareStatus) {
                    NfcHardwareStatus.Ready -> {
                        Text(
                            text = if (isReading) "Membaca Chip Kartu..." else "Siap Memindai Kartu NFC",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Tempelkan kartu e-Money di area sensor NFC (bagian tengah/atas belakang HP) dan tahan 1–2 detik.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                    }

                    NfcHardwareStatus.Disabled -> {
                        Text(
                            text = "NFC Belum Aktif",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Aktifkan NFC di pengaturan perangkat, atau gunakan Input Manual sebagai alternatif.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onOpenNfcSettings,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = extended.heroMid
                                )
                            ) {
                                Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buka NFC", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onOpenManualEntry,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                            ) {
                                Text("Input Manual", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    NfcHardwareStatus.Unsupported -> {
                        Text(
                            text = "Perangkat Tanpa Sensor NFC",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Hardware NFC tidak tersedia di perangkat ini. Kamu tetap bisa memantau kartu & selisih saldo lewat Input Manual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                        Button(
                            onClick = onOpenManualEntry,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = extended.heroMid
                            ),
                            modifier = Modifier.testTag("nfc_unsupported_manual_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Input Saldo Manual", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedBalanceCard(
    record: EmoneyCardRecord,
    maskSensitive: Boolean,
    onRecordClick: () -> Unit
) {
    val extended = LocalSakuColors.current
    val baseColor = Color(record.accentHex)
    val delta = record.balanceDelta
    val displayNumber = if (maskSensitive) EmoneyProtocolParser.maskCardNumber(record.cardNumber) else record.cardNumber

    Surface(
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nfc_balance_result_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(baseColor, baseColor.copy(alpha = 0.84f))
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Contactless,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = record.issuerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.20f)
                    ) {
                        Text(
                            text = "${record.issuerBadge} • ${record.sourceLabel}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Saldo Kartu Saat Ini",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Text(
                        text = if (maskSensitive) "Rp ••••••" else CurrencyFormatter.formatRupiah(record.balance),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (delta != null && !maskSensitive) {
                    val isSpent = delta < 0L
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "${if (isSpent) "-" else "+"}${CurrencyFormatter.formatRupiah(abs(delta))} dibanding cek terakhir",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSpent) Color(0xFFFDA4AF) else Color(0xFF6EE7B7),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayNumber,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "UID ${record.uidHex.take(16)} • ${DateFormatter.formatShortDate(record.scannedAtMillis)}, ${DateFormatter.formatTime(record.scannedAtMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onRecordClick,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extended.lime,
                            contentColor = extended.onLime
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Catat", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtectedNfcCardBanner(
    outcome: EmoneyScanOutcome.ProtectedCardDetected,
    onManualFillClick: () -> Unit
) {
    val extended = LocalSakuColors.current
    FintechCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = extended.cardRadius
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(outcome.issuer.accentHex).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color(outcome.issuer.accentHex),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${outcome.issuer.displayName} Terdeteksi",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "UID: ${outcome.uidHex} • ${outcome.techSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = outcome.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = onManualFillClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Kartu & Isi Saldo Manual", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ReadErrorBanner(
    outcome: EmoneyScanOutcome.ReadError,
    onManualFillClick: () -> Unit
) {
    val extended = LocalSakuColors.current
    FintechCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = extended.cardRadius
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(extended.expense.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = extended.expense,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = outcome.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = outcome.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(
                onClick = onManualFillClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Catat Saldo Manual Saja", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmoneyHistoryRow(
    record: EmoneyCardRecord,
    maskSensitive: Boolean,
    onRecordTx: () -> Unit,
    onUpdateManual: () -> Unit,
    onDelete: () -> Unit
) {
    val extended = LocalSakuColors.current
    val displayCardNo = if (maskSensitive) EmoneyProtocolParser.maskCardNumber(record.cardNumber) else record.cardNumber
    FintechCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = extended.cardRadius
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(record.accentHex)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Contactless,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = record.issuerName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$displayCardNo • ${record.sourceLabel}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${DateFormatter.formatShortDate(record.scannedAtMillis)}, ${DateFormatter.formatTime(record.scannedAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (maskSensitive) "Rp ••••••" else CurrencyFormatter.formatRupiah(record.balance),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Update",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onUpdateManual)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    Text(
                        text = "Catat",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onRecordTx)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Hapus riwayat kartu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualEmoneyEntryDialog(
    draft: ManualEmoneyDraft,
    onDismiss: () -> Unit,
    onSave: (issuer: EmoneyIssuer, cardNumber: String, balance: Long) -> Unit
) {
    val extended = LocalSakuColors.current
    var selectedIssuer by remember { mutableStateOf(draft.issuer) }
    var cardNoText by remember { mutableStateOf(draft.cardNumber) }
    var balanceText by remember { mutableStateOf(draft.initialBalance?.toString() ?: "") }
    val parsedBalance = balanceText.filter { it.isDigit() }.toLongOrNull()
    val validBalance = parsedBalance != null && parsedBalance in 0L..20_000_000L

    SakuBottomDrawer(
        title = if (draft.isUpdate) "Update Saldo Kartu" else "Input Saldo E-Money Manual",
        subtitle = "Cocok untuk kartu terenkripsi (BCA Flazz, BRI Brizzi) atau saat HP tidak mendukung NFC.",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Penerbit Kartu",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmoneyIssuer.entries.forEach { issuer ->
                    FilterChip(
                        selected = selectedIssuer == issuer,
                        onClick = { selectedIssuer = issuer },
                        label = { Text(issuer.displayName) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = cardNoText,
            onValueChange = { if (it.length <= 24) cardNoText = it },
            label = { Text("Nomor Kartu / 4 Digit Terakhir") },
            placeholder = { Text("Contoh: 6032 9812 3456 7890") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = balanceText,
            onValueChange = { balanceText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Saldo Kartu Saat Ini (Rp)") },
            placeholder = { Text("Contoh: 150000") },
            supportingText = {
                if (parsedBalance != null && parsedBalance > 20_000_000L) {
                    Text("Maksimal saldo e-Money adalah Rp 20.000.000")
                }
            },
            isError = parsedBalance != null && parsedBalance > 20_000_000L,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                onClick = { onSave(selectedIssuer, cardNoText, parsedBalance ?: 0L) },
                enabled = validBalance,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Simpan Kartu", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun RecordEmoneyTxDialog(
    card: EmoneyCardRecord,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (walletId: Long, amount: Long, isExpense: Boolean, note: String) -> Unit
) {
    val extended = LocalSakuColors.current
    val delta = card.balanceDelta
    var isExpense by remember { mutableStateOf(delta == null || delta < 0L) }
    var amountText by remember { mutableStateOf(delta?.let { abs(it).toString() } ?: "") }
    var noteText by remember { mutableStateOf("Kartu ${card.issuerName} (${card.cardNumber})") }
    var selectedWalletId by remember { mutableLongStateOf(wallets.firstOrNull()?.id ?: 0L) }
    val parsedAmount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L

    SakuBottomDrawer(
        title = "Catat Aktivitas ${card.issuerBadge}",
        subtitle = "Simpan perubahan saldo kartu e-Money ke mutasi dompetmu",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = isExpense,
                onClick = { isExpense = true },
                label = { Text("Pengeluaran") }
            )
            FilterChip(
                selected = !isExpense,
                onClick = { isExpense = false },
                label = { Text("Top Up / Masuk") }
            )
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Nominal (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (wallets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Dompet Sumber / Tujuan",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.forEach { w ->
                        FilterChip(
                            selected = selectedWalletId == w.id,
                            onClick = { selectedWalletId = w.id },
                            leadingIcon = if (selectedWalletId == w.id) {
                                { Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                            } else {
                                { Icon(Icons.Rounded.AccountBalanceWallet, null, modifier = Modifier.size(16.dp)) }
                            },
                            label = { Text(w.name) }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Catatan") },
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
                onClick = { onConfirm(selectedWalletId, parsedAmount, isExpense, noteText) },
                enabled = parsedAmount > 0L && selectedWalletId > 0L,
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
