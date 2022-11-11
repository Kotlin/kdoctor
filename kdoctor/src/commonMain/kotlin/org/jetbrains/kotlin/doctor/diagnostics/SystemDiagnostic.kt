package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class SystemDiagnostic : Diagnostic() {
    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder("System")
        val os = System.currentOS
        val version = System.getVersion()

        if (os == OS.MacOS && version != null) {
            result.addSuccess("OS: $os ($version)\n${System.getCPUInfo().orEmpty()}")
            result.addEnvironment(setOf(EnvironmentPiece.Macos(version)))

            if (System.isUsingRosetta) {
                result.addInfo(
                    title = "You are currently using Rosetta 2.",
                    "It may cause some issues while trying to install packages using Homebrew.",
                    "Consider switching off Rosetta 2 or ignore this message in case you actually need it."
                )
            }
        } else {
            result.addFailure("OS: $os")
        }
        return result.build()
    }
}
