package com.spinedev.georeport.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Supabase client singleton
 * Provides access to Supabase services (Storage only for GeoReport)
 */
object SupabaseClientProvider {
    
    /**
     * Supabase client instance
     * Configured with Storage module for image uploads
     */
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Postgrest)
            install(Storage)
        }
    }
    
    /**
     * Direct access to Storage module
     */
    val storage by lazy {
        client.storage
    }
}
