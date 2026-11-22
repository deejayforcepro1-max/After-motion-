package com.example.ui.editor

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DefaultTemplates
import com.example.model.ParticlePreset
import com.example.model.ShapeKind
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.LayerColorFx
import com.example.ui.theme.LayerColorImage
import com.example.ui.theme.LayerColorShape
import com.example.ui.theme.LayerColorText
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioPanelDark
import com.example.ui.theme.StudioPanelElevated
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.ExportUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddLayerSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.addImageLayer(
                    sampleAssetKey = null,
                    imageUri = uri.toString(),
                    label = "Photo Plate"
                )
            }
        }
    )

    Scaffold(
        containerColor = StudioObsidian,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        when (val state = uiState) {
            EditorUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricViolet)
                }
            }

            EditorUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Composition not found", color = InkPrimary)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Back to Studio")
                        }
                    }
                }
            }

            is EditorUiState.Ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    EditorTopBar(
                        title = state.project.name,
                        specs = "${state.project.aspectRatio.label} • ${state.project.fps}fps",
                        showGuides = state.showSafeGuides,
                        onBack = {
                            viewModel.pausePlayback()
                            onNavigateBack()
                        },
                        onToggleGuides = viewModel::toggleSafeGuides,
                        onAddLayer = { showAddLayerSheet = true },
                        onOpenExport = { showExportDialog = true }
                    )

                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val isWideLayout = maxWidth >= 720.dp

                        if (isWideLayout) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1.35f)
                                        .fillMaxHeight()
                                ) {
                                    CompositionViewport(
                                        project = state.project,
                                        currentTimeMs = state.currentTimeMs,
                                        selectedLayerId = state.selectedLayerId,
                                        isPlaying = state.isPlaying,
                                        showSafeGuides = state.showSafeGuides,
                                        onDragSelectedLayer = viewModel::dragSelectedLayerBy,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TimelinePanel(
                                        project = state.project,
                                        selectedLayerId = state.selectedLayerId,
                                        currentTimeMs = state.currentTimeMs,
                                        isPlaying = state.isPlaying,
                                        isLooping = state.isLooping,
                                        canUndo = state.canUndo,
                                        canRedo = state.canRedo,
                                        onTogglePlayPause = viewModel::togglePlayPause,
                                        onJumpToStart = viewModel::jumpToStart,
                                        onToggleLoop = viewModel::toggleLoop,
                                        onUndo = viewModel::undo,
                                        onRedo = viewModel::redo,
                                        onScrubTo = viewModel::scrubTo,
                                        onSelectLayer = viewModel::selectLayer,
                                        onToggleVisibility = viewModel::toggleLayerVisibility,
                                        onToggleLock = viewModel::toggleLayerLock,
                                        modifier = Modifier.height(220.dp)
                                    )
                                }
                                InspectorPanel(
                                    state = state,
                                    onSelectTab = viewModel::selectTab,
                                    onUpdateProperty = viewModel::updatePropertyValue,
                                    onToggleKeyframe = viewModel::toggleKeyframe,
                                    onSetEasing = viewModel::setDefaultEasing,
                                    onResetTransform = viewModel::resetSelectedTransform,
                                    onUpdateEffects = viewModel::updateSelectedEffects,
                                    onApplyVfxPreset = viewModel::applyVfxPreset,
                                    onUpdateStyle = viewModel::updateSelectedStyle,
                                    onUpdateTiming = viewModel::updateSelectedTiming,
                                    onMoveOrder = viewModel::moveLayerOrder,
                                    onDuplicateLayer = viewModel::duplicateSelectedLayer,
                                    onDeleteLayer = viewModel::deleteLayer,
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .fillMaxHeight()
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                CompositionViewport(
                                    project = state.project,
                                    currentTimeMs = state.currentTimeMs,
                                    selectedLayerId = state.selectedLayerId,
                                    isPlaying = state.isPlaying,
                                    showSafeGuides = state.showSafeGuides,
                                    onDragSelectedLayer = viewModel::dragSelectedLayerBy,
                                    modifier = Modifier.weight(0.36f)
                                )
                                TimelinePanel(
                                    project = state.project,
                                    selectedLayerId = state.selectedLayerId,
                                    currentTimeMs = state.currentTimeMs,
                                    isPlaying = state.isPlaying,
                                    isLooping = state.isLooping,
                                    canUndo = state.canUndo,
                                    canRedo = state.canRedo,
                                    onTogglePlayPause = viewModel::togglePlayPause,
                                    onJumpToStart = viewModel::jumpToStart,
                                    onToggleLoop = viewModel::toggleLoop,
                                    onUndo = viewModel::undo,
                                    onRedo = viewModel::redo,
                                    onScrubTo = viewModel::scrubTo,
                                    onSelectLayer = viewModel::selectLayer,
                                    onToggleVisibility = viewModel::toggleLayerVisibility,
                                    onToggleLock = viewModel::toggleLayerLock,
                                    modifier = Modifier.weight(0.31f)
                                )
                                InspectorPanel(
                                    state = state,
                                    onSelectTab = viewModel::selectTab,
                                    onUpdateProperty = viewModel::updatePropertyValue,
                                    onToggleKeyframe = viewModel::toggleKeyframe,
                                    onSetEasing = viewModel::setDefaultEasing,
                                    onResetTransform = viewModel::resetSelectedTransform,
                                    onUpdateEffects = viewModel::updateSelectedEffects,
                                    onApplyVfxPreset = viewModel::applyVfxPreset,
                                    onUpdateStyle = viewModel::updateSelectedStyle,
                                    onUpdateTiming = viewModel::updateSelectedTiming,
                                    onMoveOrder = viewModel::moveLayerOrder,
                                    onDuplicateLayer = viewModel::duplicateSelectedLayer,
                                    onDeleteLayer = viewModel::deleteLayer,
                                    modifier = Modifier.weight(0.33f)
                                )
                            }
                        }
                    }
                }

                if (showAddLayerSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showAddLayerSheet = false },
                        containerColor = StudioPanelDark,
                        contentColor = InkPrimary
                    ) {
                        AddLayerSheetContent(
                            onAddText = {
                                showAddLayerSheet = false
                                viewModel.addTextLayer()
                            },
                            onAddShape = { kind ->
                                showAddLayerSheet = false
                                viewModel.addShapeLayer(kind)
                            },
                            onAddFx = { preset ->
                                showAddLayerSheet = false
                                viewModel.addParticleLayer(preset)
                            },
                            onPickDevicePhoto = {
                                showAddLayerSheet = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onAddSamplePlate = { key, title ->
                                showAddLayerSheet = false
                                viewModel.addImageLayer(sampleAssetKey = key, label = title)
                            }
                        )
                    }
                }

                if (showExportDialog) {
                    ExportCompositionDialog(
                        exportState = state.exportState,
                        compName = state.project.name,
                        onExportMp4 = { viewModel.exportMp4(context) },
                        onExportPng = { viewModel.exportPngFrame(context) },
                        onShare = { uri, mimeType ->
                            shareExportedFile(context, uri, mimeType)
                        },
                        onDismiss = {
                            viewModel.dismissExportDialog()
                            showExportDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    specs: String,
    showGuides: Boolean,
    onBack: () -> Unit,
    onToggleGuides: () -> Unit,
    onAddLayer: () -> Unit,
    onOpenExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioPanelDark)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("editor_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = InkPrimary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = specs,
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary
            )
        }

        IconButton(
            onClick = onToggleGuides,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("toggle_guides_button")
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = stringResource(R.string.cd_toggle_guides),
                tint = if (showGuides) CyberCyan else InkMuted
            )
        }

        OutlinedButton(
            onClick = onAddLayer,
            border = BorderStroke(1.dp, ElectricViolet),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("add_layer_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Layer", style = MaterialTheme.typography.labelMedium, color = InkPrimary)
        }

        Button(
            onClick = onOpenExport,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("export_button")
        ) {
            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.cd_export), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Export", style = MaterialTheme.typography.labelMedium)
        }
    }
    HorizontalDivider(color = StudioHairline)
}

