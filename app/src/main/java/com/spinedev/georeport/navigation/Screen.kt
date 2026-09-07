package com.spinedev.georeport.navigation

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object ReportList : Screen("report_list")
    data object CreateReport : Screen("create_report")
    data object Camera : Screen("camera")
    data object EditReport : Screen("edit_report/{reportId}") {
        fun createRoute(reportId: String) = "edit_report/$reportId"
    }
    data object ReportDetail : Screen("report_detail/{reportId}") {
        fun createRoute(reportId: String) = "report_detail/$reportId"
    }
}
