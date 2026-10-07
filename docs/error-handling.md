# Failure and lifecycle behavior
Scanner cancellation leaves documents unchanged. Import processes items independently
and reports skipped items; all-failed imports create no metadata. New files are cleaned
if import/edit fails before commit. PDF export never silently skips missing pages.
Native processing is bounded. Cancellation commits already completed OCR pages and
stops before the next page. Activity destruction cancels work; database documents
survive, and the selected document ID is restored. An interrupted SAF export may
leave a partial destination file, which the user can remove or overwrite.
No external exception strings or document content are logged or surfaced.
