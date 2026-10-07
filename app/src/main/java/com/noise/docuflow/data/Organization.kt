package com.noise.docuflow.data

object Organization {
    fun tags(input: String): String = input.split(',').map { it.trim().take(40) }
        .filter { it.isNotBlank() }.distinct().take(20).joinToString(",")
    fun update(document: Document, title: String, category: String, tags: String, favorite: Boolean) =
        document.copy(title = title.trim().take(120).ifBlank { document.title },
            category = category.takeIf { it in categories } ?: "Other",
            tags = tags(tags), favorite = favorite, modified = System.currentTimeMillis())
}
