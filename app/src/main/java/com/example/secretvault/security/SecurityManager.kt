package com.example.secretvault.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.secretvault.model.IntruderLog
import com.example.secretvault.model.VaultMode
import java.util.UUID
import javax.crypto.SecretKey

class SecurityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vault_sec_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "sec_pin_hash"
        private const val KEY_PIN_SALT = "sec_pin_salt"
        private const val KEY_DECOY_PIN_HASH = "sec_decoy_hash"
        private const val KEY_DECOY_PIN_SALT = "sec_decoy_salt"
        private const val KEY_SEC_QUESTION = "sec_question"
        private const val KEY_SEC_ANSWER_HASH = "sec_answer_hash"
        private const val KEY_SEC_ANSWER_SALT = "sec_answer_salt"
        private const val KEY_FAILED_ATTEMPTS = "sec_failed_attempts"
        private const val KEY_CALCULATOR_DISGUISE = "pref_calculator_disguise"
        private const val KEY_BIOMETRIC_ENABLED = "pref_biometric_enabled"
        private const val KEY_FLIP_TO_LOCK = "pref_flip_lock"
        private const val KEY_SHAKE_TO_LOCK = "pref_shake_lock"
        private const val KEY_SCREENSHOT_BLOCK = "pref_screenshot_block"
        private const val KEY_INTRUDER_CAPTURE = "pref_intruder_capture"

        @Volatile
        private var INSTANCE: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var currentMode: VaultMode = VaultMode.LOCKED
        private set

    var activeSecretKey: SecretKey? = null
        private set

    fun isSetupComplete(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun hasDecoyPin(): Boolean {
        return prefs.contains(KEY_DECOY_PIN_HASH)
    }

    fun setupMasterPin(pin: String, question: String, answer: String) {
        val salt = CryptoEngine.generateSalt()
        val hash = CryptoEngine.hashPin(pin, salt)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)

        val ansSalt = CryptoEngine.generateSalt()
        val ansHash = CryptoEngine.hashPin(answer.trim().lowercase(), ansSalt)
        val ansSaltB64 = Base64.encodeToString(ansSalt, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, saltB64)
            .putString(KEY_SEC_QUESTION, question)
            .putString(KEY_SEC_ANSWER_HASH, ansHash)
            .putString(KEY_SEC_ANSWER_SALT, ansSaltB64)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .apply()

        // Derive and set active key
        activeSecretKey = CryptoEngine.deriveKey(pin, salt)
        currentMode = VaultMode.MASTER
    }

    fun setupDecoyPin(pin: String) {
        val salt = CryptoEngine.generateSalt()
        val hash = CryptoEngine.hashPin(pin, salt)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_DECOY_PIN_HASH, hash)
            .putString(KEY_DECOY_PIN_SALT, saltB64)
            .apply()
    }

    fun removeDecoyPin() {
        prefs.edit()
            .remove(KEY_DECOY_PIN_HASH)
            .remove(KEY_DECOY_PIN_SALT)
            .apply()
    }

    fun verifyPin(pin: String): VaultMode {
        // Check Master PIN
        val masterSaltB64 = prefs.getString(KEY_PIN_SALT, null)
        val masterHash = prefs.getString(KEY_PIN_HASH, null)

        if (masterSaltB64 != null && masterHash != null) {
            val salt = Base64.decode(masterSaltB64, Base64.NO_WRAP)
            val hash = CryptoEngine.hashPin(pin, salt)
            if (hash == masterHash) {
                activeSecretKey = CryptoEngine.deriveKey(pin, salt)
                currentMode = VaultMode.MASTER
                resetFailedAttempts()
                return VaultMode.MASTER
            }
        }

        // Check Decoy PIN
        val decoySaltB64 = prefs.getString(KEY_DECOY_PIN_SALT, null)
        val decoyHash = prefs.getString(KEY_DECOY_PIN_HASH, null)

        if (decoySaltB64 != null && decoyHash != null) {
            val salt = Base64.decode(decoySaltB64, Base64.NO_WRAP)
            val hash = CryptoEngine.hashPin(pin, salt)
            if (hash == decoyHash) {
                activeSecretKey = CryptoEngine.deriveKey(pin, salt)
                currentMode = VaultMode.DECOY
                resetFailedAttempts()
                return VaultMode.DECOY
            }
        }

        // Failed attempt
        recordFailedAttempt(pin)
        return VaultMode.LOCKED
    }

    fun unlockWithBiometric(): Boolean {
        if (!isBiometricEnabled || !isSetupComplete()) return false
        // For biometric unlock, we require the master salt to derive key with the stored session or default
        // In real app, biometric CryptoObject unlocks Keystore key, here we use master salt
        val masterSaltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val salt = Base64.decode(masterSaltB64, Base64.NO_WRAP)
        // If master key was cached or derive with biometric token
        // In standard flow:
        currentMode = VaultMode.MASTER
        return true
    }

    private fun recordFailedAttempt(enteredPin: String) {
        val count = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, count).apply()

        // Create Intruder Log
        val log = IntruderLog(
            id = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            failedPinEntered = "*".repeat(enteredPin.length),
            attemptNumber = count
        )
        saveIntruderLog(log)
    }

    private fun resetFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply()
    }

    fun getFailedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun getSecurityQuestion(): String {
        return prefs.getString(KEY_SEC_QUESTION, "What is your primary secret code?") ?: ""
    }

    fun recoverWithAnswer(answer: String, newPin: String): Boolean {
        val ansSaltB64 = prefs.getString(KEY_SEC_ANSWER_SALT, null) ?: return false
        val ansHash = prefs.getString(KEY_SEC_ANSWER_HASH, null) ?: return false

        val salt = Base64.decode(ansSaltB64, Base64.NO_WRAP)
        val hash = CryptoEngine.hashPin(answer.trim().lowercase(), salt)
        if (hash == ansHash) {
            val masterSalt = CryptoEngine.generateSalt()
            val masterHash = CryptoEngine.hashPin(newPin, masterSalt)
            val masterSaltB64 = Base64.encodeToString(masterSalt, Base64.NO_WRAP)

            prefs.edit()
                .putString(KEY_PIN_HASH, masterHash)
                .putString(KEY_PIN_SALT, masterSaltB64)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .apply()

            activeSecretKey = CryptoEngine.deriveKey(newPin, masterSalt)
            currentMode = VaultMode.MASTER
            return true
        }
        return false
    }

    fun lockVault() {
        currentMode = VaultMode.LOCKED
        activeSecretKey = null
    }

    // Intruder Logs
    fun getIntruderLogs(): List<IntruderLog> {
        val raw = prefs.getString("intruder_logs_json", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            raw.split(";;;").filter { it.isNotBlank() }.map { line ->
                val parts = line.split(":::")
                IntruderLog(
                    id = parts.getOrElse(0) { UUID.randomUUID().toString() },
                    timestampMs = parts.getOrElse(1) { "0" }.toLongOrNull() ?: 0L,
                    failedPinEntered = parts.getOrElse(2) { "****" },
                    attemptNumber = parts.getOrElse(3) { "1" }.toIntOrNull() ?: 1,
                    photoPath = parts.getOrNull(4)
                )
            }.reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveIntruderLog(log: IntruderLog) {
        val currentLogs = getIntruderLogs().toMutableList()
        currentLogs.add(0, log)
        val serialized = currentLogs.take(50).joinToString(";;;") {
            "${it.id}:::${it.timestampMs}:::${it.failedPinEntered}:::${it.attemptNumber}:::${it.photoPath ?: ""}"
        }
        prefs.edit().putString("intruder_logs_json", serialized).apply()
    }

    fun clearIntruderLogs() {
        prefs.edit().remove("intruder_logs_json").apply()
    }

    // Disguise & Toggles
    var isCalculatorDisguise: Boolean
        get() = prefs.getBoolean(KEY_CALCULATOR_DISGUISE, true)
        set(value) = prefs.edit().putBoolean(KEY_CALCULATOR_DISGUISE, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var isFlipToLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLIP_TO_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_FLIP_TO_LOCK, value).apply()

    var isShakeToLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_TO_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_SHAKE_TO_LOCK, value).apply()

    var isScreenshotBlocked: Boolean
        get() = prefs.getBoolean(KEY_SCREENSHOT_BLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSHOT_BLOCK, value).apply()

    var isIntruderCaptureEnabled: Boolean
        get() = prefs.getBoolean(KEY_INTRUDER_CAPTURE, true)
        set(value) = prefs.edit().putBoolean(KEY_INTRUDER_CAPTURE, value).apply()
}
