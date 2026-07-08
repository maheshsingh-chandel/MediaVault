package com.mediavault.core.environment

data class AppEnvironmentInfo(
    val version: AppVersion,
    val databasePath: String,
    val thumbnailDirectory: String,
    val logsDirectory: String,
    val cacheDirectory: String,
    val configurationDirectory: String,
    val dependencies: List<RuntimeDependencyStatus>,
)

data class RuntimeDependencyStatus(
    val name: String,
    val available: Boolean,
    val details: String,
)
