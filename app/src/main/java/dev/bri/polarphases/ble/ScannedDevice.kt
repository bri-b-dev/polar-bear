package dev.bri.polarphases.ble

import android.bluetooth.BluetoothDevice

data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val bluetoothDevice: BluetoothDevice,
)
