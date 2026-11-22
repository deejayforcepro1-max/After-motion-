package com.example.ui.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DefaultTemplates
import com.example.engine.AssetHelper
import com.example.model.AspectRatioPreset
import com.example.model.CompositionProject
import com.example.model.KeyframeEngine
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioPanelDark
import com.example.ui.theme.StudioPanelElevated
import com.example.viewmodel.ProjectsViewModel

private data class TemplatePreviewInfo(
    val title: String,
    val subtitle: String,
    val specs: String,
    val drawableRes: Int
)

@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onOpenProject: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    val templates = remember {
        listOf(
            TemplatePreviewInfo(
                title = "Neon Title Sequence",
                subtitle = "Kinetic type + Screen nebula",
                specs = "16:9 • 30fps • 5s",
                drawableRes = R.drawable.img_sample_cyber_city
            ),
            TemplatePreviewInfo(
                title = "Cyberpunk Visor Reel",
                subtitle = "Chromatic split + light sweep",
                specs = "9:16 • 30fps • 4.5s",
                drawableRes = R.drawable.img_sample_portrait
            ),
            TemplatePreviewInfo(
                title = "Kinetic Prism Morph",
                subtitle = "60fps vector shapes + grid",
                specs = "1:1 • 60fps • 4s",
                drawableRes = R.drawable.img_sample_nebula
            )
        )
    }

    Scaffold(
        containerColor = StudioObsidian,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = ElectricViolet,
                contentColor = InkPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("new_composition_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.new_composition),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ElectricViolet.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * 0.15f, size.height * 0.08f),
                            radius = size.maxDimension * 0.45f
                        ),
                        radius = size.maxDimension * 0.45f,
                        center = Offset(size.width * 0.15f, size.height * 0.08f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.22f),
                            radius = size.maxDimension * 0.4f
                        ),
                        radius = size.maxDimension * 0.4f,
                        center = Offset(size.width * 0.88f, size.height * 0.22f)
                    )
                }
        ) {
            val columns = if (maxWidth >= 640.dp) 2 else 1

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 48.dp,
                    bottom = 112.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(columns) }) {
                    StudioHeader(projectCount = projects.size)
                }

                item(span = { GridItemSpan(columns) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Motion Templates",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(templates) { idx, tpl ->
                                TemplateCard(
                                    info = tpl,
                                    onClick = {
                                        viewModel.createFromTemplate(idx, onOpenProject)
                                    },
                                    modifier = Modifier.testTag("template_card_$idx")
                                )
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(columns) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compositions",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                        Text(
                            text = "${projects.size} saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                    }
                }

                if (projects.isEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        EmptyProjectsCard(onNewComp = { showCreateDialog = true })
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        CompositionCard(
                            project = project,
                            onOpen = { onOpenProject(project.id) },
                            onDuplicate = { viewModel.duplicateComposition(project) },
                            onDelete = { viewModel.deleteComposition(project.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NewCompositionDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, ratio, durationMs, fps, bgHex ->
                showCreateDialog = false
                viewModel.createNewComposition(name, ratio, durationMs, fps, bgHex, onOpenProject)
            }
        )
    }
}

@Composable
private fun StudioHeader(projectCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(ElectricViolet, CyberCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieCreation,
                        contentDescription = null,
                        tint = InkPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.projects_header_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkPrimary
                    )
                    Text(
                        text = stringResource(R.string.projects_header_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            }
        }
        Surface(
            color = StudioPanelElevated,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, StudioHairline)
        ) {
            Text(
                text = "VFX • $projectCount",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TemplateCard(
    info: TemplatePreviewInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(228.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = StudioPanelDark),
        border = BorderStroke(1.dp, StudioHairline),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Image(
                    painter = painterResource(id = info.drawableRes),
                    contentDescription = info.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, StudioPanelDark.copy(alpha = 0.88f))
                            )
                        )
                )
                Surface(
                    color = StudioObsidian.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = info.specs,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(ElectricViolet),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Use template",
                        tint = InkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = info.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompositionCard(
    project: CompositionProject,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val previewKey = project.layers.firstOrNull { it.sampleAssetKey != null }?.sampleAssetKey
    val sampleDrawable = AssetHelper.drawableForSampleKey(previewKey)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("composition_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioPanelDark),
        border = BorderStroke(1.dp, StudioHairline),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(project.backgroundColorHex))
                    .border(1.dp, StudioHairline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (sampleDrawable != null) {
                    Image(
                        painter = painterResource(id = sampleDrawable),
                        contentDescription = project.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        ElectricViolet.copy(alpha = 0.35f),
                                        CyberCyan.copy(alpha = 0.2f)
                                    )
                                )
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(StudioObsidian.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Open composition",
                        tint = InkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = StudioPanelElevated,
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = project.aspectRatio.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${KeyframeEngine.formatSecondsCompact(project.durationMs)} • ${project.fps}fps",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = InkMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${project.layers.size} layers • ${project.layers.sumOf { it.keyframes.size }} keyframes",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("duplicate_comp_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate composition",
                        tint = InkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_comp_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete composition",
                        tint = RoseAlert.copy(alpha = 0.85f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyProjectsCard(onNewComp: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioPanelDark),
        border = BorderStroke(1.dp, StudioHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MovieCreation,
                contentDescription = null,
                tint = ElectricViolet,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "No compositions yet",
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary
            )
            Text(
                text = "Start from a template above or create a blank composition to animate text, shapes, and VFX.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
            Button(
                onClick = onNewComp,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
            ) {
                Text(stringResource(R.string.create_composition))
            }
        }
    }
}

@Composable
private fun NewCompositionDialog(
    onDismiss: () -> Unit,
    onCreate: (String, AspectRatioPreset, Long, Int, Long) -> Unit
) {
    var name by remember { mutableStateOf("Motion Comp 01") }
    var selectedRatio by remember { mutableStateOf(AspectRatioPreset.LANDSCAPE_16_9) }
    var durationSec by remember { mutableFloatStateOf(5f) }
    var selectedFps by remember { mutableIntStateOf(30) }
    var selectedBgHex by remember { mutableLongStateOf(0xFF0A0C12) }

    val bgSwatches = remember {
        listOf(0xFF0A0C12, 0xFF000000, 0xFF141026, 0xFF0A1D24, 0xFFF4F6FA)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanelDark,
        titleContentColor = InkPrimary,
        textContentColor = InkSecondary,
        title = {
            Text(
                text = stringResource(R.string.create_composition),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Composition Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("comp_name_input")
                )

                Text("Aspect Ratio", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AspectRatioPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedRatio == preset,
                            onClick = { selectedRatio = preset },
                            label = {
                                Text(
                                    text = preset.label.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = InkPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duration", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                    Text(
                        text = "${durationSec.toInt()}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                }
                Slider(
                    value = durationSec,
                    onValueChange = { durationSec = it },
                    valueRange = 2f..15f,
                    steps = 12
                )

                Text("Frame Rate", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 30, 60).forEach { fps ->
                        FilterChip(
                            selected = selectedFps == fps,
                            onClick = { selectedFps = fps },
                            label = { Text("$fps fps", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = StudioObsidian
                            )
                        )
                    }
                }

                Text("Canvas Backdrop", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    bgSwatches.forEach { hex ->
                        val isSelected = selectedBgHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) CyberCyan else StudioHairline,
                                    shape = CircleShape
                                )
                                .clickable { selectedBgHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        name,
                        selectedRatio,
                        (durationSec.toInt() * 1000L),
                        selectedFps,
                        selectedBgHex
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("confirm_create_comp_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = InkSecondary)
            }
        }
    )
}
