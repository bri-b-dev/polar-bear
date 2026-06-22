package dev.bri.polarbear.ui.screen

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dev.bri.polarbear.ble.BleUiState
import dev.bri.polarbear.ble.ScannedDevice
import dev.bri.polarbear.viewmodel.BleViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HrMonitorScreen(
    viewModel: BleViewModel,
    onNavigateToZones: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        )
    } else {
        rememberMultiplePermissionsState(listOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Polar Bear") },
                actions = {
                    TextButton(onClick = onNavigateToTemplates) { Text("Templates") }
                    IconButton(onClick = onNavigateToZones) {
                        Icon(Icons.Default.Settings, contentDescription = "HR Zones")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (!blePermissions.allPermissionsGranted) {
                PermissionContent(
                    shouldShowRationale = blePermissions.shouldShowRationale,
                    onRequest = { blePermissions.launchMultiplePermissionRequest() },
                )
            } else {
                BleContent(
                    state = state,
                    onScan = { viewModel.startScan() },
                    onStopScan = { viewModel.stopScan() },
                    onConnect = { device -> viewModel.connect(device) },
                    onDisconnect = { viewModel.disconnect() },
                )
            }
        }
    }
}

@Composable
private fun PermissionContent(shouldShowRationale: Boolean, onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (shouldShowRationale)
                "Bluetooth permission is needed to connect to the Polar H10."
            else
                "Bluetooth access is required to scan for and connect to the Polar H10.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant Permission") }
    }
}

@Composable
private fun BleContent(
    state: BleUiState,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (ScannedDevice) -> Unit,
    onDisconnect: () -> Unit,
) {
    when (state) {
        is BleUiState.Idle -> IdleContent(onScan = onScan)
        is BleUiState.Scanning -> ScanningContent(
            devices = state.devices,
            onStop = onStopScan,
            onConnect = onConnect,
        )
        is BleUiState.DevicesFound -> DevicesFoundContent(
            devices = state.devices,
            onScan = onScan,
            onConnect = onConnect,
        )
        is BleUiState.Connecting -> ConnectingContent(deviceName = state.deviceName)
        is BleUiState.Reconnecting -> ConnectingContent(deviceName = state.deviceName)
        is BleUiState.Connected -> ConnectedContent(
            deviceName = state.deviceName,
            bpm = state.bpm,
            batteryLevel = state.batteryLevel,
            onDisconnect = onDisconnect,
        )
        is BleUiState.Error -> ErrorContent(message = state.message, onRetry = onScan)
    }
}

@Composable
private fun IdleContent(onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Ready to connect to Polar H10", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onScan) { Text("Scan for Devices") }
    }
}

@Composable
private fun ScanningContent(
    devices: List<ScannedDevice>,
    onStop: () -> Unit,
    onConnect: (ScannedDevice) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Scanning…", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onStop) { Text("Stop") }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        if (devices.isNotEmpty()) DeviceList(devices = devices, onConnect = onConnect)
    }
}

@Composable
private fun DevicesFoundContent(
    devices: List<ScannedDevice>,
    onScan: () -> Unit,
    onConnect: (ScannedDevice) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${devices.size} device(s) found", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onScan) { Text("Scan Again") }
        }
        Spacer(Modifier.height(16.dp))
        if (devices.isEmpty()) {
            Text(
                text = "No Heart Rate devices found nearby.\nMake sure the H10 is worn and active.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            DeviceList(devices = devices, onConnect = onConnect)
        }
    }
}

@Composable
private fun DeviceList(devices: List<ScannedDevice>, onConnect: (ScannedDevice) -> Unit) {
    LazyColumn {
        items(devices) { device ->
            DeviceItem(device = device, onConnect = { onConnect(device) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun DeviceItem(device: ScannedDevice, onConnect: () -> Unit) {
    ListItem(
        headlineContent = { Text(device.name) },
        supportingContent = { Text("${device.address}  •  ${device.rssi} dBm") },
        trailingContent = { Button(onClick = onConnect) { Text("Connect") } },
        modifier = Modifier.clickable { onConnect() },
    )
}

@Composable
private fun ConnectingContent(deviceName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Connecting to $deviceName…", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ConnectedContent(deviceName: String, bpm: Int?, batteryLevel: Int?, onDisconnect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = bpm?.toString() ?: "–",
            fontSize = 96.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = if (bpm != null) "bpm" else "waiting for data…",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = deviceName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (batteryLevel != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Battery: $batteryLevel%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Connection Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}
