package com.spinedev.georeport.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val TAG = "OpenStreetMapView"

/**
 * OpenStreetMap view for Jetpack Compose
 * Displays map with user location and report markers
 */
@Composable
fun OpenStreetMapView(
    modifier: Modifier = Modifier,
    reportMarkers: List<ReportMarker> = emptyList(),
    hasLocationPermission: Boolean = false,
    onMarkerClick: (String) -> Unit = {},
    onMapClick: (Double, Double) -> Unit = { _, _ -> },
    centerOnLocation: Boolean = false,
    onLocationCentered: () -> Unit = {}
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }
    
    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        }
    }
    
    // Center on user location initially when permission is granted
    LaunchedEffect(hasLocationPermission, mapView) {
        if (hasLocationPermission && mapView != null) {
            Log.d(TAG, "Permission granted, attempting to get location...")
            val location = getCurrentLocation(context)
            if (location != null) {
                Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")
                
                // Center map
                mapView?.controller?.apply {
                    setCenter(location)
                    setZoom(15.0)
                }
                
                // Add user location marker
                mapView?.let { map ->
                    userLocationMarker?.let { map.overlays.remove(it) }
                    
                    val marker = Marker(map).apply {
                        position = location
                        title = "Mi Ubicación"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        
                        // Set custom icon
                        try {
                            val drawable = ContextCompat.getDrawable(context, com.spinedev.georeport.R.drawable.ic_my_location)
                            if (drawable != null) {
                                icon = drawable
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not load marker icon: ${e.message}")
                        }
                    }
                    
                    map.overlays.add(marker)
                    userLocationMarker = marker
                    map.invalidate()
                }
            } else {
                Log.w(TAG, "Could not obtain location")
            }
        }
    }
    
    // Center on user location when requested
    LaunchedEffect(centerOnLocation) {
        if (centerOnLocation && hasLocationPermission) {
            Log.d(TAG, "Center button clicked")
            mapView?.let { map ->
                val location = getCurrentLocation(context)
                if (location != null) {
                    Log.d(TAG, "Centering on: ${location.latitude}, ${location.longitude}")
                    map.controller.animateTo(location)
                    
                    // Update user location marker
                    userLocationMarker?.let { marker ->
                        marker.position = location
                        map.invalidate()
                    } ?: run {
                        // Create marker if it doesn't exist
                        val marker = Marker(map).apply {
                            position = location
                            title = "Mi Ubicación"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            
                            try {
                                val drawable = ContextCompat.getDrawable(context, com.spinedev.georeport.R.drawable.ic_my_location)
                                if (drawable != null) {
                                    icon = drawable
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not load marker icon: ${e.message}")
                            }
                        }
                        
                        map.overlays.add(marker)
                        userLocationMarker = marker
                        map.invalidate()
                    }
                } else {
                    Log.w(TAG, "Could not obtain location for centering")
                }
                onLocationCentered()
            }
        }
    }
    
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                
                // Set initial position (Bogotá)
                controller.apply {
                    setCenter(GeoPoint(4.6097, -74.0817))
                    setZoom(12.0)
                }
                
                mapView = this
            }
        },
        update = { map ->
            // Only update report markers, don't touch location marker
            // Remove only report markers (not the user location marker)
            map.overlays.removeAll { overlay ->
                overlay is Marker && overlay != userLocationMarker
            }
            
            // Add report markers
            reportMarkers.forEach { reportMarker ->
                val marker = Marker(map).apply {
                    position = GeoPoint(reportMarker.latitude, reportMarker.longitude)
                    title = reportMarker.title
                    snippet = reportMarker.description
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    
                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(reportMarker.reportId)
                        true
                    }
                }
                map.overlays.add(marker)
            }
            
            map.invalidate()
        }
    )
    
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}

/**
 * Data class for report markers
 */
data class ReportMarker(
    val reportId: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String
)

/**
 * Convert Drawable to Bitmap
 */
private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    
    return bitmap
}

/**
 * Get current location using FusedLocationProvider
 */
@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: Context): GeoPoint? {
    return try {
        Log.d(TAG, "Requesting location from FusedLocationProvider...")
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        // Try to get last known location first
        val lastLocation = fusedLocationClient.lastLocation.await()
        if (lastLocation != null) {
            Log.d(TAG, "Got last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
            return GeoPoint(lastLocation.latitude, lastLocation.longitude)
        }
        
        Log.d(TAG, "No last location, requesting current location...")
        // If no last location, request current location
        val cancellationToken = CancellationTokenSource()
        val currentLocation = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).await()
        
        if (currentLocation != null) {
            Log.d(TAG, "Got current location: ${currentLocation.latitude}, ${currentLocation.longitude}")
            GeoPoint(currentLocation.latitude, currentLocation.longitude)
        } else {
            Log.w(TAG, "Current location is null")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting location: ${e.message}", e)
        null
    }
}
