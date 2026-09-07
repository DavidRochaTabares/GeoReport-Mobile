package com.spinedev.georeport.data.model

import java.util.UUID

/**
 * Domain model for a Report
 * This is the model used throughout the app
 */
data class Report(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: ReportCategory,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
) {
    /**
     * Check if report has a valid image (either local or remote)
     */
    fun hasImage(): Boolean = !localImagePath.isNullOrEmpty() || !imageUrl.isNullOrEmpty()
    
    /**
     * Get the image path/URL to display (prefer remote URL if synced)
     */
    fun getDisplayImagePath(): String? = imageUrl ?: localImagePath
}
