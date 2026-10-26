package com.example.secretvault.security

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // in bits
    private const val GCM_IV_LENGTH = 12   // 12 bytes recommended for GCM
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 65536
    private const val KEY_LENGTH = 256     // 256-bit AES
    private const val MAGIC_HEADER = "SVLT" // 4 bytes Secret Vault Magic Header

    private val secureRandom = SecureRandom()

    fun generateSalt(length: Int = 16): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun generateIV(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        return iv
    }

    fun deriveKey(pin: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun hashPin(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun encryptBytes(data: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = generateIV()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val ciphertext = cipher.doFinal(data)
        // Format: [4 bytes MAGIC] + [12 bytes IV] + [Ciphertext with 16-byte GCM Tag]
        val magicBytes = MAGIC_HEADER.toByteArray(Charsets.UTF_8)
        val output = ByteArray(magicBytes.size + iv.size + ciphertext.size)
        System.arraycopy(magicBytes, 0, output, 0, magicBytes.size)
        System.arraycopy(iv, 0, output, magicBytes.size, iv.size)
        System.arraycopy(ciphertext, 0, output, magicBytes.size + iv.size, ciphertext.size)
        return output
    }

    fun decryptBytes(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
        val magicBytes = MAGIC_HEADER.toByteArray(Charsets.UTF_8)
        val headerLen = magicBytes.size + GCM_IV_LENGTH
        require(encryptedData.size >= headerLen) { "Encrypted data is too short or invalid" }

        // Verify magic header if present
        val hasMagic = (0 until magicBytes.size).all { encryptedData[it] == magicBytes[it] }
        val ivOffset = if (hasMagic) magicBytes.size else 0
        val actualHeaderLen = if (hasMagic) headerLen else GCM_IV_LENGTH

        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(encryptedData, ivOffset, iv, 0, GCM_IV_LENGTH)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val ciphertextOffset = actualHeaderLen
        val ciphertextSize = encryptedData.size - actualHeaderLen

        return cipher.doFinal(encryptedData, ciphertextOffset, ciphertextSize)
    }

    fun encryptStream(inputStream: InputStream, outputStream: OutputStream, secretKey: SecretKey) {
        val iv = generateIV()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        // Write MAGIC + IV
        outputStream.write(MAGIC_HEADER.toByteArray(Charsets.UTF_8))
        outputStream.write(iv)

        CipherOutputStream(outputStream, cipher).use { cipherOut ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                cipherOut.write(buffer, 0, bytesRead)
            }
            cipherOut.flush()
        }
    }

    fun decryptStream(inputStream: InputStream, outputStream: OutputStream, secretKey: SecretKey) {
        val magicBytes = MAGIC_HEADER.toByteArray(Charsets.UTF_8)
        val header = ByteArray(magicBytes.size)
        val readHeader = inputStream.read(header)
        val isMagic = readHeader == magicBytes.size && header.contentEquals(magicBytes)

        val iv = ByteArray(GCM_IV_LENGTH)
        if (isMagic) {
            val ivRead = inputStream.read(iv)
            require(ivRead == GCM_IV_LENGTH) { "Invalid IV in encrypted stream" }
        } else {
            // First bytes were the IV if no magic header
            System.arraycopy(header, 0, iv, 0, readHeader)
            if (readHeader < GCM_IV_LENGTH) {
                val remaining = GCM_IV_LENGTH - readHeader
                val rest = ByteArray(remaining)
                inputStream.read(rest)
                System.arraycopy(rest, 0, iv, readHeader, remaining)
            }
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        CipherInputStream(inputStream, cipher).use { cipherIn ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (cipherIn.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
        }
    }
}
