# Local full-text search
SQLite FTS4 with unicode61 indexes titles, recognized page text, tags and categories.
Queries use bound parameters and sanitized Unicode word tokens, AND prefix matching.
Snippets are real indexed text. Results are paginated; OCR bodies are not loaded
into library listings. Metadata/page/index updates share one transaction.
Rebuild index reads one document at a time. Search does not interpret arbitrary FTS syntax.
