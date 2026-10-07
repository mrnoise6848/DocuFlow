package com.noise.docuflow.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

class DocumentStore(context: Context) : SQLiteOpenHelper(context, "documents.db", null, 1) {
    private val pageDirectory = File(context.filesDir, "pages")
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE documents(id TEXT PRIMARY KEY,title TEXT NOT NULL,created INTEGER,modified INTEGER,category TEXT,tags TEXT,favorite INTEGER)")
        db.execSQL("CREATE TABLE pages(id TEXT PRIMARY KEY,document_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,position INTEGER,source TEXT,image TEXT,text TEXT,ocr TEXT)")
        db.execSQL("CREATE INDEX pages_document ON pages(document_id,position)")
        createIndex(db)
    }
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    private fun createIndex(db: SQLiteDatabase) {
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS search USING fts4(document_id UNINDEXED,title,body,tags,category,tokenize=unicode61)")
    }
    private inline fun transaction(block: (SQLiteDatabase) -> Unit) {
        val db = writableDatabase
        db.beginTransaction()
        try { block(db); db.setTransactionSuccessful() } finally { db.endTransaction() }
    }
    fun save(document: Document) = transaction { db ->
        val values = ContentValues().apply {
            put("id", document.id); put("title", document.title); put("created", document.created)
            put("modified", document.modified); put("category", document.category)
            put("tags", document.tags); put("favorite", if (document.favorite) 1 else 0)
        }
        // REPLACE would delete pages through the foreign key; explicitly update instead.
        if (db.update("documents", values, "id=?", arrayOf(document.id)) == 0) db.insertOrThrow("documents", null, values)
        db.delete("pages", "document_id=?", arrayOf(document.id))
        document.pages.forEachIndexed { index, page ->
            db.insertOrThrow("pages", null, ContentValues().apply {
                put("id", page.id); put("document_id", document.id); put("position", index)
                put("source", page.source); put("image", page.image); put("text", page.text); put("ocr", page.ocr)
            })
        }
        index(db, document)
    }
    private fun index(db: SQLiteDatabase, document: Document) {
        db.delete("search", "document_id=?", arrayOf(document.id))
        db.insertOrThrow("search", null, ContentValues().apply {
            put("document_id", document.id); put("title", document.title)
            put("body", document.pages.joinToString("\n") { it.text })
            put("tags", document.tags); put("category", document.category)
        })
    }
    fun get(id: String): Document? {
        val db = readableDatabase
        val document = db.rawQuery("SELECT * FROM documents WHERE id=?", arrayOf(id)).use { c ->
            if (!c.moveToFirst()) return null
            Document(c.getString(0), c.getString(1), c.getLong(2), c.getLong(3), c.getString(4), c.getString(5), c.getInt(6) == 1)
        }
        val pages = db.rawQuery("SELECT id,source,image,text,ocr FROM pages WHERE document_id=? ORDER BY position", arrayOf(id)).use { c ->
            buildList { while (c.moveToNext()) add(Page(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4))) }
        }
        return document.copy(pages = pages)
    }
    fun list(limit: Int = 100, offset: Int = 0): List<DocumentSummary> =
        summaries("", emptyArray(), limit, offset)

    private fun summaries(where: String, arguments: Array<String>, limit: Int, offset: Int, join: String = "", snippet: String = "''"): List<DocumentSummary> {
        val sql = """SELECT d.id,d.title,d.modified,d.category,d.tags,d.favorite,
            (SELECT count(*) FROM pages p WHERE p.document_id=d.id),
            (SELECT image FROM pages p WHERE p.document_id=d.id ORDER BY position LIMIT 1),$snippet
            FROM documents d $join $where ORDER BY d.modified DESC,d.id LIMIT ? OFFSET ?"""
        return readableDatabase.rawQuery(sql, arguments + arrayOf(limit.toString(), offset.toString())).use { c ->
            buildList {
                while (c.moveToNext()) add(DocumentSummary(c.getString(0), c.getString(1), c.getLong(2),
                    c.getString(3), c.getString(4), c.getInt(5) == 1, c.getInt(6), c.getString(7), c.getString(8)))
            }
        }
    }
    fun delete(id: String) {
        val files = get(id)?.pages?.flatMap { listOf(it.source, it.image) }.orEmpty()
        transaction { db ->
            db.delete("search", "document_id=?", arrayOf(id))
            db.delete("documents", "id=?", arrayOf(id))
        }
        files.distinct().forEach { File(it).takeIf { file -> file.parentFile == pageDirectory }?.delete() }
    }
}
