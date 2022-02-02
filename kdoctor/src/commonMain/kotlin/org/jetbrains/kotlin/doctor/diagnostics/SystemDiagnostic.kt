package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.entity.SystemType

class SystemDiagnostic : Diagnostic("System") {
    override fun runChecks() = listOf(
        Message(
            if (System.type == SystemType.MacOS) ResultType.Success else ResultType.Failure,
            """
                ${System.type} (${System.getVersion() ?: "N/A"})
                ${System.getHardwareInfo() ?: ""}
            """.trimIndent()
        )
    )
}