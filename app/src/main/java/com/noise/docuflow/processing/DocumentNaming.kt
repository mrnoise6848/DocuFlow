package com.noise.docuflow.processing

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DocumentNaming {
    fun suggest(text: String, created: Long): String {
        val first = text.lineSequence().map { it.trim() }
            .firstOrNull { it.length in 5..70 && it.any(Char::isLetter) }
        return first ?: "Scanned Document — " + DateTimeFormatter.ISO_LOCAL_DATE.format(
            Instant.ofEpochMilli(created).atZone(ZoneId.systemDefault()))
    }
}
