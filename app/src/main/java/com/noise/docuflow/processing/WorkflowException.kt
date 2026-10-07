package com.noise.docuflow.processing

/** Only app-authored messages may be shown; external exception text can contain private URIs. */
class WorkflowException(message: String) : Exception(message)
