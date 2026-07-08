package com.mediavault.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mediavault.core.environment.AppEnvironmentInfo

@Composable
fun AboutScreen(
    environmentInfo: AppEnvironmentInfo,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = environmentInfo.version.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Version ${environmentInfo.version.version} (${environmentInfo.version.updateChannel})")
                Text("Vendor: ${environmentInfo.version.vendor}")
                Text("Credits: Built with Kotlin, Compose Desktop, SQLite, Exposed, Koin, VLCJ, and FFmpeg tooling.")
                Text("License: Copyright (C) 2026 Mahesh Chandel. All rights reserved.")
            }
        }
    }
}
