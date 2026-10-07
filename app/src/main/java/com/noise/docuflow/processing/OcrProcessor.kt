package com.noise.docuflow.processing

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.noise.docuflow.data.Page
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrProcessor {
    suspend fun recognize(page: Page): Page {
        val bitmap = BitmapFactory.decodeFile(page.image) ?: return page.copy(text = "", ocr = "Unreadable")
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val text = suspendCancellableCoroutine<String> { continuation ->
                client.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(it.text) }
                    .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                    .addOnCompleteListener { bitmap.recycle(); client.close() }
            }
            page.copy(text = text, ocr = if (text.isBlank()) "No Latin text detected" else "Available")
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { page.copy(text = "", ocr = "Failed — retry available") }
    }
}
