package com.noise.docuflow

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import com.noise.docuflow.data.*
import com.noise.docuflow.processing.*
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

class DocumentController(private val context: Context, private val scope: CoroutineScope) {
    private val store = DocumentStore(context)
    var documents by mutableStateOf<List<DocumentSummary>>(emptyList()); private set
    var selected by mutableStateOf<Document?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)
    var query by mutableStateOf(""); private set
    var category by mutableStateOf<String?>(null); private set
    var favorites by mutableStateOf(false); private set
    var tag by mutableStateOf(""); private set
    var hasMore by mutableStateOf(false); private set
    var highQuality by mutableStateOf(false)
    private var work: Job? = null
    private var searchJob: Job? = null
    private var offset = 0

    init {
        runWork {
            withContext(Dispatchers.IO) {
                store.recoverFiles()
                File(context.cacheDir, "exports").listFiles()?.filter {
                    it.lastModified() < System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                }?.forEach { it.delete() }
            }
            refresh()
        }
    }
    private fun runWork(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        work = scope.launch {
            try { block() }
            catch (cancel: CancellationException) { message = "Operation canceled"; throw cancel }
            catch (error: Exception) { message = friendlyError(error) }
            finally { busy = false }
        }
    }
    fun cancel() { work?.cancel() }
    fun report(text: String) { message = text }
    private suspend fun refresh(loadMore: Boolean = false) {
        val pageOffset = if (loadMore) offset else 0
        val q = query; val c = category; val f = favorites; val t = tag
        val result = withContext(Dispatchers.IO) { store.search(q, c, f, t, 100, pageOffset) }
        currentCoroutineContext().ensureActive()
        documents = if (loadMore) documents + result else result
        offset = pageOffset + result.size
        hasMore = result.size == 100
    }
    fun filter(text: String = query, selectedCategory: String? = category, onlyFavorites: Boolean = favorites, selectedTag: String = tag) {
        query = text; category = selectedCategory; favorites = onlyFavorites; tag = selectedTag
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(250)
            try { refresh() } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { message = "Search unavailable. Try rebuilding the index." }
        }
    }
    fun loadMore() {
        searchJob?.cancel()
        searchJob = scope.launch { try { refresh(true) } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { message = "Could not load more documents" } }
    }
    fun open(id: String) = runWork {
        selected = withContext(Dispatchers.IO) { store.get(id) }
        if (selected == null) message = "Document is no longer available"
    }
    fun back() { if (!busy) { selected = null; filter() } }
    fun save(document: Document) = runWork {
        persist(document.copy(modified = System.currentTimeMillis()))
    }
    private suspend fun persist(document: Document) {
        withContext(Dispatchers.IO) { store.save(document) }
        selected = document
        try { refresh() } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { message = "Changes saved. Refresh the library to update the listing." }
    }
    fun import(uris: List<Uri>, replacement: Int? = null) = runWork {
        val base = selected
        val imported = mutableListOf<File>()
        var committed = false
        var failures = 0
        try {
            withContext(Dispatchers.IO) {
                val room = if (replacement == null) 50 - (base?.pages?.size ?: 0) else 1
                for (uri in uris.take(50)) {
                    ensureActive()
                    if (imported.size >= room) { failures++; continue }
                    try {
                        if (context.contentResolver.getType(uri) == "application/pdf") {
                            imported += PdfImporter.import(context, uri, room - imported.size)
                        } else imported += Capture.importImage(context, uri)
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { failures++ }
                }
            }
            if (imported.isEmpty()) throw WorkflowException("No readable pages could be imported. Check the file format and the 50-page limit.")
            val pages = imported.map { Page(UUID.randomUUID().toString(), it.path) }
            val now = System.currentTimeMillis()
            val document = base ?: Document(UUID.randomUUID().toString(), DocumentNaming.suggest("", now), now)
            val updated = document.copy(pages = if (replacement == null) document.pages + pages else PageOrder.replace(document.pages, replacement, pages.first()), modified = now)
            // Commit and publish atomically with respect to coroutine cancellation.
            withContext(NonCancellable) { persist(updated); committed = true }
            message = if (failures == 0) "Pages imported. Review them, then run OCR." else "Pages imported; $failures items could not be added."
        } finally { if (!committed) imported.forEach { it.delete() } }
    }
    fun edit(index: Int, rotation: Int, filter: PageEditor.Filter, inset: Float) = runWork {
        val document = selected ?: return@runWork
        val page = document.pages.getOrNull(index) ?: return@runWork
        var rendered: File? = null
        var committed = false
        try {
            withContext(Dispatchers.IO) { rendered = PageEditor.render(File(page.source), rotation, filter, inset) }
            val updated = page.copy(image = checkNotNull(rendered).path, text = "", ocr = "Pending")
            withContext(NonCancellable) {
                persist(document.copy(pages = PageOrder.replace(document.pages, index, updated), modified = System.currentTimeMillis()))
                committed = true
            }
        } finally { if (!committed) rendered?.delete() }
    }
    fun ocr() = runWork {
        var document = selected ?: return@runWork
        val processor = OcrProcessor()
        for (index in document.pages.indices) {
            currentCoroutineContext().ensureActive()
            val page = withContext(Dispatchers.Default) { processor.recognize(document.pages[index]) }
            document = document.copy(pages = PageOrder.replace(document.pages, index, page), modified = System.currentTimeMillis())
            withContext(NonCancellable) { persist(document) }
        }
        val readable = document.pages.count { it.text.isNotBlank() }
        message = "OCR complete: $readable of ${document.pages.size} pages contain recognized text."
    }
    fun delete() = runWork {
        val id = selected?.id ?: return@runWork
        withContext(NonCancellable + Dispatchers.IO) { store.delete(id) }
        selected = null; refresh(); message = "Document deleted"
    }
    fun rebuild() = runWork {
        withContext(Dispatchers.IO) { store.rebuildIndex() }
        refresh(); message = "Search index rebuilt"
    }
    fun export(uri: Uri) = runWork {
        val document = selected ?: return@runWork
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "wt")?.use { PdfExporter.write(document, it, highQuality) }
                ?: error("Export destination unavailable")
        }
        message = "PDF saved"
    }
    fun share(pdf: Boolean, deliver: (List<File>, String) -> Unit) = runWork {
        val document = selected ?: return@runWork
        if (!pdf) { deliver(document.pages.map { File(it.image) }, "image/jpeg"); return@runWork }
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}-${PdfExporter.filename(document.title)}")
        try {
            withContext(Dispatchers.IO) { file.outputStream().use { PdfExporter.write(document, it, highQuality) } }
            deliver(listOf(file), "application/pdf")
        } catch (error: Exception) { file.delete(); throw error }
    }
    private fun friendlyError(error: Exception): String = when (error) {
        is WorkflowException -> error.message ?: "Operation could not finish"
        is java.io.IOException -> "File operation failed. Check available storage and access, then retry."
        is SecurityException -> "File access was denied. Select the file again."
        else -> "Operation could not finish. Check the pages and retry."
    }
}
