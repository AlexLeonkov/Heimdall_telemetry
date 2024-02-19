
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
import androidx.datastore.core.DataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tomcory.heimdall.Preferences
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.datastore.PreferencesSerializer
import de.tomcory.heimdall.ui.database.DatabaseViewModel
import de.tomcory.heimdall.ui.main.preferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch



 object AnonymizationFlags {
    const val NOT_ANONYMIZED = 1 shl 0 // 0b0001 or 1
    const val ESSENTIAL_DATA_ONLY = 1 shl 1 // 0b0010 or 2
    const val INCLUDE_HEADERS = 1 shl 2 // 0b0100 or 4
    const val INCLUDE_CONTENT = 1 shl 3 // 0b1000 or 8
    // You can extend this object with more flags as needed.
}
@Composable
fun TelemetryScreen(viewModel: TelemetryViewModel = viewModel()) {
    val context = LocalContext.current
    val dataStore = context.preferencesStore
    val coroutineScope = rememberCoroutineScope()

    val preferences = dataStore.data.collectAsStateWithLifecycle(initialValue = PreferencesSerializer.defaultValue)
    val anonymizationSettings = preferences.value.anonymizationFlags

    var isNotAnonymized by remember { mutableStateOf(anonymizationSettings and AnonymizationFlags.NOT_ANONYMIZED != 0) }
    var isEssentialDataOnly by remember { mutableStateOf(anonymizationSettings and AnonymizationFlags.ESSENTIAL_DATA_ONLY != 0) }
    var isIncludeHeaders by remember { mutableStateOf(anonymizationSettings and AnonymizationFlags.INCLUDE_HEADERS != 0) }
    var isIncludeContent by remember { mutableStateOf(anonymizationSettings and AnonymizationFlags.INCLUDE_CONTENT != 0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Text(text = "Anonymization Settings")
        Spacer(modifier = Modifier.height(16.dp))

        // "Not Anonymized" switch
        SwitchOption("Not Anonymized", isNotAnonymized) { isChecked ->
            isNotAnonymized = isChecked
            updateAnonymizationSettings(dataStore, coroutineScope, AnonymizationFlags.NOT_ANONYMIZED, isChecked)
            // If "Not Anonymized" is being turned off or on, update UI states accordingly
            if (isChecked) {
                isEssentialDataOnly = false
                isIncludeHeaders = false
                isIncludeContent = false
            }
        }

        // "Essential Data Only" switch
        SwitchOption("Essential Data Only", isEssentialDataOnly) { isChecked ->
            isEssentialDataOnly = isChecked
            // Turning on any other option should set "Not Anonymized" to false
            isNotAnonymized = false
            updateAnonymizationSettings(dataStore, coroutineScope, AnonymizationFlags.ESSENTIAL_DATA_ONLY, isChecked)
        }

        // "Include Headers" switch
        SwitchOption("Include Headers", isIncludeHeaders) { isChecked ->
            isIncludeHeaders = isChecked
            // Turning on any other option should set "Not Anonymized" to false
            isNotAnonymized = false
            updateAnonymizationSettings(dataStore, coroutineScope, AnonymizationFlags.INCLUDE_HEADERS, isChecked)
        }

        // "Include Content" switch
        SwitchOption("Include Content", isIncludeContent) { isChecked ->
            isIncludeContent = isChecked
            // Turning on any other option should set "Not Anonymized" to false
            isNotAnonymized = false
            updateAnonymizationSettings(dataStore, coroutineScope, AnonymizationFlags.INCLUDE_CONTENT, isChecked)
        }

        Button(onClick = { viewModel.createFakeData() }) {
            Text("Create Fake Data")
        }
    }
}
@Composable
fun SwitchOption(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column {
        Text(text = label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    Spacer(modifier = Modifier.height(10.dp))
}

fun updateAnonymizationSettings(dataStore: DataStore<Preferences>, coroutineScope: CoroutineScope, flag: Int, isEnabled: Boolean) {
    coroutineScope.launch {
        dataStore.updateData { preferences ->
            var currentSettings = preferences.anonymizationFlags

            if (flag == AnonymizationFlags.NOT_ANONYMIZED && isEnabled) {
                // If "Not Anonymized" is being set to true, clear all other flags by setting only this flag.
                currentSettings = AnonymizationFlags.NOT_ANONYMIZED
            } else if (isEnabled) {
                // If any other flag is being set to true, ensure "Not Anonymized" is cleared, and set this flag.
                currentSettings = (currentSettings or flag) and AnonymizationFlags.NOT_ANONYMIZED.inv()
            } else {
                // If a flag is being set to false, just clear this flag without affecting others.
                currentSettings = currentSettings and flag.inv()
            }

            preferences.toBuilder().setAnonymizationFlags(currentSettings).build()
        }
    }
}
















//
//
//
//
//package de.tomcory.heimdall.ui.telemetry
//
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Button
//import androidx.compose.material3.Switch
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import androidx.lifecycle.viewmodel.compose.viewModel
//import de.tomcory.heimdall.persistence.database.HeimdallDatabase
//import de.tomcory.heimdall.persistence.datastore.PreferencesSerializer
//import de.tomcory.heimdall.ui.database.DatabaseViewModel
//import de.tomcory.heimdall.ui.main.preferencesStore
//import kotlinx.coroutines.launch
//
////import de.tomcory.heimdall.telemetry.TelemetryExport
//@Composable
//fun TelemetryScreen(viewModel: TelemetryViewModel = viewModel()) {
//    val dataStore = LocalContext.current.preferencesStore
//    val coroutineScope = rememberCoroutineScope() // CoroutineScope tied to the Composable's lifecycle
//    // Assume PreferencesSerializer has boolean fields for telemetry settings
//    val preferences = dataStore.data.collectAsStateWithLifecycle(initialValue = PreferencesSerializer.defaultValue)
//
//    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
//        var isFullyAnonymized by remember { mutableStateOf(preferences.value.isFullyAnonymized) }
//        var isPartlyAnonymized by remember { mutableStateOf(preferences.value.isPartlyAnonymized) }
//        var isNotAnonymized by remember { mutableStateOf(preferences.value.isNotAnonymized) }
//
//        Text("Telemetry Data")
//
//        Spacer(modifier = Modifier.height(20.dp))
//
//        // Example button to demonstrate other actions
//        Button(onClick = { viewModel.createFakeData() }) {
//            Text(text = "Create Fake Data")
//        }
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        Text(text = "Fully Anonymized Data:")
//        Switch(
//                checked = isFullyAnonymized,
//                onCheckedChange = { newValue ->
//                    if (newValue) {
//                        isFullyAnonymized = true
//                        isPartlyAnonymized = false
//                        isNotAnonymized = false
//                        coroutineScope.launch {
//                            dataStore.updateData { prefs ->
//                                prefs.toBuilder()
//                                        .setIsFullyAnonymized(true)
//                                        .setIsPartlyAnonymized(false)
//                                        .setIsNotAnonymized(false)
//                                        .build()
//                            }
//                        }
//                    }
//                }
//        )
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        Text(text = "Partly Anonymized Data:")
//        Switch(
//                checked = isPartlyAnonymized,
//                onCheckedChange = { newValue ->
//                    if (newValue) {
//                        isFullyAnonymized = false
//                        isPartlyAnonymized = true
//                        isNotAnonymized = false
//                        coroutineScope.launch {
//                            dataStore.updateData { prefs ->
//                                prefs.toBuilder()
//                                        .setIsFullyAnonymized(false)
//                                        .setIsPartlyAnonymized(true)
//                                        .setIsNotAnonymized(false)
//                                        .build()
//                            }
//                        }
//                    }
//                }
//        )
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        Text(text = "Not Anonymized Data:")
//        Switch(
//                checked = isNotAnonymized,
//                onCheckedChange = { newValue ->
//                    if (newValue) {
//                        isFullyAnonymized = false
//                        isPartlyAnonymized = false
//                        isNotAnonymized = true
//                        coroutineScope.launch {
//                            dataStore.updateData { prefs ->
//                                prefs.toBuilder()
//                                        .(false)
//                                        .setIsPartlyAnonymized(false)
//                                        .setIsNotAnonymized(true)
//                                        .build()
//                            }
//                        }
//                    }
//                }
//        )
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        // Additional UI elements and functionality as needed
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun TelemetryScreenPreview() {
//    TelemetryScreen()
//}
















//
//
//
//
//package de.tomcory.heimdall.ui.telemetry
//
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Button
//import androidx.compose.material3.Switch
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import androidx.lifecycle.viewmodel.compose.viewModel
//import de.tomcory.heimdall.persistence.database.HeimdallDatabase
//import de.tomcory.heimdall.persistence.datastore.PreferencesSerializer
//import de.tomcory.heimdall.ui.database.DatabaseViewModel
//import de.tomcory.heimdall.ui.main.preferencesStore
//import kotlinx.coroutines.launch
//
////import de.tomcory.heimdall.telemetry.TelemetryExport
//@Composable
//fun TelemetryScreen(viewModel: TelemetryViewModel = viewModel()) {
//    val dataStore = LocalContext.current.preferencesStore
//    val coroutineScope = rememberCoroutineScope() // CoroutineScope tied to the Composable's lifecycle
//    // Assume PreferencesSerializer has boolean fields for telemetry settings
//    val preferences = dataStore.data.collectAsStateWithLifecycle(initialValue = PreferencesSerializer.defaultValue)
//
//    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
//        var isFullyAnonymized by remember { mutableStateOf(preferences.value.isFullyAnonymized) }
//        var isPartlyAnonymized by remember { mutableStateOf(preferences.value.isPartlyAnonymized) }
//        var isNotAnonymized by remember { mutableStateOf(preferences.value.isNotAnonymized) }
//
//        Text("Telemetry Data")
//
//        Spacer(modifier = Modifier.height(20.dp))
//
//        // Example button to demonstrate other actions
//        Button(onClick = { viewModel.createFakeData() }) {
//            Text(text = "Create Fake Data")
//        }
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        Text(text = "Fully Anonymized Data:")
//        Switch(
//                checked = isFullyAnonymized,
//                onCheckedChange = { newValue ->
//                    if (newValue) {
//                        isFullyAnonymized = true
//                        isPartlyAnonymized = false
//                        isNotAnonymized = false
//                        coroutineScope.launch {
//                            dataStore.updateData { prefs ->
//                                prefs.toBuilder()
//                                        .setIsFullyAnonymized(true)
//                                        .setIsPartlyAnonymized(false)
//                                        .setIsNotAnonymized(false)
//                                        .build()
//                            }
//                        }
//                    }
//                }
//        )
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        Text(text = "Partly Anonymized Data:")
//        Switch(
//                checked = isPartlyAnonymized,
//                onCheckedChange = { newValue ->
//                    if (newValue) {
//                        isFullyAnonymized = false
//                        isPartlyAnonymized = true
//                        isNotAnonymized = false
//                        coroutineScope.launch {
//                            dataStore.updateData { prefs ->
//                                prefs.toBuilder()
//                                        .setIsFullyAnonymized(false)
//                                        .setIsPartlyAnonymized(true)
//                                        .setIsNotAnonymized(false)
//                                        .build()
//                            }
//                        }
//                    }
//                }
//        )
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        Text(text = "Not Anonymized Data:")
//        Switch(
//                checked = isNotAnonymized,
//                onCheckedChange = { newValue ->
//                    if (newValue) {
//                        isFullyAnonymized = false
//                        isPartlyAnonymized = false
//                        isNotAnonymized = true
//                        coroutineScope.launch {
//                            dataStore.updateData { prefs ->
//                                prefs.toBuilder()
//                                        .(false)
//                                        .setIsPartlyAnonymized(false)
//                                        .setIsNotAnonymized(true)
//                                        .build()
//                            }
//                        }
//                    }
//                }
//        )
//
//        Spacer(modifier = Modifier.height(10.dp))
//
//        // Additional UI elements and functionality as needed
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun TelemetryScreenPreview() {
//    TelemetryScreen()
//}