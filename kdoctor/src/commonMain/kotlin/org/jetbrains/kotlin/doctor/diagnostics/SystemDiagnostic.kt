package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.OS
import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.entity.currentOS

class SystemDiagnostic : Diagnostic("System") {

    override fun runChecks() = buildList {
        add(
            Message(
                resultType = if (System.currentOS == OS.MacOS) ResultType.Success else ResultType.Failure,
                text = """
                    OS: ${System.currentOS} (${System.getVersion() ?: "N/A"})
                    ${System.getCPUInfo() ?: ""}
                """.trimIndent()
            )
        )

        if (System.isUsingRosetta) {
            this.addInfo(
                title = "You are currently using Rosetta 2.",
                "It may cause some issues while trying to install packages using Homebrew.",
                "Consider switching off Rosetta 2 or ignore this message in case you actually need it."
            )
        }
    }
}
