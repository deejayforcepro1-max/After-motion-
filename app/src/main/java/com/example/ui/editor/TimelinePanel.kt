package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CompositionProject
import com.example.model.KeyframeEngine
import com.example.model.LayerItem
import com.example.model.LayerType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.LayerColorFx
import com.example.ui.theme.LayerColorImage
import com.example.ui.theme.LayerColorShape
import com.example.ui.theme.LayerColorText
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioPanelDark
import com.example.ui.theme.StudioPanelElevated
import com.example.ui.theme.StudioPanelHover
import kotlin.math.abs

@Composable
fun TimelinePanel(
    project: CompositionProject,
    selectedLayerId: String?,
    currentTimeMs: Long,
    isPlaying: Boolean,
    isLooping: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onTogglePlayPause: () -> Unit,
    onJumpToStart: () -> Unit,
    onToggleLoop: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onScrubTo: (Long) -> Unit,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val headerWidth = 148.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioPanelDark)
            .border(BorderStroke(1.dp, StudioHairline))
    ) {
        // Transport Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioPanelElevated)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onJumpToStart,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("jump_start_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_jump_start),
                        tint = InkSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) CyberCyan else ElectricViolet)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = if (isPlaying) StudioObsidian else InkPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("loop_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Toggle loop",
                        tint = if (isLooping) CyberCyan else InkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // SMPTE Timecode Readout
            Surface(
                color = StudioObsidian,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, StudioHairline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = KeyframeEngine.formatTimecode(currentTimeMs, project.fps),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                    Text(
                        text = "/ ${KeyframeEngine.formatTimecode(project.durationMs, project.fps)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.cd_undo),
                        tint = if (canUndo) InkPrimary else InkMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = stringResource(R.string.cd_redo),
                        tint = if (canRedo) InkPrimary else InkMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = StudioHairline)

        // Ruler Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(StudioObsidian)
        ) {
            Box(
                modifier = Modifier
                    .width(headerWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "LAYERS (${project.layers.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(project.durationMs) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                            onScrubTo((frac * project.durationMs).toLong())
                        }
                    }
                    .pointerInput(project.durationMs) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                            onScrubTo((frac * project.durationMs).toLong())
                        }
                    }
                    .testTag("timeline_ruler")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dur = project.durationMs.coerceAtLeast(1000L)
                    val tickCount = (dur / 500L).toInt().coerceIn(4, 30)
                    for (i in 0..tickCount) {
                        val x = (i.toFloat() / tickCount.toFloat()) * size.width
                        val isMajor = i % 2 == 0
                        val h = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                        drawLine(
                            color = if (isMajor) InkSecondary else StudioHairline,
                            start = Offset(x, size.height - h),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Playhead cap
                    val playX = (currentTimeMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f) * size.width
                    val capPath = Path().apply {
                        moveTo(playX - 6.dp.toPx(), 2.dp.toPx())
                        lineTo(playX + 6.dp.toPx(), 2.dp.toPx())
                        lineTo(playX + 6.dp.toPx(), 10.dp.toPx())
                        lineTo(playX, 17.dp.toPx())
                        lineTo(playX - 6.dp.toPx(), 10.dp.toPx())
                        close()
                    }
                    drawPath(capPath, color = CyberCyan)
                    drawLine(
                        color = CyberCyan,
                        start = Offset(playX, 16.dp.toPx()),
                        end = Offset(playX, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        HorizontalDivider(color = StudioHairline)

        // Layer Tracks (top-rendered layer first, like After Effects)
        if (project.layers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_layers_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            val reversedLayers = project.layers.asReversed()
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(reversedLayers, key = { it.id }) { layer ->
                        LayerTimelineRow(
                            layer = layer,
                            durationMs = project.durationMs,
                            currentTimeMs = currentTimeMs,
                            isSelected = layer.id == selectedLayerId,
                            headerWidth = headerWidth,
                            onSelect = { onSelectLayer(layer.id) },
                            onToggleVisibility = { onToggleVisibility(layer.id) },
                            onToggleLock = { onToggleLock(layer.id) },
                            onJumpToKeyframe = { kfTime -> onScrubTo(kfTime) }
                        )
                        HorizontalDivider(color = StudioHairline.copy(alpha = 0.55f))
                    }
                }

                // Vertical Playhead Needle overlay across tracks
                Row(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.width(headerWidth))
                    Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val dur = project.durationMs.coerceAtLeast(1000L)
                        val playX = (currentTimeMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f) * size.width
                        drawLine(
                            color = CyberCyan.copy(alpha = 0.85f),
                            start = Offset(playX, 0f),
                            end = Offset(playX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerTimelineRow(
    layer: LayerItem,
    durationMs: Long,
    currentTimeMs: Long,
    isSelected: Boolean,
    headerWidth: androidx.compose.ui.unit.Dp,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onJumpToKeyframe: (Long) -> Unit
) {
    val trackColor = when (layer.type) {
        LayerType.TEXT -> LayerColorText
        LayerType.SHAPE -> LayerColorShape
        LayerType.IMAGE -> LayerColorImage
        LayerType.PARTICLE_FX -> LayerColorFx
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (isSelected) StudioPanelHover else StudioPanelDark)
            .clickable(onClick = onSelect)
            .testTag("layer_row_${layer.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Header
        Row(
            modifier = Modifier
                .width(headerWidth)
                .fillMaxHeight()
                .padding(start = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(trackColor)
            )
            Text(
                text = layer.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) InkPrimary else InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle visibility",
                    tint = if (layer.visible) InkSecondary else InkMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(
                onClick = onToggleLock,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (layer.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle lock",
                    tint = if (layer.locked) KeyframeAmber else InkMuted.copy(alpha = 0.55f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Right Timeline Span + Diamond Keyframes
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(StudioObsidian.copy(alpha = 0.35f))
                .pointerInput(layer.keyframes, durationMs) {
                    detectTapGestures { offset ->
                        val dur = durationMs.coerceAtLeast(1000L)
                        val tappedMs = ((offset.x / size.width.toFloat().coerceAtLeast(1f)) * dur).toLong()
                        val nearestKf = layer.keyframes.minByOrNull { abs(it.timeMs - tappedMs) }
                        if (nearestKf != null && abs(nearestKf.timeMs - tappedMs) <= (dur * 0.04f)) {
                            onJumpToKeyframe(nearestKf.timeMs)
                        } else {
                            onSelect()
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dur = durationMs.coerceAtLeast(1000L).toFloat()
                val startX = (layer.inTimeMs / dur).coerceIn(0f, 1f) * size.width
                val endX = (layer.outTimeMs / dur).coerceIn(0f, 1f) * size.width
                val barW = (endX - startX).coerceAtLeast(6.dp.toPx())
                val barTop = 10.dp.toPx()
                val barH = size.height - 20.dp.toPx()

                drawRoundRect(
                    color = trackColor.copy(alpha = if (isSelected) 0.42f else 0.25f),
                    topLeft = Offset(startX, barTop),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = trackColor.copy(alpha = if (isSelected) 0.9f else 0.55f),
                    topLeft = Offset(startX, barTop),
                    size = Size(barW, 3.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // Keyframe diamonds (deduplicated by timestamp cluster)
                val distinctTimes = layer.keyframes.map { it.timeMs }.distinct()
                val cy = size.height / 2f
                val r = 5.5.dp.toPx()
                distinctTimes.forEach { kfTime ->
                    val kx = (kfTime / dur).coerceIn(0f, 1f) * size.width
                    val isNearPlayhead = abs(kfTime - currentTimeMs) <= 45L
                    val diamondColor = if (isNearPlayhead) CyberCyan else KeyframeAmber
                    val path = Path().apply {
                        moveTo(kx, cy - r)
                        lineTo(kx + r, cy)
                        lineTo(kx, cy + r)
                        lineTo(kx - r, cy)
                        close()
                    }
                    drawPath(path, color = diamondColor)
                }
            }
        }
    }
}
