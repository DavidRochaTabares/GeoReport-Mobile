package com.spinedev.georeport.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.spinedev.georeport.GeoReportApplication
import com.spinedev.georeport.data.local.database.GeoReportDatabase
import com.spinedev.georeport.data.repository.ReportRepository
import com.spinedev.georeport.utils.ConnectivityObserver

private const val TAG = "SyncWorker"

/**
 * Background worker for syncing reports
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started")
        
        try {
            // Check if user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.d(TAG, "No authenticated user, skipping sync")
                return Result.success()
            }
            
            val userId = currentUser.uid
            
            // Initialize dependencies
            val database = GeoReportDatabase.getDatabase(applicationContext)
            val reportRepository = ReportRepository(database.reportDao())
            val connectivityObserver = ConnectivityObserver(applicationContext)
            val syncManager = SyncManager(applicationContext, reportRepository, connectivityObserver)
            
            // Check connectivity
            if (!connectivityObserver.isConnected()) {
                Log.d(TAG, "No internet connection, will retry later")
                return Result.retry()
            }
            
            // Perform sync
            val syncResult = syncManager.performSync(userId)
            
            if (syncResult.success) {
                Log.d(TAG, "Sync completed successfully: $syncResult")
                return Result.success()
            } else {
                Log.e(TAG, "Sync failed: ${syncResult.error}")
                return Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed", e)
            return Result.retry()
        }
    }
}
