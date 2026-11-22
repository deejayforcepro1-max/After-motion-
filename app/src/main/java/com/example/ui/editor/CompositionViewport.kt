package com.example.ui.editor

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.AssetHelper
import com.example.model.CompositionProject
import com.example.model.EvaluatedTransform
import com.example.model.KeyframeEngine
import com.example.model.LayerEffects
import com.example.model.LayerItem
import com.example.model.LayerType
import com.example.model.ParticlePreset
import com.example.model.ShapeKind
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioObsidian
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CompositionViewport(
    project: CompositionProject,
    currentTimeMs: Long,
    selectedLayerId: String?,
    isPlaying: Boolean,
    showSafeGuides: Boolean,
    onDragSelectedLayer: (dxFraction: Float, dyFraction: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioObsidian)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val ratio = project.aspectRatio.ratio
        val maxW = maxWidth
        val maxH = maxHeight
        val viewportModifier = if (maxW / maxH > ratio) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(ratio)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
        }

        val density = LocalDensity.current

        Box(
            modifier = viewportModifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(project.backgroundColorHex))
                .border(1.dp, StudioHairline, RoundedCornerShape(10.dp))
                .clipToBounds()
                .pointerInput(selectedLayerId, isPlaying) {
                    if (selectedLayerId != null && !isPlaying) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            onDragSelectedLayer(dragAmount.x / w, dragAmount.y / h)
                        }
                    }
                }
                .testTag("composition_viewport")
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasWidthPx = with(density) { maxWidth.toPx() }
                val canvasHeightPx = with(density) { maxHeight.toPx() }
                val baseScaleFactor = (min(canvasWidthPx, canvasHeightPx) / 360f).coerceIn(0.45f, 2.5f)

                project.layers.forEach { layer ->
                    if (!KeyframeEngine.isLayerActiveAt(layer, currentTimeMs)) return@forEach
                    val transform = KeyframeEngine.evaluateTransform(layer, currentTimeMs)
                    if (transform.opacity <= 0.01f) return@forEach

                    val isSelected = layer.id == selectedLayerId && !isPlaying

                    ViewportLayerRenderer(
                        layer = layer,
                        transform = transform,
                        timeMs = currentTimeMs,
                        canvasWidthPx = canvasWidthPx,
                        canvasHeightPx = canvasHeightPx,
                        baseScaleFactor = baseScaleFactor,
                        isSelected = isSelected
                    )
                }

                if (showSafeGuides) {
                    SafeGuidesOverlay()
                }
            }
        }
    }
}

