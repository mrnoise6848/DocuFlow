package com.noise.docuflow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.noise.docuflow.data.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DocumentLibrary(
    documents: List<DocumentSummary>, query: String, category: String?, favorite: Boolean, tag: String,
    busy: Boolean, hasMore: Boolean, onFilter: (String, String?, Boolean, String) -> Unit,
    onOpen: (String) -> Unit, onScan: () -> Unit, onImport: () -> Unit,
    onMore: () -> Unit, onRebuild: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("DocuFlow", style = MaterialTheme.typography.headlineLarge)
            Text("Scan it once. Find it later.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onScan, enabled = !busy) { Text("Scan document") }
                OutlinedButton(onClick = onImport, enabled = !busy) { Text("Import") }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(query, { onFilter(it, category, favorite, tag) },
                label = { Text("Search titles, text, tags") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(favorite, { onFilter(query, category, it, tag) })
                Text("Favorites")
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expanded = true }) { Text(category ?: "All categories") }
                    DropdownMenu(expanded, { expanded = false }) {
                        DropdownMenuItem(text = { Text("All categories") }, onClick = { onFilter(query, null, favorite, tag); expanded = false })
                        categories.forEach { value ->
                            DropdownMenuItem(text = { Text(value) }, onClick = { onFilter(query, value, favorite, tag); expanded = false })
                        }
                    }
                }
            }
            OutlinedTextField(tag, { onFilter(query, category, favorite, it) },
                label = { Text("Filter by exact tag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (documents.isEmpty() && !busy) item {
            Column(Modifier.padding(vertical = 32.dp)) {
                Text(if (query.isBlank() && category == null && !favorite && tag.isBlank()) "No documents yet." else "No matching documents found.",
                    style = MaterialTheme.typography.titleLarge)
                Text("Scan or import an image or PDF to get started.")
            }
        }
        items(documents, key = { it.id }) { document ->
            Column(Modifier.fillMaxWidth().clickable(enabled = !busy) { onOpen(document.id) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    document.thumbnail?.let { PageThumbnail(it, Modifier.size(64.dp, 80.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text((if (document.favorite) "★ " else "") + document.title, style = MaterialTheme.typography.titleMedium)
                        Text("${document.count} pages • ${document.category}", style = MaterialTheme.typography.bodyMedium)
                        Text(DateTimeFormatter.ofPattern("d MMM yyyy").format(
                            Instant.ofEpochMilli(document.modified).atZone(ZoneId.systemDefault())),
                            style = MaterialTheme.typography.bodySmall)
                        if (document.tags.isNotBlank()) Text(document.tags.replace(",", " · "), style = MaterialTheme.typography.bodySmall)
                        if (document.snippet.isNotBlank()) Text(document.snippet, maxLines = 3, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
            }
        }
        if (hasMore) item { OutlinedButton(onClick = onMore, enabled = !busy) { Text("Load more") } }
        item {
            Text("Documents are stored on this device. OCR supports Latin text. Scanner setup may need a download.",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRebuild, enabled = !busy) { Text("Rebuild search index") }
        }
    }
}
