package dev.bri.polarbear.ble

sealed class BleUiState {
    data object Idle : BleUiState()
    data class Scanning(val devices: List<ScannedDevice> = emptyList()) : BleUiState()
    data class DevicesFound(val devices: List<ScannedDevice>) : BleUiState()
    data class Connecting(val deviceName: String) : BleUiState()
    data class Reconnecting(val deviceName: String) : BleUiState()
    data class Connected(val deviceName: String, val bpm: Int?, val batteryLevel: Int? = null) : BleUiState()
    data class Error(val message: String) : BleUiState()
}
