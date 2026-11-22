package com.example.model

import androidx.compose.ui.graphics.BlendMode
import com.squareup.moshi.JsonClass

enum class AspectRatioPreset(
    val label: String,
    val widthPx: Int,
    val heightPx: Int
) {
    LANDSCAPE_16_9("16:9 HD", 1280, 720),
    PORTRAIT_9_16("9:16 Reel", 720, 1280),
    SQUARE_1_1("1:1 Square", 1080, 1080),
    PORTRAIT_4_5("4:5 Social", 864, 1080);

    val ratio: Float get() = widthPx.toFloat() / heightPx.toFloat()
}

enum class LayerType(val label: String) {
    TEXT("Text"),
    SHAPE("Shape"),
    IMAGE("Media"),
    PARTICLE_FX("VFX")
}

enum class ShapeKind(val label: String) {
    RECTANGLE("Rect"),
    CIRCLE("Circle"),
    RING("Ring"),
    STAR("Star"),
    DIAMOND("Prism"),
    BAR_ACCENT("Scan Bar")
}

enum class ParticlePreset(val label: String) {
    NEON_STARFIELD("Starfield"),
    LIGHT_SWEEP("Light Sweep"),
    CYBER_GRID("Cyber Grid"),
    AURORA_ORBS("Aurora Orbs")
}

enum class BlendModeOption(val label: String, val composeMode: BlendMode) {
    NORMAL("Normal", BlendMode.SrcOver),
    SCREEN("Screen", BlendMode.Screen),
    MULTIPLY("Multiply", BlendMode.Multiply),
    OVERLAY("Overlay", BlendMode.Overlay),
    ADD("Add", BlendMode.Plus),
    LIGHTEN("Lighten", BlendMode.Lighten),
    DARKEN("Darken", BlendMode.Darken)
}

enum class AnimatableProperty(val label: String, val shortLabel: String) {
    POSITION_X("Position X", "X"),
    POSITION_Y("Position Y", "Y"),
    SCALE("Scale", "Scl"),
    ROTATION("Rotation", "Rot"),
    OPACITY("Opacity", "Op")
}

enum class EasingType(val label: String) {
    LINEAR("Linear"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out"),
    EASE_IN_OUT("Ease In/Out"),
    SPRING("Overshoot")
}

enum class VfxPreset(val label: String) {
    CLEAN("Reset"),
    CYBERPUNK_NEON("Cyber Neon"),
    DREAMY_BLOOM("Dream Bloom"),
    VINTAGE_FILM("Film Matte"),
    NOIR_CONTRAST("Noir Split")
}

@JsonClass(generateAdapter = true)
data class Keyframe(
    val id: String,
    val property: AnimatableProperty,
    val timeMs: Long,
    val value: Float,
    val easing: EasingType = EasingType.EASE_IN_OUT
)

@JsonClass(generateAdapter = true)
data class LayerEffects(
    val blurRadius: Float = 0f,
    val glowIntensity: Float = 0f,
    val glowColorHex: Long = 0xFF7C6BFFFF,
    val chromaticShift: Float = 0f,
    val vignette: Float = 0f,
    val scanlines: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val hueShift: Float = 0f
)

@JsonClass(generateAdapter = true)
data class LayerItem(
    val id: String,
    val name: String,
    val type: LayerType,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val inTimeMs: Long = 0L,
    val outTimeMs: Long = 6000L,
    // Normalized position relative to canvas center: -0.5..0.5 means left..right edge
    val basePosX: Float = 0f,
    val basePosY: Float = 0f,
    val baseScale: Float = 1f,
    val baseRotation: Float = 0f,
    val baseOpacity: Float = 1f,
    val blendMode: BlendModeOption = BlendModeOption.NORMAL,
    // Text properties
    val textContent: String = "AFTER MOTION",
    val textSizeSp: Float = 36f,
    val textTracking: Float = 2f,
    val fillColorHex: Long = 0xFFF3F4F8,
    val strokeColorHex: Long = 0xFF22D3EE,
    val strokeWidth: Float = 0f,
    // Shape properties
    val shapeKind: ShapeKind = ShapeKind.RECTANGLE,
    val shapeWidthFraction: Float = 0.42f,
    val shapeHeightFraction: Float = 0.24f,
    val cornerRadiusDp: Float = 16f,
    // Image properties
    val imageUri: String? = null,
    val sampleAssetKey: String? = null,
    // Particle VFX properties
    val particlePreset: ParticlePreset = ParticlePreset.NEON_STARFIELD,
    val particleDensity: Float = 0.65f,
    // Effects & Keyframes
    val effects: LayerEffects = LayerEffects(),
    val keyframes: List<Keyframe> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CompositionProject(
    val id: String,
    val name: String,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.LANDSCAPE_16_9,
    val durationMs: Long = 6000L,
    val fps: Int = 30,
    val backgroundColorHex: Long = 0xFF0A0C12,
    val layers: List<LayerItem> = emptyList(),
    val updatedAtMs: Long = System.currentTimeMillis()
)
