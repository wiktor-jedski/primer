package com.example.primer.data.model

data class AppSettings(
    val githubRepo: String,
    val githubBranch: String,
    val journalMarkers: List<String>,
    val affirmation: String,
    val habits: List<String>,
    val strategies: List<String>,
    val notificationHour: Int,
    val notificationMinute: Int
)
