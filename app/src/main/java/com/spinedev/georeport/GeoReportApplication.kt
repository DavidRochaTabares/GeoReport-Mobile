package com.spinedev.georeport

import android.app.Application
import android.util.Log
import androidx.work.*
import com.spinedev.georeport.sync.SyncWorker
import java.util.concurrent.TimeUnit

private const val TAG = "GeoReportApplication"
private const val SYNC_WORK_NAME = "report_sync_work"

/**
 * Application class for GeoReport
 * This will be used to initialize Firebase and other app-wide components
 */
class GeoReportApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Firebase will be initialized automatically
        // Room database will be initialized lazily
        
        // Schedule periodic sync
        scheduleSyncWork()
        
        Log.d(TAG, "GeoReport Application initialized")
    }
    
    /**
     * Schedule periodic sync work
     */
    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Minimum interval for periodic work
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        
        Log.d(TAG, "Periodic sync work scheduled")
    }
    
    /**
     * Trigger immediate sync
     */
    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueue(syncRequest)
        
        Log.d(TAG, "Immediate sync triggered")
    }
}
