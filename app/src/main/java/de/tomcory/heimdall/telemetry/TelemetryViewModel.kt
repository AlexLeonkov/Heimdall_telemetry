//package de.tomcory.heimdall.telemetry
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.launch
//import de.tomcory.heimdall.telemetry.TelemetryExport
//// Assume TelemetryExport is imported correctly
//
//class TelemetryViewModel(private val telemetryExport: TelemetryExport) : ViewModel() {
//
//    fun exportTelemetryData() {
//        viewModelScope.launch {
//            telemetryExport.exportDataToServer()
//        }
//    }
//}
