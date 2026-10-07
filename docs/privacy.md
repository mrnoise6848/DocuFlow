# Privacy
DocuFlow has no account, backend, analytics or cloud document processing. Documents,
titles, tags and OCR stay in app-private files/database until the user exports/shares.
App INTERNET and ACCESS_NETWORK_STATE permissions are explicitly removed from
dependency manifests. Backups and device transfers exclude all app document data.
FileProvider is unexported with narrowly scoped page/export directories and temporary
read grants. No OCR, filenames, URIs or document content are logged.

Google scanner runs through Play Services, which may download its components on first
use. Bundled Latin OCR runs locally. ML Kit SDK terms/privacy still apply, including
SDK operational diagnostics documented by Google; this is not a claim of zero SDK telemetry.
Google terms: https://developers.google.com/ml-kit/terms
The app adds no content upload calls. User-chosen share targets receive exported content.
There is no additional at-rest encryption beyond Android app sandbox/device encryption.
Uninstalling the app removes its library; exported copies remain where the user saved them.
