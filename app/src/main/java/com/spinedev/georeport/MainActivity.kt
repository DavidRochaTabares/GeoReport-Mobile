package com.spinedev.georeport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.spinedev.georeport.data.repository.AuthRepository
import com.spinedev.georeport.navigation.NavGraph
import com.spinedev.georeport.navigation.Screen
import com.spinedev.georeport.ui.theme.GeoReportTheme

/**
 * Main Activity for GeoReport
 * Handles navigation and app lifecycle
 */
class MainActivity : ComponentActivity() {
    
    private val authRepository = AuthRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoReportTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Check if user is logged in to determine start destination
                    val startDestination = if (authRepository.isUserLoggedIn()) {
                        Screen.Home.route
                    } else {
                        Screen.Login.route
                    }
                    
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}