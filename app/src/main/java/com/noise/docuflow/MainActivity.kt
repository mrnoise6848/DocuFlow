package com.noise.docuflow

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.noise.docuflow.processing.*
import com.noise.docuflow.ui.*
import com.noise.docuflow.ui.theme.DocuFlowTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var controller: DocumentController
    private var replacement: Int? = null
    private var scannerPending by mutableStateOf(false)
    private var pickerPending by mutableStateOf(false)
    private var scanLaunched = false
    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        scannerPending = false
        scanLaunched = false
        if (result.resultCode == Activity.RESULT_OK) {
            val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty()
            if (pages.isNotEmpty()) controller.import(pages.map { it.imageUri })
            else controller.report("Scanner returned no pages. Try again or import an image.")
        }
    }
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pickerPending = false
        val target = replacement
        replacement = null
        if (uris.isNotEmpty()) controller.import(if (target == null) uris else uris.take(1), target)
    }
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        pickerPending = false
        if (uri != null) controller.export(uri)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanLaunched = savedInstanceState?.getBoolean("scanLaunched") ?: false
        scannerPending = scanLaunched
        pickerPending = savedInstanceState?.getBoolean("pickerPending") ?: false
        replacement = savedInstanceState?.getInt("replacement", -1)?.takeIf { it >= 0 }
        controller = DocumentController(applicationContext, lifecycleScope, savedInstanceState?.getString("document"))
        enableEdgeToEdge()
        setContent {
            DocuFlowTheme {
                val snackbar = remember { SnackbarHostState() }
                val message = controller.message
                LaunchedEffect(message) {
                    if (message != null) {
                        snackbar.showSnackbar(message)
                        if (controller.message == message) controller.message = null
                    }
                }
                val busy = controller.busy || scannerPending || pickerPending
                BackHandler(controller.selected != null) {
                    if (!busy) controller.back()
                }
                Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        if (busy) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Row(Modifier.padding(horizontal = 20.dp)) {
                                Text(if (scannerPending) "Opening scanner…" else if (pickerPending) "Choosing a file…" else "Processing…", Modifier.weight(1f))
                                if (controller.busy) TextButton(onClick = controller::cancel) { Text("Cancel") }
                            }
                        }
                        val document = controller.selected
                        if (document == null) {
                            DocumentLibrary(
                                controller.documents, controller.query, controller.category, controller.favorites, controller.tag,
                                busy, controller.hasMore,
                                { q, c, f, t -> controller.filter(q, c, f, t) },
                                controller::open, ::scan, { import(null) }, controller::loadMore, controller::rebuild
                            )
                        } else {
                            Column {
                                Row(Modifier.padding(horizontal = 20.dp)) {
                                    Checkbox(controller.highQuality, { controller.highQuality = it }, enabled = !busy)
                                    Text("High-quality PDF", Modifier.padding(top = 12.dp))
                                }
                                DocumentDetail(
                                    document, busy, controller::save, controller::back, ::scan, ::import,
                                    controller::ocr, controller::edit,
                                    {
                                        pickerPending = true
                                        try { exportLauncher.launch(PdfExporter.filename(document.title)) }
                                        catch (_: Exception) { pickerPending = false; controller.report("System save picker is unavailable.") }
                                    },
                                    { pdf -> controller.share(pdf) { files, mime ->
                                        try { DocumentSharing.share(this@MainActivity, files, mime) }
                                        catch (_: Exception) { controller.report("No compatible sharing app is available.") }
                                    } },
                                    controller::delete,
                                    { page ->
                                        try { DocumentSharing.open(this@MainActivity, File(page.image)) }
                                        catch (_: Exception) { controller.report("Page unavailable or no image viewer is installed.") }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    private fun import(index: Int?) {
        replacement = index
        pickerPending = true
        try { importLauncher.launch(arrayOf("image/*", "application/pdf")) }
        catch (_: Exception) { pickerPending = false; replacement = null; controller.report("System file picker is unavailable.") }
    }
    private fun scan() {
        if ((controller.selected?.pages?.size ?: 0) >= 50) {
            controller.report("Maximum 50 pages per document. Create a new document to continue.")
            return
        }
        scannerPending = true
        Capture.scanner.getStartScanIntent(this)
            .addOnSuccessListener { sender ->
                if (!isDestroyed) {
                    try { scanLaunched = true; scanLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
                    catch (_: Exception) { scanLaunched = false; scannerPending = false; controller.report("Scanner could not open. Try importing instead.") }
                }
            }
            .addOnFailureListener {
                scannerPending = false
                controller.report("Scanner unavailable. Check Google Play Services and its setup download, or import a file.")
            }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("scanLaunched", scanLaunched)
        outState.putBoolean("pickerPending", pickerPending)
        outState.putString("document", controller.selected?.id)
        outState.putInt("replacement", replacement ?: -1)
        super.onSaveInstanceState(outState)
    }
}
