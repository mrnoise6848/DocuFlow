# Document pipeline and storage
Scanner JPEG/SAF image/PDF → private bounded JPEG source → editable rendered page →
per-page OCR → metadata/FTS → PDF or granted share URI.
Sources are preserved when filtering/rotating/cropping, so original can be restored.
PDF imports render one page at a time; PDF attachments/text layers are not retained.
Document limits: 50 pages; PDF import 150 MB. Unsupported/encrypted PDFs report errors.
SQLite stores metadata and derived OCR separately from private files. Transactions
keep page order and index consistent. Unreferenced old files are recovered after 24h;
missing source files remain visible as unavailable and can be replaced by the user.
Explicit document/page deletion removes private files after metadata commits.
