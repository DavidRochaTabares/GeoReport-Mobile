package com.spinedev.georeport.ui.components

import android.Manifest
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Handles location permission requests
 * Returns true if permissions are granted
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberLocationPermission(
    onPermissionResult: (Boolean) -> Unit = {}
): LocationPermissionState {
    val context = LocalContext.current
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ) { permissions ->
        val granted = permissions.values.any { it }
        onPermissionResult(granted)
    }
    
    return LocationPermissionState(
        hasPermission = permissionsState.allPermissionsGranted || permissionsState.permissions.any { it.status.isGranted },
        shouldShowRationale = permissionsState.permissions.any { it.status.shouldShowRationale },
        requestPermission = { permissionsState.launchMultiplePermissionRequest() }
    )
}

/**
 * State for location permission
 */
data class LocationPermissionState(
    val hasPermission: Boolean,
    val shouldShowRationale: Boolean,
    val requestPermission: () -> Unit
)
