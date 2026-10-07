package com.noise.docuflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noise.docuflow.data.*
import com.noise.docuflow.processing.*

@Composable
fun DocumentDetail(
    document: Document, busy: Boolean, onSave: (Document) -> Unit,
    onBack: () -> Unit, onScan: () -> Unit, onImport: (Int?) -> Unit,
    onOcr: () -> Unit, onEdit: (Int, Int, PageEditor.Filter, Float) -> Unit,
    onPdf: () -> Unit, onShare: (Boolean) -> Unit, onDelete: () -> Unit,
    onOpen: (Page) -> Unit
) {
    var title by remember(document.id) { mutableStateOf(document.title) }
    var tags by remember(document.id) { mutableStateOf(document.tags) }
    var category by remember(document.id) { mutableStateOf(document.category) }
    var favorite by remember(document.id) { mutableStateOf(document.favorite) }
    var delete by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var textQuery by remember { mutableStateOf("") }
    val suggestion = remember(document.pages) { DocumentClassifier.suggest(document.pages.joinToString("\n") { it.text }) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(20.dp)) {
        item {
            TextButton(onClick = onBack, enabled = !busy) { Text("Back to library") }
            Text("Document", style = MaterialTheme.typography.headlineMedium)
            Text("${document.pages.size} pages • ${document.category}")
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
            TextButton(onClick = { title = DocumentNaming.suggest(document.pages.joinToString("\n") { it.text }, document.created) }, enabled = !busy) { Text("Suggest title from text") }
            OutlinedTextField(tags, { tags = it }, label = { Text("Tags, separated by commas") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { expanded = true }, enabled = !busy) { Text("Category: $category") }
                DropdownMenu(expanded, { expanded = false }) {
                    categories.forEach { value ->
                        DropdownMenuItem(text = { Text(value) }, onClick = { category = value; expanded = false })
                    }
                }
            }
            if (suggestion.evidence.isNotEmpty()) {
                Text("Likely type: ${suggestion.category}. Detected: ${suggestion.evidence.joinToString()}")
                TextButton(onClick = { category = suggestion.category }, enabled = !busy) { Text("Use suggested category") }
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(favorite, { favorite = it }, enabled = !busy); Text("Favorite")
            }
            Button(onClick = { onSave(Organization.update(document, title, category, tags, favorite)) }, enabled = !busy) { Text("Save changes") }
            Row {
                TextButton(onClick = onScan, enabled = !busy) { Text("Add scan") }
                TextButton(onClick = { onImport(null) }, enabled = !busy) { Text("Add images") }
                TextButton(onClick = onOcr, enabled = !busy && document.pages.isNotEmpty()) { Text("Run OCR") }
            }
            Row {
                TextButton(onClick = onPdf, enabled = !busy && document.pages.isNotEmpty()) { Text("Save PDF") }
                TextButton(onClick = { onShare(true) }, enabled = !busy && document.pages.isNotEmpty()) { Text("Share PDF") }
                TextButton(onClick = { onShare(false) }, enabled = !busy && document.pages.isNotEmpty()) { Text("Share images") }
            }
            OutlinedTextField(textQuery, { textQuery = it }, label = { Text("Find in recognized text") }, modifier = Modifier.fillMaxWidth())
        }
        itemsIndexed(document.pages, key = { _, page -> page.id }) { index, page ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Page ${index + 1} • ${page.ocr}", style = MaterialTheme.typography.titleMedium)
                PageThumbnail(page.image, Modifier.fillMaxWidth().height(200.dp))
                Row {
                    TextButton(onClick = { onOpen(page) }, enabled = !busy) { Text("Open") }
                    TextButton(onClick = { editing = index }, enabled = !busy) { Text("Edit") }
                    TextButton(onClick = { onImport(index) }, enabled = !busy) { Text("Replace") }
                    TextButton(onClick = { removing = index }, enabled = !busy) { Text("Remove") }
                }
                Row {
                    TextButton(onClick = { onSave(document.copy(pages = PageOrder.move(document.pages, index, index - 1))) }, enabled = !busy && index > 0) { Text("Move up") }
                    TextButton(onClick = { onSave(document.copy(pages = PageOrder.move(document.pages, index, index + 1))) }, enabled = !busy && index < document.pages.lastIndex) { Text("Move down") }
                }
                if (page.text.isNotBlank() && (textQuery.isBlank() || page.text.contains(textQuery, true))) {
                    androidx.compose.foundation.text.selection.SelectionContainer { Text(page.text) }
                } else if (page.text.isBlank()) Text("Text unavailable. Latin script is supported; try a clearer page.")
                HorizontalDivider()
            }
        }
        item { TextButton(onClick = { delete = true }, enabled = !busy) { Text("Delete document", color = MaterialTheme.colorScheme.error) } }
    }
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Delete this document?") },
        text = { Text("Its pages and recognized text will be permanently removed.") },
        confirmButton = { TextButton(onClick = { delete = false; onDelete() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { delete = false }) { Text("Cancel") } })
    removing?.let { index ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("Remove page ${index + 1}?") },
            confirmButton = { TextButton(onClick = { removing = null; onSave(document.copy(pages = document.pages.filterIndexed { i, _ -> i != index })) }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } })
    }
    editing?.let { index ->
        var rotation by remember(index) { mutableIntStateOf(0) }
        var inset by remember(index) { mutableFloatStateOf(0f) }
        var filter by remember(index) { mutableStateOf(PageEditor.Filter.ORIGINAL) }
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Edit page ${index + 1}") },
            text = {
                Column {
                    Text("Edits use the preserved original. Perspective crop is available in the scanner.")
                    TextButton(onClick = { rotation = (rotation + 90) % 360 }) { Text("Rotation: $rotation°") }
                    Text("Trim edges: ${(inset * 100).toInt()}%")
                    Slider(inset, { inset = it }, valueRange = 0f..0.4f)
                    PageEditor.Filter.entries.forEach { value ->
                        TextButton(onClick = { filter = value }) { Text(if (filter == value) "✓ ${value.name}" else value.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editing = null; onEdit(index, rotation, filter, inset) }) { Text("Apply") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
}
