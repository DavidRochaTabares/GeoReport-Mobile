package com.spinedev.georeport.data.remote

import com.spinedev.georeport.BuildConfig

/**
 * Supabase configuration
 * Publishable key is loaded from local.properties via BuildConfig
 * 
 * To configure:
 * 1. Add to local.properties: SUPABASE_PUBLISHABLE_KEY=your_key_here
 * 2. Sync/rebuild project
 */
object SupabaseConfig {
    /**
     * Supabase project URL
     */
    const val SUPABASE_URL = "https://nwaqlowijqrauptqqdlr.supabase.co"
    
    /**
     * Supabase publishable key (loaded from local.properties)
     * This is the public anon key - safe to use in client apps
     */
    val SUPABASE_PUBLISHABLE_KEY: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    
    /**
     * Storage bucket name for report images
     */
    const val BUCKET_NAME = "report-images"
}
