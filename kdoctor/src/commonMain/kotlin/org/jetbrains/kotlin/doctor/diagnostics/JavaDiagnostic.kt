package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Application
import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.entity.Version
import org.jetbrains.kotlin.doctor.entity.execute
import org.jetbrains.kotlin.doctor.entity.fileExists
import org.jetbrains.kotlin.doctor.entity.getEnvVar

class JavaDiagnostic : Diagnostic("Java") {
    override fun runChecks(): List<Message> {
        val messages = mutableListOf<Message>()

        var javaLocation = System.execute("which", "java").output
        if (javaLocation == "/usr/bin/java") {
            javaLocation = System.execute("/usr/libexec/java_home").output?.plus("/bin/java")
        }

        val javaVersion = System.execute("java", "--version").output?.lineSequence()?.firstOrNull()
        var java: Application? = null
        if (javaLocation.isNullOrBlank() || javaLocation.contains("not found", ignoreCase = true) ||
            javaVersion.isNullOrBlank() || javaVersion.contains("unable to locate", ignoreCase = true)
        ) {
            messages.addFailure(
                "Java not found",
                "Get JDK from https://www.oracle.com/java/technologies/javase-downloads.html"
            )
        } else {
            java = Application("Java", Version(javaVersion), javaLocation)
            messages.addSuccess("${java.name} (${java.version})\nLocation: ${java.location}")
        }

        val javaHome = System.getEnvVar("JAVA_HOME")
        if (javaHome.isNullOrBlank()) {
            messages.addInfo("JAVA_HOME is not set", javaHomeHint(java))
        } else {
            val javaHomeCanonical = javaHome.removeSuffix("/")
            val javaCmdLocations = listOf("$javaHomeCanonical/bin/java", "$javaHomeCanonical/bin/jre/sh/java")
            if (javaCmdLocations.none { System.fileExists(it) }) {
                messages.addFailure(
                    "JAVA_HOME is set to an invalid directory: $javaHome",
                    javaHomeHint(java)
                )
            } else {
                messages.addSuccess("JAVA_HOME=$javaHome")
                if (javaCmdLocations.none { it == javaLocation }) {
                    messages.addWarning(
                        "JAVA_HOME does not match current java installation",
                        javaHomeHint(java)
                    )
                }
            }
        }

        return messages
    }

    private fun javaHomeHint(java: Application?) = """
        Consider adding the following to ${System.getShell()?.profile ?: "your shell profile"}
        export JAVA_HOME=${java?.location?.removeSuffix("/bin/java") ?: "<path to java>"}
    """.trimIndent()
}