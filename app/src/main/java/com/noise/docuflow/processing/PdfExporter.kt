package com.noise.docuflow.processing

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.noise.docuflow.data.Document
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.OutputStream

object PdfExporter {
    fun filename(title: String): String = title.replace(Regex("[^\\p{L}\\p{N}._-]+"), "_").trim('_').take(80).ifBlank { "Document" } + ".pdf"
    suspend fun write(document: Document, stream: OutputStream, highQuality: Boolean = false) {
        require(document.pages.isNotEmpty()) { "Document has no pages" }
        val pdf = PdfDocument()
        try {
            document.pages.forEachIndexed { index, source ->
                currentCoroutineContext().ensureActive()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(source.image, bounds)
                val max = if (highQuality) 2400 else 1600
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > max) sample *= 2
                val bitmap = BitmapFactory.decodeFile(source.image, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?: error("A document page is missing or unreadable")
                try {
                    val width = if (bitmap.width > bitmap.height) 842 else 595
                    val height = if (bitmap.width > bitmap.height) 595 else 842
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, index + 1).create())
                    page.canvas.drawColor(Color.WHITE)
                    val scale = minOf((width - 32f) / bitmap.width, (height - 32f) / bitmap.height)
                    val w = bitmap.width * scale; val h = bitmap.height * scale
                    page.canvas.drawBitmap(bitmap, null, RectF((width-w)/2, (height-h)/2, (width+w)/2, (height+h)/2), Paint(Paint.FILTER_BITMAP_FLAG))
                    pdf.finishPage(page)
                } finally { bitmap.recycle() }
            }
            currentCoroutineContext().ensureActive()
            pdf.writeTo(stream)
        } finally { pdf.close() }
    }
}
