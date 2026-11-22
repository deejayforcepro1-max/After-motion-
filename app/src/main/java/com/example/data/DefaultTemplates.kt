package com.example.data

import com.example.model.AnimatableProperty
import com.example.model.AspectRatioPreset
import com.example.model.BlendModeOption
import com.example.model.CompositionProject
import com.example.model.EasingType
import com.example.model.Keyframe
import com.example.model.LayerEffects
import com.example.model.LayerItem
import com.example.model.LayerType
import com.example.model.ParticlePreset
import com.example.model.ShapeKind
import java.util.UUID

object DefaultTemplates {

    const val ASSET_CYBER_CITY = "cyber_city"
    const val ASSET_NEBULA = "nebula"
    const val ASSET_PORTRAIT = "portrait"

    fun createStarterProjects(): List<CompositionProject> {
        val now = System.currentTimeMillis()
        return listOf(
            neonTitleSequence(now),
            cyberpunkGlitchOpener(now - 60_000L),
            kineticShapePrism(now - 120_000L)
        )
    }

    fun neonTitleSequence(timestamp: Long = System.currentTimeMillis()): CompositionProject {
        return CompositionProject(
            id = "comp_neon_title_${UUID.randomUUID().toString().take(6)}",
            name = "Neon Title Sequence",
            aspectRatio = AspectRatioPreset.LANDSCAPE_16_9,
            durationMs = 5000L,
            fps = 30,
            backgroundColorHex = 0xFF090B12,
            updatedAtMs = timestamp,
            layers = listOf(
                LayerItem(
                    id = "layer_bg_city",
                    name = "Tokyo Neon Plate",
                    type = LayerType.IMAGE,
                    sampleAssetKey = ASSET_CYBER_CITY,
                    inTimeMs = 0L,
                    outTimeMs = 5000L,
                    baseScale = 1.08f,
                    effects = LayerEffects(
                        vignette = 0.55f,
                        contrast = 1.15f,
                        saturation = 1.1f,
                        blurRadius = 1.5f
                    ),
                    keyframes = listOf(
                        Keyframe("kf_1", AnimatableProperty.SCALE, 0L, 1.02f, EasingType.LINEAR),
                        Keyframe("kf_2", AnimatableProperty.SCALE, 5000L, 1.16f, EasingType.EASE_OUT)
                    )
                ),
                LayerItem(
                    id = "layer_nebula_fx",
                    name = "Plasma Nebula Overlay",
                    type = LayerType.IMAGE,
                    sampleAssetKey = ASSET_NEBULA,
                    blendMode = BlendModeOption.SCREEN,
                    inTimeMs = 0L,
                    outTimeMs = 5000L,
                    baseOpacity = 0.78f,
                    baseScale = 1.25f,
                    effects = LayerEffects(glowIntensity = 0.4f),
                    keyframes = listOf(
                        Keyframe("kf_3", AnimatableProperty.ROTATION, 0L, -8f, EasingType.LINEAR),
                        Keyframe("kf_4", AnimatableProperty.ROTATION, 5000L, 18f, EasingType.EASE_IN_OUT)
                    )
                ),
                LayerItem(
                    id = "layer_starfield",
                    name = "VFX Starfield Particles",
                    type = LayerType.PARTICLE_FX,
                    particlePreset = ParticlePreset.NEON_STARFIELD,
                    blendMode = BlendModeOption.ADD,
                    inTimeMs = 0L,
                    outTimeMs = 5000L,
                    baseOpacity = 0.85f
                ),
                LayerItem(
                    id = "layer_accent_ring",
                    name = "HUD Orbit Ring",
                    type = LayerType.SHAPE,
                    shapeKind = ShapeKind.RING,
                    shapeWidthFraction = 0.42f,
                    shapeHeightFraction = 0.42f,
                    fillColorHex = 0x00000000,
                    strokeColorHex = 0xFF22D3EE,
                    strokeWidth = 5f,
                    inTimeMs = 300L,
                    outTimeMs = 5000L,
                    effects = LayerEffects(glowIntensity = 0.75f, glowColorHex = 0xFF22D3EE),
                    keyframes = listOf(
                        Keyframe("kf_5", AnimatableProperty.SCALE, 300L, 0.35f, EasingType.EASE_OUT),
                        Keyframe("kf_6", AnimatableProperty.SCALE, 1400L, 1.05f, EasingType.SPRING),
                        Keyframe("kf_7", AnimatableProperty.ROTATION, 300L, 0f, EasingType.LINEAR),
                        Keyframe("kf_8", AnimatableProperty.ROTATION, 5000L, 120f, EasingType.LINEAR),
                        Keyframe("kf_9", AnimatableProperty.OPACITY, 300L, 0f, EasingType.LINEAR),
                        Keyframe("kf_10", AnimatableProperty.OPACITY, 900L, 0.9f, EasingType.EASE_OUT)
                    )
                ),
                LayerItem(
                    id = "layer_hero_title",
                    name = "Kinetic Title",
                    type = LayerType.TEXT,
                    textContent = "AFTER MOTION",
                    textSizeSp = 38f,
                    textTracking = 5f,
                    fillColorHex = 0xFFF8FAFC,
                    inTimeMs = 500L,
                    outTimeMs = 5000L,
                    effects = LayerEffects(
                        chromaticShift = 0.35f,
                        glowIntensity = 0.7f,
                        glowColorHex = 0xFF7C6BFF
                    ),
                    keyframes = listOf(
                        Keyframe("kf_11", AnimatableProperty.SCALE, 500L, 1.45f, EasingType.EASE_OUT),
                        Keyframe("kf_12", AnimatableProperty.SCALE, 1300L, 1.0f, EasingType.EASE_IN_OUT),
                        Keyframe("kf_13", AnimatableProperty.OPACITY, 500L, 0f, EasingType.LINEAR),
                        Keyframe("kf_14", AnimatableProperty.OPACITY, 1100L, 1f, EasingType.EASE_OUT)
                    )
                )
            )
        )
    }

