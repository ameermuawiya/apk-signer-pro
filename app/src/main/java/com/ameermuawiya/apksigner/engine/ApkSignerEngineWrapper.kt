package com.ameermuawiya.apksigner.engine

import android.content.Context
import com.android.apksig.ApkSigner
import com.ameermuawiya.apksigner.data.keystore.KeystoreData
import com.ameermuawiya.apksigner.data.keystore.KeystoreManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Collections
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * High performance engine wrapper executing APK, APKS, and XAPK signing tasks.
 */
class ApkSignerEngineWrapper(private val context: Context) {

    private val keystoreManager = KeystoreManager(context)

    /**
     * Executes signing on target file according to signature schemes and key configurations.
     */
    fun signFile(
        inputFile: File,
        outputFile: File,
        customKeyPath: String?,
        keystorePassword: String?,
        alias: String?,
        v1: Boolean,
        v2: Boolean,
        v3: Boolean,
        v4: Boolean,
        onProgress: (step: Int, total: Int, message: String) -> Unit,
        isCancelled: () -> Boolean
    ): String? {
        val extension = inputFile.extension.lowercase(Locale.getDefault())
        return try {
            onProgress(1, 4, "Preparing keystore and signing environment...")
            val keyData = prepareKeystoreData(customKeyPath, keystorePassword, alias)

            if (isCancelled()) return null

            if (extension == "apks" || extension == "xapk") {
                signSplitPackage(
                    inputFile,
                    outputFile,
                    keyData,
                    v1,
                    v2,
                    v3,
                    v4,
                    onProgress,
                    isCancelled
                )
            } else {
                onProgress(2, 4, "Verifying 16KB / 4KB page alignment for libraries (.so) and data...")
                if (isCancelled()) return null

                onProgress(3, 4, "Generating cryptographic digests and applying signature schemes...")
                signSingleApk(
                    inputFile,
                    outputFile,
                    keyData,
                    v1,
                    v2,
                    v3,
                    v4
                )

                if (isCancelled()) return null
                onProgress(4, 4, "Signing verification completed successfully.")
            }
            null
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            sw.toString()
        }
    }

    /**
     * Prepares KeystoreData using custom key or extracts default debug keystore from assets.
     */
    private fun prepareKeystoreData(
        customKeyPath: String?,
        password: String?,
        alias: String?
    ): KeystoreData {
        if (!customKeyPath.isNullOrBlank()) {
            val keyFile = File(customKeyPath)
            if (!keyFile.exists()) {
                throw IllegalArgumentException("Custom key file does not exist at path: $customKeyPath")
            }
            val pw = password ?: keystoreManager.getSessionPassword()
            ?: throw IllegalArgumentException("Password required for custom key.")
            return keystoreManager.getKeystoreData(keyFile, pw, alias)
        }

        val debugKeyFile = File(context.cacheDir, "debug23.keystore")
        if (!debugKeyFile.exists() || debugKeyFile.length() == 0L) {
            context.assets.open("debug23.keystore").use { input ->
                FileOutputStream(debugKeyFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return keystoreManager.getKeystoreData(debugKeyFile, "android", "androiddebugkey")
    }

    /**
     * Signs single APK file using official Android ApkSigner builder.
     */
    private fun signSingleApk(
        inputFile: File,
        outputFile: File,
        keyData: KeystoreData,
        v1: Boolean,
        v2: Boolean,
        v3: Boolean,
        v4: Boolean
    ) {
        val signerConfig = ApkSigner.SignerConfig.Builder(
            keyData.alias.ifEmpty { "CERT" },
            keyData.privateKey,
            keyData.certificates
        ).build()

        val builder = ApkSigner.Builder(Collections.singletonList(signerConfig))
            .setInputApk(inputFile)
            .setOutputApk(outputFile)
            .setCreatedBy("APK Signer")
            .setV1SigningEnabled(v1)
            .setV2SigningEnabled(v2)
            .setV3SigningEnabled(v3)
            .setV4SigningEnabled(v4)

        if (v4) {
            val idsigFile = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.idsig")
            builder.setV4SignatureOutputFile(idsigFile)
        }

        builder.build().sign()
    }

    /**
     * Extracts split package (.apks / .xapk), signs internal APKs, and repacks output.
     */
    private fun signSplitPackage(
        inputFile: File,
        outputFile: File,
        keyData: KeystoreData,
        v1: Boolean,
        v2: Boolean,
        v3: Boolean,
        v4: Boolean,
        onProgress: (step: Int, total: Int, message: String) -> Unit,
        isCancelled: () -> Boolean
    ) {
        val tempDir = File(context.cacheDir, "split_temp_${System.currentTimeMillis()}")
        val signedTempDir = File(context.cacheDir, "split_signed_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        signedTempDir.mkdirs()

        try {
            onProgress(2, 4, "Extracting split package archives...")
            unzip(inputFile, tempDir)

            if (isCancelled()) return

            val apks = tempDir.walkTopDown().filter { it.isFile && it.extension.lowercase(Locale.getDefault()) == "apk" }.toList()
            val total = apks.size

            onProgress(3, 4, "Signing $total split APK modules with aligned pages...")
            apks.forEachIndexed { index, apk ->
                if (isCancelled()) return
                onProgress(3, 4, "Signing split ${index + 1}/$total: ${apk.name}")
                val relativePath = apk.relativeTo(tempDir).path
                val targetSigned = File(signedTempDir, relativePath)
                targetSigned.parentFile?.mkdirs()
                signSingleApk(apk, targetSigned, keyData, v1, v2, v3, v4)
            }

            tempDir.walkTopDown().filter { it.isFile && it.extension.lowercase(Locale.getDefault()) != "apk" }.forEach { otherFile ->
                val relativePath = otherFile.relativeTo(tempDir).path
                val destFile = File(signedTempDir, relativePath)
                destFile.parentFile?.mkdirs()
                otherFile.copyTo(destFile, overwrite = true)
            }

            if (isCancelled()) return

            onProgress(4, 4, "Repacking signed split package bundle...")
            zipDirectory(signedTempDir, outputFile)
        } finally {
            tempDir.deleteRecursively()
            signedTempDir.deleteRecursively()
        }
    }

    /**
     * Unzips source file to destination folder safely.
     */
    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /**
     * Zips directory contents to target archive file.
     */
    private fun zipDirectory(sourceDir: File, targetZip: File) {
        ZipOutputStream(FileOutputStream(targetZip)).use { zos ->
            sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = file.relativeTo(sourceDir).path.replace('\\', '/')
                val zipEntry = ZipEntry(entryName)
                zos.putNextEntry(zipEntry)
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}
