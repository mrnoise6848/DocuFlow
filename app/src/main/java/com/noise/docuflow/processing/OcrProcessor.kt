package com.noise.docuflow.processing

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.noise.docuflow.data.Page
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OcrProcessor {
    suspend fun recognize(page: Page): Page {
        val bitmap = BitmapFactory.decodeFile(page.image)
            ?: return page.copy(text = "", ocr = "Unreadable")
        try {
            val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                // Native recognition cannot be interrupted. Await this bounded page so
                // cancellation/retry cannot accumulate clients or recycle an in-use bitmap.
                val text = withContext(NonCancellable) {
                    suspendCoroutine<String> { continuation ->
                        client.process(InputImage.fromBitmap(bitmap, 0)).addOnCompleteListener { task ->
                            if (task.isSuccessful) continuation.resume(task.result.text)
                            else continuation.resumeWithException(task.exception ?: IllegalStateException("OCR failed"))
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                return page.copy(text = text, ocr = if (text.isBlank()) "No Latin text detected" else "Available")
            } finally { client.close() }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { return page.copy(text = "", ocr = "Failed — retry available") }
        finally { bitmap.recycle() }
    }
}
