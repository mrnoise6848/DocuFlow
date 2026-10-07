# DocuFlow

**Turn scanned paper and imported files into a document library you can search on your device.**

A photo of a receipt, letter or form preserves its appearance, but finding it later still depends on remembering a filename or browsing thumbnails. DocuFlow connects capture to retrieval: page cleanup and Latin OCR feed a local full-text index, so words inside a document become a way to find it again.

The workflow keeps originals, editable page images and recognized text together. Documents can be named, tagged, categorized, marked as favorites and exported or shared without an account or document-upload service.

## Capture is only the beginning

```text
ML Kit scanner / system image picker / PDF import
    → private page sources, with originals preserved
    → review, rotate, trim, filter and reorder
    → per-page Latin OCR
    → title + text + tags + category in SQLite FTS4
    → search with snippets → view → PDF export or sharing
```

The scanner supplies perspective correction and enhancement. File import is a separate route for existing images and PDFs. Editing supports adding, replacing and deleting pages, with up to 50 pages per document. Naming and category suggestions remain editable.

Search covers both metadata and recognized page text. Results include snippets from indexed text and filters for organization; users do not have to open every document to inspect its contents. See [document pipeline](docs/document-pipeline.md), [OCR](docs/ocr.md) and [search](docs/search.md).

## Keeping files, text and search consistent

The engineering challenge is that a document spans private image files, ordered page records, OCR results and a derived search index.

- **Transactional metadata:** page order, document metadata and FTS updates share a SQLite transaction. Old files are deleted after metadata commits; unreferenced files are recovered after 24 hours. Missing pages remain visible as unavailable.
- **Cancellation at safe boundaries:** imports clean up uncommitted files. A native OCR operation is awaited before releasing its bitmap and recognizer; cancellation is checked before publishing its result. Short persistence steps finish in a non-cancellable section.
- **Bounded processing:** PDFs are copied to seekable private temporary storage with a 150 MB import limit, then rasterized one page at a time. Library queries paginate results without loading full OCR bodies.
- **Recoverable search:** the index can be rebuilt from stored documents. Search sanitizes word tokens and binds parameters instead of exposing arbitrary FTS syntax.

These choices are visible in [DocumentController](app/src/main/java/com/noise/docuflow/DocumentController.kt), [DocumentStore](app/src/main/java/com/noise/docuflow/data/DocumentStore.kt), [PDF import](app/src/main/java/com/noise/docuflow/processing/PdfImporter.kt) and [OCR processing](app/src/main/java/com/noise/docuflow/processing/OcrProcessor.kt). The app uses one Kotlin/Compose module and Android's SQLite, PDF and file-picker APIs. See [architecture](docs/architecture.md) and [decisions](docs/decisions/).

## What stays local—and what export means

Documents reside in private app storage. The app declares no network permission and excludes its library from backup/device transfer. The ML Kit scanner can download components through Google Play Services on first use. Sharing explicitly grants another app temporary access through FileProvider. See [privacy](docs/privacy.md).

PDF export produces multipage A4 pages in standard or high quality. **The exported PDF has no searchable text layer**: OCR search belongs to the local library. Imported PDFs are rasterized; their original text layer and attachments are not retained.

## Try the workflow

Use the JDK and Android SDK versions configured in the existing project (JDK 25, SDK 37):

```bash
./gradlew assembleDebug
```

Scan or import a document, run OCR, search for a word inside a page, edit its title/tags, and export it through the system file picker. Check cancellation, missing/corrupt files and page replacement before relying on a large library.

The [implementation record](docs/implementation.md) reports a successful debug build and lint pass, with template warnings. Device/manual validation, collection benchmarks and a substantive automated test suite remain absent. No screenshots or benchmark results are supplied here.

## Constraints

Latin OCR does not cover Persian/Arabic. Scanner availability depends on compatible Google Play Services and device resources; file import works separately. There is no additional document encryption or sync service. Cancellation can wait for native work; interrupted system exports may leave a partial file to remove. Uninstalling removes the local library.

No project license has been selected. Google SDK terms and sample licensing are documented in the scanner decision.
