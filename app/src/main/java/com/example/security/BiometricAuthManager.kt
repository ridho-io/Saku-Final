package com.example.security

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

enum class BiometricCapability {
    BIOMETRIC_READY,
    DEVICE_CREDENTIAL_ONLY,
    NOT_ENROLLED,
    UNAVAILABLE
}

enum class BiometricFailureKind {
    USER_CANCELED,
    USE_PIN_FALLBACK,
    LOCKOUT_TEMPORARY,
    LOCKOUT_PERMANENT,
    NOT_ENROLLED,
    HARDWARE_UNAVAILABLE,
    OTHER
}

data class BiometricStatusInfo(
    val capability: BiometricCapability,
    val supportsStrongBiometric: Boolean,
    val supportsDeviceCredential: Boolean,
    val statusLabel: String
) {
    val canAuthenticateAtAll: Boolean
        get() = capability == BiometricCapability.BIOMETRIC_READY ||
            capability == BiometricCapability.DEVICE_CREDENTIAL_ONLY
}

object BiometricAuthManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val BIOMETRIC_KEY_ALIAS = "saku_biometric_launch_key"

    fun inspectStatus(context: Context): BiometricStatusInfo {
        val manager = BiometricManager.from(context)
        val strongStatus = manager.canAuthenticate(BIOMETRIC_STRONG)
        val biometricStatus = manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        val deviceCredStatus = manager.canAuthenticate(DEVICE_CREDENTIAL)

        val supportsStrong = strongStatus == BiometricManager.BIOMETRIC_SUCCESS
        val supportsDeviceCred = deviceCredStatus == BiometricManager.BIOMETRIC_SUCCESS

        return when {
            biometricStatus == BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricStatusInfo(
                    capability = BiometricCapability.BIOMETRIC_READY,
                    supportsStrongBiometric = supportsStrong,
                    supportsDeviceCredential = supportsDeviceCred,
                    statusLabel = if (supportsStrong) {
                        "Sidik jari / biometrik kuat siap digunakan"
                    } else {
                        "Biometrik perangkat siap digunakan"
                    }
                )
            }

            supportsDeviceCred -> {
                BiometricStatusInfo(
                    capability = BiometricCapability.DEVICE_CREDENTIAL_ONLY,
                    supportsStrongBiometric = false,
                    supportsDeviceCredential = true,
                    statusLabel = if (biometricStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                        "Biometrik belum didaftarkan • PIN/Pola perangkat siap"
                    } else {
                        "Menggunakan kunci layar perangkat (PIN/Pola/Sandi)"
                    }
                )
            }

            biometricStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricStatusInfo(
                    capability = BiometricCapability.NOT_ENROLLED,
                    supportsStrongBiometric = false,
                    supportsDeviceCredential = false,
                    statusLabel = "Sidik jari atau wajah belum didaftarkan di Pengaturan HP"
                )
            }

            else -> {
                BiometricStatusInfo(
                    capability = BiometricCapability.UNAVAILABLE,
                    supportsStrongBiometric = false,
                    supportsDeviceCredential = false,
                    statusLabel = "Perangkat tidak mendukung sensor biometrik"
                )
            }
        }
    }

    fun buildEnrollIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_STRONG or BIOMETRIC_WEAK
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }.also { intent ->
            if (intent.resolveActivity(context.packageManager) == null) {
                intent.action = Settings.ACTION_SECURITY_SETTINGS
                intent.removeExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED)
            }
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String? = null,
        allowDeviceCredentialFallback: Boolean,
        negativeButtonText: String = "Gunakan PIN",
        confirmationRequired: Boolean = false,
        onPromptStarted: () -> Unit = {},
        onPromptFinished: () -> Unit = {},
        onSuccess: () -> Unit,
        onAuthFailedAttempt: () -> Unit = {},
        onFailure: (BiometricFailureKind, String) -> Unit
    ) {
        val status = inspectStatus(activity)

        val useDeviceCredential = allowDeviceCredentialFallback && status.supportsDeviceCredential
        if (status.capability == BiometricCapability.UNAVAILABLE ||
            (status.capability == BiometricCapability.NOT_ENROLLED && !useDeviceCredential)
        ) {
            val kind = if (status.capability == BiometricCapability.NOT_ENROLLED) {
                BiometricFailureKind.NOT_ENROLLED
            } else {
                BiometricFailureKind.HARDWARE_UNAVAILABLE
            }
            onFailure(kind, status.statusLabel)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onPromptFinished()
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onAuthFailedAttempt()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onPromptFinished()
                    val mapped = when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                            BiometricFailureKind.USE_PIN_FALLBACK to "Beralih ke verifikasi PIN"

                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED ->
                            BiometricFailureKind.USER_CANCELED to "Verifikasi biometrik dibatalkan"

                        BiometricPrompt.ERROR_LOCKOUT ->
                            BiometricFailureKind.LOCKOUT_TEMPORARY to "Terlalu banyak percobaan gagal. Coba lagi sebentar atau gunakan PIN."

                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                            BiometricFailureKind.LOCKOUT_PERMANENT to "Sensor biometrik terkunci sementara. Gunakan PIN atau kunci perangkat."

                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ->
                            BiometricFailureKind.NOT_ENROLLED to "Biometrik atau kunci layar belum aktif di perangkat ini."

                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                            BiometricFailureKind.HARDWARE_UNAVAILABLE to "Sensor biometrik sedang tidak tersedia."

                        else ->
                            BiometricFailureKind.OTHER to errString.toString()
                    }
                    onFailure(mapped.first, mapped.second)
                }
            }
        )

        // Note: AndroidX Biometric forbids calling setNegativeButtonText() when DEVICE_CREDENTIAL is included.
        val allowedAuthenticators = if (useDeviceCredential) {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK
        }

        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowedAuthenticators)
            .setConfirmationRequired(confirmationRequired)

        if (!description.isNullOrBlank()) {
            infoBuilder.setDescription(description)
        }
        if (!useDeviceCredential) {
            infoBuilder.setNegativeButtonText(negativeButtonText)
        }

        val promptInfo = infoBuilder.build()

        // Use Keystore-backed CryptoObject when BIOMETRIC_STRONG is exclusively used and supported;
        // gracefully fall back to standard promptInfo when DEVICE_CREDENTIAL/BIOMETRIC_WEAK or virtual HAL is active.
        val strongCryptoObject = if (!useDeviceCredential && status.supportsStrongBiometric) {
            createOrRecoverCryptoObject()
        } else {
            null
        }

        onPromptStarted()
        runCatching {
            if (strongCryptoObject != null) {
                prompt.authenticate(promptInfo, strongCryptoObject)
            } else {
                prompt.authenticate(promptInfo)
            }
        }.onFailure { err ->
            if (strongCryptoObject != null) {
                // Retry once without CryptoObject in case device keystore HAL rejects cipher binding
                runCatching {
                    prompt.authenticate(promptInfo)
                }.onFailure { retryErr ->
                    onPromptFinished()
                    onFailure(
                        BiometricFailureKind.HARDWARE_UNAVAILABLE,
                        retryErr.localizedMessage ?: "Gagal membuka dialog biometrik"
                    )
                }
            } else {
                onPromptFinished()
                onFailure(
                    BiometricFailureKind.HARDWARE_UNAVAILABLE,
                    err.localizedMessage ?: "Gagal membuka dialog biometrik"
                )
            }
        }
    }

    private fun createOrRecoverCryptoObject(): BiometricPrompt.CryptoObject? {
        return runCatching {
            val cipher = initBiometricCipher()
            BiometricPrompt.CryptoObject(cipher)
        }.recoverCatching { err ->
            if (err is KeyPermanentlyInvalidatedException) {
                deleteBiometricKey()
                BiometricPrompt.CryptoObject(initBiometricCipher())
            } else {
                throw err
            }
        }.getOrNull()
    }

    private fun initBiometricCipher(): Cipher {
        val secretKey = getOrCreateBiometricSecretKey()
        return Cipher.getInstance(
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
        ).apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
    }

    private fun getOrCreateBiometricSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val specBuilder = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            specBuilder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            specBuilder.setInvalidatedByBiometricEnrollment(true)
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(specBuilder.build())
        return keyGenerator.generateKey()
    }

    private fun deleteBiometricKey() {
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
            }
        }
    }
}
