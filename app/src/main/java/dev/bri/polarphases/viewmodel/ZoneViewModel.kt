package dev.bri.polarphases.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bri.polarphases.PolarPhasesApp
import dev.bri.polarphases.data.model.HrZone
import dev.bri.polarphases.util.defaultZones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ZoneFormState(
    val id: Long = 0,
    val name: String = "",
    val colorArgb: Int = 0xFF64B5F6.toInt(),
    val bpmMin: String = "",
    val bpmMax: String = "",
    val isEdit: Boolean = false,
)

data class KarvonenFormState(
    val restingHr: String = "62",
    val maxHr: String = "179",
)

class ZoneViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as PolarPhasesApp).zoneRepository

    val zones: StateFlow<List<HrZone>> = repo.observeZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val zoneDialog = MutableStateFlow<ZoneFormState?>(null)
    val karvonenDialog = MutableStateFlow<KarvonenFormState?>(null)

    fun openAddDialog() {
        zoneDialog.value = ZoneFormState()
    }

    fun openEditDialog(zone: HrZone) {
        zoneDialog.value = ZoneFormState(
            id = zone.id,
            name = zone.name,
            colorArgb = zone.colorArgb,
            bpmMin = zone.bpmMin.toString(),
            bpmMax = zone.bpmMax.toString(),
            isEdit = true,
        )
    }

    fun dismissZoneDialog() {
        zoneDialog.value = null
    }

    fun updateZoneForm(update: ZoneFormState.() -> ZoneFormState) {
        zoneDialog.update { it?.update() }
    }

    fun saveZone() {
        val form = zoneDialog.value ?: return
        val min = form.bpmMin.toIntOrNull() ?: return
        val max = form.bpmMax.toIntOrNull() ?: return
        if (form.name.isBlank() || min <= 0 || max <= 0 || min >= max) return
        viewModelScope.launch {
            if (form.isEdit) {
                repo.updateZone(
                    HrZone(
                        id = form.id,
                        name = form.name.trim(),
                        colorArgb = form.colorArgb,
                        bpmMin = min,
                        bpmMax = max,
                        sortOrder = zones.value.find { it.id == form.id }?.sortOrder ?: 0,
                    )
                )
            } else {
                repo.addZone(form.name.trim(), form.colorArgb, min, max)
            }
            zoneDialog.value = null
        }
    }

    fun deleteZone(id: Long) {
        viewModelScope.launch { repo.deleteZone(id) }
    }

    fun openKarvonenDialog() {
        karvonenDialog.value = KarvonenFormState()
    }

    fun dismissKarvonenDialog() {
        karvonenDialog.value = null
    }

    fun updateKarvonenForm(update: KarvonenFormState.() -> KarvonenFormState) {
        karvonenDialog.update { it?.update() }
    }

    fun applyKarvonen() {
        val form = karvonenDialog.value ?: return
        val resting = form.restingHr.toIntOrNull() ?: return
        val max = form.maxHr.toIntOrNull() ?: return
        if (resting <= 0 || max <= resting) return
        viewModelScope.launch {
            repo.replaceWithKarvonenZones(defaultZones(resting, max))
            karvonenDialog.value = null
        }
    }
}
