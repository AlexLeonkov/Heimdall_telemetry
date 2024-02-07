





















package de.tomcory.heimdall.ui.telemetry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.datastore.PreferencesSerializer
import de.tomcory.heimdall.ui.database.DatabaseViewModel
import de.tomcory.heimdall.ui.main.preferencesStore
import kotlinx.coroutines.launch

//import de.tomcory.heimdall.telemetry.TelemetryExport


@Composable
fun TelemetryScreen(viewModel: TelemetryViewModel = viewModel()) {
    val dataStore = LocalContext.current.preferencesStore
    val coroutineScope = rememberCoroutineScope() // CoroutineScope tied to the Composable's lifecycle
    // Assume PreferencesSerializer has boolean fields for telemetry settings
    val preferences = dataStore.data.collectAsStateWithLifecycle(initialValue = PreferencesSerializer.defaultValue)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        var isFullyAnonymized by remember { mutableStateOf(preferences.value.isFullyAnonymized) }
        var isNoIpTimestamps by remember { mutableStateOf(preferences.value.isNoIpTimestamps) }
        var isDisclosedContent by remember { mutableStateOf(preferences.value.isDisclosedContent) }

        Text("Telemetry Data")

        Spacer(modifier = Modifier.height(20.dp))

        // Example button to demonstrate other actions
        Button(onClick = {   viewModel.createFakeData()}) {
            Text(text = "create fake data")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "Fully Anonymized Data:")
        Switch(
                checked = isFullyAnonymized,
                onCheckedChange = { newValue ->
                    isFullyAnonymized = newValue
                    coroutineScope.launch {
                        dataStore.updateData { prefs ->
                            prefs.toBuilder().setIsFullyAnonymized(newValue).build()
                        }
                    }
                }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "No IP/Timestamps:")
        Switch(
                checked = isNoIpTimestamps,
                onCheckedChange = { newValue ->
                    isNoIpTimestamps = newValue
                    coroutineScope.launch {
                        dataStore.updateData { prefs ->
                            prefs.toBuilder().setIsNoIpTimestamps(newValue).build()
                        }
                    }
                }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "Disclosed Content:")
        Switch(
                checked = isDisclosedContent,
                onCheckedChange = { newValue ->
                    isDisclosedContent = newValue
                    coroutineScope.launch {
                        dataStore.updateData { prefs ->
                            prefs.toBuilder().setIsDisclosedContent(newValue).build()
                        }
                    }
                }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Additional UI elements and functionality as needed
    }
}


@Preview(showBackground = true)
@Composable
fun TelemetryScreenPreview() {
    TelemetryScreen()
}