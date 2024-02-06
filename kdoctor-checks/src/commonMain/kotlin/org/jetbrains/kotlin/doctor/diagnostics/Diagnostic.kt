package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Diagnosis

abstract class Diagnostic {
    abstract val title: String
    abstract suspend fun diagnose(): Diagnosis
}
