package com.example.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.DefaultTemplates
import com.example.model.AnimatableProperty
import com.example.model.BlendModeOption
import com.example.model.EasingType
import com.example.model.KeyframeEngine
import com.example.model.LayerEffects
import com.example.model.LayerItem
import com.example.model.LayerType
import com.example.model.ParticlePreset
import com.example.model.ShapeKind
import com.example.model.VfxPreset
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioPanelDark
import com.example.ui.theme.StudioPanelElevated
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.InspectorTab
import kotlin.math.roundToInt

private val StudioColorSwatches = listOf(
    0xFFF8FAFC, 0xFF7C6BFF, 0xFF22D3EE, 0xFFF43F5E,
    0xFFF59E0B, 0xFF10B981, 0xFF3B82F6, 0xFF0A0C12
)

@Composable
fun InspectorPanel(
    state: EditorUiState.Ready,
    onSelectTab: (InspectorTab) -> Unit,
    onUpdateProperty: (AnimatableProperty, Float) -> Unit,
    onToggleKeyframe: (AnimatableProperty) -> Unit,
    onSetEasing: (EasingType) -> Unit,
    onResetTransform: () -> Unit,
    onUpdateEffects: ((LayerEffects) -> LayerEffects) -> Unit,
    onApplyVfxPreset: (VfxPreset) -> Unit,
    onUpdateStyle: ((LayerItem) -> LayerItem) -> Unit,
    onUpdateTiming: (Long, Long) -> Unit,
    onMoveOrder: (String, Boolean) -> Unit,
    onDuplicateLayer: () -> Unit,
    onDeleteLayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = state.selectedLayer
    var showEasingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioPanelDark)
    ) {
        TabRow(
            selectedTabIndex = state.activeTab.ordinal,
            containerColor = StudioPanelElevated,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[state.activeTab.ordinal]),
                    color = CyberCyan
                )
            }
        ) {
            InspectorTab.entries.forEach { tab ->
                Tab(
                    selected = state.activeTab == tab,
                    onClick = { onSelectTab(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    },
                    selectedContentColor = CyberCyan,
                    unselectedContentColor = InkSecondary,
                    modifier = Modifier.testTag("inspector_tab_${tab.name.lowercase()}")
                )
            }
        }

        if (selected == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select or add a layer to inspect keyframes, blend modes, and VFX.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (state.activeTab) {
                    InspectorTab.TRANSFORM -> TransformTabContent(
                        layer = selected,
                        currentTimeMs = state.currentTimeMs,
                        defaultEasing = state.defaultEasing,
                        onUpdateProperty = onUpdateProperty,
                        onToggleKeyframe = onToggleKeyframe,
                        onOpenEasingPicker = { showEasingDialog = true },
                        onResetTransform = onResetTransform
                    )
                    InspectorTab.EFFECTS -> EffectsTabContent(
                        layer = selected,
                        onUpdateEffects = onUpdateEffects,
                        onApplyPreset = onApplyVfxPreset
                    )
                    InspectorTab.STYLE -> StyleTabContent(
                        layer = selected,
                        onUpdateStyle = onUpdateStyle
                    )
                    InspectorTab.TIMING -> TimingTabContent(
                        layer = selected,
                        durationMs = state.project.durationMs,
                        onUpdateTiming = onUpdateTiming,
                        onMoveUp = { onMoveOrder(selected.id, true) },
                        onMoveDown = { onMoveOrder(selected.id, false) },
                        onDuplicate = onDuplicateLayer,
                        onDelete = { onDeleteLayer(selected.id) }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showEasingDialog) {
        EasingCurveDialog(
            currentEasing = state.defaultEasing,
            onSelect = {
                onSetEasing(it)
                showEasingDialog = false
            },
            onDismiss = { showEasingDialog = false }
        )
    }
}

@Composable
private fun TransformTabContent(
    layer: LayerItem,
    currentTimeMs: Long,
    defaultEasing: EasingType,
    onUpdateProperty: (AnimatableProperty, Float) -> Unit,
    onToggleKeyframe: (AnimatableProperty) -> Unit,
    onOpenEasingPicker: () -> Unit,
    onResetTransform: () -> Unit
) {
    val eval = KeyframeEngine.evaluateTransform(layer, currentTimeMs)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onOpenEasingPicker,
            border = BorderStroke(1.dp, StudioHairline),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("easing_curve_button")
        ) {
            Icon(Icons.Default.Timeline, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Easing: ${defaultEasing.label}", style = MaterialTheme.typography.labelSmall, color = InkPrimary)
        }

        TextButton(onClick = onResetTransform) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = InkSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Reset", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
        }
    }

    KeyframeSliderRow(
        label = "Position X",
        valueText = "${(eval.posX * 100f).roundToInt()}%",
        value = eval.posX,
        valueRange = -0.75f..0.75f,
        hasKeyframe = KeyframeEngine.hasKeyframeNear(layer, AnimatableProperty.POSITION_X, currentTimeMs) != null,
        hasAnyKeyframes = layer.keyframes.any { it.property == AnimatableProperty.POSITION_X },
        onValueChange = { onUpdateProperty(AnimatableProperty.POSITION_X, it) },
        onToggleKeyframe = { onToggleKeyframe(AnimatableProperty.POSITION_X) },
        tag = "prop_pos_x"
    )

    KeyframeSliderRow(
        label = "Position Y",
        valueText = "${(eval.posY * 100f).roundToInt()}%",
        value = eval.posY,
        valueRange = -0.75f..0.75f,
        hasKeyframe = KeyframeEngine.hasKeyframeNear(layer, AnimatableProperty.POSITION_Y, currentTimeMs) != null,
        hasAnyKeyframes = layer.keyframes.any { it.property == AnimatableProperty.POSITION_Y },
        onValueChange = { onUpdateProperty(AnimatableProperty.POSITION_Y, it) },
        onToggleKeyframe = { onToggleKeyframe(AnimatableProperty.POSITION_Y) },
        tag = "prop_pos_y"
    )

    KeyframeSliderRow(
        label = "Scale",
        valueText = "${(eval.scale * 100f).roundToInt()}%",
        value = eval.scale,
        valueRange = 0.1f..2.5f,
        hasKeyframe = KeyframeEngine.hasKeyframeNear(layer, AnimatableProperty.SCALE, currentTimeMs) != null,
        hasAnyKeyframes = layer.keyframes.any { it.property == AnimatableProperty.SCALE },
        onValueChange = { onUpdateProperty(AnimatableProperty.SCALE, it) },
        onToggleKeyframe = { onToggleKeyframe(AnimatableProperty.SCALE) },
        tag = "prop_scale"
    )

    KeyframeSliderRow(
        label = "Rotation",
        valueText = "${eval.rotation.roundToInt()}°",
        value = eval.rotation,
        valueRange = -360f..360f,
        hasKeyframe = KeyframeEngine.hasKeyframeNear(layer, AnimatableProperty.ROTATION, currentTimeMs) != null,
        hasAnyKeyframes = layer.keyframes.any { it.property == AnimatableProperty.ROTATION },
        onValueChange = { onUpdateProperty(AnimatableProperty.ROTATION, it) },
        onToggleKeyframe = { onToggleKeyframe(AnimatableProperty.ROTATION) },
        tag = "prop_rotation"
    )

    KeyframeSliderRow(
        label = "Opacity",
        valueText = "${(eval.opacity * 100f).roundToInt()}%",
        value = eval.opacity,
        valueRange = 0f..1f,
        hasKeyframe = KeyframeEngine.hasKeyframeNear(layer, AnimatableProperty.OPACITY, currentTimeMs) != null,
        hasAnyKeyframes = layer.keyframes.any { it.property == AnimatableProperty.OPACITY },
        onValueChange = { onUpdateProperty(AnimatableProperty.OPACITY, it) },
        onToggleKeyframe = { onToggleKeyframe(AnimatableProperty.OPACITY) },
        tag = "prop_opacity"
    )
}

