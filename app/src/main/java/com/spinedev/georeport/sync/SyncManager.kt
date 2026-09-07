package com.spinedev.georeport.sync

import android.content.Context
import android.util.Log
import com.spinedev.georeport.data.model.Report
import com.spinedev.georeport.data.model.SyncStatus
import com.spinedev.georeport.data.repository.ReportRepository
import com.spinedev.georeport.utils.ConnectivityObserver
import java.io.File

private const val TAG = "SyncManager"

/**
 * Manages synchronization between local and remote data
 */
class SyncManager(
    private val context: Context,
    private val reportRepository: ReportRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    
    /**
     * Perform full synchronization
     * 1. Upload pending reports to Firestore
     * 2. Upload pending images to Supabase
     * 3. Download new/updated reports from Firestore
     */
    suspend fun performSync(userId: String): SyncResult {
        if (!connectivityObserver.isConnected()) {
            Log.d(TAG, "No internet connection, skipping sync")
            return SyncResult(
                success = false,
                uploadedReports = 0,
                uploadedImages = 0,
                downloadedReports = 0,
                error = "No internet connection"
            )
        }
        
        Log.d(TAG, "Starting sync for user: $userId")
        
        var uploadedReports = 0
        var uploadedImages = 0
        var downloadedReports = 0
        var lastError: String? = null
        
        try {
            // Step 1: Get unsynced reports
            val unsyncedReports = reportRepository.getUnsyncedReports()
            Log.d(TAG, "Found ${unsyncedReports.size} unsynced reports")
            
            // Step 2: Upload reports and images
            for (report in unsyncedReports) {
                try {
                    Log.d(TAG, "=== Processing report: ${report.id} ===")
                    Log.d(TAG, "  localImagePath: ${report.localImagePath}")
                    Log.d(TAG, "  imageUrl: ${report.imageUrl}")
                    
                    // Upload image first if exists and not uploaded
                    var reportToSync = report
                    if (report.localImagePath != null && report.imageUrl == null) {
                        val imageFile = File(report.localImagePath)
                        Log.d(TAG, "  Image file path: ${imageFile.absolutePath}")
                        Log.d(TAG, "  Image file exists: ${imageFile.exists()}")
                        
                        if (imageFile.exists()) {
                            Log.d(TAG, "  Image file size: ${imageFile.length()} bytes")
                            Log.d(TAG, "  Starting image upload...")
                            
                            val uploadResult = reportRepository.uploadImageToStorage(imageFile, report.id)
                            
                            if (uploadResult.isSuccess) {
                                val imageUrl = uploadResult.getOrNull()!!
                                Log.d(TAG, "  ✓ Image uploaded successfully!")
                                Log.d(TAG, "  Public URL: $imageUrl")
                                
                                reportRepository.updateImageUrl(report.id, imageUrl)
                                Log.d(TAG, "  ✓ imageUrl updated in Room")
                                
                                reportToSync = report.copy(imageUrl = imageUrl)
                                uploadedImages++
                            } else {
                                val error = uploadResult.exceptionOrNull()
                                Log.e(TAG, "  ✗ Failed to upload image for report: ${report.id}")
                                Log.e(TAG, "  Error: ${error?.message}", error)
                                lastError = "Failed to upload image: ${error?.message}"
                            }
                        } else {
                            Log.w(TAG, "  ⚠ Image file does not exist at path: ${imageFile.absolutePath}")
                        }
                    } else {
                        Log.d(TAG, "  Skipping image upload (localImagePath=${report.localImagePath}, imageUrl=${report.imageUrl})")
                    }
                    
                    // Upload report to Firestore
                    val syncResult = reportRepository.syncReportToFirestore(reportToSync)
                    if (syncResult.isSuccess) {
                        uploadedReports++
                        Log.d(TAG, "Report synced: ${report.id}")
                    } else {
                        Log.e(TAG, "Failed to sync report: ${report.id}")
                        lastError = "Failed to sync report"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing report: ${report.id}", e)
                    lastError = e.message
                }
            }
            
            // Step 3: Download reports from Firestore
            try {
                val downloadResult = reportRepository.downloadReportsFromFirestore(userId)
                if (downloadResult.isSuccess) {
                    downloadedReports = downloadResult.getOrNull() ?: 0
                    Log.d(TAG, "Downloaded $downloadedReports reports")
                } else {
                    Log.e(TAG, "Failed to download reports")
                    lastError = "Failed to download reports"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading reports", e)
                lastError = e.message
            }
            
            Log.d(TAG, "Sync completed: uploaded=$uploadedReports, images=$uploadedImages, downloaded=$downloadedReports")
            
            return SyncResult(
                success = lastError == null,
                uploadedReports = uploadedReports,
                uploadedImages = uploadedImages,
                downloadedReports = downloadedReports,
                error = lastError
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            return SyncResult(
                success = false,
                uploadedReports = uploadedReports,
                uploadedImages = uploadedImages,
                downloadedReports = downloadedReports,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Sync a single report immediately
     */
    suspend fun syncReportNow(report: Report): Boolean {
        if (!connectivityObserver.isConnected()) {
            Log.d(TAG, "No internet connection, cannot sync report")
            return false
        }
        
        try {
            // Upload image if needed
            var reportToSync = report
            if (report.localImagePath != null && report.imageUrl == null) {
                val imageFile = File(report.localImagePath)
                if (imageFile.exists()) {
                    val uploadResult = reportRepository.uploadImageToStorage(imageFile, report.id)
                    if (uploadResult.isSuccess) {
                        val imageUrl = uploadResult.getOrNull()!!
                        reportRepository.updateImageUrl(report.id, imageUrl)
                        reportToSync = report.copy(imageUrl = imageUrl)
                    }
                }
            }
            
            // Sync to Firestore
            val result = reportRepository.syncReportToFirestore(reportToSync)
            return result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync report immediately", e)
            return false
        }
    }
}

/**
 * Result of a sync operation
 */
data class SyncResult(
    val success: Boolean,
    val uploadedReports: Int,
    val uploadedImages: Int,
    val downloadedReports: Int,
    val error: String? = null
)
