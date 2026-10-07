# Existing project and integration

Phase 1 inspection: single Android application module, package com.noise.docuflow;
Compose Activity and Material3 theme, no existing navigation, persistence or document workflow.
Gradle 9.8.0; AGP 9.4.1; Kotlin/Compose plugin 2.4.20; Compose BOM 2026.09.00;
SDK 37, minimum 29; Java source/target 11; daemon toolchain 25. All preserved.
Existing Activity Compose, Core KTX and Lifecycle Runtime KTX are reused.
Template unit/instrumentation tests exist; no tests are run at the user's request.

Integration: data package owns SQLite metadata/FTS and private files, processing package
owns page transformations, local OCR and PDF. An Activity-scoped controller exposes
Compose state and runs bounded background operations. Existing Material3 theme is retained.
No backend, new module, architecture migration or dependency version upgrades.
