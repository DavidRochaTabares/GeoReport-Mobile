package com.spinedev.georeport.data.model

/**
 * Categories for urban incident reports
 */
enum class ReportCategory(val displayName: String) {
    POTHOLE("Bache"),
    LIGHTING("Alumbrado Público"),
    TRASH("Basura"),
    GRAFFITI("Grafiti"),
    DAMAGED_SIGN("Señal Dañada"),
    OTHER("Otro");
    
    companion object {
        fun fromString(value: String): ReportCategory {
            return entries.find { it.name == value } ?: OTHER
        }
    }
}
