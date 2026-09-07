package com.spinedev.georeport.data.model

/**
 * Synchronization status for offline-first architecture
 */
enum class SyncStatus {
    /** Report is synced with server */
    SYNCED,
    
    /** Report is pending upload to server */
    PENDING,
    
    /** Report failed to sync (will retry) */
    FAILED,
    
    /** Report is currently being synced */
    SYNCING
}
