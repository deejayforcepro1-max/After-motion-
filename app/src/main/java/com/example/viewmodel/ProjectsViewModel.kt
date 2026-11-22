package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CompositionRepository
import com.example.data.DefaultTemplates
import com.example.model.AspectRatioPreset
import com.example.model.CompositionProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val repository: CompositionRepository
) : ViewModel() {

    val projects: StateFlow<List<CompositionProject>> = repository.compositions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            _isReady.value = true
        }
    }

    fun createNewComposition(
        name: String,
        aspectRatio: AspectRatioPreset,
        durationMs: Long,
        fps: Int,
        backgroundColorHex: Long,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val created = repository.createBlankProject(
                name = name,
                aspectRatio = aspectRatio,
                durationMs = durationMs,
                fps = fps,
                backgroundColorHex = backgroundColorHex
            )
            onCreated(created.id)
        }
    }

    fun createFromTemplate(templateIndex: Int, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val base = when (templateIndex) {
                0 -> DefaultTemplates.neonTitleSequence()
                1 -> DefaultTemplates.cyberpunkGlitchOpener()
                else -> DefaultTemplates.kineticShapePrism()
            }
            repository.saveProject(base)
            onCreated(base.id)
        }
    }

    fun duplicateComposition(project: CompositionProject) {
        viewModelScope.launch {
            repository.duplicateProject(project)
        }
    }

    fun deleteComposition(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    class Factory(private val repository: CompositionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectsViewModel(repository) as T
        }
    }
}
