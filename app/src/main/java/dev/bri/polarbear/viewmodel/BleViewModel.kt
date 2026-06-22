package dev.bri.polarbear.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dev.bri.polarbear.ble.BleManager
import dev.bri.polarbear.ble.BleUiState
import dev.bri.polarbear.ble.ScannedDevice
import kotlinx.coroutines.flow.StateFlow

class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = BleManager(application)
    val state: StateFlow<BleUiState> = bleManager.state

    init {
        bleManager.tryAutoReconnect()
    }

    fun startScan() = bleManager.startScan()
    fun stopScan() = bleManager.stopScan()
    fun connect(device: ScannedDevice) = bleManager.connect(device)
    fun disconnect() = bleManager.disconnect()

    override fun onCleared() {
        super.onCleared()
        bleManager.close()
    }
}
