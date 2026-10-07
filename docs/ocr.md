# OCR
Bundled ML Kit Latin text recognition 16.0.1 runs on-device without model download.
Persian/Arabic OCR is not supported by this model. Empty text is not proof of an empty
page: low quality or unsupported scripts may also produce no text. Each page has
its own status and can be retried. Edited pages invalidate old OCR. No text is fabricated.
Task cancellation waits for the current native page, releases its bitmap/client,
and suppresses its result. SDK terms: https://developers.google.com/ml-kit/terms
API: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
