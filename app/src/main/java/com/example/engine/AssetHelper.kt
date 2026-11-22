package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.net.Uri
import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.DefaultTemplates
import com.example.model.LayerEffects
import kotlin.math.cos
import kotlin.math.sin

object AssetHelper {

    @DrawableRes
    fun drawableForSampleKey(key: String?): Int? {
        return when (key) {
            DefaultTemplates.ASSET_CYBER_CITY -> R.drawable.img_sample_cyber_city
            DefaultTemplates.ASSET_NEBULA -> R.drawable.img_sample_nebula
            DefaultTemplates.ASSET_PORTRAIT -> R.drawable.img_sample_portrait
            else -> null
        }
    }

    fun loadBitmap(
        context: Context,
        sampleAssetKey: String?,
        imageUri: String?,
        maxDimension: Int = 1280
    ): Bitmap? {
        return runCatching {
            val resId = drawableForSampleKey(sampleAssetKey)
            if (resId != null) {
                decodeSampledResource(context, resId, maxDimension)
            } else if (!imageUri.isNullOrBlank()) {
                decodeSampledUri(context, Uri.parse(imageUri), maxDimension)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun decodeSampledResource(context: Context, resId: Int, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, opts)
        opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, maxDim, maxDim)
        opts.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(context.resources, resId, opts)
    }

    private fun decodeSampledUri(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, maxDim, maxDim)
        opts.inJustDecodeBounds = false
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun buildAndroidColorMatrix(effects: LayerEffects): ColorMatrix {
        val matrix = ColorMatrix()

        // Saturation
        if (effects.saturation != 1f) {
            val sat = ColorMatrix().apply { setSaturation(effects.saturation.coerceIn(0f, 3f)) }
            matrix.postConcat(sat)
        }

        // Contrast & Brightness
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
            matrix.postConcat(cb)
        }

        // Hue shift
        if (effects.hueShift != 0f) {
            val rad = Math.toRadians(effects.hueShift.toDouble()).toFloat()
            val cosVal = cos(rad)
            val sinVal = sin(rad)
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            val hueMatrix = ColorMatrix(
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
            matrix.postConcat(hueMatrix)
        }

        return matrix
    }
}
