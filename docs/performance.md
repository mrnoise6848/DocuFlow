# Performance design
Library/search reads at most 100 summaries per batch and avoids OCR bodies.
Queries debounce 250ms and use FTS4. LazyColumn keys are stable document/page IDs.
Thumbnails decode off-main to <=600px, with an 8MB LRU cache. Normalized images
are <=2400px and PDF import pages <=2000px. Processing is serial, one page at a time.
Exports offer standard/high resolution, with a 50-page bound. PdfDocument retains
native drawing data; real-device memory profiling remains required.
No collection-size performance measurements are claimed. Cache exports expire
after 24h on the next application start. Cancellation may wait for one native operation.