@Composable
private fun ViewportLayerRenderer(
    layer: LayerItem,
    transform: EvaluatedTransform,
    timeMs: Long,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    baseScaleFactor: Float,
    isSelected: Boolean
) {
    val colorMatrix = remember(layer.effects) {
        buildComposeColorMatrix(layer.effects)
    }
    val colorFilter = remember(colorMatrix) { ColorFilter.colorMatrix(colorMatrix) }
    val blurModifier = if (layer.effects.blurRadius > 0.2f) {
        Modifier.blur(layer.effects.blurRadius.dp)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = transform.posX * canvasWidthPx
                translationY = transform.posY * canvasHeightPx
                scaleX = transform.scale
                scaleY = transform.scale
                rotationZ = transform.rotation
                alpha = transform.opacity
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .then(blurModifier),
        contentAlignment = Alignment.Center
    ) {
        when (layer.type) {
            LayerType.IMAGE -> {
                val sampleRes = AssetHelper.drawableForSampleKey(layer.sampleAssetKey)
                val imgModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }

                if (layer.effects.chromaticShift > 0.05f) {
                    val shiftPx = (layer.effects.chromaticShift * 10f).roundToInt()
                    RenderImageSource(
                        sampleRes = sampleRes,
                        imageUri = layer.imageUri,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0x8822D3EE), BlendMode.Modulate),
                        modifier = imgModifier.offset { IntOffset(-shiftPx, 0) }
                    )
                    RenderImageSource(
                        sampleRes = sampleRes,
                        imageUri = layer.imageUri,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0x88F43F5E), BlendMode.Modulate),
                        modifier = imgModifier.offset { IntOffset(shiftPx, 0) }
                    )
                }

                RenderImageSource(
                    sampleRes = sampleRes,
                    imageUri = layer.imageUri,
                    contentDescription = layer.name,
                    colorFilter = colorFilter,
                    modifier = imgModifier
                )

                // Blend overlay emulation when non-normal
                if (layer.blendMode.composeMode != BlendMode.SrcOver) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = Color.Transparent,
                            blendMode = layer.blendMode.composeMode
                        )
                    }
                }
            }

            LayerType.SHAPE -> {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val baseDim = min(size.width, size.height)
                    val w = size.width * layer.shapeWidthFraction
                    val h = if (layer.shapeKind == ShapeKind.RECTANGLE || layer.shapeKind == ShapeKind.BAR_ACCENT) {
                        size.height * layer.shapeHeightFraction
                    } else {
                        baseDim * layer.shapeWidthFraction
                    }
                    val effectiveW = if (layer.shapeKind == ShapeKind.RECTANGLE || layer.shapeKind == ShapeKind.BAR_ACCENT) w else h
                    val topLeft = Offset((size.width - effectiveW) / 2f, (size.height - h) / 2f)
                    val rectSize = Size(effectiveW, h)
                    val fillColor = Color(layer.fillColorHex)
                    val strokeColor = Color(layer.strokeColorHex)
                    val blend = layer.blendMode.composeMode

                    if (layer.effects.glowIntensity > 0.05f) {
                        val glowCol = Color(layer.effects.glowColorHex).copy(
                            alpha = (layer.effects.glowIntensity * 0.45f).coerceIn(0f, 0.7f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowCol, Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = maxOf(effectiveW, h) * 0.9f
                            ),
                            radius = maxOf(effectiveW, h) * 0.9f,
                            center = Offset(size.width / 2f, size.height / 2f),
                            blendMode = BlendMode.Screen
                        )
                    }

                    if (fillColor.alpha > 0f) {
                        when (layer.shapeKind) {
                            ShapeKind.RECTANGLE, ShapeKind.BAR_ACCENT -> {
                                val r = layer.cornerRadiusDp.dp.toPx()
                                drawRoundRect(
                                    color = fillColor,
                                    topLeft = topLeft,
                                    size = rectSize,
                                    cornerRadius = CornerRadius(r, r),
                                    blendMode = blend
                                )
                            }
                            ShapeKind.CIRCLE -> drawOval(
                                color = fillColor,
                                topLeft = topLeft,
                                size = rectSize,
                                blendMode = blend
                            )
                            ShapeKind.RING -> Unit
                            ShapeKind.DIAMOND -> drawPath(
                                path = diamondPath(size.width / 2f, size.height / 2f, effectiveW / 2f, h / 2f),
                                color = fillColor,
                                blendMode = blend
                            )
                            ShapeKind.STAR -> drawPath(
                                path = starPath(size.width / 2f, size.height / 2f, effectiveW / 2f),
                                color = fillColor,
                                blendMode = blend
                            )
                        }
                    }

                    if (layer.strokeWidth > 0f || layer.shapeKind == ShapeKind.RING) {
                        val sw = (layer.strokeWidth.coerceAtLeast(2f) * baseScaleFactor).dp.toPx() * 0.55f
                        val strokeStyle = Stroke(width = sw, cap = StrokeCap.Round)
                        when (layer.shapeKind) {
                            ShapeKind.RECTANGLE, ShapeKind.BAR_ACCENT -> {
                                val r = layer.cornerRadiusDp.dp.toPx()
                                drawRoundRect(
                                    color = strokeColor,
                                    topLeft = topLeft,
                                    size = rectSize,
                                    cornerRadius = CornerRadius(r, r),
                                    style = strokeStyle,
                                    blendMode = blend
                                )
                            }
                            ShapeKind.CIRCLE, ShapeKind.RING -> drawOval(
                                color = strokeColor,
                                topLeft = topLeft,
                                size = rectSize,
                                style = strokeStyle,
                                blendMode = blend
                            )
                            ShapeKind.DIAMOND -> drawPath(
                                path = diamondPath(size.width / 2f, size.height / 2f, effectiveW / 2f, h / 2f),
                                color = strokeColor,
                                style = strokeStyle,
                                blendMode = blend
                            )
                            ShapeKind.STAR -> drawPath(
                                path = starPath(size.width / 2f, size.height / 2f, effectiveW / 2f),
                                color = strokeColor,
                                style = strokeStyle,
                                blendMode = blend
                            )
                        }
                    }
                }
            }

            LayerType.TEXT -> {
                val scaledSp = (layer.textSizeSp * baseScaleFactor * 0.85f).sp
                val glowShadow = if (layer.effects.glowIntensity > 0.05f) {
                    Shadow(
                        color = Color(layer.effects.glowColorHex).copy(
                            alpha = (layer.effects.glowIntensity * 0.9f).coerceIn(0f, 1f)
                        ),
                        offset = Offset.Zero,
                        blurRadius = layer.effects.glowIntensity * 28f
                    )
                } else null

                Box(contentAlignment = Alignment.Center) {
                    if (layer.effects.chromaticShift > 0.05f) {
                        val shiftPx = (layer.effects.chromaticShift * 8f).roundToInt()
                        Text(
                            text = layer.textContent,
                            style = TextStyle(
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = scaledSp,
                                letterSpacing = layer.textTracking.sp,
                                color = Color(0xAA22D3EE)
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset { IntOffset(-shiftPx, 0) }
                        )
                        Text(
                            text = layer.textContent,
                            style = TextStyle(
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = scaledSp,
                                letterSpacing = layer.textTracking.sp,
                                color = Color(0xAAF43F5E)
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset { IntOffset(shiftPx, 0) }
                        )
                    }
                    Text(
                        text = layer.textContent,
                        style = TextStyle(
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = scaledSp,
                            letterSpacing = layer.textTracking.sp,
                            color = Color(layer.fillColorHex),
                            shadow = glowShadow
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            LayerType.PARTICLE_FX -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tSec = timeMs / 1000f
                    val blend = layer.blendMode.composeMode
                    when (layer.particlePreset) {
                        ParticlePreset.NEON_STARFIELD -> {
                            val count = (28 + (layer.particleDensity * 52)).toInt()
                            for (i in 0 until count) {
                                val seedA = sin(i * 12.9898f) * 43758.5453f
                                val seedB = sin(i * 78.233f) * 19341.12f
                                val fx = seedA - floor(seedA)
                                val fy = ((seedB - floor(seedB)) - tSec * (0.08f + (i % 5) * 0.03f)) % 1f
                                val wrappedY = if (fy < 0f) fy + 1f else fy
                                val radius = (1.8f + (i % 4) * 1.1f) * baseScaleFactor
                                val col = if (i % 2 == 0) CyberCyan else ElectricViolet
                                drawCircle(
                                    color = col.copy(alpha = 0.45f + (i % 3) * 0.22f),
                                    radius = radius,
                                    center = Offset(fx * size.width, wrappedY * size.height),
                                    blendMode = blend
                                )
                            }
                        }
                        ParticlePreset.LIGHT_SWEEP -> {
                            val sweepPos = ((tSec * 0.45f) % 1.6f) - 0.3f
                            val cx = sweepPos * size.width
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        CyberCyan.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.65f),
                                        ElectricViolet.copy(alpha = 0.4f),
                                        Color.Transparent
                                    ),
                                    start = Offset(cx - size.width * 0.22f, 0f),
                                    end = Offset(cx + size.width * 0.22f, size.height)
                                ),
                                blendMode = blend
                            )
                        }
                        ParticlePreset.CYBER_GRID -> {
                            val step = min(size.width, size.height) / 10f
                            val offset = (tSec * 28f * baseScaleFactor) % step
                            val gridCol = ElectricViolet.copy(alpha = 0.42f)
                            var x = 0f
                            while (x <= size.width) {
                                drawLine(gridCol, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.2f)
                                x += step
                            }
                            var y = offset
                            while (y <= size.height) {
                                drawLine(gridCol, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.2f)
                                y += step
                            }
                        }
                        ParticlePreset.AURORA_ORBS -> {
                            val radius = min(size.width, size.height) * 0.26f
                            for (i in 0 until 6) {
                                val angle = tSec * 0.6f + i * 1.05f
                                val ox = size.width / 2f + cos(angle) * size.width * 0.28f
                                val oy = size.height / 2f + sin(angle * 1.3f) * size.height * 0.24f
                                val col = if (i % 2 == 0) ElectricViolet.copy(alpha = 0.48f) else CyberCyan.copy(alpha = 0.48f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(col, Color.Transparent),
                                        center = Offset(ox, oy),
                                        radius = radius
                                    ),
                                    radius = radius,
                                    center = Offset(ox, oy),
                                    blendMode = blend
                                )
                            }
                        }
                    }
                }
            }
        }

        // Layer-level scanlines & vignette overlays
        if (layer.effects.scanlines > 0.05f || layer.effects.vignette > 0.05f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (layer.effects.scanlines > 0.05f) {
                    val alpha = (layer.effects.scanlines * 0.45f).coerceIn(0f, 0.55f)
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color.Black.copy(alpha = alpha),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5f
                        )
                        y += 4.5f
                    }
                }
                if (layer.effects.vignette > 0.05f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = (layer.effects.vignette * 0.85f).coerceIn(0f, 0.92f))
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = maxOf(size.width, size.height) * 0.72f
                        )
                    )
                }
            }
        }

        if (isSelected) {
            SelectionReticle(layerType = layer.type)
        }
    }
}