@Composable
private fun KeyframeSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    hasKeyframe: Boolean,
    hasAnyKeyframes: Boolean,
    onValueChange: (Float) -> Unit,
    onToggleKeyframe: () -> Unit,
    tag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Diamond Keyframe Toggle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (hasKeyframe) KeyframeAmber.copy(alpha = 0.22f)
                            else StudioPanelElevated
                        )
                        .clickable(onClick = onToggleKeyframe)
                        .testTag("${tag}_keyframe_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        val r = size.minDimension / 2f
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val path = Path().apply {
                            moveTo(cx, cy - r)
                            lineTo(cx + r, cy)
                            lineTo(cx, cy + r)
                            lineTo(cx - r, cy)
                            close()
                        }
                        if (hasKeyframe) {
                            drawPath(path, color = KeyframeAmber)
                        } else {
                            drawPath(
                                path,
                                color = if (hasAnyKeyframes) CyberCyan else InkMuted,
                                style = Stroke(width = 1.6.dp.toPx())
                            )
                        }
                    }
                }
                Text(label, style = MaterialTheme.typography.bodyMedium, color = InkPrimary)
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                color = if (hasKeyframe) KeyframeAmber else CyberCyan
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ElectricViolet,
                activeTrackColor = ElectricViolet,
                inactiveTrackColor = StudioHairline
            ),
            modifier = Modifier
                .height(28.dp)
                .testTag("${tag}_slider")
        )
    }
}

