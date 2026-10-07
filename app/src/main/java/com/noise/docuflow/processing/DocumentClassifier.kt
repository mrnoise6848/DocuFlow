package com.noise.docuflow.processing

data class Classification(val category: String, val evidence: List<String>)
object DocumentClassifier {
    private val rules = linkedMapOf(
        "Invoice" to listOf("invoice", "invoice number"),
        "Receipt" to listOf("receipt", "cash receipt"),
        "Bill" to listOf("bill", "amount due"),
        "Contract" to listOf("contract", "agreement"),
        "Certificate" to listOf("certificate", "certified"),
        "Form" to listOf("application form", "registration form"),
        "ID-related document" to listOf("passport", "identity card"),
        "Note" to listOf("meeting notes", "memo"))
    fun suggest(text: String): Classification {
        val matches = rules.map { (category, words) ->
            Classification(category, words.filter { Regex("(?i)\\b${Regex.escape(it)}\\b").containsMatchIn(text) })
        }.filter { it.evidence.isNotEmpty() }
        return if (matches.size == 1) matches.first() else Classification("Other", emptyList())
    }
}
