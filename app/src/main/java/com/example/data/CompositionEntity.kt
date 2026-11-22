package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compositions")
data class CompositionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aspectRatioName: String,
    val durationMs: Long,
    val fps: Int,
    val layerCount: Int,
    val previewAssetKey: String?,
    val projectJson: String,
    val updatedAtMs: Long
)
