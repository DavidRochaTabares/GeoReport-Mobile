package com.spinedev.georeport.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spinedev.georeport.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Login Screen
 * Handles authentication logic
 */
class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    /**
     * Sign in with email and password
     */
    fun signIn(email: String, password: String) {
        // Reset state first to clear any previous errors
        _uiState.value = LoginUiState.Idle
        
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Por favor completa todos los campos")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            
            val result = authRepository.signInWithEmail(email, password)
            
            _uiState.value = result.fold(
                onSuccess = { userId -> LoginUiState.Success(userId) },
                onFailure = { error -> LoginUiState.Error(error.message ?: "Error desconocido") }
            )
        }
    }
    
    /**
     * Reset UI state to idle
     */
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}

/**
 * UI States for Login Screen
 */
sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val userId: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
