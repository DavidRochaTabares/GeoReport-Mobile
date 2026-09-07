package com.spinedev.georeport.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spinedev.georeport.data.local.dao.ReportDao
import com.spinedev.georeport.data.local.entity.ReportEntity

/**
 * Room database for GeoReport
 */
@Database(
    entities = [ReportEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GeoReportDatabase : RoomDatabase() {
    
    abstract fun reportDao(): ReportDao
    
    companion object {
        @Volatile
        private var INSTANCE: GeoReportDatabase? = null
        
        fun getDatabase(context: Context): GeoReportDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeoReportDatabase::class.java,
                    "georeport_database"
                )
                    .fallbackToDestructiveMigration() // For development only
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