@Composable
private fun AddLayerSheetContent(
    onAddText: () -> Unit,
    onAddShape: (ShapeKind) -> Unit,
    onAddFx: (ParticlePreset) -> Unit,
    onPickDevicePhoto: () -> Unit,
    onAddSamplePlate: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Layer to Composition", style = MaterialTheme.typography.titleLarge, color = InkPrimary)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LayerTileButton(
                icon = Icons.Default.TextFields,
                label = "Kinetic Text",
                accent = LayerColorText,
                onClick = onAddText,
                modifier = Modifier.weight(1f).testTag("add_text_layer")
            )
            LayerTileButton(
                icon = Icons.Default.Category,
                label = "Vector Ring",
                accent = LayerColorShape,
                onClick = { onAddShape(ShapeKind.RING) },
                modifier = Modifier.weight(1f).testTag("add_shape_layer")
            )
            LayerTileButton(
                icon = Icons.Default.AutoAwesome,
                label = "Starfield FX",
                accent = LayerColorFx,
                onClick = { onAddFx(ParticlePreset.NEON_STARFIELD) },
                modifier = Modifier.weight(1f).testTag("add_fx_layer")
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LayerTileButton(
                icon = Icons.Default.PhotoLibrary,
                label = "Device Photo",
                accent = LayerColorImage,
                onClick = onPickDevicePhoto,
                modifier = Modifier.weight(1f).testTag("add_photo_layer")
            )
            LayerTileButton(
                icon = Icons.Default.Image,
                label = "Tokyo Neon",
                accent = CyberCyan,
                onClick = { onAddSamplePlate(DefaultTemplates.ASSET_CYBER_CITY, "Tokyo Neon Plate") },
                modifier = Modifier.weight(1f)
            )
            LayerTileButton(
                icon = Icons.Default.AutoAwesome,
                label = "Nebula FX",
                accent = ElectricViolet,
                onClick = { onAddSamplePlate(DefaultTemplates.ASSET_NEBULA, "Plasma Nebula") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LayerTileButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioPanelElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioHairline),
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = InkPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun ExportCompositionDialog(
    exportState: ExportUiState,
    compName: String,
    onExportMp4: () -> Unit,
    onExportPng: () -> Unit,
    onShare: (android.net.Uri, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (exportState !is ExportUiState.Exporting) onDismiss()
        },
        containerColor = StudioPanelDark,
        titleContentColor = InkPrimary,
        title = {
            Text("Render & Export", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            when (exportState) {
                ExportUiState.Idle -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Render \"$compName\" with all keyframes, blend modes, and VFX shaders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                        Button(
                            onClick = onExportMp4,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("render_mp4_button")
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Render Video (H.264 MP4)")
                        }
                        OutlinedButton(
                            onClick = onExportPng,
                            border = BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("render_png_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = CyberCyan)
                            Spacer(Modifier.width(8.dp))
                            Text("Export Current Frame (PNG)", color = CyberCyan)
                        }
                    }
                }

                is ExportUiState.Exporting -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { exportState.progress.coerceIn(0f, 1f) },
                            color = CyberCyan,
                            trackColor = StudioHairline,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkPrimary
                        )
                    }
                }

                is ExportUiState.Success -> {
                    val res = exportState.result
                    val isVideo = res.formatLabel.startsWith("MP4")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldActive)
                            Text("Render Complete", style = MaterialTheme.typography.titleMedium, color = InkPrimary)
                        }
                        Text(
                            text = "${res.formatLabel} • ${res.fileSizeKb} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                        if (res.savedToGallery) {
                            Text(
                                text = "Saved to your device Gallery under AfterMotion.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }
                        Button(
                            onClick = {
                                onShare(res.shareUri, if (isVideo) "video/mp4" else "image/png")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share Render")
                        }
                    }
                }

                is ExportUiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoseAlert
                        )
                        OutlinedButton(
                            onClick = onExportPng,
                            border = BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Current Frame (PNG) Instead", color = CyberCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (exportState !is ExportUiState.Exporting) {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = InkSecondary)
                }
            }
        }
    )
}

private fun shareExportedFile(context: Context, uri: android.net.Uri, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share AfterMotion Render"))
}
