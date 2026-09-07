package com.spinedev.georeport.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Data source for Supabase Storage operations
 * Handles image upload/download for report images
 */
class SupabaseStorageDataSource {
    
    private val storage = SupabaseClientProvider.storage
    private val bucketName = SupabaseConfig.BUCKET_NAME
    
    companion object {
        private const val TAG = "SupabaseStorage"
    }
    
    /**
     * Upload image to Supabase Storage
     * @param imageFile Local file to upload
     * @param reportId ID of the report (for organizing files)
     * @return Result with public URL of uploaded image, or error
     */
    suspend fun uploadImage(imageFile: File, reportId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!imageFile.exists()) {
                    return@withContext Result.failure(Exception("Image file does not exist"))
                }
                
                // Generate unique filename
                val timestamp = System.currentTimeMillis()
                val uniqueId = UUID.randomUUID().toString().take(8)
                val fileName = "${reportId}_${timestamp}_${uniqueId}.jpg"
                
                Log.d(TAG, "Uploading image: $fileName (${imageFile.length()} bytes)")
                
                // Read file bytes
                val imageBytes = imageFile.readBytes()
                
                // Upload to Supabase Storage
                storage.from(bucketName).upload(
                    path = fileName,
                    data = imageBytes,
                    upsert = false
                )
                
                // Get public URL
                val publicUrl = storage.from(bucketName).publicUrl(fileName)
                
                Log.d(TAG, "Upload successful: $publicUrl")
                Result.success(publicUrl)
                
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Delete image from Supabase Storage
     * @param imageUrl Public URL of the image
     * @return Result indicating success or failure
     */
    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Extract filename from URL
                // URL format: https://xxx.supabase.co/storage/v1/object/public/report-images/filename.jpg
                val fileName = imageUrl.substringAfterLast("/")
                
                if (fileName.isBlank()) {
                    return@withContext Result.failure(Exception("Invalid image URL"))
                }
                
                Log.d(TAG, "Deleting image: $fileName")
                
                // Delete from Supabase Storage
                storage.from(bucketName).delete(fileName)
                
                Log.d(TAG, "Delete successful: $fileName")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if image exists in storage
     * @param imageUrl Public URL of the image
     * @return true if image exists, false otherwise
     */
    suspend fun imageExists(imageUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = imageUrl.substringAfterLast("/")
                if (fileName.isBlank()) return@withContext false
                
                // Try to get file info
                val files = storage.from(bucketName).list()
                files.any { it.name == fileName }
                
            } catch (e: Exception) {
                Log.e(TAG, "Check existence failed", e)
                false
            }
        }
    }
    
    /**
     * Get public URL for a filename
     * @param fileName Name of the file in storage
     * @return Public URL
     */
    fun getPublicUrl(fileName: String): String {
        return storage.from(bucketName).publicUrl(fileName)
    }
}