    fun cyberpunkGlitchOpener(timestamp: Long = System.currentTimeMillis()): CompositionProject {
        return CompositionProject(
            id = "comp_cyber_reel_${UUID.randomUUID().toString().take(6)}",
            name = "Cyberpunk Visor Reel",
            aspectRatio = AspectRatioPreset.PORTRAIT_9_16,
            durationMs = 4500L,
            fps = 30,
            backgroundColorHex = 0xFF070910,
            updatedAtMs = timestamp,
            layers = listOf(
                LayerItem(
                    id = "layer_portrait",
                    name = "Visor Subject",
                    type = LayerType.IMAGE,
                    sampleAssetKey = ASSET_PORTRAIT,
                    inTimeMs = 0L,
                    outTimeMs = 4500L,
                    baseScale = 1.05f,
                    effects = LayerEffects(
                        chromaticShift = 0.45f,
                        contrast = 1.25f,
                        saturation = 1.3f,
                        scanlines = 0.25f,
                        vignette = 0.55f
                    ),
                    keyframes = listOf(
                        Keyframe("kf_c1", AnimatableProperty.POSITION_Y, 0L, 0.04f, EasingType.EASE_IN_OUT),
                        Keyframe("kf_c2", AnimatableProperty.POSITION_Y, 4500L, -0.04f, EasingType.EASE_IN_OUT)
                    )
                ),
                LayerItem(
                    id = "layer_sweep",
                    name = "Anamorphic Light Sweep",
                    type = LayerType.PARTICLE_FX,
                    particlePreset = ParticlePreset.LIGHT_SWEEP,
                    blendMode = BlendModeOption.SCREEN,
                    inTimeMs = 0L,
                    outTimeMs = 4500L,
                    baseOpacity = 0.7f
                ),
                LayerItem(
                    id = "layer_scan_bar",
                    name = "Lower Third Bar",
                    type = LayerType.SHAPE,
                    shapeKind = ShapeKind.BAR_ACCENT,
                    shapeWidthFraction = 0.76f,
                    shapeHeightFraction = 0.015f,
                    fillColorHex = 0xFF22D3EE,
                    basePosY = 0.28f,
                    inTimeMs = 400L,
                    outTimeMs = 4500L,
                    effects = LayerEffects(glowIntensity = 0.9f, glowColorHex = 0xFF22D3EE),
                    keyframes = listOf(
                        Keyframe("kf_c3", AnimatableProperty.POSITION_X, 400L, -0.6f, EasingType.EASE_OUT),
                        Keyframe("kf_c4", AnimatableProperty.POSITION_X, 1100L, 0f, EasingType.EASE_IN_OUT)
                    )
                ),
                LayerItem(
                    id = "layer_cyber_txt",
                    name = "VFX Subtitle",
                    type = LayerType.TEXT,
                    textContent = "SYNTHESIS // 09",
                    textSizeSp = 24f,
                    textTracking = 3.5f,
                    fillColorHex = 0xFF22D3EE,
                    basePosY = 0.22f,
                    inTimeMs = 600L,
                    outTimeMs = 4500L,
                    effects = LayerEffects(chromaticShift = 0.3f, glowIntensity = 0.5f, glowColorHex = 0xFF22D3EE),
                    keyframes = listOf(
                        Keyframe("kf_c5", AnimatableProperty.OPACITY, 600L, 0f, EasingType.LINEAR),
                        Keyframe("kf_c6", AnimatableProperty.OPACITY, 1200L, 1f, EasingType.EASE_OUT)
                    )
                )
            )
        )
    }

