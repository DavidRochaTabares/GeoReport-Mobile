package com.spinedev.georeport.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.tasks.await

/**
 * Repository for Authentication
 * Handles Firebase Authentication with Email/Password
 */
class AuthRepository {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Check if user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("User ID is null"))
            Result.success(userId)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Usuario no encontrado"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Credenciales incorrectas"))
        } catch (e: Exception) {
            Result.failure(Exception("Error de autenticación: ${e.message}"))
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
