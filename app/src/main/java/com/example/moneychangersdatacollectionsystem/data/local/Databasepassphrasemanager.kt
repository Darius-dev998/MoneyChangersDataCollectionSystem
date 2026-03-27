package com.example.moneychangersdatacollectionsystem.data.local

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DatabasePassphraseManager — Secure SQLCipher database passphrase management.
 *
 * Generates a cryptographically random 256-bit passphrase on first launch,
 * encrypts it with a hardware-backed AES-256-GCM key stored in the
 * Android Keystore, and persists the encrypted passphrase in SharedPreferences.
 *
 * WHY THIS EXISTS:
 * The original DatabaseModule used "mcdcs_secure_passphrase".toByteArray() —
 * a hardcoded plaintext string. Anyone who decompiles the release APK (trivial
 * with jadx or apktool) immediately has the database decryption key.
 * For a financial data collection system this is unacceptable.
 *
 * SECURITY PROPERTIES:
 *  — Passphrase: 32 bytes (256 bits) from SecureRandom. Unique per installation.
 *  — Wrapped by: AES-256-GCM key bound to the Android Keystore.
 *  — Storage: Encrypted passphrase + IV stored in private SharedPreferences.
 *  — Hardware: Attempts StrongBox (physical secure element) first,
 *              falls back to TEE (Trusted Execution Environment).
 *
 * IMPORTANT: If getPassphrase() throws, DO NOT silently generate a new one.
 * A new passphrase would permanently lock the existing database.
 * The caller (DatabaseModule) must propagate the exception.
 */
@Singleton
class DatabasePassphraseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DBPassphraseManager"
        private const val KEY_ALIAS = "MCDCS_DB_KEY_V1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "mcdcs_secure_vault"
        private const val ENCRYPTED_PASSPHRASE_KEY = "db_passphrase_enc"
        private const val IV_KEY = "db_passphrase_iv"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val PASSPHRASE_LENGTH_BYTES = 32
    }

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    /**
     * Returns the database passphrase as a ByteArray.
     *
     * On first call: generates a random passphrase, encrypts and persists it.
     * On subsequent calls: decrypts and returns the stored passphrase.
     *
     * @throws Exception if the Keystore key is unavailable (e.g., device is
     *         not yet unlocked). Caller must handle — do NOT silently rotate.
     */
    fun getPassphrase(): ByteArray {
        val encryptedB64 = prefs.getString(ENCRYPTED_PASSPHRASE_KEY, null)
        val ivB64 = prefs.getString(IV_KEY, null)

        if (encryptedB64 == null || ivB64 == null) {
            // First run — generate, encrypt, and store a new passphrase
            Log.i(TAG, "First run: generating database passphrase.")
            val passphrase = ByteArray(PASSPHRASE_LENGTH_BYTES).also {
                SecureRandom().nextBytes(it)
            }
            encryptAndStore(passphrase)
            return passphrase
        }

        // Subsequent runs — decrypt the stored passphrase
        return try {
            val encrypted = Base64.decode(encryptedB64, Base64.DEFAULT)
            val iv = Base64.decode(ivB64, Base64.DEFAULT)
            decrypt(encrypted, iv)
        } catch (e: Exception) {
            // DO NOT generate a new passphrase here — that would permanently
            // lock the existing database. Log and re-throw so the caller can
            // surface a meaningful error (e.g., "Please unlock your device").
            Log.e(TAG, "Passphrase decryption failed. Device may need to be unlocked.", e)
            throw Exception(
                "Database passphrase unavailable. Ensure the device is unlocked and try again.",
                e
            )
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existing?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        return try {
            generateKey(useStrongBox = true)
        } catch (e: Exception) {
            Log.w(TAG, "StrongBox unavailable, using TEE: ${e.message}")
            generateKey(useStrongBox = false)
        }
    }

    private fun generateKey(useStrongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (useStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        ).apply { init(builder.build()) }.generateKey()
    }

    private fun encryptAndStore(passphrase: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(passphrase)
        prefs.edit()
            .putString(ENCRYPTED_PASSPHRASE_KEY, Base64.encodeToString(encrypted, Base64.DEFAULT))
            .putString(IV_KEY, Base64.encodeToString(iv, Base64.DEFAULT))
            .apply()
    }

    private fun decrypt(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(encrypted)
    }
}