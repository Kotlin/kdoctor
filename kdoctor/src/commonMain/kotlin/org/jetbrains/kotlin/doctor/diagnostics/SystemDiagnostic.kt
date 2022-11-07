package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class SystemDiagnostic : Diagnostic() {
    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder("System")
        result.addPlainMessage(
            if (System.currentOS == OS.MacOS) DiagnosisResult.Success else DiagnosisResult.Failure,
            """
                OS: ${System.currentOS} (${System.getVersion() ?: "N/A"})
                ${System.getCPUInfo() ?: ""}
            """.trimIndent()
        )

        if (System.isUsingRosetta) {
            result.addInfo(
                title = "You are currently using Rosetta 2.",
                "It may cause some issues while trying to install packages using Homebrew.",
                "Consider switching off Rosetta 2 or ignore this message in case you actually need it."
            )
        }
        return result.build()
    }
}
