package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.FreshAtmosphericBackdrop
import com.example.ui.components.SakuBottomDrawer
import com.example.ui.components.SakuBrandMark
import com.example.ui.theme.LocalSakuColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    displayName: String,
    hasPin: Boolean,
    biometricEnabled: Boolean,
    lockSessionId: Int,
    biometricStatusLabel: String,
    pinCooldownSeconds: Int,
    onRequestBiometric: () -> Unit,
    onVerifyPin: suspend (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val extended = LocalSakuColors.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var pinDigits by remember(lockSessionId) { mutableStateOf("") }
    var errorMessage by remember(lockSessionId) { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }

    // Prevent back button from bypassing lock gate; minimize app safely instead.
    BackHandler(enabled = true) {
        (context as? Activity)?.moveTaskToBack(true)
    }

    // Automatically invoke androidx.biometric BiometricPrompt once the Activity is RESUMED
    // for every fresh lock session (cold launch or returning from background).
    LaunchedEffect(lockSessionId, biometricEnabled) {
        if (biometricEnabled) {
            lifecycleOwner.lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
            onRequestBiometric()
        }
    }

    fun submitPin(showFailureWhenShort: Boolean) {
        if (pinCooldownSeconds > 0) {
            errorMessage = "Terlalu banyak percobaan. Tunggu ${pinCooldownSeconds}d atau pakai biometrik."
            return
        }
        if (verifying || pinDigits.length < 4) {
            if (showFailureWhenShort && pinDigits.isNotEmpty()) {
                errorMessage = "PIN minimal 4 digit"
            }
            return
        }
        verifying = true
        scope.launch {
            val ok = onVerifyPin(pinDigits)
            verifying = false
            if (!ok) {
                if (showFailureWhenShort || pinDigits.length >= 6) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    errorMessage = if (pinCooldownSeconds > 0) {
                        "Terlalu banyak percobaan. Tunggu ${pinCooldownSeconds}d."
                    } else {
                        "PIN tidak cocok, coba lagi"
                    }
                    pinDigits = ""
                }
            }
        }
    }

    fun appendDigit(digit: String) {
        if (verifying || pinCooldownSeconds > 0 || pinDigits.length >= 6) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        errorMessage = null
        pinDigits += digit
        if (pinDigits.length >= 4) {
            submitPin(showFailureWhenShort = false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("app_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        FreshAtmosphericBackdrop(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!hasPin) {
            BiometricOnlyLockView(
                displayName = displayName,
                biometricStatusLabel = biometricStatusLabel,
                onRequestBiometric = onRequestBiometric
            )
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        SakuBrandMark(
                            size = 68.dp,
                            cornerRadius = 21.dp
                        )
                        Surface(
                            shape = CircleShape,
                            color = extended.lime,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = extended.onLime,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (displayName.isNotBlank()) "Saku • $displayName" else "Saku Terkunci",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val helperText = when {
                        pinCooldownSeconds > 0 ->
                            "Terlalu banyak percobaan PIN. Tunggu ${pinCooldownSeconds}d atau buka dengan biometrik."
                        errorMessage != null -> errorMessage!!
                        else -> "Masukkan PIN 4–6 digit atau gunakan biometrik"
                    }
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pinCooldownSeconds > 0 || errorMessage != null) {
                            extended.expense
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val dotCount = pinDigits.length.coerceAtLeast(4).coerceAtMost(6)
                        for (i in 0 until dotCount) {
                            val filled = i < pinDigits.length
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) extended.lime else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { digit ->
                                LockKeypadButton(
                                    label = digit,
                                    enabled = pinCooldownSeconds == 0,
                                    onClick = { appendDigit(digit) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (biometricEnabled) {
                            Surface(
                                onClick = onRequestBiometric,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.9.dp, extended.cardBorder),
                                modifier = Modifier
                                    .size(68.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("lock_biometric_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = "Buka dengan Biometrik",
                                        tint = extended.onLimeContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(68.dp))
                        }

                        LockKeypadButton(
                            label = "0",
                            enabled = pinCooldownSeconds == 0,
                            onClick = { appendDigit("0") }
                        )

                        if (pinDigits.isNotEmpty()) {
                            Surface(
                                onClick = {
                                    errorMessage = null
                                    pinDigits = pinDigits.dropLast(1)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.9.dp, extended.cardBorder),
                                modifier = Modifier
                                    .size(68.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("lock_backspace_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = "Hapus angka",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(68.dp))
                        }
                    }

                    if (pinDigits.length >= 4) {
                        Button(
                            onClick = { submitPin(showFailureWhenShort = true) },
                            enabled = pinCooldownSeconds == 0 && !verifying,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extended.lime,
                                contentColor = extended.onLime
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("lock_submit_pin_btn")
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Saku", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun BiometricOnlyLockView(
    displayName: String,
    biometricStatusLabel: String,
    onRequestBiometric: () -> Unit
) {
    val extended = LocalSakuColors.current
    val inf = rememberInfiniteTransition(label = "biometric_pulse")
    val pulse by inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxSize()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.9.dp, extended.cardBorder)
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SakuBrandMark(size = 22.dp, cornerRadius = 7.dp)
                Text(
                    text = "Saku • Akses Biometrik",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                        .clip(CircleShape)
                        .background(extended.lime.copy(alpha = 0.14f))
                )
                Surface(
                    onClick = onRequestBiometric,
                    shape = CircleShape,
                    color = extended.lime,
                    modifier = Modifier
                        .size(86.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = "Verifikasi Sidik Jari atau Wajah",
                            tint = extended.onLime,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Text(
                text = if (displayName.isNotBlank()) "Selamat Datang, $displayName" else "Buka Kunci Saku",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Gunakan sidik jari, pengenalan wajah, atau kunci perangkat untuk mengakses data keuanganmu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = biometricStatusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = extended.onLimeContainer,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onRequestBiometric,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = extended.lime,
                contentColor = extended.onLime
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("lock_biometric_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Verifikasi Biometrik", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LockKeypadButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val extended = LocalSakuColors.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.9.dp, extended.cardBorder),
        modifier = Modifier
            .size(68.dp)
            .minimumInteractiveComponentSize()
            .testTag("lock_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp
                ),
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                }
            )
        }
    }
}

@Composable
fun SetupPinDialog(
    initialBiometricEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (pin: String, biometricEnabled: Boolean) -> Unit
) {
    val extended = LocalSakuColors.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var useBiometric by remember { mutableStateOf(initialBiometricEnabled) }
    val cleanPin = pin.filter { it.isDigit() }
    val cleanConfirm = confirmPin.filter { it.isDigit() }
    val isValid = cleanPin.length in 4..6 && cleanPin == cleanConfirm

    SakuBottomDrawer(
        title = "Atur PIN Keamanan",
        subtitle = "PIN 4–6 digit disimpan dalam bentuk hash terenkripsi (PBKDF2) di perangkatmu.",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) },
            label = { Text("PIN Baru (4–6 digit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it.filter { ch -> ch.isDigit() }.take(6) },
            label = { Text("Ulangi PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmPin.isNotEmpty() && cleanPin != cleanConfirm,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable { useBiometric = !useBiometric }
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Checkbox(checked = useBiometric, onCheckedChange = { useBiometric = it })
            Text(
                text = "Izinkan buka cepat dengan Sidik Jari / Wajah",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
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
                onClick = { onConfirm(cleanPin, useBiometric) },
                enabled = isValid,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.lime,
                    contentColor = extended.onLime
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
            ) {
                Text("Simpan PIN", fontWeight = FontWeight.Bold)
            }
        }
    }
}
