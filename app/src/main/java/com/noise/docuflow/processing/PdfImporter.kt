package com.noise.docuflow.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import androidx.core.graphics.createBitmap
import java.io.File
import java.util.UUID

object PdfImporter {
    suspend fun import(context: Context, uri: Uri, remaining: Int): List<File> {
        val output = mutableListOf<File>()
        // SAF providers are not necessarily seekable. Copy to private temporary storage.
        val temporary = File.createTempFile("import-", ".pdf", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { target ->
                    val buffer = ByteArray(64 * 1024)
                    var bytes = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        bytes += count
                        require(bytes <= 150L * 1024 * 1024) { "PDF exceeds 150 MB import limit" }
                        target.write(buffer, 0, count)
                    }
                }
            } ?: error("PDF is inaccessible")
            android.os.ParcelFileDescriptor.open(temporary, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    require(renderer.pageCount in 1..remaining) { "Maximum 50 pages per document" }
                    repeat(renderer.pageCount) { index ->
                        currentCoroutineContext().ensureActive()
                        renderer.openPage(index).use { page ->
                            val scale = 2000f / maxOf(page.width, page.height).coerceAtLeast(1)
                            val bitmap = createBitmap((page.width * scale).toInt().coerceAtLeast(1),
                                (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                            try {
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val file = File(File(context.filesDir, "pages").apply { mkdirs() }, "${UUID.randomUUID()}.jpg")
                                output += file
                                file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
                            } finally { bitmap.recycle() }
                        }
                    }
                }
            }
            return output
        } catch (error: Exception) { output.forEach { it.delete() }; throw error }
        finally { temporary.delete() }
    }
}
