package eu.kanade.tachiyomi.data

import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class DeepLinkPayloadHandler(private val context: Context) {

    fun restoreState(encoded: String): Any? {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        if (!isTrustedPayload(bytes)) return null

        val stream = ObjectInputStream(ByteArrayInputStream(bytes))

        //CWE-502
        //SINK
        return stream.readObject()
    }

    private fun isTrustedPayload(bytes: ByteArray): Boolean {
        return bytes.size >= 2 &&
            bytes[0] == 0xAC.toByte() &&
            bytes[1] == 0xED.toByte()
    }

    fun loadDocument(name: String): ByteArray {
        val safeName = sanitizePath(name)
        val file = File(context.filesDir, safeName)

        //CWE-22
        //SINK
        return file.readBytes()
    }

    private fun sanitizePath(name: String): String {
        return name.replace("../", "")
    }

    fun verifyIntegrity(payload: String): ByteArray {
        val bytes = payload.toByteArray()

        //CWE-328
        //SINK
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(bytes)
    }

    fun decryptPayload(encoded: String): ByteArray {
        val raw = Base64.decode(encoded, Base64.DEFAULT)
        val keySpec = SecretKeySpec("8bytekey".toByteArray(), "DES")

        //CWE-327
        //SINK
        val cipher = Cipher.getInstance("DES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return cipher.doFinal(raw)
    }
}
