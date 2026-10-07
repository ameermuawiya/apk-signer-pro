package com.ameermuawiya.apksigner.data.keystore

import android.content.Context
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.security.spec.ECGenParameterSpec
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Parameters required to generate a new cryptographic signing keystore.
 */
data class KeystoreGenParams(
    val format: String,
    val algorithm: String,
    val fileName: String,
    val alias: String,
    val password: String,
    val validityYears: Int,
    val commonName: String,
    val organization: String = "",
    val organizationalUnit: String = "",
    val locality: String = "",
    val state: String = "",
    val country: String = "",
    val expiryDate: Date? = null
)

/**
 * Result data returned upon successful generation of a keystore.
 */
data class GeneratedKeystoreResult(
    val file: File,
    val alias: String,
    val format: String,
    val summary: String,
    val inspectionResult: KeystoreInspectionResult
)

/**
 * High performance cryptographic generator supporting standard keystore formats and algorithms.
 */
object KeystoreGenerator {

    /**
     * Generates a new cryptographic keystore and self-signed certificate on disk.
     */
    fun generateKeystore(
        context: Context,
        params: KeystoreGenParams,
        workingDir: String
    ): GeneratedKeystoreResult {
        val bcProvider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) ?: BouncyCastleProvider().also {
            Security.addProvider(it)
        }

        val (keyPair, sigAlg) = generateKeyPairAndSignatureAlgorithm(params.algorithm, bcProvider)
        val cert = generateCertificate(params, keyPair, sigAlg, bcProvider)

        val targetDir = File(workingDir, "Keystores").apply {
            if (!exists()) mkdirs()
        }

        val extension = when (params.format.uppercase(Locale.ROOT)) {
            "PKCS12" -> ".p12"
            "BKS" -> ".bks"
            else -> ".jks"
        }

        val cleanBaseName = params.fileName.trim()
            .removeSuffix(".jks")
            .removeSuffix(".p12")
            .removeSuffix(".bks")
            .removeSuffix(".keystore")
        val cleanName = if (cleanBaseName.isBlank()) "release_key" else cleanBaseName
        val targetFile = File(targetDir, "$cleanName$extension")

        val keyStore = when (params.format.uppercase(Locale.ROOT)) {
            "BKS" -> KeyStore.getInstance("BKS", bcProvider)
            "JKS" -> KeyStore.getInstance("JKS", bcProvider)
            else -> KeyStore.getInstance("PKCS12")
        }

        keyStore.load(null, null)
        keyStore.setKeyEntry(
            params.alias.trim(),
            keyPair.private,
            params.password.toCharArray(),
            arrayOf(cert)
        )

        FileOutputStream(targetFile).use { fos ->
            keyStore.store(fos, params.password.toCharArray())
        }

        val keystoreManager = KeystoreManager(context)
        val inspection = keystoreManager.inspectKeystore(targetFile, params.password)

        return GeneratedKeystoreResult(
            file = targetFile,
            alias = params.alias.trim(),
            format = params.format,
            summary = inspection.summary,
            inspectionResult = inspection
        )
    }

    /**
     * Initializes key pair generator according to chosen algorithm and key size.
     */
    private fun generateKeyPairAndSignatureAlgorithm(
        algoSelection: String,
        provider: java.security.Provider
    ): Pair<KeyPair, String> {
        return when {
            algoSelection.contains("3072") -> {
                val kpg = KeyPairGenerator.getInstance("RSA", provider)
                kpg.initialize(3072)
                Pair(kpg.generateKeyPair(), "SHA384withRSA")
            }
            algoSelection.contains("4096") -> {
                val kpg = KeyPairGenerator.getInstance("RSA", provider)
                kpg.initialize(4096)
                Pair(kpg.generateKeyPair(), "SHA512withRSA")
            }
            algoSelection.contains("secp256r1") -> {
                val kpg = KeyPairGenerator.getInstance("EC", provider)
                kpg.initialize(ECGenParameterSpec("secp256r1"))
                Pair(kpg.generateKeyPair(), "SHA256withECDSA")
            }
            algoSelection.contains("secp384r1") -> {
                val kpg = KeyPairGenerator.getInstance("EC", provider)
                kpg.initialize(ECGenParameterSpec("secp384r1"))
                Pair(kpg.generateKeyPair(), "SHA384withECDSA")
            }
            algoSelection.contains("DSA") -> {
                val kpg = KeyPairGenerator.getInstance("DSA", provider)
                kpg.initialize(2048)
                Pair(kpg.generateKeyPair(), "SHA256withDSA")
            }
            else -> {
                val kpg = KeyPairGenerator.getInstance("RSA", provider)
                kpg.initialize(2048)
                Pair(kpg.generateKeyPair(), "SHA256withRSA")
            }
        }
    }

    /**
     * Constructs X.509 certificate with distinguished name attributes.
     */
    private fun generateCertificate(
        params: KeystoreGenParams,
        keyPair: KeyPair,
        sigAlg: String,
        provider: java.security.Provider
    ): java.security.cert.X509Certificate {
        val x500Builder = X500NameBuilder(BCStyle.INSTANCE)
        if (params.commonName.isNotBlank()) x500Builder.addRDN(BCStyle.CN, params.commonName.trim())
        if (params.organizationalUnit.isNotBlank()) x500Builder.addRDN(BCStyle.OU, params.organizationalUnit.trim())
        if (params.organization.isNotBlank()) x500Builder.addRDN(BCStyle.O, params.organization.trim())
        if (params.locality.isNotBlank()) x500Builder.addRDN(BCStyle.L, params.locality.trim())
        if (params.state.isNotBlank()) x500Builder.addRDN(BCStyle.ST, params.state.trim())
        if (params.country.isNotBlank()) x500Builder.addRDN(BCStyle.C, params.country.trim().take(2).uppercase(Locale.ROOT))
        val subject = x500Builder.build()

        val notBefore = Date(System.currentTimeMillis() - 60000L)
        val notAfter = if (params.expiryDate != null && params.expiryDate.after(notBefore)) {
            params.expiryDate
        } else {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, params.validityYears.coerceIn(1, 100))
            cal.time
        }

        val serial = BigInteger(64, SecureRandom())

        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        val contentSigner = JcaContentSignerBuilder(sigAlg).setProvider(provider).build(keyPair.private)
        val certHolder = certBuilder.build(contentSigner)
        return JcaX509CertificateConverter().setProvider(provider).getCertificate(certHolder)
    }
}
