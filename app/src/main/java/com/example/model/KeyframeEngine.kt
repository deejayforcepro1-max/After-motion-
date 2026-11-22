package com.example.model

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

data class EvaluatedTransform(
    val posX: Float,
    val posY: Float,
    val scale: Float,
    val rotation: Float,
    val opacity: Float
)

object KeyframeEngine {

    fun evaluateTransform(layer: LayerItem, timeMs: Long): EvaluatedTransform {
        return EvaluatedTransform(
            posX = evaluateProperty(layer, AnimatableProperty.POSITION_X, timeMs, layer.basePosX),
            posY = evaluateProperty(layer, AnimatableProperty.POSITION_Y, timeMs, layer.basePosY),
            scale = evaluateProperty(layer, AnimatableProperty.SCALE, timeMs, layer.baseScale).coerceAtLeast(0.05f),
            rotation = evaluateProperty(layer, AnimatableProperty.ROTATION, timeMs, layer.baseRotation),
            opacity = evaluateProperty(layer, AnimatableProperty.OPACITY, timeMs, layer.baseOpacity).coerceIn(0f, 1f)
        )
    }

    fun evaluateProperty(
        layer: LayerItem,
        property: AnimatableProperty,
        timeMs: Long,
        fallback: Float
    ): Float {
        val keys = layer.keyframes
            .filter { it.property == property }
            .sortedBy { it.timeMs }
        if (keys.isEmpty()) return fallback
        if (timeMs <= keys.first().timeMs) return keys.first().value
        if (timeMs >= keys.last().timeMs) return keys.last().value

        for (i in 0 until keys.size - 1) {
            val prev = keys[i]
            val next = keys[i + 1]
            if (timeMs in prev.timeMs..next.timeMs) {
                val span = (next.timeMs - prev.timeMs).coerceAtLeast(1L)
                val rawT = (timeMs - prev.timeMs).toFloat() / span.toFloat()
                val easedT = applyEasing(rawT, next.easing)
                return prev.value + (next.value - prev.value) * easedT
            }
        }
        return keys.last().value
    }

    fun applyEasing(t: Float, easing: EasingType): Float {
        val x = t.coerceIn(0f, 1f)
        return when (easing) {
            EasingType.LINEAR -> x
            EasingType.EASE_IN -> x * x * x
            EasingType.EASE_OUT -> 1f - (1f - x).pow(3)
            EasingType.EASE_IN_OUT -> {
                if (x < 0.5f) 4f * x * x * x
                else 1f - (-2f * x + 2f).pow(3) / 2f
            }
            EasingType.SPRING -> {
                if (x == 0f || x == 1f) x
                else {
                    val c4 = (2f * PI.toFloat()) / 3f
                    2f.pow(-10f * x) * sin((x * 10f - 0.75f) * c4) + 1f
                }
            }
        }
    }

    fun isLayerActiveAt(layer: LayerItem, timeMs: Long): Boolean {
        return layer.visible && timeMs in layer.inTimeMs..layer.outTimeMs
    }

    fun hasKeyframeNear(
        layer: LayerItem,
        property: AnimatableProperty,
        timeMs: Long,
        toleranceMs: Long = 45L
    ): Keyframe? {
        return layer.keyframes.firstOrNull {
            it.property == property && kotlin.math.abs(it.timeMs - timeMs) <= toleranceMs
        }
    }

    fun formatTimecode(timeMs: Long, fps: Int = 30): String {
        val clamped = timeMs.coerceAtLeast(0L)
        val totalSeconds = clamped / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val remainderMs = clamped % 1000L
        val frames = ((remainderMs * fps) / 1000L).coerceIn(0L, (fps - 1).toLong())
        return "%02d:%02d:%02d".format(minutes, seconds, frames)
    }

    fun formatSecondsCompact(timeMs: Long): String {
        val sec = timeMs / 1000f
        return "%.1fs".format(sec)
    }

    fun applyVfxPreset(preset: VfxPreset): LayerEffects {
        return when (preset) {
            VfxPreset.CLEAN -> LayerEffects()
            VfxPreset.CYBERPUNK_NEON -> LayerEffects(
                chromaticShift = 0.45f,
                glowIntensity = 0.65f,
                glowColorHex = 0xFF22D3EE,
                contrast = 1.28f,
                saturation = 1.45f,
                scanlines = 0.35f,
                vignette = 0.4f
            )
            VfxPreset.DREAMY_BLOOM -> LayerEffects(
                blurRadius = 2.5f,
                glowIntensity = 0.85f,
                glowColorHex = 0xFFA855F7,
                brightness = 0.08f,
                saturation = 1.2f,
                vignette = 0.25f
            )
            VfxPreset.VINTAGE_FILM -> LayerEffects(
                contrast = 1.12f,
                saturation = 0.72f,
                brightness = -0.04f,
                hueShift = 12f,
                vignette = 0.65f,
                scanlines = 0.12f
            )
            VfxPreset.NOIR_CONTRAST -> LayerEffects(
                contrast = 1.55f,
                saturation = 0.05f,
                chromaticShift = 0.2f,
                vignette = 0.75f
            )
        }
    }
}