@Composable
private fun RenderImageSource(
    sampleRes: Int?,
    imageUri: String?,
    contentDescription: String?,
    colorFilter: ColorFilter,
    modifier: Modifier = Modifier
) {
    if (sampleRes != null) {
        Image(
            painter = painterResource(id = sampleRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            modifier = modifier
        )
    } else if (!imageUri.isNullOrBlank()) {
        AsyncImage(
            model = Uri.parse(imageUri),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            modifier = modifier
        )
    }
}

@Composable
private fun SelectionReticle(layerType: LayerType) {
    val boxModifier = if (layerType == LayerType.IMAGE || layerType == LayerType.PARTICLE_FX) {
        Modifier.fillMaxSize().padding(4.dp)
    } else {
        Modifier.size(160.dp, 88.dp)
    }
    Canvas(modifier = boxModifier) {
        drawRoundRect(
            color = CyberCyan.copy(alpha = 0.85f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
        )
        val corner = 4.dp.toPx()
        listOf(
            Offset(0f, 0f),
            Offset(size.width, 0f),
            Offset(0f, size.height),
            Offset(size.width, size.height)
        ).forEach { pt ->
            drawCircle(color = CyberCyan, radius = corner, center = pt)
        }
    }
}

@Composable
private fun SafeGuidesOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val guideColor = Color.White.copy(alpha = 0.25f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        // Rule of thirds
        drawLine(guideColor, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), 1f, pathEffect = dash)
        drawLine(guideColor, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), 1f, pathEffect = dash)
        drawLine(guideColor, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), 1f, pathEffect = dash)
        drawLine(guideColor, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), 1f, pathEffect = dash)

        // Title-safe 10% inset margin
        val padX = size.width * 0.1f
        val padY = size.height * 0.1f
        drawRect(
            color = CyberCyan.copy(alpha = 0.35f),
            topLeft = Offset(padX, padY),
            size = Size(size.width - padX * 2f, size.height - padY * 2f),
            style = Stroke(width = 1.2f, pathEffect = dash)
        )
    }
}

