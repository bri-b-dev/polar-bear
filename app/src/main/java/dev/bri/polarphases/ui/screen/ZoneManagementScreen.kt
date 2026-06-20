package dev.bri.polarphases.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.bri.polarphases.data.model.HrZone
import dev.bri.polarphases.viewmodel.KarvonenFormState
import dev.bri.polarphases.viewmodel.ZoneFormState
import dev.bri.polarphases.viewmodel.ZoneViewModel

private val COLOR_PALETTE = listOf(
    0xFF64B5F6.toInt(), // blue
    0xFF4DB6AC.toInt(), // teal
    0xFF66BB6A.toInt(), // green
    0xFFAED581.toInt(), // light green
    0xFFFFEE58.toInt(), // yellow
    0xFFFFB74D.toInt(), // orange
    0xFFFFA726.toInt(), // amber
    0xFFEF5350.toInt(), // red
    0xFFBA68C8.toInt(), // purple
    0xFF90A4AE.toInt(), // blue grey
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneManagementScreen(viewModel: ZoneViewModel, onBack: () -> Unit) {
    val zones by viewModel.zones.collectAsState()
    val zoneDialog by viewModel.zoneDialog.collectAsState()
    val karvonenDialog by viewModel.karvonenDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HR Zones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.openKarvonenDialog() }) {
                        Text("Karvonen")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add zone")
            }
        },
    ) { padding ->
        if (zones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No zones yet. Tap + to add one.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(zones, key = { it.id }) { zone ->
                    ZoneCard(
                        zone = zone,
                        onEdit = { viewModel.openEditDialog(zone) },
                        onDelete = { viewModel.deleteZone(zone.id) },
                    )
                }
            }
        }
    }

    if (zoneDialog != null) {
        ZoneDialog(
            form = zoneDialog!!,
            onDismiss = { viewModel.dismissZoneDialog() },
            onSave = { viewModel.saveZone() },
            onUpdate = { viewModel.updateZoneForm(it) },
        )
    }

    if (karvonenDialog != null) {
        KarvonenDialog(
            form = karvonenDialog!!,
            onDismiss = { viewModel.dismissKarvonenDialog() },
            onApply = { viewModel.applyKarvonen() },
            onUpdate = { viewModel.updateKarvonenForm(it) },
        )
    }
}

@Composable
private fun ZoneCard(zone: HrZone, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(zone.colorArgb)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(zone.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${zone.bpmMin}–${zone.bpmMax} bpm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ZoneDialog(
    form: ZoneFormState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdate: (ZoneFormState.() -> ZoneFormState) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.isEdit) "Edit Zone" else "Add Zone") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { v -> onUpdate { copy(name = v) } },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.bpmMin,
                        onValueChange = { v -> onUpdate { copy(bpmMin = v.filter { it.isDigit() }) } },
                        label = { Text("Min bpm") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.bpmMax,
                        onValueChange = { v -> onUpdate { copy(bpmMax = v.filter { it.isDigit() }) } },
                        label = { Text("Max bpm") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Color", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLOR_PALETTE.forEach { colorArgb ->
                        val selected = colorArgb == form.colorArgb
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorArgb))
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { onUpdate { copy(colorArgb = colorArgb) } },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun KarvonenDialog(
    form: KarvonenFormState,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onUpdate: (KarvonenFormState.() -> KarvonenFormState) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Karvonen Zone Seed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Replaces all current zones with 5 zones computed from the Karvonen formula.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = form.restingHr,
                    onValueChange = { v -> onUpdate { copy(restingHr = v.filter { it.isDigit() }) } },
                    label = { Text("Resting HR (bpm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.maxHr,
                    onValueChange = { v -> onUpdate { copy(maxHr = v.filter { it.isDigit() }) } },
                    label = { Text("Max HR (bpm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onApply) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
