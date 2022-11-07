package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Diagnosis

abstract class Diagnostic {
    abstract fun diagnose(): Diagnosis
}
