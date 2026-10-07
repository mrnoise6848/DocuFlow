package com.noise.docuflow.data

data class Page(val id: String, val source: String, val image: String = source,
    val text: String = "", val ocr: String = "Pending")
data class Document(val id: String, val title: String, val created: Long,
    val modified: Long = created, val category: String = "Other",
    val tags: String = "", val favorite: Boolean = false, val pages: List<Page> = emptyList())
data class DocumentSummary(val id: String, val title: String, val modified: Long,
    val category: String, val tags: String, val favorite: Boolean, val count: Int,
    val thumbnail: String?, val snippet: String = "")
val categories = listOf("Receipt", "Invoice", "Bill", "Contract", "Form", "Note", "Certificate", "ID-related document", "Other")

object PageOrder {
    fun move(pages: List<Page>, from: Int, to: Int): List<Page> =
        pages.toMutableList().apply { if (from in indices && to in indices) add(to, removeAt(from)) }
    fun replace(pages: List<Page>, index: Int, page: Page): List<Page> =
        pages.toMutableList().apply { if (index in indices) this[index] = page }
}