private fun diamondPath(cx: Float, cy: Float, rx: Float, ry: Float): Path {
    return Path().apply {
        moveTo(cx, cy - ry)
        lineTo(cx + rx, cy)
        lineTo(cx, cy + ry)
        lineTo(cx - rx, cy)
        close()
    }
}

private fun starPath(cx: Float, cy: Float, outerR: Float): Path {
    val innerR = outerR * 0.44f
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = -PI / 2.0 + i * (PI / 5.0)
        val px = cx + (r * cos(angle)).toFloat()
        val py = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

private fun buildComposeColorMatrix(effects: LayerEffects): ColorMatrix {
    val result = ColorMatrix()

    if (effects.saturation != 1f) {
        val sat = ColorMatrix().apply { setToSaturation(effects.saturation.coerceIn(0f, 3f)) }
        result.timesAssign(sat)
    }

    val contrast = effects.contrast.coerceIn(0.2f, 2.5f)
    val brightnessOffset = effects.brightness * 255f
    val translate = (-0.5f * contrast + 0.5f) * 255f + brightnessOffset
    if (contrast != 1f || effects.brightness != 0f) {
        val cb = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.timesAssign(cb)
    }

    if (effects.hueShift != 0f) {
        val rad = Math.toRadians(effects.hueShift.toDouble()).toFloat()
        val cosVal = cos(rad)
        val sinVal = sin(rad)
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        val hue = ColorMatrix(
            floatArrayOf(
                lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
                lumG + cosVal * (-lumG) + sinVal * (-lumG),
                lumB + cosVal * (-lumB) + sinVal * (1 - lumB),
                0f, 0f,
                lumR + cosVal * (-lumR) + sinVal * 0.143f,
                lumG + cosVal * (1 - lumG) + sinVal * 0.140f,
                lumB + cosVal * (-lumB) + sinVal * (-0.283f),
                0f, 0f,
                lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
                lumG + cosVal * (-lumG) + sinVal * lumG,
                lumB + cosVal * (1 - lumB) + sinVal * lumB,
                0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.timesAssign(hue)
    }

    return result
}
