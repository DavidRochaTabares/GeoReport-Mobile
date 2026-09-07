package com.spinedev.georeport.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import android.net.Uri
import com.spinedev.georeport.ui.screens.camera.CameraScreen
import com.spinedev.georeport.ui.screens.create.CreateReportScreen
import com.spinedev.georeport.ui.screens.detail.ReportDetailScreen
import com.spinedev.georeport.ui.screens.home.HomeScreen
import com.spinedev.georeport.ui.screens.list.ReportListScreen
import com.spinedev.georeport.ui.screens.login.LoginScreen

/**
 * Navigation graph for the app
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Home Screen (Map + Bottom Navigation)
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToReportList = {
                    navController.navigate(Screen.ReportList.route)
                },
                onNavigateToCreateReport = {
                    navController.navigate(Screen.CreateReport.route)
                },
                onNavigateToReportDetail = { reportId ->
                    navController.navigate(Screen.ReportDetail.createRoute(reportId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Report List Screen
        composable(Screen.ReportList.route) {
            ReportListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { reportId ->
                    navController.navigate(Screen.ReportDetail.createRoute(reportId))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateReport.route)
                }
            )
        }
        
        // Create Report Screen
        composable(Screen.CreateReport.route) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            val capturedImageUri = savedStateHandle.get<String>("captured_image_uri")
            
            CreateReportScreen(
                capturedImageUri = capturedImageUri,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onReportCreated = { navController.popBackStack() },
                onImageUriConsumed = { savedStateHandle.remove<String>("captured_image_uri") }
            )
        }
        
        // Camera Screen
        composable(Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { uri ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("captured_image_uri", uri.toString())
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Edit Report Screen
        composable(
            route = Screen.EditReport.route,
            arguments = listOf(
                navArgument("reportId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: return@composable
            val savedStateHandle = backStackEntry.savedStateHandle
            val capturedImageUri = savedStateHandle.get<String>("captured_image_uri")
            
            CreateReportScreen(
                reportId = reportId,
                capturedImageUri = capturedImageUri,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onReportCreated = { navController.popBackStack() },
                onImageUriConsumed = { savedStateHandle.remove<String>("captured_image_uri") }
            )
        }
        
        // Report Detail Screen
        composable(
            route = Screen.ReportDetail.route,
            arguments = listOf(
                navArgument("reportId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: return@composable
            ReportDetailScreen(
                reportId = reportId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.EditReport.createRoute(reportId)) }
            )
        }
    }
}