@Composable
private fun EffectsTabContent(
    layer: LayerItem,
    onUpdateEffects: ((LayerEffects) -> LayerEffects) -> Unit,
    onApplyPreset: (VfxPreset) -> Unit
) {
    Text("VFX Look Presets", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VfxPreset.entries.forEach { preset ->
            FilterChip(
                selected = false,
                onClick = { onApplyPreset(preset) },
                label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = StudioPanelElevated,
                    labelColor = InkPrimary
                ),
                border = BorderStroke(1.dp, StudioHairline),
                modifier = Modifier.testTag("vfx_preset_${preset.name.lowercase()}")
            )
        }
    }

    EffectSlider("Gaussian Blur", "${layer.effects.blurRadius.roundToInt()} px", layer.effects.blurRadius, 0f..24f) { v ->
        onUpdateEffects { it.copy(blurRadius = v) }
    }
    EffectSlider("Neon Glow", "${(layer.effects.glowIntensity * 100).roundToInt()}%", layer.effects.glowIntensity, 0f..1f) { v ->
        onUpdateEffects { it.copy(glowIntensity = v) }
    }
    EffectSlider("Chromatic RGB Split", "${(layer.effects.chromaticShift * 100).roundToInt()}%", layer.effects.chromaticShift, 0f..1f) { v ->
        onUpdateEffects { it.copy(chromaticShift = v) }
    }
    EffectSlider("Vignette", "${(layer.effects.vignette * 100).roundToInt()}%", layer.effects.vignette, 0f..1f) { v ->
        onUpdateEffects { it.copy(vignette = v) }
    }
    EffectSlider("CRT Scanlines", "${(layer.effects.scanlines * 100).roundToInt()}%", layer.effects.scanlines, 0f..1f) { v ->
        onUpdateEffects { it.copy(scanlines = v) }
    }
    EffectSlider("Contrast", "${(layer.effects.contrast * 100).roundToInt()}%", layer.effects.contrast, 0.4f..2f) { v ->
        onUpdateEffects { it.copy(contrast = v) }
    }
    EffectSlider("Saturation", "${(layer.effects.saturation * 100).roundToInt()}%", layer.effects.saturation, 0f..2f) { v ->
        onUpdateEffects { it.copy(saturation = v) }
    }
    EffectSlider("Hue Shift", "${layer.effects.hueShift.roundToInt()}°", layer.effects.hueShift, -180f..180f) { v ->
        onUpdateEffects { it.copy(hueShift = v) }
    }
}

