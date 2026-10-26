package com.example.secretvault

import com.example.secretvault.security.CryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.AEADBadTagException

class CryptoEngineTest {

    @Test
    fun testKeyDerivationAndHash() {
        val pin = "1234"
        val salt = CryptoEngine.generateSalt()
        assertEquals(16, salt.size)

        val hash1 = CryptoEngine.hashPin(pin, salt)
        val hash2 = CryptoEngine.hashPin(pin, salt)
        assertEquals(hash1, hash2)

        val wrongHash = CryptoEngine.hashPin("4321", salt)
        assertNotEquals(hash1, wrongHash)

        val key = CryptoEngine.deriveKey(pin, salt)
        assertNotNull(key)
        assertEquals("AES", key.algorithm)
    }

    @Test
    fun testBytesEncryptionAndDecryption() {
        val pin = "8765"
        val salt = CryptoEngine.generateSalt()
        val key = CryptoEngine.deriveKey(pin, salt)

        val originalText = "Top secret document: passwords, bank PINs and private photos!"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val encrypted = CryptoEngine.encryptBytes(originalBytes, key)
        assertTrue(encrypted.size > originalBytes.size)

        val decrypted = CryptoEngine.decryptBytes(encrypted, key)
        val decryptedText = String(decrypted, Charsets.UTF_8)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testStreamEncryptionAndDecryption() {
        val pin = "9999"
        val salt = CryptoEngine.generateSalt()
        val key = CryptoEngine.deriveKey(pin, salt)

        val originalData = ByteArray(65536) { (it % 256).toByte() }
        val inputStream = ByteArrayInputStream(originalData)
        val encryptedOut = ByteArrayOutputStream()

        CryptoEngine.encryptStream(inputStream, encryptedOut, key)
        val encryptedBytes = encryptedOut.toByteArray()
        assertTrue(encryptedBytes.size > originalData.size)

        val decryptIn = ByteArrayInputStream(encryptedBytes)
        val decryptedOut = ByteArrayOutputStream()
        CryptoEngine.decryptStream(decryptIn, decryptedOut, key)

        val decryptedBytes = decryptedOut.toByteArray()
        assertEquals(originalData.size, decryptedBytes.size)
        assertTrue(originalData.contentEquals(decryptedBytes))
    }

    @Test
    fun testDecryptionFailsWithWrongKey() {
        val salt = CryptoEngine.generateSalt()
        val correctKey = CryptoEngine.deriveKey("1234", salt)
        val wrongKey = CryptoEngine.deriveKey("9999", salt)

        val data = "Confidential data".toByteArray(Charsets.UTF_8)
        val encrypted = CryptoEngine.encryptBytes(data, correctKey)

        try {
            CryptoEngine.decryptBytes(encrypted, wrongKey)
            fail("Decryption should fail with wrong key")
        } catch (e: Exception) {
            // Expected AEADBadTagException or GeneralSecurityException
            assertTrue(e is AEADBadTagException || e.cause is AEADBadTagException || e.message != null)
        }
    }
}
