package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CompositionRepository
import com.example.engine.ExportEngine
import com.example.engine.ExportResult
import com.example.model.AnimatableProperty
import com.example.model.BlendModeOption
import com.example.model.CompositionProject
import com.example.model.EasingType
import com.example.model.Keyframe
import com.example.model.KeyframeEngine
import com.example.model.LayerEffects
import com.example.model.LayerItem
import com.example.model.LayerType
import com.example.model.ParticlePreset
import com.example.model.ShapeKind
import com.example.model.VfxPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class InspectorTab(val label: String) {
    TRANSFORM("Transform"),
    EFFECTS("Effects"),
    STYLE("Blend & Style"),
    TIMING("Timing")
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(val progress: Float, val message: String) : ExportUiState
    data class Success(val result: ExportResult) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

sealed interface EditorUiState {
    data object Loading : EditorUiState
    data object NotFound : EditorUiState
    data class Ready(
        val project: CompositionProject,
        val selectedLayerId: String?,
        val currentTimeMs: Long = 0L,
        val isPlaying: Boolean = false,
        val isLooping: Boolean = true,
        val showSafeGuides: Boolean = false,
        val activeTab: InspectorTab = InspectorTab.TRANSFORM,
        val defaultEasing: EasingType = EasingType.EASE_IN_OUT,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
        val exportState: ExportUiState = ExportUiState.Idle
    ) : EditorUiState {
        val selectedLayer: LayerItem?
            get() = project.layers.firstOrNull { it.id == selectedLayerId }
    }
}

class EditorViewModel(
    private val projectId: String,
    private val repository: CompositionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Loading)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<CompositionProject>()
    private val redoStack = ArrayDeque<CompositionProject>()

    private var playbackJob: Job? = null
    private var saveJob: Job? = null
    private var exportJob: Job? = null

    init {
        viewModelScope.launch {
            val loaded = repository.getProject(projectId)
            if (loaded == null) {
                _uiState.value = EditorUiState.NotFound
            } else {
                _uiState.value = EditorUiState.Ready(
                    project = loaded,
                    selectedLayerId = loaded.layers.lastOrNull()?.id
                )
            }
        }
    }

    private inline fun updateProject(
        recordUndo: Boolean = true,
        crossinline transform: (CompositionProject) -> CompositionProject
    ) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val updatedProject = transform(current.project).copy(updatedAtMs = System.currentTimeMillis())
        if (updatedProject == current.project) return

        if (recordUndo) {
            undoStack.addLast(current.project)
            if (undoStack.size > 30) undoStack.removeFirst()
            redoStack.clear()
        }

        val stillExists = updatedProject.layers.any { it.id == current.selectedLayerId }
        val nextSelectedId = if (stillExists) current.selectedLayerId else updatedProject.layers.lastOrNull()?.id

        _uiState.value = current.copy(
            project = updatedProject,
            selectedLayerId = nextSelectedId,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
        scheduleAutoSave(updatedProject)
    }

    private fun scheduleAutoSave(project: CompositionProject) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350L)
            repository.saveProject(project)
        }
    }

    fun undo() {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(current.project)
        val nextSelected = if (prev.layers.any { it.id == current.selectedLayerId }) {
            current.selectedLayerId
        } else {
            prev.layers.lastOrNull()?.id
        }
        _uiState.value = current.copy(
            project = prev,
            selectedLayerId = nextSelected,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
        scheduleAutoSave(prev)
    }

    fun redo() {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(current.project)
        val nextSelected = if (next.layers.any { it.id == current.selectedLayerId }) {
            current.selectedLayerId
        } else {
            next.layers.lastOrNull()?.id
        }
        _uiState.value = current.copy(
            project = next,
            selectedLayerId = nextSelected,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
        scheduleAutoSave(next)
    }

    fun togglePlayPause() {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        if (current.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        playbackJob?.cancel()
        val startFrom = if (current.currentTimeMs >= current.project.durationMs) 0L else current.currentTimeMs
        _uiState.value = current.copy(isPlaying = true, currentTimeMs = startFrom)

        playbackJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (isActive) {
                delay(16L)
                val nowNs = System.nanoTime()
                val deltaMs = ((nowNs - lastNs) / 1_000_000L).coerceIn(1L, 64L)
                lastNs = nowNs

                val state = _uiState.value as? EditorUiState.Ready ?: break
                if (!state.isPlaying) break
                val duration = state.project.durationMs.coerceAtLeast(1000L)
                val nextTime = state.currentTimeMs + deltaMs
                if (nextTime >= duration) {
                    if (state.isLooping) {
                        _uiState.value = state.copy(currentTimeMs = 0L)
                    } else {
                        _uiState.value = state.copy(currentTimeMs = duration, isPlaying = false)
                        break
                    }
                } else {
                    _uiState.value = state.copy(currentTimeMs = nextTime)
                }
            }
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(isPlaying = false) ?: state
        }
    }

    fun jumpToStart() {
        pausePlayback()
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(currentTimeMs = 0L) ?: state
        }
    }

    fun toggleLoop() {
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(isLooping = !state.isLooping) ?: state
        }
    }

    fun toggleSafeGuides() {
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(showSafeGuides = !state.showSafeGuides) ?: state
        }
    }

    fun scrubTo(timeMs: Long) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        if (current.isPlaying) pausePlayback()
        val clamped = timeMs.coerceIn(0L, current.project.durationMs)
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(currentTimeMs = clamped) ?: state
        }
    }

    fun selectTab(tab: InspectorTab) {
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(activeTab = tab) ?: state
        }
    }

    fun selectLayer(layerId: String) {
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(selectedLayerId = layerId) ?: state
        }
    }

    fun setDefaultEasing(easing: EasingType) {
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(defaultEasing = easing) ?: state
        }
        // Also update easing of any keyframe near current playhead on selected layer
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val selected = current.selectedLayer ?: return
        val hasNearKeys = selected.keyframes.any { kotlin.math.abs(it.timeMs - current.currentTimeMs) <= 45L }
        if (hasNearKeys) {
            updateProject { project ->
                project.copy(layers = project.layers.map { layer ->
                    if (layer.id != selected.id) layer
                    else layer.copy(keyframes = layer.keyframes.map { kf ->
                        if (kotlin.math.abs(kf.timeMs - current.currentTimeMs) <= 45L) kf.copy(easing = easing)
                        else kf
                    })
                })
            }
        }
    }

    fun addTextLayer(text: String = "KINETIC TEXT") {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val id = "layer_${UUID.randomUUID().toString().take(6)}"
        val newLayer = LayerItem(
            id = id,
            name = "Text • ${text.take(10)}",
            type = LayerType.TEXT,
            textContent = text,
            textSizeSp = 32f,
            textTracking = 3f,
            inTimeMs = 0L,
            outTimeMs = current.project.durationMs
        )
        addAndSelectLayer(newLayer)
    }

    fun addShapeLayer(kind: ShapeKind = ShapeKind.RING) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val id = "layer_${UUID.randomUUID().toString().take(6)}"
        val isRing = kind == ShapeKind.RING
        val newLayer = LayerItem(
            id = id,
            name = "Shape • ${kind.label}",
            type = LayerType.SHAPE,
            shapeKind = kind,
            shapeWidthFraction = if (kind == ShapeKind.BAR_ACCENT) 0.7f else 0.36f,
            shapeHeightFraction = if (kind == ShapeKind.BAR_ACCENT) 0.02f else 0.24f,
            fillColorHex = if (isRing) 0x00000000 else 0xFF7C6BFF,
            strokeColorHex = 0xFF22D3EE,
            strokeWidth = if (isRing) 5f else 0f,
            inTimeMs = 0L,
            outTimeMs = current.project.durationMs
        )
        addAndSelectLayer(newLayer)
    }

    fun addParticleLayer(preset: ParticlePreset = ParticlePreset.NEON_STARFIELD) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val id = "layer_${UUID.randomUUID().toString().take(6)}"
        val newLayer = LayerItem(
            id = id,
            name = "FX • ${preset.label}",
            type = LayerType.PARTICLE_FX,
            particlePreset = preset,
            blendMode = BlendModeOption.SCREEN,
            inTimeMs = 0L,
            outTimeMs = current.project.durationMs
        )
        addAndSelectLayer(newLayer)
    }

    fun addImageLayer(sampleAssetKey: String? = null, imageUri: String? = null, label: String = "Media Plate") {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val id = "layer_${UUID.randomUUID().toString().take(6)}"
        val newLayer = LayerItem(
            id = id,
            name = label,
            type = LayerType.IMAGE,
            sampleAssetKey = sampleAssetKey,
            imageUri = imageUri,
            inTimeMs = 0L,
            outTimeMs = current.project.durationMs
        )
        addAndSelectLayer(newLayer)
    }

    private fun addAndSelectLayer(layer: LayerItem) {
        updateProject { project ->
            project.copy(layers = project.layers + layer)
        }
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(selectedLayerId = layer.id) ?: state
        }
    }

    fun duplicateSelectedLayer() {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val selected = current.selectedLayer ?: return
        val copy = selected.copy(
            id = "layer_${UUID.randomUUID().toString().take(6)}",
            name = "${selected.name} Copy",
            basePosX = (selected.basePosX + 0.04f).coerceIn(-0.6f, 0.6f),
            basePosY = (selected.basePosY + 0.04f).coerceIn(-0.6f, 0.6f)
        )
        addAndSelectLayer(copy)
    }

    fun deleteLayer(layerId: String) {
        updateProject { project ->
            project.copy(layers = project.layers.filterNot { it.id == layerId })
        }
    }

    fun moveLayerOrder(layerId: String, moveUpInStack: Boolean) {
        updateProject { project ->
            val list = project.layers.toMutableList()
            val idx = list.indexOfFirst { it.id == layerId }
            if (idx == -1) return@updateProject project
            val targetIdx = if (moveUpInStack) idx + 1 else idx - 1
            if (targetIdx !in list.indices) return@updateProject project
            val item = list.removeAt(idx)
            list.add(targetIdx, item)
            project.copy(layers = list)
        }
    }

    fun toggleLayerVisibility(layerId: String) {
        updateProject { project ->
            project.copy(layers = project.layers.map {
                if (it.id == layerId) it.copy(visible = !it.visible) else it
            })
        }
    }

    fun toggleLayerLock(layerId: String) {
        updateProject { project ->
            project.copy(layers = project.layers.map {
                if (it.id == layerId) it.copy(locked = !it.locked) else it
            })
        }
    }

    fun updatePropertyValue(property: AnimatableProperty, newValue: Float) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val selected = current.selectedLayer ?: return
        if (selected.locked) return

        val timeMs = current.currentTimeMs
        val hasKeysForProp = selected.keyframes.any { it.property == property }

        updateProject { project ->
            project.copy(layers = project.layers.map { layer ->
                if (layer.id != selected.id) return@map layer
                if (hasKeysForProp) {
                    val existing = KeyframeEngine.hasKeyframeNear(layer, property, timeMs)
                    val updatedKeys = if (existing != null) {
                        layer.keyframes.map { if (it.id == existing.id) it.copy(value = newValue) else it }
                    } else {
                        layer.keyframes + Keyframe(
                            id = "kf_${UUID.randomUUID().toString().take(6)}",
                            property = property,
                            timeMs = timeMs,
                            value = newValue,
                            easing = current.defaultEasing
                        )
                    }
                    layer.copy(keyframes = updatedKeys.sortedBy { it.timeMs })
                } else {
                    when (property) {
                        AnimatableProperty.POSITION_X -> layer.copy(basePosX = newValue)
                        AnimatableProperty.POSITION_Y -> layer.copy(basePosY = newValue)
                        AnimatableProperty.SCALE -> layer.copy(baseScale = newValue)
                        AnimatableProperty.ROTATION -> layer.copy(baseRotation = newValue)
                        AnimatableProperty.OPACITY -> layer.copy(baseOpacity = newValue)
                    }
                }
            })
        }
    }

    fun dragSelectedLayerBy(deltaXFraction: Float, deltaYFraction: Float) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val selected = current.selectedLayer ?: return
        if (selected.locked) return
        val eval = KeyframeEngine.evaluateTransform(selected, current.currentTimeMs)
        val nextX = (eval.posX + deltaXFraction).coerceIn(-0.85f, 0.85f)
        val nextY = (eval.posY + deltaYFraction).coerceIn(-0.85f, 0.85f)
        updatePropertyValue(AnimatableProperty.POSITION_X, nextX)
        updatePropertyValue(AnimatableProperty.POSITION_Y, nextY)
    }

    fun toggleKeyframe(property: AnimatableProperty) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val selected = current.selectedLayer ?: return
        val timeMs = current.currentTimeMs
        val existing = KeyframeEngine.hasKeyframeNear(selected, property, timeMs)

        updateProject { project ->
            project.copy(layers = project.layers.map { layer ->
                if (layer.id != selected.id) return@map layer
                if (existing != null) {
                    layer.copy(keyframes = layer.keyframes.filterNot { it.id == existing.id })
                } else {
                    val eval = KeyframeEngine.evaluateTransform(layer, timeMs)
                    val currentVal = when (property) {
                        AnimatableProperty.POSITION_X -> eval.posX
                        AnimatableProperty.POSITION_Y -> eval.posY
                        AnimatableProperty.SCALE -> eval.scale
                        AnimatableProperty.ROTATION -> eval.rotation
                        AnimatableProperty.OPACITY -> eval.opacity
                    }
                    val newKey = Keyframe(
                        id = "kf_${UUID.randomUUID().toString().take(6)}",
                        property = property,
                        timeMs = timeMs,
                        value = currentVal,
                        easing = current.defaultEasing
                    )
                    layer.copy(keyframes = (layer.keyframes + newKey).sortedBy { it.timeMs })
                }
            })
        }
    }

    fun resetSelectedTransform() {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val selected = current.selectedLayer ?: return
        updateProject { project ->
            project.copy(layers = project.layers.map { layer ->
                if (layer.id != selected.id) layer
                else layer.copy(
                    basePosX = 0f,
                    basePosY = 0f,
                    baseScale = 1f,
                    baseRotation = 0f,
                    baseOpacity = 1f,
                    keyframes = emptyList()
                )
            })
        }
    }

    fun updateSelectedEffects(transform: (LayerEffects) -> LayerEffects) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val selected = current.selectedLayer ?: return
        updateProject { project ->
            project.copy(layers = project.layers.map { layer ->
                if (layer.id == selected.id) layer.copy(effects = transform(layer.effects)) else layer
            })
        }
    }

    fun applyVfxPreset(preset: VfxPreset) {
        updateSelectedEffects { KeyframeEngine.applyVfxPreset(preset) }
    }

    fun updateSelectedStyle(transform: (LayerItem) -> LayerItem) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val selected = current.selectedLayer ?: return
        updateProject { project ->
            project.copy(layers = project.layers.map { layer ->
                if (layer.id == selected.id) transform(layer) else layer
            })
        }
    }

    fun updateSelectedTiming(inTimeMs: Long, outTimeMs: Long) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        val selected = current.selectedLayer ?: return
        val maxDur = current.project.durationMs
        val clampedIn = inTimeMs.coerceIn(0L, maxDur - 200L)
        val clampedOut = outTimeMs.coerceIn(clampedIn + 200L, maxDur)
        updateProject { project ->
            project.copy(layers = project.layers.map { layer ->
                if (layer.id == selected.id) layer.copy(inTimeMs = clampedIn, outTimeMs = clampedOut) else layer
            })
        }
    }

    fun updateCompositionBackground(colorHex: Long) {
        updateProject { project ->
            project.copy(backgroundColorHex = colorHex)
        }
    }

    fun renameComposition(newName: String) {
        if (newName.isBlank()) return
        updateProject { project ->
            project.copy(name = newName.trim())
        }
    }

    fun exportMp4(context: Context) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        pausePlayback()
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _uiState.update { state ->
                (state as? EditorUiState.Ready)?.copy(
                    exportState = ExportUiState.Exporting(0f, "Rendering H.264 MP4 frames...")
                ) ?: state
            }
            val result = ExportEngine.exportVideoMp4(context, current.project) { progress ->
                _uiState.update { state ->
                    (state as? EditorUiState.Ready)?.copy(
                        exportState = ExportUiState.Exporting(
                            progress,
                            "Encoding video ${(progress * 100).toInt()}%"
                        )
                    ) ?: state
                }
            }
            _uiState.update { state ->
                val ready = state as? EditorUiState.Ready ?: return@update state
                result.fold(
                    onSuccess = { ready.copy(exportState = ExportUiState.Success(it)) },
                    onFailure = { err ->
                        ready.copy(
                            exportState = ExportUiState.Error(
                                err.localizedMessage ?: "Video encoder unavailable on this device."
                            )
                        )
                    }
                )
            }
        }
    }

    fun exportPngFrame(context: Context) {
        val current = _uiState.value as? EditorUiState.Ready ?: return
        pausePlayback()
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _uiState.update { state ->
                (state as? EditorUiState.Ready)?.copy(
                    exportState = ExportUiState.Exporting(0.5f, "Rendering full-res PNG frame...")
                ) ?: state
            }
            val result = ExportEngine.exportStillPng(context, current.project, current.currentTimeMs)
            _uiState.update { state ->
                val ready = state as? EditorUiState.Ready ?: return@update state
                result.fold(
                    onSuccess = { ready.copy(exportState = ExportUiState.Success(it)) },
                    onFailure = { err ->
                        ready.copy(
                            exportState = ExportUiState.Error(
                                err.localizedMessage ?: "Failed to export frame."
                            )
                        )
                    }
                )
            }
        }
    }

    fun dismissExportDialog() {
        exportJob?.cancel()
        _uiState.update { state ->
            (state as? EditorUiState.Ready)?.copy(exportState = ExportUiState.Idle) ?: state
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
    }

    class Factory(
        private val projectId: String,
        private val repository: CompositionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(projectId, repository) as T
        }
    }
}
