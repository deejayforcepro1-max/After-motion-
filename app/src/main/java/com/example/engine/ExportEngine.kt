package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.BlendModeOption
import com.example.model.CompositionProject
import com.example.model.KeyframeEngine
import com.example.model.LayerItem
import com.example.model.LayerType
import com.example.model.ParticlePreset
import com.example.model.ShapeKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class ExportResult(
    val shareUri: Uri,
    val savedToGallery: Boolean,
    val formatLabel: String,
    val fileSizeKb: Long
)

object ExportEngine {

    suspend fun exportVideoMp4(
        context: Context,
        project: CompositionProject,
        onProgress: (Float) -> Unit
    ): Result<ExportResult> = withContext(Dispatchers.Default) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = project.name.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "AfterMotion" }
            val outFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.mp4")

            // Ensure dimensions are multiples of 16 for AVC encoder compatibility
            val width = (project.aspectRatio.widthPx / 2).let { (it / 16) * 16 }.coerceAtLeast(320)
            val height = (project.aspectRatio.heightPx / 2).let { (it / 16) * 16 }.coerceAtLeast(320)
            val fps = project.fps.coerceIn(24, 60)
            val totalFrames = ((project.durationMs * fps) / 1000L).toInt().coerceAtLeast(1)

            val bitmaps = preloadLayerBitmaps(context, project, maxOf(width, height))
            val titleTypeface = runCatching {
                ResourcesCompat.getFont(context, R.font.space_grotesk)
            }.getOrNull() ?: Typeface.DEFAULT_BOLD

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            var muxer: MediaMuxer? = null
            var trackIndex = -1
            var muxerStarted = false

            try {
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val inputSurface = encoder.createInputSurface()
                encoder.start()
                muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val bufferInfo = MediaCodec.BufferInfo()
                val frameDurationUs = 1_000_000L / fps
                val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val offscreenCanvas = Canvas(frameBitmap)

                for (frameIdx in 0 until totalFrames) {
                    val timeMs = (frameIdx * 1000L) / fps
                    drawCompositionFrame(
                        canvas = offscreenCanvas,
                        width = width.toFloat(),
                        height = height.toFloat(),
                        project = project,
                        timeMs = timeMs,
                        bitmaps = bitmaps,
                        typeface = titleTypeface
                    )

                    val surfaceCanvas = inputSurface.lockCanvas(null)
                    try {
                        surfaceCanvas.drawBitmap(frameBitmap, 0f, 0f, null)
                    } finally {
                        inputSurface.unlockCanvasAndPost(surfaceCanvas)
                    }

                    // Drain encoder
                    while (true) {
                        val outStatus = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                        if (outStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break
                        } else if (outStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (!muxerStarted) {
                                trackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        } else if (outStatus >= 0) {
                            val encodedData = encoder.getOutputBuffer(outStatus)
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    bufferInfo.presentationTimeUs = frameIdx * frameDurationUs
                                    encodedData.position(bufferInfo.offset)
                                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                                }
                            }
                            encoder.releaseOutputBuffer(outStatus, false)
                        }
                    }
                    onProgress((frameIdx + 1).toFloat() / totalFrames.toFloat())
                }

                encoder.signalEndOfInputStream()
                var eos = false
                while (!eos) {
                    val outStatus = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    if (outStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (outStatus >= 0) {
                        val encodedData = encoder.getOutputBuffer(outStatus)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                        }
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            eos = true
                        }
                        encoder.releaseOutputBuffer(outStatus, false)
                    }
                }
                frameBitmap.recycle()
            } finally {
                runCatching { encoder.stop() }
                runCatching { encoder.release() }
                if (muxerStarted) runCatching { muxer?.stop() }
                runCatching { muxer?.release() }
            }

            val savedGallery = saveVideoToMediaStore(context, outFile, "${safeName}.mp4")
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            ExportResult(
                shareUri = shareUri,
                savedToGallery = savedGallery,
                formatLabel = "MP4 Video (${width}x${height} @ ${fps}fps)",
                fileSizeKb = (outFile.length() / 1024L).coerceAtLeast(1L)
            )
        }
    }

    suspend fun exportStillPng(
        context: Context,
        project: CompositionProject,
        timeMs: Long
    ): Result<ExportResult> = withContext(Dispatchers.Default) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = project.name.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "AfterMotion" }
            val outFile = File(exportDir, "${safeName}_frame_${System.currentTimeMillis()}.png")

            val width = project.aspectRatio.widthPx
            val height = project.aspectRatio.heightPx
            val bitmaps = preloadLayerBitmaps(context, project, maxOf(width, height))
            val titleTypeface = runCatching {
                ResourcesCompat.getFont(context, R.font.space_grotesk)
            }.getOrNull() ?: Typeface.DEFAULT_BOLD

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawCompositionFrame(
                canvas = canvas,
                width = width.toFloat(),
                height = height.toFloat(),
                project = project,
                timeMs = timeMs,
                bitmaps = bitmaps,
                typeface = titleTypeface
            )
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            bitmap.recycle()

            val savedGallery = saveImageToMediaStore(context, outFile, "${safeName}_frame.png")
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            ExportResult(
                shareUri = shareUri,
                savedToGallery = savedGallery,
                formatLabel = "PNG Still (${width}x${height})",
                fileSizeKb = (outFile.length() / 1024L).coerceAtLeast(1L)
            )
        }
    }

    private fun preloadLayerBitmaps(
        context: Context,
        project: CompositionProject,
        maxDim: Int
    ): Map<String, Bitmap> {
        val map = mutableMapOf<String, Bitmap>()
        project.layers.forEach { layer ->
            if (layer.type == LayerType.IMAGE) {
                AssetHelper.loadBitmap(context, layer.sampleAssetKey, layer.imageUri, maxDim)?.let {
                    map[layer.id] = it
                }
            }
        }
        return map
    }

    private fun drawCompositionFrame(
        canvas: Canvas,
        width: Float,
        height: Float,
        project: CompositionProject,
        timeMs: Long,
        bitmaps: Map<String, Bitmap>,
        typeface: Typeface
    ) {
        canvas.drawColor(project.backgroundColorHex.toInt())
        val baseDim = min(width, height)

        project.layers.forEach { layer ->
            if (!KeyframeEngine.isLayerActiveAt(layer, timeMs)) return@forEach
            val transform = KeyframeEngine.evaluateTransform(layer, timeMs)
            if (transform.opacity <= 0.01f) return@forEach

            val layerSave = canvas.saveLayer(0f, 0f, width, height, null)
            val centerX = width / 2f + transform.posX * width
            val centerY = height / 2f + transform.posY * height

            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(transform.rotation)
            canvas.scale(transform.scale, transform.scale)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (transform.opacity * 255f).toInt().coerceIn(0, 255)
                colorFilter = ColorMatrixColorFilter(AssetHelper.buildAndroidColorMatrix(layer.effects))
                applyBlendMode(this, layer.blendMode)
                if (layer.effects.glowIntensity > 0.05f) {
                    setShadowLayer(
                        layer.effects.glowIntensity * 28f,
                        0f,
                        0f,
                        layer.effects.glowColorHex.toInt()
                    )
                }
            }

            when (layer.type) {
                LayerType.IMAGE -> {
                    val bmp = bitmaps[layer.id]
                    if (bmp != null) {
                        val scaleFit = maxOf(width / bmp.width, height / bmp.height)
                        val drawW = bmp.width * scaleFit
                        val drawH = bmp.height * scaleFit
                        val dst = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
                        if (layer.effects.chromaticShift > 0.05f) {
                            val offset = layer.effects.chromaticShift * 14f
                            val ghostPaint = Paint(paint).apply {
                                alpha = (transform.opacity * 110f).toInt().coerceIn(0, 255)
                            }
                            canvas.drawBitmap(bmp, null, RectF(dst.left - offset, dst.top, dst.right - offset, dst.bottom), ghostPaint)
                            canvas.drawBitmap(bmp, null, RectF(dst.left + offset, dst.top, dst.right + offset, dst.bottom), ghostPaint)
                        }
                        canvas.drawBitmap(bmp, null, dst, paint)
                    }
                }
                LayerType.SHAPE -> {
                    drawShapeLayer(canvas, layer, width, height, baseDim, paint)
                }
                LayerType.TEXT -> {
                    drawTextLayer(canvas, layer, baseDim, paint, typeface)
                }
                LayerType.PARTICLE_FX -> {
                    drawParticleLayer(canvas, layer, width, height, timeMs, paint)
                }
            }

            canvas.restore()

            // Per-layer screen effects (vignette / scanlines)
            if (layer.effects.scanlines > 0.05f) {
                val scanPaint = Paint().apply {
                    color = Color.argb((layer.effects.scanlines * 115f).toInt().coerceIn(0, 140), 0, 0, 0)
                    strokeWidth = 2f
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, scanPaint)
                    y += 6f
                }
            }
            if (layer.effects.vignette > 0.05f) {
                val vigPaint = Paint().apply {
                    shader = RadialGradient(
                        width / 2f,
                        height / 2f,
                        maxOf(width, height) * 0.72f,
                        intArrayOf(
                            Color.TRANSPARENT,
                            Color.argb((layer.effects.vignette * 215f).toInt().coerceIn(0, 240), 0, 0, 0)
                        ),
                        floatArrayOf(0.4f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, vigPaint)
            }

            canvas.restoreToCount(layerSave)
        }
    }

    private fun applyBlendMode(paint: Paint, blend: BlendModeOption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.blendMode = when (blend) {
                BlendModeOption.NORMAL -> android.graphics.BlendMode.SRC_OVER
                BlendModeOption.SCREEN -> android.graphics.BlendMode.SCREEN
                BlendModeOption.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
                BlendModeOption.OVERLAY -> android.graphics.BlendMode.OVERLAY
                BlendModeOption.ADD -> android.graphics.BlendMode.PLUS
                BlendModeOption.LIGHTEN -> android.graphics.BlendMode.LIGHTEN
                BlendModeOption.DARKEN -> android.graphics.BlendMode.DARKEN
            }
        } else {
            val mode = when (blend) {
                BlendModeOption.NORMAL -> PorterDuff.Mode.SRC_OVER
                BlendModeOption.SCREEN -> PorterDuff.Mode.SCREEN
                BlendModeOption.MULTIPLY -> PorterDuff.Mode.MULTIPLY
                BlendModeOption.OVERLAY -> PorterDuff.Mode.OVERLAY
                BlendModeOption.ADD -> PorterDuff.Mode.ADD
                BlendModeOption.LIGHTEN -> PorterDuff.Mode.LIGHTEN
                BlendModeOption.DARKEN -> PorterDuff.Mode.DARKEN
            }
            paint.xfermode = PorterDuffXfermode(mode)
        }
    }

    private fun drawShapeLayer(
        canvas: Canvas,
        layer: LayerItem,
        width: Float,
        height: Float,
        baseDim: Float,
        basePaint: Paint
    ) {
        val w = width * layer.shapeWidthFraction
        val h = if (layer.shapeKind == ShapeKind.RING || layer.shapeKind == ShapeKind.CIRCLE ||
            layer.shapeKind == ShapeKind.STAR || layer.shapeKind == ShapeKind.DIAMOND
        ) {
            baseDim * layer.shapeWidthFraction
        } else {
            height * layer.shapeHeightFraction
        }
        val effectiveW = if (layer.shapeKind == ShapeKind.RECTANGLE || layer.shapeKind == ShapeKind.BAR_ACCENT) w else h
        val rect = RectF(-effectiveW / 2f, -h / 2f, effectiveW / 2f, h / 2f)

        val fillAlpha = ((layer.fillColorHex ushr 24) and 0xFF).toInt()
        if (fillAlpha > 0) {
            val fillPaint = Paint(basePaint).apply {
                style = Paint.Style.FILL
                color = layer.fillColorHex.toInt()
                alpha = ((fillAlpha * basePaint.alpha) / 255).coerceIn(0, 255)
            }
            when (layer.shapeKind) {
                ShapeKind.RECTANGLE, ShapeKind.BAR_ACCENT ->
                    canvas.drawRoundRect(rect, layer.cornerRadiusDp * 2f, layer.cornerRadiusDp * 2f, fillPaint)
                ShapeKind.CIRCLE -> canvas.drawOval(rect, fillPaint)
                ShapeKind.RING -> Unit
                ShapeKind.DIAMOND -> canvas.drawPath(buildDiamondPath(effectiveW / 2f, h / 2f), fillPaint)
                ShapeKind.STAR -> canvas.drawPath(buildStarPath(effectiveW / 2f), fillPaint)
            }
        }

        if (layer.strokeWidth > 0f || layer.shapeKind == ShapeKind.RING) {
            val strokePaint = Paint(basePaint).apply {
                style = Paint.Style.STROKE
                color = layer.strokeColorHex.toInt()
                strokeWidth = layer.strokeWidth.coerceAtLeast(3f) * 1.6f
            }
            when (layer.shapeKind) {
                ShapeKind.RECTANGLE, ShapeKind.BAR_ACCENT ->
                    canvas.drawRoundRect(rect, layer.cornerRadiusDp * 2f, layer.cornerRadiusDp * 2f, strokePaint)
                ShapeKind.CIRCLE, ShapeKind.RING -> canvas.drawOval(rect, strokePaint)
                ShapeKind.DIAMOND -> canvas.drawPath(buildDiamondPath(effectiveW / 2f, h / 2f), strokePaint)
                ShapeKind.STAR -> canvas.drawPath(buildStarPath(effectiveW / 2f), strokePaint)
            }
        }
    }

    private fun buildDiamondPath(rx: Float, ry: Float): Path {
        return Path().apply {
            moveTo(0f, -ry)
            lineTo(rx, 0f)
            lineTo(0f, ry)
            lineTo(-rx, 0f)
            close()
        }
    }

    private fun buildStarPath(outerR: Float): Path {
        val innerR = outerR * 0.44f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = -PI / 2.0 + i * (PI / 5.0)
            val px = (r * cos(angle)).toFloat()
            val py = (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
    }

    private fun drawTextLayer(
        canvas: Canvas,
        layer: LayerItem,
        baseDim: Float,
        basePaint: Paint,
        typeface: Typeface
    ) {
        val textPaint = Paint(basePaint).apply {
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = (layer.textSizeSp / 380f) * baseDim * 1.45f
            letterSpacing = layer.textTracking * 0.04f
            color = layer.fillColorHex.toInt()
        }
        val fm = textPaint.fontMetrics
        val baselineOffset = -(fm.ascent + fm.descent) / 2f

        if (layer.effects.chromaticShift > 0.05f) {
            val shift = layer.effects.chromaticShift * 12f
            val cyanPaint = Paint(textPaint).apply {
                color = Color.argb((basePaint.alpha * 0.65f).toInt(), 34, 211, 238)
            }
            val rosePaint = Paint(textPaint).apply {
                color = Color.argb((basePaint.alpha * 0.65f).toInt(), 244, 63, 94)
            }
            canvas.drawText(layer.textContent, -shift, baselineOffset, cyanPaint)
            canvas.drawText(layer.textContent, shift, baselineOffset, rosePaint)
        }
        canvas.drawText(layer.textContent, 0f, baselineOffset, textPaint)
    }

    private fun drawParticleLayer(
        canvas: Canvas,
        layer: LayerItem,
        width: Float,
        height: Float,
        timeMs: Long,
        basePaint: Paint
    ) {
        val tSec = timeMs / 1000f
        when (layer.particlePreset) {
            ParticlePreset.NEON_STARFIELD -> {
                val count = (28 + (layer.particleDensity * 52)).toInt()
                val p = Paint(basePaint).apply { style = Paint.Style.FILL }
                for (i in 0 until count) {
                    val seedA = sin(i * 12.9898f) * 43758.5453f
                    val seedB = sin(i * 78.233f) * 19341.12f
                    val fx = (seedA - kotlin.math.floor(seedA))
                    val fy = ((seedB - kotlin.math.floor(seedB)) - tSec * (0.08f + (i % 5) * 0.03f)) % 1f
                    val wrappedY = if (fy < 0f) fy + 1f else fy
                    val px = (fx - 0.5f) * width
                    val py = (wrappedY - 0.5f) * height
                    val radius = 2.2f + (i % 4) * 1.4f
                    p.color = if (i % 2 == 0) 0xFF22D3EE.toInt() else 0xFF7C6BFF.toInt()
                    p.alpha = ((0.35f + 0.55f * ((i % 3) / 2f)) * basePaint.alpha).toInt().coerceIn(0, 255)
                    canvas.drawCircle(px, py, radius, p)
                }
            }
            ParticlePreset.LIGHT_SWEEP -> {
                val sweepPos = ((tSec * 0.45f) % 1.6f) - 0.8f
                val cx = sweepPos * width
                val sweepPaint = Paint(basePaint).apply {
                    shader = LinearGradient(
                        cx - width * 0.18f, -height / 2f,
                        cx + width * 0.18f, height / 2f,
                        intArrayOf(Color.TRANSPARENT, 0x9922D3EE.toInt(), 0xCCFFFFFF.toInt(), 0x887C6BFF.toInt(), Color.TRANSPARENT),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(-width / 2f, -height / 2f, width / 2f, height / 2f, sweepPaint)
            }
            ParticlePreset.CYBER_GRID -> {
                val gridPaint = Paint(basePaint).apply {
                    color = 0xFF7C6BFF.toInt()
                    strokeWidth = 1.8f
                    alpha = (basePaint.alpha * 0.45f).toInt().coerceIn(0, 255)
                }
                val step = min(width, height) / 10f
                val offset = (tSec * 40f) % step
                var x = -width / 2f
                while (x <= width / 2f) {
                    canvas.drawLine(x, -height / 2f, x, height / 2f, gridPaint)
                    x += step
                }
                var y = -height / 2f + offset
                while (y <= height / 2f) {
                    canvas.drawLine(-width / 2f, y, width / 2f, y, gridPaint)
                    y += step
                }
            }
            ParticlePreset.AURORA_ORBS -> {
                val orbPaint = Paint(basePaint).apply { style = Paint.Style.FILL }
                for (i in 0 until 6) {
                    val angle = tSec * 0.6f + i * 1.05f
                    val ox = cos(angle) * width * 0.28f
                    val oy = sin(angle * 1.3f) * height * 0.25f
                    val r = min(width, height) * 0.24f
                    val col = if (i % 2 == 0) 0x887C6BFF.toInt() else 0x8822D3EE.toInt()
                    orbPaint.shader = RadialGradient(ox, oy, r, col, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                    canvas.drawCircle(ox, oy, r, orbPaint)
                }
            }
        }
    }

    private fun saveVideoToMediaStore(context: Context, file: File, displayName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AfterMotion")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }

    private fun saveImageToMediaStore(context: Context, file: File, displayName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AfterMotion")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }
}
