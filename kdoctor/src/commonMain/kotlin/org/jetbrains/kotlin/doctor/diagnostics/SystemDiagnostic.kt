package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.entity.SystemType

class SystemDiagnostic : Diagnostic("System") {
    override fun runChecks() = listOf(
        Message(
            if (System.type == SystemType.MacOS) Result.Success else Result.Failure,
            """
                ${System.type} (${System.getVersion()})
                ${System.getHardwareInfo()}
            """.trimIndent()
        )
    )
}