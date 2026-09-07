package com.spinedev.georeport.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.spinedev.georeport.data.model.Report
import com.spinedev.georeport.data.model.ReportCategory
import com.spinedev.georeport.data.model.SyncStatus
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreDataSource"
private const val REPORTS_COLLECTION = "reports"

/**
 * Data source for Firestore operations
 */
class FirestoreDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    /**
     * Upload a report to Firestore
     */
    suspend fun uploadReport(report: Report): Result<Unit> {
        return try {
            val data = mapReportToFirestore(report)
            
            firestore.collection(REPORTS_COLLECTION)
                .document(report.id)
                .set(data, SetOptions.merge())
                .await()
            
            Log.d(TAG, "Report uploaded: ${report.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload report: ${report.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update a report in Firestore
     */
    suspend fun updateReport(report: Report): Result<Unit> {
        return uploadReport(report) // Same operation for this MVP
    }
    
    /**
     * Soft delete a report in Firestore
     */
    suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            firestore.collection(REPORTS_COLLECTION)
                .document(reportId)
                .update(
                    mapOf(
                        "isDeleted" to true,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Log.d(TAG, "Report deleted: $reportId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete report: $reportId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download all reports for a user
     */
    suspend fun downloadReports(userId: String): Result<List<Report>> {
        return try {
            val snapshot = firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
            
            val reports = snapshot.documents.mapNotNull { doc ->
                try {
                    mapFirestoreToReport(doc.id, doc.data ?: emptyMap())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse report: ${doc.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "Downloaded ${reports.size} reports for user: $userId")
            Result.success(reports)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download reports for user: $userId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download a single report
     */
    suspend fun downloadReport(reportId: String): Result<Report?> {
        return try {
            val doc = firestore.collection(REPORTS_COLLECTION)
                .document(reportId)
                .get()
                .await()
            
            if (!doc.exists()) {
                Log.d(TAG, "Report not found: $reportId")
                return Result.success(null)
            }
            
            val report = mapFirestoreToReport(doc.id, doc.data ?: emptyMap())
            Log.d(TAG, "Downloaded report: $reportId")
            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download report: $reportId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Map Report to Firestore data
     */
    private fun mapReportToFirestore(report: Report): Map<String, Any?> {
        return mapOf(
            "title" to report.title,
            "description" to report.description,
            "category" to report.category.name,
            "latitude" to report.latitude,
            "longitude" to report.longitude,
            "imageUrl" to report.imageUrl,
            "userId" to report.userId,
            "createdAt" to report.createdAt,
            "updatedAt" to report.updatedAt,
            "isDeleted" to report.isDeleted
        )
    }
    
    /**
     * Map Firestore data to Report
     */
    private fun mapFirestoreToReport(id: String, data: Map<String, Any>): Report {
        return Report(
            id = id,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            category = ReportCategory.valueOf(data["category"] as? String ?: "OTROS"),
            latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
            imageUrl = data["imageUrl"] as? String,
            localImagePath = null, // Remote reports don't have local paths
            userId = data["userId"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED, // Downloaded reports are synced
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }
}
