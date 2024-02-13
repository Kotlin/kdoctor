package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Application
import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.System

class XcodeDiagnosticStandlone(system: System) : XcodeDiagnosticBase(system) {
    override suspend fun diagnoseInstallations(xcodeInstallations: List<Application>, result: Diagnosis.Builder) {
        super.diagnoseInstallations(xcodeInstallations, result)

        if (xcodeInstallations.isNotEmpty()) {
            val xcodeJavaHome =
                system.execute("defaults", "read", "com.apple.dt.Xcode", "IDEApplicationwideBuildSettings").output
                    ?.lines()
                    ?.lastOrNull { it.contains("\"JAVA_HOME\"") }
                    ?.split("=")
                    ?.lastOrNull()
                    ?.trim(' ', '"', ';')
                    ?: system.execute("/usr/libexec/java_home").output
            if (xcodeJavaHome != null) {
                result.addInfo(
                    "Xcode JAVA_HOME: $xcodeJavaHome",
                    "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
                )
            }
        }
    }
}