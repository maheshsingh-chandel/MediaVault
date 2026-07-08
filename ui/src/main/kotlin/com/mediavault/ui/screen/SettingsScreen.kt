package com.mediavault.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediavault.core.environment.AppEnvironmentInfo
import com.mediavault.core.environment.RuntimeDependencyStatus

@Composable
fun SettingsScreen(
    environmentInfo: AppEnvironmentInfo,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        StorageLocationsCard(environmentInfo)
        RuntimeDependenciesCard(environmentInfo.dependencies)
    }
}

@Composable
private fun StorageLocationsCard(
    environmentInfo: AppEnvironmentInfo,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SettingPathRow("Database", environmentInfo.databasePath)
            SettingPathRow("Thumbnails", environmentInfo.thumbnailDirectory)
            SettingPathRow("Logs", environmentInfo.logsDirectory)
            SettingPathRow("Config", environmentInfo.configurationDirectory)
            SettingPathRow("Temporary cache", environmentInfo.cacheDirectory)
        }
    }
}

@Composable
private fun RuntimeDependenciesCard(
    dependencies: List<RuntimeDependencyStatus>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Runtime dependencies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            dependencies.forEach { dependency ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = dependency.name,
                        modifier = Modifier.weight(0.22f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (dependency.available) "Available" else "Missing",
                        modifier = Modifier.weight(0.18f),
                        color = if (dependency.available) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Text(
                        text = dependency.details,
                        modifier = Modifier.weight(0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingPathRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.22f),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
