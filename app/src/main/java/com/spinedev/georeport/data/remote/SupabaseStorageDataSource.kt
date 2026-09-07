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
                Log.d(TAG, "=== SUPABASE UPLOAD START ===")
                Log.d(TAG, "Report ID: $reportId")
                Log.d(TAG, "Image file: ${imageFile.absolutePath}")
                Log.d(TAG, "File exists: ${imageFile.exists()}")
                
                if (!imageFile.exists()) {
                    Log.e(TAG, "✗ Image file does not exist!")
                    return@withContext Result.failure(Exception("Image file does not exist"))
                }
                
                Log.d(TAG, "File size: ${imageFile.length()} bytes")
                
                // Generate unique filename
                val timestamp = System.currentTimeMillis()
                val uniqueId = UUID.randomUUID().toString().take(8)
                val fileName = "${reportId}_${timestamp}_${uniqueId}.jpg"
                
                Log.d(TAG, "Generated filename: $fileName")
                Log.d(TAG, "Target bucket: $bucketName")
                Log.d(TAG, "Supabase URL: ${SupabaseConfig.SUPABASE_URL}")
                
                // Read file bytes
                val imageBytes = imageFile.readBytes()
                Log.d(TAG, "Read ${imageBytes.size} bytes from file")
                
                // Upload to Supabase Storage
                Log.d(TAG, "Calling storage.from($bucketName).upload()...")
                storage.from(bucketName).upload(
                    path = fileName,
                    data = imageBytes,
                    upsert = false
                )
                Log.d(TAG, "✓ Upload call completed")
                
                // Get public URL
                val publicUrl = storage.from(bucketName).publicUrl(fileName)
                Log.d(TAG, "✓ Public URL obtained: $publicUrl")
                
                Log.d(TAG, "=== SUPABASE UPLOAD SUCCESS ===")
                Result.success(publicUrl)
                
            } catch (e: Exception) {
                Log.e(TAG, "=== SUPABASE UPLOAD FAILED ===", e)
                Log.e(TAG, "Exception type: ${e.javaClass.name}")
                Log.e(TAG, "Exception message: ${e.message}")
                e.printStackTrace()
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
