package com.example.data

import com.example.model.AspectRatioPreset
import com.example.model.CompositionProject
import com.example.model.LayerItem
import com.example.model.LayerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class CompositionRepository(
    private val dao: CompositionDao
) {
    val compositions: Flow<List<CompositionProject>> = dao.observeAllCompositions().map { list ->
        list.mapNotNull { entity ->
            CompositionSerializer.fromJson(entity.projectJson)
        }
    }

    suspend fun ensureSeeded() {
        if (dao.getCount() == 0) {
            DefaultTemplates.createStarterProjects().forEach { project ->
                saveProject(project)
            }
        }
    }

    suspend fun getProject(id: String): CompositionProject? {
        val entity = dao.getCompositionById(id) ?: return null
        return CompositionSerializer.fromJson(entity.projectJson)
    }

    suspend fun saveProject(project: CompositionProject) {
        val updated = project.copy(updatedAtMs = System.currentTimeMillis())
        val previewKey = updated.layers.firstOrNull { it.sampleAssetKey != null }?.sampleAssetKey
        val entity = CompositionEntity(
            id = updated.id,
            name = updated.name,
            aspectRatioName = updated.aspectRatio.name,
            durationMs = updated.durationMs,
            fps = updated.fps,
            layerCount = updated.layers.size,
            previewAssetKey = previewKey,
            projectJson = CompositionSerializer.toJson(updated),
            updatedAtMs = updated.updatedAtMs
        )
        dao.upsertComposition(entity)
    }

    suspend fun createBlankProject(
        name: String,
        aspectRatio: AspectRatioPreset,
        durationMs: Long,
        fps: Int,
        backgroundColorHex: Long
    ): CompositionProject {
        val id = "comp_${UUID.randomUUID().toString().take(8)}"
        val defaultTitle = LayerItem(
            id = "layer_${UUID.randomUUID().toString().take(6)}",
            name = "Title Text",
            type = LayerType.TEXT,
            textContent = name.uppercase(),
            textSizeSp = 32f,
            textTracking = 3f,
            inTimeMs = 0L,
            outTimeMs = durationMs
        )
        val project = CompositionProject(
            id = id,
            name = name.ifBlank { "Untitled Comp" },
            aspectRatio = aspectRatio,
            durationMs = durationMs,
            fps = fps,
            backgroundColorHex = backgroundColorHex,
            layers = listOf(defaultTitle),
            updatedAtMs = System.currentTimeMillis()
        )
        saveProject(project)
        return project
    }

    suspend fun duplicateProject(project: CompositionProject): CompositionProject {
        val copy = project.copy(
            id = "comp_${UUID.randomUUID().toString().take(8)}",
            name = "${project.name} Copy",
            updatedAtMs = System.currentTimeMillis()
        )
        saveProject(copy)
        return copy
    }

    suspend fun deleteProject(id: String) {
        dao.deleteById(id)
    }
}
