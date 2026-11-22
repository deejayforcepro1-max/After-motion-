package com.example

import com.example.data.CompositionSerializer
import com.example.data.DefaultTemplates
import com.example.model.AnimatableProperty
import com.example.model.EasingType
import com.example.model.Keyframe
import com.example.model.KeyframeEngine
import com.example.model.LayerItem
import com.example.model.LayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyframeEngineTest {

    @Test
    fun linearKeyframeInterpolation_evaluatesMidpointCorrectly() {
        val layer = LayerItem(
            id = "l1",
            name = "Test",
            type = LayerType.TEXT,
            keyframes = listOf(
                Keyframe("k1", AnimatableProperty.POSITION_X, 0L, -0.5f, EasingType.LINEAR),
                Keyframe("k2", AnimatableProperty.POSITION_X, 1000L, 0.5f, EasingType.LINEAR)
            )
        )
        assertEquals(-0.5f, KeyframeEngine.evaluateTransform(layer, 0L).posX, 0.001f)
        assertEquals(0.0f, KeyframeEngine.evaluateTransform(layer, 500L).posX, 0.001f)
        assertEquals(0.5f, KeyframeEngine.evaluateTransform(layer, 1000L).posX, 0.001f)
    }

    @Test
    fun easeInOutInterpolation_staysWithinBoundsAndSymmetric() {
        val layer = LayerItem(
            id = "l2",
            name = "ScaleTest",
            type = LayerType.SHAPE,
            keyframes = listOf(
                Keyframe("k1", AnimatableProperty.SCALE, 0L, 1.0f, EasingType.EASE_IN_OUT),
                Keyframe("k2", AnimatableProperty.SCALE, 1000L, 2.0f, EasingType.EASE_IN_OUT)
            )
        )
        assertEquals(1.5f, KeyframeEngine.evaluateTransform(layer, 500L).scale, 0.01f)
        val early = KeyframeEngine.evaluateTransform(layer, 200L).scale
        assertTrue("Ease-in-out should be slower than linear early", early < 1.2f)
    }

    @Test
    fun smpteTimecodeFormatting_formatsMinutesSecondsFrames() {
        assertEquals("00:00:00", KeyframeEngine.formatTimecode(0L, 30))
        assertEquals("00:02:15", KeyframeEngine.formatTimecode(2500L, 30))
        assertEquals("01:05:00", KeyframeEngine.formatTimecode(65000L, 30))
    }

    @Test
    fun compositionSerializer_roundTripsStarterTemplates() {
        val starters = DefaultTemplates.createStarterProjects()
        starters.forEach { original ->
            val json = CompositionSerializer.toJson(original)
            val decoded = CompositionSerializer.fromJson(json)
            assertNotNull(decoded)
            assertEquals(original.id, decoded?.id)
            assertEquals(original.layers.size, decoded?.layers?.size)
            assertEquals(original.name, decoded?.name)
        }
    }
}
