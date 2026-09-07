package com.spinedev.georeport.data.repository

import android.util.Log
import com.spinedev.georeport.data.local.dao.ReportDao
import com.spinedev.georeport.data.local.entity.ReportEntity
import com.spinedev.georeport.data.model.Report
import com.spinedev.georeport.data.model.SyncStatus
import com.spinedev.georeport.data.remote.FirestoreDataSource
import com.spinedev.georeport.data.remote.SupabaseStorageDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private const val TAG = "ReportRepository"

/**
 * Repository for Report data
 * Implements offline-first architecture
 * Coordinates between local (Room) and remote (Firebase Firestore + Supabase Storage) data sources
 */
class ReportRepository(
    private val reportDao: ReportDao,
    private val firestoreDataSource: FirestoreDataSource = FirestoreDataSource(),
    private val supabaseStorage: SupabaseStorageDataSource = SupabaseStorageDataSource()
) {
    
    /**
     * Get all reports as a Flow (reactive)
     */
    fun getAllReports(): Flow<List<Report>> {
        return reportDao.getAllReports().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Get reports for a specific user
     */
    fun getReportsByUser(userId: String): Flow<List<Report>> {
        return reportDao.getReportsByUser(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Get a single report by ID
     */
    suspend fun getReportById(reportId: String): Report? {
        return reportDao.getReportById(reportId)?.toDomainModel()
    }
    
    /**
     * Create a new report (saves locally, will sync later)
     */
    suspend fun createReport(report: Report): Result<Report> {
        return try {
            val entity = ReportEntity.fromDomainModel(
                report.copy(syncStatus = SyncStatus.PENDING)
            )
            reportDao.insertReport(entity)
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing report
     */
    suspend fun updateReport(report: Report): Result<Report> {
        return try {
            val entity = ReportEntity.fromDomainModel(
                report.copy(
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING // Mark as pending sync
                )
            )
            reportDao.updateReport(entity)
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a report (soft delete)
     */
    suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            reportDao.softDeleteReport(reportId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get reports that need to be synced
     */
    suspend fun getUnsyncedReports(): List<Report> {
        return reportDao.getUnsyncedReports().map { it.toDomainModel() }
    }
    
    /**
     * Update sync status of a report
     */
    suspend fun updateSyncStatus(reportId: String, status: SyncStatus) {
        reportDao.updateSyncStatus(reportId, status.name)
    }
    
    // ========== Supabase Storage Methods ==========
    
    /**
     * Upload image to Supabase Storage
     * @param imageFile Local file to upload
     * @param reportId ID of the report
     * @return Result with public URL of uploaded image
     */
    suspend fun uploadImageToStorage(imageFile: File, reportId: String): Result<String> {
        return supabaseStorage.uploadImage(imageFile, reportId)
    }
    
    /**
     * Delete image from Supabase Storage
     * @param imageUrl Public URL of the image to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteImageFromStorage(imageUrl: String): Result<Unit> {
        return supabaseStorage.deleteImage(imageUrl)
    }
    
    /**
     * Check if image exists in storage
     * @param imageUrl Public URL of the image
     * @return true if exists, false otherwise
     */
    suspend fun imageExistsInStorage(imageUrl: String): Boolean {
        return supabaseStorage.imageExists(imageUrl)
    }
    
    // ========== Firestore Sync Methods ==========
    
    /**
     * Sync a single report to Firestore
     */
    suspend fun syncReportToFirestore(report: Report): Result<Unit> {
        return try {
            firestoreDataSource.uploadReport(report).getOrThrow()
            updateSyncStatus(report.id, SyncStatus.SYNCED)
            Log.d(TAG, "Report synced to Firestore: ${report.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            updateSyncStatus(report.id, SyncStatus.FAILED)
            Log.e(TAG, "Failed to sync report to Firestore: ${report.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download reports from Firestore and save to Room
     */
    suspend fun downloadReportsFromFirestore(userId: String): Result<Int> {
        return try {
            val remoteReports = firestoreDataSource.downloadReports(userId).getOrThrow()
            var updatedCount = 0
            
            for (remoteReport in remoteReports) {
                val localReport = reportDao.getReportById(remoteReport.id)
                
                if (localReport == null) {
                    // New report from server
                    reportDao.insertReport(ReportEntity.fromDomainModel(remoteReport))
                    updatedCount++
                    Log.d(TAG, "Downloaded new report: ${remoteReport.id}")
                } else {
                    // Check if remote is newer (last-write-wins)
                    if (remoteReport.updatedAt > localReport.updatedAt) {
                        reportDao.updateReport(
                            ReportEntity.fromDomainModel(
                                remoteReport.copy(
                                    localImagePath = localReport.localImagePath // Preserve local image
                                )
                            )
                        )
                        updatedCount++
                        Log.d(TAG, "Updated report from server: ${remoteReport.id}")
                    }
                }
            }
            
            Log.d(TAG, "Downloaded/updated $updatedCount reports from Firestore")
            Result.success(updatedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download reports from Firestore", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update image URL in Room and Firestore
     */
    suspend fun updateImageUrl(reportId: String, imageUrl: String): Result<Unit> {
        return try {
            reportDao.updateImageUrl(reportId, imageUrl)
            
            val report = reportDao.getReportById(reportId)?.toDomainModel()
            if (report != null) {
                firestoreDataSource.uploadReport(report).getOrThrow()
                Log.d(TAG, "Image URL updated for report: $reportId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update image URL for report: $reportId", e)
            Result.failure(e)
        }
    }
}