    fun kineticShapePrism(timestamp: Long = System.currentTimeMillis()): CompositionProject {
        return CompositionProject(
            id = "comp_prism_morph_${UUID.randomUUID().toString().take(6)}",
            name = "Kinetic Prism Morph",
            aspectRatio = AspectRatioPreset.SQUARE_1_1,
            durationMs = 4000L,
            fps = 60,
            backgroundColorHex = 0xFF0C0E18,
            updatedAtMs = timestamp,
            layers = listOf(
                LayerItem(
                    id = "layer_grid",
                    name = "Perspective Grid",
                    type = LayerType.PARTICLE_FX,
                    particlePreset = ParticlePreset.CYBER_GRID,
                    inTimeMs = 0L,
                    outTimeMs = 4000L,
                    baseOpacity = 0.55f
                ),
                LayerItem(
                    id = "layer_outer_prism",
                    name = "Violet Keyframe Prism",
                    type = LayerType.SHAPE,
                    shapeKind = ShapeKind.DIAMOND,
                    shapeWidthFraction = 0.48f,
                    shapeHeightFraction = 0.48f,
                    fillColorHex = 0x447C6BFF,
                    strokeColorHex = 0xFF7C6BFF,
                    strokeWidth = 6f,
                    inTimeMs = 0L,
                    outTimeMs = 4000L,
                    effects = LayerEffects(glowIntensity = 0.8f, glowColorHex = 0xFF7C6BFF),
                    keyframes = listOf(
                        Keyframe("kf_p1", AnimatableProperty.ROTATION, 0L, 0f, EasingType.LINEAR),
                        Keyframe("kf_p2", AnimatableProperty.ROTATION, 4000L, 180f, EasingType.EASE_IN_OUT),
                        Keyframe("kf_p3", AnimatableProperty.SCALE, 0L, 0.75f, EasingType.EASE_IN_OUT),
                        Keyframe("kf_p4", AnimatableProperty.SCALE, 2000L, 1.15f, EasingType.EASE_IN_OUT),
                        Keyframe("kf_p5", AnimatableProperty.SCALE, 4000L, 0.75f, EasingType.EASE_IN_OUT)
                    )
                ),
                LayerItem(
                    id = "layer_inner_star",
                    name = "Cyan Core Star",
                    type = LayerType.SHAPE,
                    shapeKind = ShapeKind.STAR,
                    shapeWidthFraction = 0.24f,
                    shapeHeightFraction = 0.24f,
                    fillColorHex = 0xFF22D3EE,
                    blendMode = BlendModeOption.SCREEN,
                    inTimeMs = 0L,
                    outTimeMs = 4000L,
                    effects = LayerEffects(glowIntensity = 0.9f, glowColorHex = 0xFF22D3EE),
                    keyframes = listOf(
                        Keyframe("kf_p6", AnimatableProperty.ROTATION, 0L, 0f, EasingType.LINEAR),
                        Keyframe("kf_p7", AnimatableProperty.ROTATION, 4000L, -360f, EasingType.LINEAR)
                    )
                ),
                LayerItem(
                    id = "layer_prism_label",
                    name = "Studio Badge",
                    type = LayerType.TEXT,
                    textContent = "60 FPS MOTION",
                    textSizeSp = 22f,
                    textTracking = 4f,
                    fillColorHex = 0xFFF8FAFC,
                    basePosY = 0.34f,
                    inTimeMs = 200L,
                    outTimeMs = 4000L
                )
            )
        )
    }
}
