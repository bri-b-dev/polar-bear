package dev.bri.polarbear.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.bri.polarbear.data.model.HrZone
import dev.bri.polarbear.viewmodel.BlockFormState
import dev.bri.polarbear.viewmodel.BlockPhaseForm
import dev.bri.polarbear.viewmodel.PhaseFormState
import dev.bri.polarbear.viewmodel.SequenceItemDraft
import dev.bri.polarbear.viewmodel.TemplateBuilderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateBuilderScreen(
    viewModel: TemplateBuilderViewModel,
    onBack: () -> Unit,
) {
    val templateName by viewModel.templateName.collectAsState()
    val sequenceItems by viewModel.sequenceItems.collectAsState()
    val phaseDialog by viewModel.phaseDialog.collectAsState()
    val blockDialog by viewModel.blockDialog.collectAsState()
    val zones by viewModel.zones.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val validationError by viewModel.validationError.collectAsState()
    val editingTemplateId by viewModel.editingTemplateId.collectAsState()

    LaunchedEffect(isSaved) {
        if (isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingTemplateId != null) "Edit Template" else "New Template") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveTemplate() }) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = templateName,
                onValueChange = viewModel::updateTemplateName,
                label = { Text("Template name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = validationError != null && templateName.isBlank(),
            )
            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(sequenceItems) { index, item ->
                    val canMoveUp = index > 0
                    val canMoveDown = index < sequenceItems.size - 1
                    when (item) {
                        is SequenceItemDraft.Phase -> PhaseItemCard(
                            item = item,
                            onEdit = { viewModel.openEditPhaseDialog(index) },
                            onDelete = { viewModel.removeSequenceItem(index) },
                            onMoveUp = if (canMoveUp) { { viewModel.moveItemUp(index) } } else null,
                            onMoveDown = if (canMoveDown) { { viewModel.moveItemDown(index) } } else null,
                        )
                        is SequenceItemDraft.Block -> BlockItemCard(
                            item = item,
                            onEdit = { viewModel.openEditBlockDialog(index) },
                            onDelete = { viewModel.removeSequenceItem(index) },
                            onMoveUp = if (canMoveUp) { { viewModel.moveItemUp(index) } } else null,
                            onMoveDown = if (canMoveDown) { { viewModel.moveItemDown(index) } } else null,
                        )
                    }
                }
            }

            if (zones.isEmpty()) {
                Text(
                    "Set up HR Zones first before building templates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.openAddPhaseDialog() },
                    modifier = Modifier.weight(1f),
                    enabled = zones.isNotEmpty(),
                ) { Text("+ Phase") }
                OutlinedButton(
                    onClick = { viewModel.openAddBlockDialog() },
                    modifier = Modifier.weight(1f),
                    enabled = zones.isNotEmpty(),
                ) { Text("+ Block") }
            }
        }
    }

    if (phaseDialog != null) {
        PhaseDialog(
            form = phaseDialog!!,
            zones = zones,
            onUpdate = viewModel::updatePhaseForm,
            onToggleZone = viewModel::togglePhaseZone,
            onConfirm = viewModel::confirmAddPhase,
            onDismiss = viewModel::dismissPhaseDialog,
        )
    }

    if (blockDialog != null) {
        BlockDialog(
            form = blockDialog!!,
            zones = zones,
            onRepeatCountChange = viewModel::updateBlockRepeatCount,
            onUpdatePhase = viewModel::updateBlockPhase,
            onToggleBlockPhaseZone = viewModel::toggleBlockPhaseZone,
            onAddPhase = viewModel::addPhaseToBlock,
            onRemovePhase = viewModel::removePhaseFromBlock,
            onConfirm = viewModel::confirmAddBlock,
            onDismiss = viewModel::dismissBlockDialog,
        )
    }
}

// ── Sequence cards ────────────────────────────────────────────────────────────

@Composable
private fun PhaseItemCard(
    item: SequenceItemDraft.Phase,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            IconButton(
                onClick = { onMoveUp?.invoke() },
                enabled = onMoveUp != null,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { onMoveDown?.invoke() },
                enabled = onMoveDown != null,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
        ZoneDots(colors = item.zoneColors, size = 20)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleSmall)
            Text(
                buildString {
                    append(item.zoneNames.joinToString(" · "))
                    append(" · ")
                    append(formatDuration(item.durationSeconds))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit phase")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Remove phase")
        }
    }
}

