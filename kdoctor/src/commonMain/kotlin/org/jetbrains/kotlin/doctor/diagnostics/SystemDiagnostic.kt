package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.entity.SystemType

class SystemDiagnostic : Diagnostic("System") {
    override fun runChecks() = listOf(
        Message(
            if (System.type == SystemType.MacOS) ResultType.Success else ResultType.Failure,
            """
                OS: ${System.type} (${System.getVersion() ?: "N/A"})
                ${System.getHardwareInfo() ?: ""}
                Using Rosetta 2: ${if (System.isUsingRosetta) "Yes" else "No"}
                
            """.trimIndent()
        )
    )
}