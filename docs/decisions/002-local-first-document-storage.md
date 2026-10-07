# Private local storage
App filesDir/pages owns normalized sources and edited JPEGs. Content URIs are copied
immediately, avoiding fragile external path or permission assumptions. SQLite and
FTS hold metadata and derived text. PDF exports use private cache or user-selected SAF.
No server or user account.
