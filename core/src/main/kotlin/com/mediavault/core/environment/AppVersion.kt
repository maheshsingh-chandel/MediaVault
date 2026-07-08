package com.mediavault.core.environment

data class AppVersion(
    val name: String,
    val version: String,
    val vendor: String,
    val updateChannel: String,
) {
    companion object {
        val Current = AppVersion(
            name = "MediaVault",
            version = "1.0.0",
            vendor = "Mahesh Chandel",
            updateChannel = "stable",
        )
    }
}
