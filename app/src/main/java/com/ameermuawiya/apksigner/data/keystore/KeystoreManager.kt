package com.ameermuawiya.apksigner.data.keystore

import android.content.Context
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.security.interfaces.RSAKey
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Result data holder containing loaded private key, certificates, and alias.
 */
data class KeystoreData(
    val privateKey: PrivateKey,
    val certificates: List<X509Certificate>,
    val alias: String
)

/**
 * Detailed metadata inspected from a valid keystore.
 */
data class KeystoreInspectionResult(
    val format: String,
    val aliases: List<String>,
    val selectedAlias: String,
    val algorithm: String,
    val validity: String,
    val subject: String,
    val fingerprint: String,
    val summary: String
)

/**
 * Robust manager for custom and default keystores handling JKS, PKCS12, BKS formats safely.
 */
class KeystoreManager(private val context: Context) {

    init {
        ensureBouncyCastle()
    }

    private var sessionPasswordCache: String? = null

    /**
     * Registers BouncyCastle security provider for full key algorithm support.
     */
    private fun ensureBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * Sets in-memory cached session password for custom keystore.
     */
    fun setSessionPassword(password: String?) {
        sessionPasswordCache = password
    }

    /**
     * Retrieves in-memory cached session password.
     */
    fun getSessionPassword(): String? = sessionPasswordCache

    /**
     * Attempts loading a KeyStore from file testing PKCS12, JKS, BKS, and UBKS types cleanly.
     */
    fun loadKeyStoreWithType(keyFile: File, password: String): Pair<KeyStore, String> {
        ensureBouncyCastle()
        val types = arrayOf("PKCS12", "JKS", "BKS", "BKS-V1", "UBER", "BCFKS", KeyStore.getDefaultType())
        val charPassword = password.toCharArray()

        var lastException: Exception? = null
        for (type in types) {
            try {
                FileInputStream(keyFile).use { fis ->
                    val ks = KeyStore.getInstance(type)
                    ks.load(fis, charPassword)
                    return Pair(ks, type)
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException ?: IllegalArgumentException("Unsupported keystore format or incorrect password.")
    }

    /**
     * Loads keystore instance using all standard and extended key providers.
     */
    fun loadKeyStore(keyFile: File, password: String): KeyStore {
        return loadKeyStoreWithType(keyFile, password).first
    }

    /**
     * Extracts list of available private key aliases from loaded keystore.
     */
    fun getPrivateKeyAliases(keystore: KeyStore, password: String): List<String> {
        val aliases = mutableListOf<String>()
        val charPassword = password.toCharArray()
        val enumeration = keystore.aliases()
        while (enumeration.hasMoreElements()) {
            val alias = enumeration.nextElement()
            try {
                if (keystore.isKeyEntry(alias)) {
                    val entry = keystore.getEntry(alias, KeyStore.PasswordProtection(charPassword))
                    if (entry is KeyStore.PrivateKeyEntry) {
                        aliases.add(alias)
                    }
                }
            } catch (ignored: Exception) {
                if (keystore.isKeyEntry(alias)) {
                    aliases.add(alias)
                }
            }
        }
        return aliases
    }

    /**
     * Inspects keystore certificate details and returns structured metadata.
     */
    fun inspectKeystore(keyFile: File, password: String): KeystoreInspectionResult {
        val (keystore, detectedFormat) = loadKeyStoreWithType(keyFile, password)
        val aliases = getPrivateKeyAliases(keystore, password)
        if (aliases.isEmpty()) {
            throw IllegalStateException("Keystore opened but contains no private key entries.")
        }
        val targetAlias = aliases.first()
        val charPassword = password.toCharArray()
        val entry = keystore.getEntry(targetAlias, KeyStore.PasswordProtection(charPassword)) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Alias '$targetAlias' is not a PrivateKeyEntry.")

        val cert = entry.certificateChain.firstOrNull() as? X509Certificate
            ?: throw IllegalStateException("No X.509 certificate found for alias '$targetAlias'.")

        val key = entry.privateKey
        val keySize = if (key is RSAKey) "${key.modulus.bitLength()} bits" else ""
        val algoText = "${cert.publicKey.algorithm} $keySize".trim()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val validFrom = dateFormat.format(cert.notBefore)
        val validTo = dateFormat.format(cert.notAfter)
        val validityText = "$validFrom to $validTo"

        val subjectText = cert.subjectX500Principal.name

        val digest = MessageDigest.getInstance("SHA-256")
        val fingerprintBytes = digest.digest(cert.encoded)
        val fingerprintHex = fingerprintBytes.joinToString(":") { String.format("%02X", it) }

        val summaryText = "$detectedFormat • $algoText • Valid to $validTo"

        return KeystoreInspectionResult(
            format = detectedFormat,
            aliases = aliases,
            selectedAlias = targetAlias,
            algorithm = algoText,
            validity = validityText,
            subject = subjectText,
            fingerprint = fingerprintHex,
            summary = summaryText
        )
    }

    /**
     * Loads KeystoreData containing private key and certificate chain for signing.
     */
    fun getKeystoreData(
        keyFile: File,
        storePassword: String,
        preferredAlias: String? = null,
        keyPassword: String? = null
    ): KeystoreData {
        val keystore = loadKeyStore(keyFile, storePassword)
        val aliases = getPrivateKeyAliases(keystore, storePassword)
        if (aliases.isEmpty()) {
            throw IllegalStateException("No private key entry found in keystore.")
        }

        val targetAlias = if (!preferredAlias.isNullOrBlank() && aliases.contains(preferredAlias)) {
            preferredAlias
        } else {
            aliases.first()
        }

        val effectiveKeyPass = (keyPassword ?: storePassword).toCharArray()
        val entry = keystore.getEntry(targetAlias, KeyStore.PasswordProtection(effectiveKeyPass))
                as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Key alias '$targetAlias' is not a PrivateKeyEntry.")

        val certs = entry.certificateChain.map { it as X509Certificate }
        return KeystoreData(
            privateKey = entry.privateKey,
            certificates = certs,
            alias = targetAlias
        )
    }
}
