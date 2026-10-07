# DocuFlow

A scanned document is hard to use if you cannot find it again.
DocuFlow turns paper and imported images/PDFs into searchable documents on your device.

**Scan it once. Find it later.**

Scan → Review → OCR → Organize → Search → Export

## Implemented
- ML Kit document scanner with perspective correction, crop and enhancement.
- System image/PDF import, private bounded page storage, up to 50 pages per document.
- Preserved originals, rotation, edge trim, grayscale and high-contrast cleanup.
- Add, replace, remove and reorder pages, with explicit deletion confirmation.
- Bundled Latin OCR with individual page results and cancellation.
- Editable evidence-based naming/category suggestions, tags and favorites.
- SQLite FTS4 search across titles, OCR, tags and categories, with snippets and filters.
- Document/page viewer and selectable OCR text.
- Standard/high-quality multipage A4 PDF export through the system file picker.
- System PDF/image sharing via temporary FileProvider grants.

## Architecture and privacy
Existing single-module Kotlin/Compose project and toolchain preserved.
Platform SQLite/PdfDocument/PdfRenderer/SAF reused. Only scanner and OCR SDKs added.
Documents stay in private app storage. No account or document upload.
Backups/device transfers excluded; app network permissions removed.
See [architecture](docs/architecture.md), [pipeline](docs/document-pipeline.md),
[OCR](docs/ocr.md), [search](docs/search.md), [privacy](docs/privacy.md),
[performance](docs/performance.md) and [decisions](docs/decisions).

## Limitations
Latin OCR does not support Persian/Arabic. Scanner needs compatible Google Play Services,
at least 1.7GB RAM, and may download components on first use. File import works separately.
PDF imports become page images; original PDF text/attachments are not retained.
Generated PDFs have no searchable text layer. No extra document encryption.
No collection performance benchmark or device/manual validation has been performed.
Cancellation may wait for the current native operation. Partial system exports may
need to be removed by the user. Uninstall removes the local library.

## Build and verification
Use the existing JDK/toolchain 25 and Android SDK 37: `./gradlew assembleDebug`.
Tests are intentionally not run at the user's request. Build status is tracked in
[implementation](docs/implementation.md). Template tests are unchanged.
Manual checklist: scan/import, edit/reorder/replace, OCR, rename/tag/filter/search,
PDF save/share, delete, reindex, rotate/recreate Activity, cancellation, corrupt files,
low storage and large collections.

## Roadmap
Additional OCR scripts, searchable PDF text layers, real-device profiling and
instrumented accessibility/lifecycle verification.

## License
No project license has been selected by the repository owner.
Google SDKs are subject to their own terms; official sample licensing is documented
in the scanner decision. No third-party application source was copied.
