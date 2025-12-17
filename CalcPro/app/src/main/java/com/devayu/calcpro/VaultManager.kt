package com.devayu.calcpro

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.OpenableColumns
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.FileOutputStream

object VaultManager {
    private const val VAULT_DIR = "vault_data"
    private const val THUMB_DIR = "vault_thumbs"

    data class VaultFile(val encryptedFile: File, val thumbFile: File)

    private fun getMainKey(context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    fun hideFile(context: Context, sourceUri: Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            // Get MIME type to determine if video or image
            val type = contentResolver.getType(sourceUri) ?: "application/octet-stream"
            val isVideo = type.startsWith("video/")

            val timestamp = System.currentTimeMillis()

            // Directories
            val vaultDir = File(context.filesDir, VAULT_DIR).apply { mkdirs() }
            val thumbDir = File(context.filesDir, THUMB_DIR).apply { mkdirs() }

            // File Names (LINKED BY TIMESTAMP)
            val encName = "${timestamp}_${if(isVideo) "vid" else "img"}.enc"
            val thumbName = "${timestamp}.jpg"

            val destEncFile = File(vaultDir, encName)
            val destThumbFile = File(thumbDir, thumbName)

            // 1. GENERATE THUMBNAIL (With Fallback)
            var thumbBitmap: Bitmap? = null

            try {
                if (isVideo) {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, sourceUri)
                    thumbBitmap = retriever.getFrameAtTime(1000000) // 1 second mark
                    if (thumbBitmap == null) thumbBitmap = retriever.getFrameAtTime(0) // Start
                    retriever.release()
                } else {
                    val stream = contentResolver.openInputStream(sourceUri)
                    thumbBitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    // Resize to save space if it's huge
                    if (thumbBitmap != null && thumbBitmap.width > 512) {
                        thumbBitmap = ThumbnailUtils.extractThumbnail(thumbBitmap, 512, 512)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // If generation failed, create a colored placeholder so it's not invisible
            if (thumbBitmap == null) {
                thumbBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                thumbBitmap.eraseColor(if(isVideo) Color.DKGRAY else Color.LTGRAY)
            }

            // Save Thumbnail
            FileOutputStream(destThumbFile).use { out ->
                thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // 2. ENCRYPT FILE
            val encryptedFile = EncryptedFile.Builder(
                context,
                destEncFile,
                getMainKey(context),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            contentResolver.openInputStream(sourceUri)?.use { input ->
                encryptedFile.openFileOutput().use { output ->
                    input.copyTo(output)
                }
            }

            true // Success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getVaultFiles(context: Context): List<VaultFile> {
        val vaultDir = File(context.filesDir, VAULT_DIR)
        val thumbDir = File(context.filesDir, THUMB_DIR)

        if (!vaultDir.exists()) return emptyList()

        // Match files: 123_vid.enc -> 123.jpg
        return vaultDir.listFiles()?.mapNotNull { encFile ->
            val timestamp = encFile.name.substringBefore("_") // Extract "123" from "123_vid.enc"
            val thumbFile = File(thumbDir, "$timestamp.jpg")

            // Return the pair (even if thumb is missing, we point to it)
            VaultFile(encFile, thumbFile)
        } ?: emptyList()
    }

    fun getDecryptedFile(context: Context, encryptedFile: File): File? {
        return try {
            val tempFile = File(context.cacheDir, "temp_${encryptedFile.name}.decrypted")
            if (tempFile.exists()) tempFile.delete() // Always refresh

            val cryptFile = EncryptedFile.Builder(
                context,
                encryptedFile,
                getMainKey(context),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            cryptFile.openFileInput().use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}