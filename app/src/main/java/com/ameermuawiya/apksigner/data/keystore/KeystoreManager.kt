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
 * JKS KeyStore provider bridge for Android using BouncyCastle's internal JKS engine.
 */
class BcJksKeyStoreSpi : org.bouncycastle.jcajce.provider.keystore.util.JKSKeyStoreSpi(
    org.bouncycastle.jcajce.util.BCJcaJceHelper()
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
     * Registers BouncyCastle security provider with BKS v1 and JKS support enabled.
     */
    private fun ensureBouncyCastle() {
        try {
            System.setProperty("org.bouncycastle.bks.enable_v1", "true")
        } catch (ignored: Exception) {}

        val existingBc = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        val bc = if (existingBc is BouncyCastleProvider) existingBc else BouncyCastleProvider()
        try {
            bc.put("KeyStore.JKS", BcJksKeyStoreSpi::class.java.name)
        } catch (ignored: Exception) {}

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(bc, 1)
    }

    /**
     * Inspects magic header bytes of keystore file to prioritize matching format.
     */
    private fun detectProbableKeystoreType(keyFile: File): String? {
        if (!keyFile.exists() || keyFile.length() < 4) return null
        return try {
            FileInputStream(keyFile).use { fis ->
                val b = ByteArray(4)
                val read = fis.read(b)
                if (read < 4) return null
                val b0 = b[0].toInt() and 0xFF
                val b1 = b[1].toInt() and 0xFF
                val b2 = b[2].toInt() and 0xFF
                val b3 = b[3].toInt() and 0xFF

                when {
                    b0 == 0xFE && b1 == 0xED && b2 == 0xFE && b3 == 0xED -> "JKS"
                    b0 == 0xCE && b1 == 0xCE && b2 == 0xCE && b3 == 0xCE -> "JCEKS"
                    b0 == 0x00 && b1 == 0x00 && b2 == 0x00 && (b3 == 0x01 || b3 == 0x02) -> "BKS"
                    b0 == 0x30 && b1 == 0x82 -> "PKCS12"
                    else -> null
                }
            }
        } catch (ignored: Exception) {
            null
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
     * Attempts loading a KeyStore from file testing PKCS12, BKS, JKS, and standard formats cleanly.
     */
    fun loadKeyStoreWithType(keyFile: File, password: String): Pair<KeyStore, String> {
        ensureBouncyCastle()
        val charPassword = password.toCharArray()

        val bcProvider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        val providers: List<java.security.Provider?> = listOfNotNull(bcProvider) + listOf(null)

        val probable = detectProbableKeystoreType(keyFile)
        val types = buildList {
            if (probable != null) {
                add(probable)
                if (probable == "BKS") add("BKS-V1")
                if (probable == "PKCS12") add("BCFKS")
            }
            addAll(listOf("PKCS12", "BKS", "BKS-V1", "JKS", "BCFKS", "UBER", "BOUNCYCASTLE", "JCEKS"))
            add(KeyStore.getDefaultType())
        }.distinct()

        var lastException: Exception? = null
        var passwordException: Exception? = null
        var emptyKeystoreCandidate: Pair<KeyStore, String>? = null

        for (type in types) {
            for (provider in providers) {
                val ks = try {
                    if (provider != null) KeyStore.getInstance(type, provider) else KeyStore.getInstance(type)
                } catch (ignored: Exception) {
                    continue
                }

                try {
                    FileInputStream(keyFile).use { fis ->
                        ks.load(fis, charPassword)
                        if (ks.aliases().hasMoreElements()) {
                            return Pair(ks, type)
                        } else if (emptyKeystoreCandidate == null) {
                            emptyKeystoreCandidate = Pair(ks, type)
                        }
                    }
                } catch (e: Exception) {
                    val msg = (e.message ?: "").lowercase(Locale.US)
                    val causeMsg = (e.cause?.message ?: "").lowercase(Locale.US)
                    val isPwErr = msg.contains("password") || msg.contains("mac") || msg.contains("tampered") ||
                            msg.contains("unrecoverable") || msg.contains("incorrect") || msg.contains("badpadding") ||
                            causeMsg.contains("password") || causeMsg.contains("mac") || causeMsg.contains("tampered")

                    if (isPwErr) {
                        if (passwordException == null) {
                            passwordException = e
                        }
                    } else if (!msg.contains("not found") && !msg.contains("not available")) {
                        lastException = e
                    }
                }
            }
        }

        if (emptyKeystoreCandidate != null) {
            return emptyKeystoreCandidate
        }

        if (passwordException != null) {
            throw passwordException
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
