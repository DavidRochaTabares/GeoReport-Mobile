package com.spinedev.georeport.ui.components

import android.Manifest
import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Handles camera permission requests
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberCameraPermission(
    onPermissionResult: (Boolean) -> Unit = {}
): CameraPermissionState {
    val permissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    ) { granted ->
        onPermissionResult(granted)
    }
    
    return CameraPermissionState(
        hasPermission = permissionState.status.isGranted,
        requestPermission = { permissionState.launchPermissionRequest() }
    )
}

/**
 * State for camera permission
 */
data class CameraPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit
)
