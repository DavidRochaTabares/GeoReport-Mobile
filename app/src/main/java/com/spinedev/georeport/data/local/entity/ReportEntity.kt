package com.spinedev.georeport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spinedev.georeport.data.model.Report
import com.spinedev.georeport.data.model.ReportCategory
import com.spinedev.georeport.data.model.SyncStatus

/**
 * Room entity for Report
 * This is the database representation
 */
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String, // Stored as String, converted to/from enum
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val localImagePath: String?,
    val userId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String, // Stored as String, converted to/from enum
    val isDeleted: Boolean
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): Report {
        return Report(
            id = id,
            title = title,
            description = description,
            category = ReportCategory.fromString(category),
            latitude = latitude,
            longitude = longitude,
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            userId = userId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.valueOf(syncStatus),
            isDeleted = isDeleted
        )
    }
    
    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(report: Report): ReportEntity {
            return ReportEntity(
                id = report.id,
                title = report.title,
                description = report.description,
                category = report.category.name,
                latitude = report.latitude,
                longitude = report.longitude,
                imageUrl = report.imageUrl,
                localImagePath = report.localImagePath,
                userId = report.userId,
                createdAt = report.createdAt,
                updatedAt = report.updatedAt,
                syncStatus = report.syncStatus.name,
                isDeleted = report.isDeleted
            )
        }
    }
}
