package com.spinedev.georeport.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spinedev.georeport.data.repository.AuthRepository
import com.spinedev.georeport.ui.components.OpenStreetMapView
import com.spinedev.georeport.ui.components.rememberLocationPermission
import kotlinx.coroutines.launch

/**
 * Home Screen with Map and Bottom Navigation
 * Uses OpenStreetMap for map display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReportList: () -> Unit,
    onNavigateToCreateReport: () -> Unit,
    onNavigateToReportDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var centerOnLocation by remember { mutableStateOf(false) }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    
    // Location permission handling
    val locationPermission = rememberLocationPermission()
    
    // Request location permission on first load
    LaunchedEffect(Unit) {
        if (!locationPermission.hasPermission) {
            locationPermission.requestPermission()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GeoReport") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mis Reportes") },
                            onClick = {
                                showMenu = false
                                onNavigateToReportList()
                            },
                            leadingIcon = { Icon(Icons.Default.List, contentDescription = null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Cerrar Sesión") },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    authRepository.signOut()
                                    onLogout()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Center on location button (only show on map tab)
                if (selectedTab == 0 && locationPermission.hasPermission) {
                    SmallFloatingActionButton(
                        onClick = { centerOnLocation = true },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Mi Ubicación")
                    }
                }
                
                // Create report button
                FloatingActionButton(
                    onClick = onNavigateToCreateReport,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Reporte")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Mapa") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        onNavigateToReportList()
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Lista") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    // OpenStreetMap view
                    OpenStreetMapView(
                        modifier = Modifier.fillMaxSize(),
                        reportMarkers = emptyList(), // TODO: Load from repository
                        hasLocationPermission = locationPermission.hasPermission,
                        centerOnLocation = centerOnLocation,
                        onLocationCentered = { centerOnLocation = false },
                        onMarkerClick = { reportId ->
                            onNavigateToReportDetail(reportId)
                        }
                    )
                    
                    // Show permission rationale if needed
                    if (locationPermission.shouldShowRationale) {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Permiso de Ubicación") },
                            text = { Text("GeoReport necesita acceso a tu ubicación para mostrar reportes cercanos y permitirte crear reportes geolocalizados.") },
                            confirmButton = {
                                TextButton(onClick = { locationPermission.requestPermission() }) {
                                    Text("Conceder")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
                1 -> {
                    // Navigate to list screen instead of showing inline
                    LaunchedEffect(Unit) {
                        selectedTab = 0 // Reset to map
                    }
                }
            }
        }
    }
}
