package com.spinedev.georeport.data.local.dao

import androidx.room.*
import com.spinedev.georeport.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Report operations
 */
@Dao
interface ReportDao {
    
    /**
     * Get all reports (excluding deleted ones)
     * Returns a Flow for reactive updates
     */
    @Query("SELECT * FROM reports WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<ReportEntity>>
    
    /**
     * Get all reports for a specific user
     */
    @Query("SELECT * FROM reports WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getReportsByUser(userId: String): Flow<List<ReportEntity>>
    
    /**
     * Get a single report by ID
     */
    @Query("SELECT * FROM reports WHERE id = :reportId")
    suspend fun getReportById(reportId: String): ReportEntity?
    
    /**
     * Get all reports that need to be synced
     */
    @Query("SELECT * FROM reports WHERE syncStatus IN ('PENDING', 'FAILED') AND isDeleted = 0")
    suspend fun getUnsyncedReports(): List<ReportEntity>
    
    /**
     * Insert a new report
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)
    
    /**
     * Insert multiple reports
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<ReportEntity>)
    
    /**
     * Update an existing report
     */
    @Update
    suspend fun updateReport(report: ReportEntity)
    
    /**
     * Delete a report (hard delete)
     */
    @Delete
    suspend fun deleteReport(report: ReportEntity)
    
    /**
     * Soft delete a report (mark as deleted)
     */
    @Query("UPDATE reports SET isDeleted = 1, updatedAt = :timestamp WHERE id = :reportId")
    suspend fun softDeleteReport(reportId: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Update sync status of a report
     */
    @Query("UPDATE reports SET syncStatus = :status, updatedAt = :timestamp WHERE id = :reportId")
    suspend fun updateSyncStatus(reportId: String, status: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Delete all reports (for testing/debugging)
     */
    @Query("DELETE FROM reports")
    suspend fun deleteAllReports()
}
