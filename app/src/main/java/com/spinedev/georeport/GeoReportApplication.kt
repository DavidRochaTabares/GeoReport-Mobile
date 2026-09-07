package com.spinedev.georeport

import android.app.Application

/**
 * Application class for GeoReport
 * This will be used to initialize Firebase and other app-wide components
 */
class GeoReportApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Firebase will be initialized automatically
        // Room database will be initialized lazily
    }
}
