package de.tomcory.heimdall.ui.telemetry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TelemetryScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        var isToggleOn by remember { mutableStateOf(false) }

        Text("Telemetry Data")

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Toggle Telemetry:")
        Switch(
            checked = isToggleOn,
            onCheckedChange = { isToggleOn = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = if (isToggleOn) "Telemetry: ON" else "Telemetry: OFF")
    }
}

@Preview(showBackground = true)
@Composable
fun TelemetryScreenPreview() {
    TelemetryScreen()
}
