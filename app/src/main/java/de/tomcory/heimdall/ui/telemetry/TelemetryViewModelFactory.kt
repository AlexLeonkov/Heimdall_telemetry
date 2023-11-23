//package de.tomcory.heimdall.ui.telemetry
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import de.tomcory.heimdall.telemetry.TelemetryExport
//
//class TelemetryViewModelFactory(private val telemetryExport: TelemetryExport) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(TelemetryViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return TelemetryViewModel(telemetryExport) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}
