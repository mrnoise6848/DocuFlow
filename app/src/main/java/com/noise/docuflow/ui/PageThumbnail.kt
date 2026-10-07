package com.noise.docuflow.ui

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PageThumbnail(path: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            ThumbnailCache.load(path)
        }
    }
    bitmap?.let { Image(it.asImageBitmap(), "Document page", modifier, contentScale = ContentScale.Fit) }
        ?: Text("Preview unavailable", modifier)
}