@Composable
private fun BlockItemCard(
    item: SequenceItemDraft.Block,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "${item.repeatCount}×",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Repeat Block",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit block")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove block")
            }
        }
        Spacer(Modifier.height(4.dp))
        item.phases.forEach { phase ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            ) {
                ZoneDots(colors = phase.zoneColors, size = 14)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${phase.name} · ${phase.zoneNames.joinToString(" · ")} · ${formatDuration(phase.durationSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ZoneDots(colors: List<Int>, size: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(Color(color)),
            )
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

// ── Phase dialog ──────────────────────────────────────────────────────────────

@Composable
private fun PhaseDialog(
    form: PhaseFormState,
    zones: List<HrZone>,
    onUpdate: (PhaseFormState.() -> PhaseFormState) -> Unit,
    onToggleZone: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.editingIndex != null) "Edit Phase" else "Add Phase") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { v -> onUpdate { copy(name = v) } },
                    label = { Text("Phase name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.minutes,
                        onValueChange = { v -> onUpdate { copy(minutes = v.filter { it.isDigit() }) } },
                        label = { Text("Min") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.seconds,
                        onValueChange = { v -> onUpdate { copy(seconds = v.filter { it.isDigit() }) } },
                        label = { Text("Sec") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                ZoneMultiPicker(
                    selectedZoneIds = form.zoneIds,
                    zones = zones,
                    onToggle = onToggleZone,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (form.editingIndex != null) "Save" else "Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Block dialog ──────────────────────────────────────────────────────────────

@Composable
private fun BlockDialog(
    form: BlockFormState,
    zones: List<HrZone>,
    onRepeatCountChange: (String) -> Unit,
    onUpdatePhase: (Int, BlockPhaseForm.() -> BlockPhaseForm) -> Unit,
    onToggleBlockPhaseZone: (Int, Long) -> Unit,
    onAddPhase: () -> Unit,
    onRemovePhase: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.editingIndex != null) "Edit Block" else "Add Repeat Block") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = form.repeatCount,
                    onValueChange = onRepeatCountChange,
                    label = { Text("Repeat count") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
                form.phases.forEachIndexed { index, phaseForm ->
                    BlockPhaseFormRow(
                        index = index,
                        form = phaseForm,
                        zones = zones,
                        canRemove = form.phases.size > 2,
                        onUpdate = { update -> onUpdatePhase(index, update) },
                        onToggleZone = { zoneId -> onToggleBlockPhaseZone(index, zoneId) },
                        onRemove = { onRemovePhase(index) },
                    )
                    if (index < form.phases.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                TextButton(
                    onClick = onAddPhase,
                    modifier = Modifier.align(Alignment.Start),
                ) { Text("+ Add phase to block") }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (form.editingIndex != null) "Save" else "Add Block")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BlockPhaseFormRow(
    index: Int,
    form: BlockPhaseForm,
    zones: List<HrZone>,
    canRemove: Boolean,
    onUpdate: (BlockPhaseForm.() -> BlockPhaseForm) -> Unit,
    onToggleZone: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Phase ${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove phase",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        OutlinedTextField(
            value = form.name,
            onValueChange = { v -> onUpdate { copy(name = v) } },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.minutes,
                onValueChange = { v -> onUpdate { copy(minutes = v.filter { it.isDigit() }) } },
                label = { Text("Min") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = form.seconds,
                onValueChange = { v -> onUpdate { copy(seconds = v.filter { it.isDigit() }) } },
                label = { Text("Sec") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        ZoneMultiPicker(
            selectedZoneIds = form.zoneIds,
            zones = zones,
            onToggle = onToggleZone,
        )
    }
}

// ── Zone multi-picker ─────────────────────────────────────────────────────────

@Composable
private fun ZoneMultiPicker(
    selectedZoneIds: List<Long>,
    zones: List<HrZone>,
    onToggle: (Long) -> Unit,
) {
    Column {
        Text("Zones", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        zones.forEach { zone ->
            val selected = zone.id in selectedZoneIds
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(zone.id) }
                    .padding(vertical = 2.dp),
            ) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggle(zone.id) },
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(zone.colorArgb)),
                )
                Spacer(Modifier.width(8.dp))
                Text(zone.name, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
