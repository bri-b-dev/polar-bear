package dev.bri.polarbear.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

private val HR_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
private val HR_CHAR_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
private val BATTERY_CHAR_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

class BleManager(private val context: Context) {

    private val bluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val prefs by lazy { context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE) }

    private val _state = MutableStateFlow<BleUiState>(BleUiState.Idle)
    val state: StateFlow<BleUiState> = _state.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private val scannedDevices = mutableListOf<ScannedDevice>()
    private var connectedDeviceName = ""
    private var intentionalDisconnect = false
    private var batteryLevel: Int? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.scanRecord?.deviceName ?: result.device.address
            val idx = scannedDevices.indexOfFirst { it.address == result.device.address }
            val scanned = ScannedDevice(
                name = name,
                address = result.device.address,
                rssi = result.rssi,
                bluetoothDevice = result.device,
            )
            if (idx < 0) scannedDevices.add(scanned) else scannedDevices[idx] = scanned
            _state.value = BleUiState.Scanning(scannedDevices.toList())
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = BleUiState.Error("Scan failed (error $errorCode) — is Bluetooth enabled?")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                val msg = if (status == 133)
                    "Could not connect (status 133) — H10 may already have 2 active connections"
                else
                    "Connection error (status $status)"
                _state.value = BleUiState.Error(msg)
                gatt.close()
                this@BleManager.gatt = null
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> gatt.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (intentionalDisconnect) {
                        intentionalDisconnect = false
                        batteryLevel = null
                        _state.value = BleUiState.Idle
                        gatt.close()
                        this@BleManager.gatt = null
                    } else {
                        _state.value = BleUiState.Reconnecting(connectedDeviceName)
                        gatt.connect()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.value = BleUiState.Error("Service discovery failed (status $status)")
                return
            }
            val hrChar = gatt.getService(HR_SERVICE_UUID)?.getCharacteristic(HR_CHAR_UUID)
            if (hrChar == null) {
                _state.value = BleUiState.Error("Heart Rate Service (0x180D) not found on this device")
                return
            }
            gatt.setCharacteristicNotification(hrChar, true)
            val cccd = hrChar.getDescriptor(CCCD_UUID) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(cccd)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (descriptor.uuid == CCCD_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                _state.value = BleUiState.Connected(deviceName = connectedDeviceName, bpm = null)
                // Read battery level if Battery Service is present
                gatt.getService(BATTERY_SERVICE_UUID)?.getCharacteristic(BATTERY_CHAR_UUID)
                    ?.let { gatt.readCharacteristic(it) }
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                && characteristic.uuid == BATTERY_CHAR_UUID
                && status == BluetoothGatt.GATT_SUCCESS
            ) {
                handleBatteryRead(characteristic.value)
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (characteristic.uuid == BATTERY_CHAR_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                handleBatteryRead(value)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                && characteristic.uuid == HR_CHAR_UUID
            ) {
                handleHrNotification(characteristic.value)
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == HR_CHAR_UUID) handleHrNotification(value)
        }
    }

    private fun handleHrNotification(value: ByteArray) {
        val bpm = HrParser.parse(value) ?: return
        _state.value = BleUiState.Connected(
            deviceName = connectedDeviceName,
            bpm = bpm,
            batteryLevel = batteryLevel,
        )
    }

    private fun handleBatteryRead(value: ByteArray) {
        batteryLevel = value.getOrNull(0)?.toInt()?.and(0xFF)
        val current = _state.value as? BleUiState.Connected ?: return
        _state.value = current.copy(batteryLevel = batteryLevel)
    }

    fun tryAutoReconnect() {
        val address = prefs.getString("last_address", null) ?: return
        val name = prefs.getString("last_name", address) ?: address
        if (bluetoothAdapter?.isEnabled != true) return
        try {
            val device = bluetoothAdapter.getRemoteDevice(address) ?: return
            connectedDeviceName = name
            batteryLevel = null
            _state.value = BleUiState.Reconnecting(name)
            gatt = device.connectGatt(context, true, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (_: Exception) {
            _state.value = BleUiState.Idle
        }
    }

    fun startScan() {
        if (bluetoothAdapter?.isEnabled != true) {
            _state.value = BleUiState.Error("Bluetooth is disabled — please enable it and try again")
            return
        }
        scannedDevices.clear()
        _state.value = BleUiState.Scanning()

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HR_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothAdapter.bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
    }

    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        val current = _state.value
        if (current is BleUiState.Scanning) {
            _state.value = BleUiState.DevicesFound(scannedDevices.toList())
        }
    }

    fun connect(device: ScannedDevice) {
        stopScan()
        connectedDeviceName = device.name
        batteryLevel = null
        _state.value = BleUiState.Connecting(device.name)
        prefs.edit()
            .putString("last_address", device.address)
            .putString("last_name", device.name)
            .apply()
        gatt = device.bluetoothDevice.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE,
        )
    }

    fun disconnect() {
        intentionalDisconnect = true
        gatt?.disconnect()
    }

    fun close() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        gatt?.close()
        gatt = null
    }
}
