package com.spinedev.georeport.ui.screens.report

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spinedev.georeport.data.local.database.GeoReportDatabase
import com.spinedev.georeport.data.model.Report
import com.spinedev.georeport.data.model.ReportCategory
import com.spinedev.georeport.data.model.SyncStatus
import com.spinedev.georeport.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Report CRUD operations
 */
class ReportViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = GeoReportDatabase.getDatabase(application)
    private val repository = ReportRepository(database.reportDao())
    
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()
    
    private val _currentReport = MutableStateFlow<Report?>(null)
    val currentReport: StateFlow<Report?> = _currentReport.asStateFlow()
    
    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()
    
    init {
        loadReports()
    }
    
    /**
     * Load all reports from Room
     */
    fun loadReports() {
        viewModelScope.launch {
            try {
                repository.getAllReports().collect { reportList ->
                    _reports.value = reportList
                }
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Error al cargar reportes: ${e.message}")
            }
        }
    }
    
    /**
     * Load a specific report by ID
     */
    fun loadReport(reportId: String) {
        viewModelScope.launch {
            try {
                val report = repository.getReportById(reportId)
                _currentReport.value = report
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Error al cargar reporte: ${e.message}")
            }
        }
    }
    
    /**
     * Create a new report
     */
    fun createReport(
        title: String,
        description: String,
        category: ReportCategory,
        latitude: Double,
        longitude: Double,
        imageUri: String?,
        userId: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = ReportUiState.Loading
                
                // Convert URI to file path if needed
                val localImagePath = imageUri?.let { uri ->
                    if (uri.startsWith("file://")) {
                        uri.removePrefix("file://")
                    } else {
                        uri
                    }
                }
                
                Log.d("ReportViewModel", "Creating report with image: $imageUri -> $localImagePath")
                
                val report = Report(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    category = category,
                    latitude = latitude,
                    longitude = longitude,
                    localImagePath = localImagePath,
                    userId = userId,
                    syncStatus = SyncStatus.PENDING,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                repository.createReport(report)
                _uiState.value = ReportUiState.Success("Reporte creado exitosamente")
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Error al crear reporte: ${e.message}")
            }
        }
    }
    
    /**
     * Update an existing report
     */
    fun updateReport(
        reportId: String,
        title: String,
        description: String,
        category: ReportCategory,
        imageUri: String?
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = ReportUiState.Loading
                
                // Convert URI to file path if needed
                val localImagePath = imageUri?.let { uri ->
                    if (uri.startsWith("file://")) {
                        uri.removePrefix("file://")
                    } else {
                        uri
                    }
                }
                
                val existingReport = repository.getReportById(reportId)
                if (existingReport != null) {
                    val updatedReport = existingReport.copy(
                        title = title,
                        description = description,
                        category = category,
                        localImagePath = localImagePath ?: existingReport.localImagePath,
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.PENDING // Mark for sync
                    )
                    
                    repository.updateReport(updatedReport)
                    _uiState.value = ReportUiState.Success("Reporte actualizado exitosamente")
                } else {
                    _uiState.value = ReportUiState.Error("Reporte no encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Error al actualizar reporte: ${e.message}")
            }
        }
    }
    
    /**
     * Delete a report
     */
    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ReportUiState.Loading
                
                repository.deleteReport(reportId)
                _uiState.value = ReportUiState.Success("Reporte eliminado exitosamente")
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Error al eliminar reporte: ${e.message}")
            }
        }
    }
    
    /**
     * Reset UI state
     */
    fun resetUiState() {
        _uiState.value = ReportUiState.Idle
    }
    
    /**
     * Clear current report
     */
    fun clearCurrentReport() {
        _currentReport.value = null
    }
}

/**
 * UI States for Report operations
 */
sealed class ReportUiState {
    data object Idle : ReportUiState()
    data object Loading : ReportUiState()
    data class Success(val message: String) : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}
