package com.spinedev.georeport.ui.screens.create

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.spinedev.georeport.data.model.ReportCategory
import com.spinedev.georeport.data.repository.AuthRepository
import com.spinedev.georeport.ui.components.rememberCameraPermission
import com.spinedev.georeport.ui.components.rememberLocationPermission
import com.spinedev.georeport.ui.screens.report.ReportUiState
import com.spinedev.georeport.ui.screens.report.ReportViewModel
import kotlinx.coroutines.launch

/**
 * Create/Edit Report Screen with Camera integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    reportId: String? = null,
    capturedImageUri: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onReportCreated: () -> Unit,
    onImageUriConsumed: () -> Unit = {},
    viewModel: ReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    
    val isEditMode = reportId != null
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ReportCategory.OTHER) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var currentLatitude by remember { mutableStateOf(4.6097) }
    var currentLongitude by remember { mutableStateOf(-74.0817) }
    
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermission = rememberCameraPermission()
    val locationPermission = rememberLocationPermission()
    
    // Handle captured image from camera
    LaunchedEffect(capturedImageUri) {
        if (capturedImageUri != null) {
            imageUri = Uri.parse(capturedImageUri)
            onImageUriConsumed()
        }
    }
    
    // Load report if editing
    LaunchedEffect(reportId) {
        if (reportId != null) {
            viewModel.loadReport(reportId)
        }
    }
    
    // Populate fields when editing
    LaunchedEffect(viewModel.currentReport.collectAsState().value) {
        viewModel.currentReport.value?.let { report ->
            title = report.title
            description = report.description
            selectedCategory = report.category
            currentLatitude = report.latitude
            currentLongitude = report.longitude
            report.imageUrl?.let { imageUri = Uri.parse(it) }
        }
    }
    
    // Handle success
    LaunchedEffect(uiState) {
        if (uiState is ReportUiState.Success) {
            onReportCreated()
            viewModel.resetUiState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Editar Reporte" else "Nuevo Reporte") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                placeholder = { Text("Ej: Bache en Av. Principal") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is ReportUiState.Loading
            )
            
            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                placeholder = { Text("Describe el problema en detalle...") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 6,
                enabled = uiState !is ReportUiState.Loading
            )
            
            // Category Selector
            ExposedDropdownMenuBox(
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = !showCategoryMenu }
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = uiState !is ReportUiState.Loading
                )
                
                ExposedDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    ReportCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                selectedCategory = category
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }
            
            // Camera Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Evidencia Fotográfica",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Foto capturada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (cameraPermission.hasPermission) {
                                        onNavigateToCamera()
                                    } else {
                                        cameraPermission.requestPermission()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Camera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Tomar Otra")
                            }
                            
                            OutlinedButton(
                                onClick = { imageUri = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Eliminar")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (cameraPermission.hasPermission) {
                                    onNavigateToCamera()
                                } else {
                                    cameraPermission.requestPermission()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Camera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tomar Foto")
                        }
                    }
                }
            }
            
            // Location Info
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ubicación",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Lat: %.6f, Lon: %.6f".format(currentLatitude, currentLongitude),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Error Message
            if (uiState is ReportUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (uiState as ReportUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Save Button
            Button(
                onClick = {
                    scope.launch {
                        val userId = authRepository.getCurrentUserId() ?: "unknown"
                        
                        if (isEditMode && reportId != null) {
                            viewModel.updateReport(
                                reportId = reportId,
                                title = title,
                                description = description,
                                category = selectedCategory,
                                imageUri = imageUri?.toString()
                            )
                        } else {
                            viewModel.createReport(
                                title = title,
                                description = description,
                                category = selectedCategory,
                                latitude = currentLatitude,
                                longitude = currentLongitude,
                                imageUri = imageUri?.toString(),
                                userId = userId
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && 
                         description.isNotBlank() && 
                         uiState !is ReportUiState.Loading
            ) {
                if (uiState is ReportUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isEditMode) "Actualizar Reporte" else "Crear Reporte")
                }
            }
        }
    }
}
