package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class SystemDiagnostic(private val system: System) : Diagnostic() {
    override val title = "Operation System"

    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder(title)
        val os = system.currentOS
        val version = system.osVersion

        when(os) {
            OS.MacOS -> {
                if (version != null) {
                    result.addSuccess("Version OS: $os $version", system.cpuInfo?.let { "CPU: $it" }.orEmpty())
                    result.addEnvironment(EnvironmentPiece.Macos(version))

                    if (system.isUsingRosetta()) {
                        result.addInfo(
                            "You are currently using Rosetta 2.",
                            "It may cause some issues while trying to install packages using Homebrew.",
                            "Consider switching off Rosetta 2 or ignore this message in case you actually need it."
                        )
                    }
                    return result.build()
                }
                result.addFailure("OS: $os $version")
            }
            OS.Windows -> {
                result.addSuccess("Version OS: $os $version", system.cpuInfo?.let { "CPU: $it" }.orEmpty())
            }
            OS.Linux -> {
                result.addSuccess("Version OS: $os $version", system.cpuInfo?.let { "CPU: $it" }.orEmpty())
            }

            else -> {
                result.addFailure("OS: $os $version")
            }
        }
        return result.build()
    }
}
