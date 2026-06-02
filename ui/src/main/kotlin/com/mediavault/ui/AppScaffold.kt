package com.mediavault.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppScaffold(
    selectedScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    content: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .width(120.dp),
        ) {
            AppScreen.entries.forEach { screen ->
                NavigationRailItem(
                    selected = selectedScreen == screen,
                    onClick = { onScreenSelected(screen) },
                    icon = { Text(screen.label) },
                    label = { Text(screen.label) },
                )
            }
        }

        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                content()
            }
        }
    }
}

private val AppScreen.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }
