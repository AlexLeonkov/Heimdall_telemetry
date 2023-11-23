//package de.tomcory.heimdall.ui.telemetry
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import de.tomcory.heimdall.telemetry.TelemetryExport
//import kotlinx.coroutines.launch
//
//class TelemetryViewModel(private val telemetryExport: TelemetryExport) : ViewModel() {
//
//    fun exportTelemetryData() {
//        viewModelScope.launch {
//            telemetryExport.exportDataToServer()
//        }
//    }
//}