@Composable
private fun EffectSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = InkPrimary)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = CyberCyan)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = StudioHairline
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
private fun StyleTabContent(
    layer: LayerItem,
    onUpdateStyle: ((LayerItem) -> LayerItem) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onUpdateStyle {
                    it.copy(
                        imageUri = uri.toString(),
                        sampleAssetKey = null
                    )
                }
            }
        }
    )

    Text("Blend Mode", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BlendModeOption.entries.forEach { mode ->
            FilterChip(
                selected = layer.blendMode == mode,
                onClick = { onUpdateStyle { it.copy(blendMode = mode) } },
                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricViolet,
                    selectedLabelColor = InkPrimary,
                    containerColor = StudioPanelElevated
                )
            )
        }
    }

    when (layer.type) {
        LayerType.TEXT -> {
            OutlinedTextField(
                value = layer.textContent,
                onValueChange = { txt -> onUpdateStyle { it.copy(textContent = txt) } },
                label = { Text("Text Content") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_content_input")
            )
            EffectSlider("Font Size", "${layer.textSizeSp.roundToInt()} sp", layer.textSizeSp, 14f..72f) { v ->
                onUpdateStyle { it.copy(textSizeSp = v) }
            }
            EffectSlider("Letter Tracking", "${layer.textTracking.roundToInt()}", layer.textTracking, 0f..14f) { v ->
                onUpdateStyle { it.copy(textTracking = v) }
            }
            ColorSwatchRow("Text Fill Color", layer.fillColorHex) { hex ->
                onUpdateStyle { it.copy(fillColorHex = hex) }
            }
        }

        LayerType.SHAPE -> {
            Text("Geometry", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShapeKind.entries.forEach { kind ->
                    FilterChip(
                        selected = layer.shapeKind == kind,
                        onClick = { onUpdateStyle { it.copy(shapeKind = kind) } },
                        label = { Text(kind.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = StudioObsidian,
                            containerColor = StudioPanelElevated
                        )
                    )
                }
            }
            EffectSlider("Width", "${(layer.shapeWidthFraction * 100).roundToInt()}%", layer.shapeWidthFraction, 0.08f..0.95f) { v ->
                onUpdateStyle { it.copy(shapeWidthFraction = v) }
            }
            EffectSlider("Stroke Width", "${layer.strokeWidth.roundToInt()} dp", layer.strokeWidth, 0f..18f) { v ->
                onUpdateStyle { it.copy(strokeWidth = v) }
            }
            ColorSwatchRow("Fill Color", layer.fillColorHex) { hex ->
                onUpdateStyle { it.copy(fillColorHex = hex) }
            }
            ColorSwatchRow("Stroke Color", layer.strokeColorHex) { hex ->
                onUpdateStyle { it.copy(strokeColorHex = hex) }
            }
        }

        LayerType.IMAGE -> {
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_photo_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Replace from Device Photos")
            }
            Text("Studio Stock Plates", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    DefaultTemplates.ASSET_CYBER_CITY to "Tokyo Neon",
                    DefaultTemplates.ASSET_NEBULA to "Nebula",
                    DefaultTemplates.ASSET_PORTRAIT to "Visor"
                ).forEach { (key, title) ->
                    FilterChip(
                        selected = layer.sampleAssetKey == key,
                        onClick = {
                            onUpdateStyle { it.copy(sampleAssetKey = key, imageUri = null) }
                        },
                        label = { Text(title, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        LayerType.PARTICLE_FX -> {
            Text("FX Generator", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParticlePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = layer.particlePreset == preset,
                        onClick = { onUpdateStyle { it.copy(particlePreset = preset) } },
                        label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            EffectSlider("Density", "${(layer.particleDensity * 100).roundToInt()}%", layer.particleDensity, 0.2f..1f) { v ->
                onUpdateStyle { it.copy(particleDensity = v) }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    title: String,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StudioColorSwatches.forEach { hex ->
                val isSelected = selectedHex == hex
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) CyberCyan else StudioHairline,
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                )
            }
        }
    }
}

@Composable
private fun TimingTabContent(
    layer: LayerItem,
    durationMs: Long,
    onUpdateTiming: (Long, Long) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    EffectSlider(
        label = "Trim In Point",
        valueText = KeyframeEngine.formatSecondsCompact(layer.inTimeMs),
        value = layer.inTimeMs.toFloat(),
        range = 0f..(durationMs - 300L).coerceAtLeast(100L).toFloat()
    ) { v ->
        onUpdateTiming(v.toLong(), layer.outTimeMs)
    }

    EffectSlider(
        label = "Trim Out Point",
        valueText = KeyframeEngine.formatSecondsCompact(layer.outTimeMs),
        value = layer.outTimeMs.toFloat(),
        range = (layer.inTimeMs + 300L).coerceAtMost(durationMs).toFloat()..durationMs.toFloat()
    ) { v ->
        onUpdateTiming(layer.inTimeMs, v.toLong())
    }

    Text("Layer Stack & Actions", style = MaterialTheme.typography.labelLarge, color = InkPrimary)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onMoveUp,
            border = BorderStroke(1.dp, StudioHairline),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Forward", style = MaterialTheme.typography.labelSmall)
        }
        OutlinedButton(
            onClick = onMoveDown,
            border = BorderStroke(1.dp, StudioHairline),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Backward", style = MaterialTheme.typography.labelSmall)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onDuplicate,
            border = BorderStroke(1.dp, StudioHairline),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Duplicate", style = MaterialTheme.typography.labelSmall)
        }
        OutlinedButton(
            onClick = onDelete,
            border = BorderStroke(1.dp, RoseAlert.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAlert),
            modifier = Modifier.weight(1f).testTag("delete_layer_button")
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Delete", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EasingCurveDialog(
    currentEasing: EasingType,
    onSelect: (EasingType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanelDark,
        titleContentColor = InkPrimary,
        title = { Text("Keyframe Interpolation Curve", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EasingType.entries.forEach { easing ->
                    val isSelected = easing == currentEasing
                    Surface(
                        color = if (isSelected) StudioPanelElevated else StudioObsidian,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSelected) CyberCyan else StudioHairline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(easing) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(StudioPanelDark, RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                val path = Path()
                                val steps = 24
                                for (i in 0..steps) {
                                    val t = i / steps.toFloat()
                                    val yNorm = KeyframeEngine.applyEasing(t, easing)
                                    val px = t * size.width
                                    val py = size.height - (yNorm.coerceIn(-0.15f, 1.15f) * size.height)
                                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                }
                                drawPath(
                                    path = path,
                                    color = if (isSelected) CyberCyan else ElectricViolet,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column {
                                Text(
                                    text = easing.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) CyberCyan else InkPrimary
                                )
                                Text(
                                    text = when (easing) {
                                        EasingType.LINEAR -> "Constant mechanical velocity"
                                        EasingType.EASE_IN -> "Slow start, accelerates out"
                                        EasingType.EASE_OUT -> "Fast start, smooth deceleration"
                                        EasingType.EASE_IN_OUT -> "Smooth S-curve motion"
                                        EasingType.SPRING -> "Dynamic elastic overshoot"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = InkSecondary)
            }
        }
    )
}
