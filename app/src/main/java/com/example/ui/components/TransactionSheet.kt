package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ai.AutoTagSuggestion
import com.example.ai.ParsedTransactionDraft
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LocalSakuColors
import com.example.ui.viewmodel.AiParseState
import com.example.ui.viewmodel.SheetInputTab
import com.example.ui.viewmodel.TransactionSheetState
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.util.FinanceCategories
import com.example.util.MathEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionBottomSheet(
    sheetState: TransactionSheetState,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onSelectTab: (SheetInputTab) -> Unit,
    onAiInputChange: (String) -> Unit,
    onParseAiText: (String) -> Unit,
    onParseVoiceAudio: (ByteArray, String) -> Unit,
    onParseReceiptBitmap: (Bitmap) -> Unit,
    onUpdateDraft: (ParsedTransactionDraft) -> Unit,
    onRemoveDraft: (String) -> Unit,
    onSaveDrafts: (List<ParsedTransactionDraft>) -> Unit,
    onClearPreview: () -> Unit,
    onSaveManual: (
        editingOld: TransactionEntity?,
        merchant: String,
        amount: Long,
        type: String,
        category: String,
        walletId: Long,
        targetWalletId: Long?,
        note: String,
        timestamp: Long
    ) -> Unit,
    onDeleteExisting: (TransactionEntity) -> Unit,
    defaultWalletId: Long? = null,
    onAutoTagCategory: suspend (description: String, txType: String, allowCloudFallback: Boolean) -> AutoTagSuggestion? = { _, _, _ -> null }
) {
    if (!sheetState.isOpen) return

    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (sheetState.editingTransaction != null) "Edit Transaksi" else "Catat Transaksi",
                    style = MaterialTheme.typography.headlineLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                }
            }

            if (sheetState.editingTransaction == null) {
                val tabs = SheetInputTab.entries
                val extended = LocalSakuColors.current
                val haptic = LocalHapticFeedback.current
                val selectedIndex = tabs.indexOf(sheetState.activeTab).coerceAtLeast(0)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        val tabWidth = maxWidth / tabs.size
                        val indicatorOffset by animateDpAsState(
                            targetValue = tabWidth * selectedIndex,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "sheet_tab_slider"
                        )

                        // Clean Neutral Sliding Pill Backdrop
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(tabWidth)
                                .height(48.dp)
                                .shadow(
                                    elevation = if (extended.isDark) 0.dp else 1.dp,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            tabs.forEach { tab ->
                                val selected = sheetState.activeTab == tab
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val pressScale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.92f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "sheet_tab_press"
                                )
                                val contentColor by animateColorAsState(
                                    targetValue = if (selected) {
                                        extended.onLimeContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "sheet_tab_color"
                                )
                                val tabIcon = when (tab) {
                                    SheetInputTab.AI_MAGIC -> Icons.Rounded.AutoAwesome
                                    SheetInputTab.VOICE -> Icons.Rounded.Mic
                                    SheetInputTab.SCAN -> Icons.Rounded.CameraAlt
                                    SheetInputTab.MANUAL -> Icons.Rounded.Calculate
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .minimumInteractiveComponentSize()
                                        .graphicsLayer {
                                            scaleX = pressScale
                                            scaleY = pressScale
                                        }
                                        .clip(RoundedCornerShape(14.dp))
                                        .semantics { this.selected = selected }
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            role = Role.Tab,
                                            onClick = {
                                                if (!selected) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onSelectTab(tab)
                                                }
                                            }
                                        )
                                        .testTag("sheet_tab_${tab.name.lowercase()}"),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tabIcon,
                                        contentDescription = tab.title,
                                        tint = contentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tab.title,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = contentColor,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when {
                sheetState.editingTransaction != null || sheetState.activeTab == SheetInputTab.MANUAL -> {
                    ManualTransactionTab(
                        editing = sheetState.editingTransaction,
                        initialTimestamp = sheetState.initialTimestamp,
                        wallets = wallets,
                        defaultWalletId = defaultWalletId,
                        onAutoTagCategory = onAutoTagCategory,
                        onSave = onSaveManual,
                        onDelete = { tx ->
                            onDeleteExisting(tx)
                            onDismiss()
                        }
                    )
                }

                sheetState.activeTab == SheetInputTab.AI_MAGIC -> {
                    AiMagicTab(
                        inputText = sheetState.aiInputText,
                        parseState = sheetState.aiParseState,
                        wallets = wallets,
                        onInputChange = onAiInputChange,
                        onParseClick = { onParseAiText(sheetState.aiInputText) },
                        onUpdateDraft = onUpdateDraft,
                        onRemoveDraft = onRemoveDraft,
                        onSaveDrafts = onSaveDrafts,
                        onClearPreview = onClearPreview
                    )
                }

                sheetState.activeTab == SheetInputTab.VOICE -> {
                    VoiceTransactionTab(
                        autoStart = sheetState.autoStartVoice,
                        parseState = sheetState.aiParseState,
                        wallets = wallets,
                        onTranscriptReady = { transcript -> onParseAiText(transcript) },
                        onAudioReady = { bytes, mime -> onParseVoiceAudio(bytes, mime) },
                        onUpdateDraft = onUpdateDraft,
                        onRemoveDraft = onRemoveDraft,
                        onSaveDrafts = onSaveDrafts,
                        onClearPreview = onClearPreview
                    )
                }

                sheetState.activeTab == SheetInputTab.SCAN -> {
                    ReceiptScanTab(
                        parseState = sheetState.aiParseState,
                        wallets = wallets,
                        onBitmapReady = onParseReceiptBitmap,
                        onUpdateDraft = onUpdateDraft,
                        onRemoveDraft = onRemoveDraft,
                        onSaveDrafts = onSaveDrafts,
                        onClearPreview = onClearPreview
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMagicTab(
    inputText: String,
    parseState: AiParseState,
    wallets: List<WalletEntity>,
    onInputChange: (String) -> Unit,
    onParseClick: () -> Unit,
    onUpdateDraft: (ParsedTransactionDraft) -> Unit,
    onRemoveDraft: (String) -> Unit,
    onSaveDrafts: (List<ParsedTransactionDraft>) -> Unit,
    onClearPreview: () -> Unit
) {
    val extended = LocalSakuColors.current
    val examples = listOf(
        "Beli kopi kenangan 25rb dan parkir 3rb pakai GoPay",
        "Makan siang 45rb tunai & isi bensin 50rb SeaBank",
        "Terima gaji 11.5jt masuk BCA"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Ketik kalimat santai Bahasa Indonesia (paham goceng, ceban, rb, k, jt):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            label = { Text("Ceritakan transaksimu...") },
            placeholder = { Text("cth: Ngopi ceban dan gorengan goceng pakai GoPay") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_magic_text_field")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            examples.forEach { sample ->
                FilterChip(
                    selected = false,
                    onClick = { onInputChange(sample) },
                    label = {
                        Text(
                            text = sample,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }

        val extended = LocalSakuColors.current
        Button(
            onClick = onParseClick,
            enabled = inputText.isNotBlank() && parseState !is AiParseState.Loading,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = extended.lime,
                contentColor = extended.onLime
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("ai_magic_submit_btn")
        ) {
            if (parseState is AiParseState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = extended.onLime
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Menganalisis transaksi...")
            } else {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ekstrak Transaksi dengan AI",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }

        AiParseResultSection(
            parseState = parseState,
            wallets = wallets,
            accentColor = extended.income,
            onUpdateDraft = onUpdateDraft,
            onRemoveDraft = onRemoveDraft,
            onSaveDrafts = onSaveDrafts,
            onRetry = onParseClick,
            onReset = onClearPreview
        )
    }
}

@Composable
private fun VoiceTransactionTab(
    autoStart: Boolean,
    parseState: AiParseState,
    wallets: List<WalletEntity>,
    onTranscriptReady: (String) -> Unit,
    onAudioReady: (ByteArray, String) -> Unit,
    onUpdateDraft: (ParsedTransactionDraft) -> Unit,
    onRemoveDraft: (String) -> Unit,
    onSaveDrafts: (List<ParsedTransactionDraft>) -> Unit,
    onClearPreview: () -> Unit
) {
    val context = LocalContext.current
    val extended = LocalSakuColors.current
    val scope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var usingAudioRecorder by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var voiceStatus by remember { mutableStateOf("Ketuk mikrofon untuk mulai merekam suara (Bahasa Indonesia)") }

    val recognizerAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    val speechRecognizer = remember(recognizerAvailable) {
        if (recognizerAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }

    fun releaseRecorder() {
        runCatching {
            mediaRecorder?.stop()
        }
        runCatching {
            mediaRecorder?.release()
        }
        mediaRecorder = null
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            speechRecognizer?.destroy()
            releaseRecorder()
        }
    }

    LaunchedEffect(isListening, usingAudioRecorder) {
        if (isListening && usingAudioRecorder) {
            while (isListening && usingAudioRecorder) {
                val amp = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                rmsLevel = (amp / 18_000f).coerceIn(0.15f, 1f)
                delay(70L)
            }
        }
    }

    fun startMediaRecorderFallback() {
        releaseRecorder()
        val outFile = File(context.cacheDir, "saku_voice_${System.currentTimeMillis()}.m4a")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        val started = runCatching {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(16_000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            true
        }.getOrElse {
            runCatching { recorder.release() }
            false
        }

        if (started) {
            mediaRecorder = recorder
            recordedAudioFile = outFile
            usingAudioRecorder = true
            isListening = true
            voiceStatus = "Merekam audio langsung... Ketuk tombol merah jika selesai"
        } else {
            usingAudioRecorder = false
            isListening = false
            voiceStatus = "Mikrofon tidak dapat diakses di perangkat ini. Ketik transkrip di bawah."
        }
    }

    fun stopMediaRecorderAndSend() {
        val file = recordedAudioFile
        releaseRecorder()
        isListening = false
        usingAudioRecorder = false
        rmsLevel = 0f

        if (file != null && file.exists() && file.length() > 0L) {
            voiceStatus = "Mengirim rekaman suara ke Saku AI..."
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    val data = file.readBytes()
                    file.delete()
                    data
                }
                if (bytes.isNotEmpty()) {
                    onAudioReady(bytes, "audio/mp4")
                } else {
                    voiceStatus = "Rekaman kosong. Coba rekam ulang atau ketik di bawah."
                }
            }
        } else {
            voiceStatus = "Rekaman suara terlalu singkat. Coba lagi."
        }
    }

    fun startListeningInternal() {
        if (speechRecognizer == null) {
            startMediaRecorderFallback()
            return
        }
        usingAudioRecorder = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                voiceStatus = "Mendengarkan... Silakan ucapkan transaksi Anda"
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                rmsLevel = 0f
                voiceStatus = "Memproses suara..."
            }

            override fun onError(error: Int) {
                isListening = false
                rmsLevel = 0f
                if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_SERVER) {
                    startMediaRecorderFallback()
                    return
                }
                voiceStatus = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Suara tidak dikenali. Coba ucapkan lebih jelas atau ketik di bawah."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tidak ada suara terdeteksi."
                    else -> "Gagal mengenali suara (kode $error). Ketik atau coba rekam ulang."
                }
            }

            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) {
                    transcript = spoken
                    voiceStatus = "Transkrip siap! Mengirim ke Saku AI..."
                    onTranscriptReady(spoken)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    transcript = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        runCatching {
            speechRecognizer.startListening(intent)
        }.onFailure {
            startMediaRecorderFallback()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListeningInternal()
        } else {
            voiceStatus = "Izin mikrofon ditolak. Anda dapat mengetik transkrip di bawah."
        }
    }

    LaunchedEffect(autoStart) {
        if (autoStart && parseState is AiParseState.Idle) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                startListeningInternal()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = voiceStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Animated Waveform Canvas
        VoiceWaveformBars(
            isListening = isListening,
            rmsLevel = rmsLevel,
            barColor = if (isListening) extended.income else MaterialTheme.colorScheme.outline
        )

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (isListening) extended.expense else MaterialTheme.colorScheme.primary)
                .clickable {
                    if (isListening) {
                        if (usingAudioRecorder) {
                            stopMediaRecorderAndSend()
                        } else {
                            speechRecognizer?.stopListening()
                            isListening = false
                        }
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            startListeningInternal()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
                .testTag("voice_record_toggle_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = if (isListening) "Hentikan rekaman" else "Mulai rekam suara",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        OutlinedTextField(
            value = transcript,
            onValueChange = { transcript = it },
            label = { Text("Hasil / Edit Transkrip Suara") },
            placeholder = { Text("cth: Beli nasi padang 32rb pakai BCA") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Button(
            onClick = { onTranscriptReady(transcript) },
            enabled = transcript.isNotBlank() && parseState !is AiParseState.Loading,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(
                text = "Proses Transkrip dengan AI",
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }

        AiParseResultSection(
            parseState = parseState,
            wallets = wallets,
            accentColor = extended.income,
            onUpdateDraft = onUpdateDraft,
            onRemoveDraft = onRemoveDraft,
            onSaveDrafts = onSaveDrafts,
            onRetry = {
                if (transcript.isNotBlank()) onTranscriptReady(transcript)
            },
            onReset = onClearPreview
        )
    }
}

@Composable
private fun VoiceWaveformBars(
    isListening: Boolean,
    rmsLevel: Float,
    barColor: Color
) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isListening, rmsLevel) {
        if (isListening) {
            while (isListening) {
                phase += 0.35f
                kotlinx.coroutines.delay(50L)
            }
        }
    }
    val animatedAmp by animateFloatAsState(
        targetValue = if (isListening) rmsLevel.coerceAtLeast(0.25f) else 0.1f,
        animationSpec = tween(100),
        label = "waveform_amp"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        val barCount = 24
        val spacing = size.width / barCount
        val barWidth = (spacing * 0.45f).coerceAtLeast(4.dp.toPx())
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val wave = if (isListening) {
                ((sin(phase + i * 0.55f) + 1f) * 0.5f) * animatedAmp
            } else {
                0.14f
            }
            val barHeight = (size.height * 0.85f * wave).coerceAtLeast(6.dp.toPx())
            val x = i * spacing + (spacing - barWidth) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun ReceiptScanTab(
    parseState: AiParseState,
    wallets: List<WalletEntity>,
    onBitmapReady: (Bitmap) -> Unit,
    onUpdateDraft: (ParsedTransactionDraft) -> Unit,
    onRemoveDraft: (String) -> Unit,
    onSaveDrafts: (List<ParsedTransactionDraft>) -> Unit,
    onClearPreview: () -> Unit
) {
    val context = LocalContext.current
    val extended = LocalSakuColors.current
    val scope = rememberCoroutineScope()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scanNotice by remember { mutableStateOf<String?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }

    fun decodeSampledUri(uri: Uri): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        var sampleSize = 1
        while (maxSide / sampleSize > 1400) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { runCatching { decodeSampledUri(uri) }.getOrNull() }
                if (bmp != null) {
                    previewBitmap = bmp
                    scanNotice = null
                    onBitmapReady(bmp)
                } else {
                    scanNotice = "Gagal memuat gambar yang dipilih."
                }
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingPhotoUri
        val file = pendingPhotoFile
        if (success && uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    val decoded = runCatching { decodeSampledUri(uri) }.getOrNull()
                    file?.delete()
                    decoded
                }
                if (bmp != null) {
                    previewBitmap = bmp
                    scanNotice = null
                    onBitmapReady(bmp)
                } else {
                    scanNotice = "Gagal membaca foto dari kamera."
                }
            }
        } else {
            file?.delete()
        }
    }

    fun launchFullResCamera() {
        val tempFile = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        }.getOrNull()
        if (uri != null) {
            pendingPhotoFile = tempFile
            pendingPhotoUri = uri
            takePictureLauncher.launch(uri)
        } else {
            scanNotice = "Tidak dapat membuka file sementara kamera."
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchFullResCamera()
        } else {
            scanNotice = "Izin kamera ditolak. Gunakan Pilih Gambar untuk memilih screenshot atau foto struk."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Foto struk belanja atau pilih screenshot mutasi m-banking / QRIS untuk diekstrak otomatis oleh Saku AI.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val hasCam = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasCam) {
                        launchFullResCamera()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("scan_camera_btn")
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kamera",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("scan_gallery_btn")
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pilih Gambar",
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!scanNotice.isNullOrBlank()) {
            Text(
                text = scanNotice!!,
                style = MaterialTheme.typography.bodySmall,
                color = extended.expense
            )
        }

        previewBitmap?.let { bmp ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, extended.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Preview struk",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }

        AiParseResultSection(
            parseState = parseState,
            wallets = wallets,
            accentColor = extended.income,
            onUpdateDraft = onUpdateDraft,
            onRemoveDraft = onRemoveDraft,
            onSaveDrafts = onSaveDrafts,
            onRetry = { previewBitmap?.let(onBitmapReady) },
            onReset = onClearPreview
        )
    }
}

@Composable
private fun AiParseResultSection(
    parseState: AiParseState,
    wallets: List<WalletEntity>,
    accentColor: Color,
    onUpdateDraft: (ParsedTransactionDraft) -> Unit,
    onRemoveDraft: (String) -> Unit,
    onSaveDrafts: (List<ParsedTransactionDraft>) -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit
) {
    val extended = LocalSakuColors.current
    when (parseState) {
        is AiParseState.Idle -> Unit
        is AiParseState.Loading -> {
            FintechCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Column {
                        Text(
                            text = "Gemini sedang membaca transaksi...",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Menentukan nominal, kategori, dan dompet secara otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is AiParseState.Error -> {
            FintechCard(
                containerColor = extended.expenseContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Gagal memproses AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = extended.expense
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parseState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRetry) { Text("Coba Lagi") }
                    TextButton(onClick = onReset) { Text("Tutup") }
                }
            }
        }

        is AiParseState.Preview -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Pratinjau AI (${parseState.drafts.size} Transaksi)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onReset) { Text("Reset") }
                }

                parseState.drafts.forEach { draft ->
                    EditableDraftCard(
                        draft = draft,
                        wallets = wallets,
                        onUpdate = onUpdateDraft,
                        onRemove = { onRemoveDraft(draft.localId) }
                    )
                }

                val allValid = parseState.drafts.isNotEmpty() && parseState.drafts.all { it.amount > 0L }
                Button(
                    onClick = { onSaveDrafts(parseState.drafts) },
                    enabled = allValid,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_ai_drafts_btn")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan ${parseState.drafts.size} Transaksi")
                }
            }
        }
    }
}

@Composable
private fun EditableDraftCard(
    draft: ParsedTransactionDraft,
    wallets: List<WalletEntity>,
    onUpdate: (ParsedTransactionDraft) -> Unit,
    onRemove: () -> Unit
) {
    FintechCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = draft.type == TransactionType.EXPENSE,
                    onClick = { onUpdate(draft.copy(type = TransactionType.EXPENSE)) },
                    label = { Text("Pengeluaran", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = draft.type == TransactionType.INCOME,
                    onClick = { onUpdate(draft.copy(type = TransactionType.INCOME)) },
                    label = { Text("Pemasukan", style = MaterialTheme.typography.labelSmall) }
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus draft")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft.merchant,
                onValueChange = { onUpdate(draft.copy(merchant = it)) },
                label = { Text("Merchant") },
                singleLine = true,
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = if (draft.amount == 0L) "" else draft.amount.toString(),
                onValueChange = { raw ->
                    val num = raw.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    onUpdate(draft.copy(amount = num))
                },
                label = { Text("Nominal (Rp)") },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Dompet: ${draft.walletName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            wallets.forEach { w ->
                FilterChip(
                    selected = draft.walletName.equals(w.name, ignoreCase = true),
                    onClick = { onUpdate(draft.copy(walletName = w.name)) },
                    label = { Text(w.name, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Kategori: ${draft.category}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FinanceCategories.aiCategoryNames.forEach { cat ->
                FilterChip(
                    selected = draft.category == cat,
                    onClick = { onUpdate(draft.copy(category = cat)) },
                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = draft.note,
            onValueChange = { onUpdate(draft.copy(note = it)) },
            label = { Text("Catatan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualTransactionTab(
    editing: TransactionEntity?,
    initialTimestamp: Long? = null,
    wallets: List<WalletEntity>,
    defaultWalletId: Long? = null,
    onAutoTagCategory: suspend (description: String, txType: String, allowCloudFallback: Boolean) -> AutoTagSuggestion?,
    onSave: (
        editingOld: TransactionEntity?,
        merchant: String,
        amount: Long,
        type: String,
        category: String,
        walletId: Long,
        targetWalletId: Long?,
        note: String,
        timestamp: Long
    ) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val extended = LocalSakuColors.current
    val scope = rememberCoroutineScope()

    var txType by remember(editing?.id) { mutableStateOf(editing?.type ?: TransactionType.EXPENSE) }
    var expression by remember(editing?.id) { mutableStateOf(editing?.amount?.toString() ?: "") }
    var showNumpad by remember { mutableStateOf(true) }
    var merchant by remember(editing?.id) { mutableStateOf(editing?.merchant ?: "") }
    var note by remember(editing?.id) { mutableStateOf(editing?.note ?: "") }
    var txTimestamp by remember(editing?.id, initialTimestamp) {
        mutableStateOf(editing?.timestamp ?: initialTimestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedCategory by remember(editing?.id) {
        mutableStateOf(
            editing?.category ?: if (txType == TransactionType.INCOME) "Gaji" else "Makanan & Minuman"
        )
    }
    var userManuallyPickedCategory by remember(editing?.id) { mutableStateOf(editing != null) }
    var autoTagSuggestion by remember(editing?.id) { mutableStateOf<AutoTagSuggestion?>(null) }
    var isAutoTagging by remember(editing?.id) { mutableStateOf(false) }

    val preferredWalletId = remember(editing?.id, defaultWalletId, wallets) {
        editing?.walletId
            ?: defaultWalletId?.takeIf { id -> wallets.any { it.id == id } }
            ?: wallets.firstOrNull()?.id
            ?: 1L
    }
    var selectedWalletId by remember(editing?.id, preferredWalletId) {
        mutableStateOf(preferredWalletId)
    }
    var targetWalletId by remember(editing?.id, preferredWalletId) {
        mutableStateOf(
            editing?.targetWalletId ?: wallets.firstOrNull { it.id != preferredWalletId }?.id
        )
    }

    LaunchedEffect(wallets) {
        if (wallets.isNotEmpty() && wallets.none { it.id == selectedWalletId }) {
            selectedWalletId = defaultWalletId?.takeIf { id -> wallets.any { it.id == id } }
                ?: wallets.first().id
        }
        if (wallets.size >= 2 && (targetWalletId == null || targetWalletId == selectedWalletId || wallets.none { it.id == targetWalletId })) {
            targetWalletId = wallets.firstOrNull { it.id != selectedWalletId }?.id
        }
    }

    // Automatic hybrid tagging as user types Merchant/Note (0ms local history+keywords first, then Gemini 3.8 Flash)
    LaunchedEffect(merchant, note, txType, editing?.id) {
        if (editing != null || userManuallyPickedCategory || txType == TransactionType.TRANSFER) {
            return@LaunchedEffect
        }
        val combined = "$merchant $note".trim()
        if (combined.length < 2) {
            autoTagSuggestion = null
            return@LaunchedEffect
        }

        val validNames = if (txType == TransactionType.INCOME) {
            FinanceCategories.incomeCategoryNames
        } else {
            FinanceCategories.expenseCategoryNames
        }

        // Instant zero-token local pass first
        val localHit = onAutoTagCategory(combined, txType, false)
        if (localHit != null && localHit.category in validNames) {
            selectedCategory = localHit.category
            autoTagSuggestion = localHit
            return@LaunchedEffect
        }

        // Cloud AI fallback only when unfamiliar merchant/description >= 4 chars after debounce
        if (combined.length >= 4) {
            delay(450)
            isAutoTagging = true
            val aiHit = onAutoTagCategory(combined, txType, true)
            isAutoTagging = false
            if (aiHit != null && aiHit.category in validNames) {
                selectedCategory = aiHit.category
                autoTagSuggestion = aiHit
            }
        }
    }

    val evaluatedAmount = remember(expression) { MathEvaluator.evaluate(expression) ?: 0L }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Segmented Type Selector
        val typeOptions = listOf(
            TransactionType.EXPENSE to "Pengeluaran",
            TransactionType.INCOME to "Pemasukan",
            TransactionType.TRANSFER to "Transfer"
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            typeOptions.forEachIndexed { index, (code, label) ->
                SegmentedButton(
                    selected = txType == code,
                    onClick = {
                        txType = code
                        if (code == TransactionType.INCOME && selectedCategory !in FinanceCategories.incomeCategoryNames) {
                            selectedCategory = "Gaji"
                        } else if (code == TransactionType.EXPENSE && selectedCategory !in FinanceCategories.expenseCategoryNames) {
                            selectedCategory = "Makanan & Minuman"
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = typeOptions.size)
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }

        // Amount & Calculator Display Card + Direct Input
        FintechCard(
            onClick = { showNumpad = !showNumpad },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (expression.isBlank()) "Masukkan nominal (ketuk kalkulator / ketik)" else expression,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(evaluatedAmount),
                        style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                        color = when (txType) {
                            TransactionType.INCOME -> extended.income
                            TransactionType.EXPENSE -> extended.expense
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Calculate,
                    contentDescription = "Tampilkan/sembunyikan kalkulator",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        OutlinedTextField(
            value = expression,
            onValueChange = { raw ->
                expression = raw.filter { it.isDigit() || it in charArrayOf('+', '-', '*', '/', '.') }
            },
            label = { Text("Nominal / Ekspresi (cth: 25000 atau 15000+5000)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_amount_input")
        )

        // Interactive Calculator Numpad
        AnimatedVisibility(visible = showNumpad) {
            CalculatorNumpad(
                onKeyPress = { key ->
                    when (key) {
                        "C" -> expression = ""
                        "⌫" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
                        "=" -> {
                            val res = MathEvaluator.evaluate(expression)
                            if (res != null) expression = res.toString()
                        }
                        "+", "-", "*", "/" -> {
                            if (expression.isNotEmpty() && expression.last() !in listOf('+', '-', '*', '/')) {
                                expression += key
                            }
                        }
                        else -> expression += key
                    }
                }
            )
        }

        if (txType != TransactionType.TRANSFER) {
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Judul") },
                placeholder = { Text("cth: Kopi Kenangan, Indomaret, Gaji") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_merchant_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    autoTagSuggestion?.let { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = extended.incomeContainer,
                            modifier = Modifier.testTag("manual_auto_tag_badge")
                        ) {
                            Text(
                                text = suggestion.sourceLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = extended.income,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                val canRunManualTag = "$merchant $note".trim().length >= 2 && !isAutoTagging
                Surface(
                    onClick = {
                        val combined = "$merchant $note".trim()
                        if (combined.length >= 2) {
                            scope.launch {
                                isAutoTagging = true
                                userManuallyPickedCategory = false
                                val hit = onAutoTagCategory(combined, txType, true)
                                isAutoTagging = false
                                val validNames = if (txType == TransactionType.INCOME) {
                                    FinanceCategories.incomeCategoryNames
                                } else {
                                    FinanceCategories.expenseCategoryNames
                                }
                                if (hit != null && hit.category in validNames) {
                                    selectedCategory = hit.category
                                    autoTagSuggestion = hit
                                }
                            }
                        }
                    },
                    enabled = canRunManualTag,
                    shape = RoundedCornerShape(999.dp),
                    color = if (canRunManualTag) extended.incomeContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("manual_auto_tag_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isAutoTagging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(11.dp),
                                strokeWidth = 1.6.dp,
                                color = extended.income
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Saran kategori otomatis",
                                tint = if (canRunManualTag) extended.income else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = if (isAutoTagging) "Menandai..." else "Auto-Tag",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (canRunManualTag) extended.income else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val catNames = if (txType == TransactionType.INCOME) {
                FinanceCategories.incomeCategoryNames
            } else {
                FinanceCategories.expenseCategoryNames
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(catNames) { catName ->
                    val meta = FinanceCategories.getMeta(catName)
                    val selected = selectedCategory == catName
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedCategory = catName
                                userManuallyPickedCategory = true
                                autoTagSuggestion = null
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) meta.color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (selected) BorderStroke(1.5.dp, meta.color) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = meta.icon,
                                contentDescription = catName,
                                tint = if (selected) meta.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = catName,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Wallet Selector
        Text(
            text = if (txType == TransactionType.TRANSFER) "Dari Dompet" else "Dompet",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            wallets.forEach { wallet ->
                FilterChip(
                    selected = selectedWalletId == wallet.id,
                    onClick = { selectedWalletId = wallet.id },
                    label = { Text(wallet.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (txType == TransactionType.TRANSFER) {
            Text(
                text = "Ke Dompet",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (wallets.size < 2) {
                Text(
                    text = "Tambahkan minimal 2 dompet di tab Dompet untuk melakukan transfer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.expense
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.filter { it.id != selectedWalletId }.forEach { wallet ->
                        FilterChip(
                            selected = targetWalletId == wallet.id,
                            onClick = { targetWalletId = wallet.id },
                            label = { Text(wallet.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extended.income,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        } else {
            // Date Selector for Expense/Income
            Text(
                text = "Tanggal",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val nowMs = System.currentTimeMillis()
            val todaySelected = DateFormatter.startOfDay(txTimestamp) == DateFormatter.startOfDay(nowMs)
            val yesterdayMs = nowMs - TimeUnit.DAYS.toMillis(1)
            val yesterdaySelected = DateFormatter.startOfDay(txTimestamp) == DateFormatter.startOfDay(yesterdayMs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = todaySelected,
                    onClick = { txTimestamp = System.currentTimeMillis() },
                    label = { Text("Hari Ini") }
                )
                FilterChip(
                    selected = yesterdaySelected,
                    onClick = { txTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1) },
                    label = { Text("Kemarin") }
                )
                FilterChip(
                    selected = !todaySelected && !yesterdaySelected,
                    onClick = { showDatePicker = true },
                    leadingIcon = {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = {
                        Text(
                            if (!todaySelected && !yesterdaySelected) DateFormatter.formatShortDate(txTimestamp)
                            else "Pilih Tanggal"
                        )
                    }
                )
            }
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
            if (editing != null) {
                OutlinedButton(
                    onClick = { onDelete(editing) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = extended.expense),
                    modifier = Modifier.heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus")
                }
            }

            Button(
                onClick = {
                    onSave(
                        editing,
                        merchant,
                        evaluatedAmount,
                        txType,
                        selectedCategory,
                        selectedWalletId,
                        targetWalletId,
                        note,
                        txTimestamp
                    )
                },
                enabled = evaluatedAmount > 0L && (txType != TransactionType.TRANSFER || (targetWalletId != null && targetWalletId != selectedWalletId)),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("manual_save_btn")
            ) {
                Text(
                    text = if (editing != null) "Simpan Perubahan" else "Simpan Transaksi",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateFormatter.localMillisToUtcPickerMillis(txTimestamp)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { utc ->
                            val pickedLocal = DateFormatter.utcPickerMillisToLocalNoon(utc)
                            txTimestamp = if (DateFormatter.startOfDay(pickedLocal) == DateFormatter.startOfDay(System.currentTimeMillis())) {
                                System.currentTimeMillis()
                            } else {
                                pickedLocal
                            }
                        }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun CalculatorNumpad(
    onKeyPress: (String) -> Unit
) {
    val extended = LocalSakuColors.current
    val rows = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("C", "0", "000", "+"),
        listOf("⌫", "=")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    val isOperator = key in listOf("+", "-", "*", "/")
                    val isEquals = key == "="
                    val isClear = key == "C"
                    Surface(
                        modifier = Modifier
                            .weight(if (isEquals) 2f else 1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onKeyPress(key) }
                            .testTag("calc_key_$key"),
                        shape = RoundedCornerShape(14.dp),
                        border = if (!isEquals && !isClear && !isOperator) {
                            BorderStroke(0.8.dp, extended.cardBorder)
                        } else null,
                        color = when {
                            isEquals -> extended.lime
                            isOperator -> extended.limeContainer
                            isClear -> extended.expenseContainer
                            else -> extended.elevatedCard
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when (key) {
                                "⌫" -> Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "Hapus karakter",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                else -> Text(
                                    text = when (key) {
                                        "*" -> "×"
                                        "/" -> "÷"
                                        else -> key
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = when {
                                        isEquals -> extended.onLime
                                        isOperator -> extended.onLimeContainer
                                        isClear -> extended.expense
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
