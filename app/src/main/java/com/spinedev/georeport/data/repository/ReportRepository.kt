package com.spinedev.georeport.data.repository

import com.spinedev.georeport.data.local.dao.ReportDao
import com.spinedev.georeport.data.local.entity.ReportEntity
import com.spinedev.georeport.data.model.Report
import com.spinedev.georeport.data.model.SyncStatus
import com.spinedev.georeport.data.remote.SupabaseStorageDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Repository for Report data
 * Implements offline-first architecture
 * Coordinates between local (Room) and remote (Firebase Firestore + Supabase Storage) data sources
 */
class ReportRepository(
    private val reportDao: ReportDao,
    private val supabaseStorage: SupabaseStorageDataSource = SupabaseStorageDataSource()
    // Firebase Firestore data source will be added here after configuration
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
    
    // Firebase Firestore sync methods will be added here after Firebase configuration
    // - syncReportsToFirestore()
    // - syncReportsFromFirestore()
}
