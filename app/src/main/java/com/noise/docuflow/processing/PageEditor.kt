package com.noise.docuflow.processing

import android.graphics.*
import androidx.core.graphics.createBitmap
import java.io.File
import java.util.UUID

object PageEditor {
    enum class Filter { ORIGINAL, GRAYSCALE, BLACK_WHITE }
    fun render(source: File, rotation: Int, filter: Filter, inset: Float = 0f): File {
        val input = BitmapFactory.decodeFile(source.path) ?: error("Unreadable page")
        val edge = inset.coerceIn(0f, 0.4f)
        val crop = Bitmap.createBitmap(input, (input.width * edge).toInt(), (input.height * edge).toInt(),
            (input.width * (1 - 2 * edge)).toInt().coerceAtLeast(1),
            (input.height * (1 - 2 * edge)).toInt().coerceAtLeast(1),
            Matrix().apply { postRotate(rotation.toFloat()) }, true)
        val output = createBitmap(crop.width, crop.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (filter != Filter.ORIGINAL) {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            if (filter == Filter.BLACK_WHITE) matrix.postConcat(ColorMatrix(floatArrayOf(
                2f,0f,0f,0f,-128f, 0f,2f,0f,0f,-128f, 0f,0f,2f,0f,-128f, 0f,0f,0f,1f,0f)))
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        Canvas(output).drawBitmap(crop, 0f, 0f, paint)
        val target = File(source.parentFile, "${UUID.randomUUID()}.jpg")
        try {
            target.outputStream().use { check(output.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            return target
        } catch (error: Exception) { target.delete(); throw error }
        finally {
            output.recycle()
            if (crop !== input) crop.recycle()
            input.recycle()
        }
    }
}
