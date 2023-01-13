package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class JavaDiagnostic(private val system: System) : Diagnostic() {
    override val title = "Java"

    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder(title)

        var javaLocation = system.execute("which", "java").output
        val javaVersion = system.execute("java", "-version").output?.lineSequence()?.firstOrNull()
        val systemJavaHome = system.execute("/usr/libexec/java_home").output
        val javaHome = system.getEnvVar("JAVA_HOME")
        if (javaLocation == "/usr/bin/java") {
            javaLocation = when {
                javaHome?.isNotBlank() == true -> javaHome.plus("/bin/java")
                systemJavaHome?.isNotBlank() == true -> systemJavaHome.plus("/bin/java")
                else -> javaLocation
            }
        }
        if (javaLocation.isNullOrBlank() || javaVersion.isNullOrBlank()) {
            result.addFailure(
                "Java not found",
                "Get JDK from https://www.oracle.com/java/technologies/javase-downloads.html"
            )
            return result.build()
        }

        val java = Application("Java", Version(javaVersion), javaLocation)
        result.addSuccess("${java.name} (${java.version})\nLocation: ${java.location}")
        result.addEnvironment(EnvironmentPiece.Jdk(java.version))

        val javaHomeHint = """
            Consider adding the following to ${system.shell?.profile ?: "your shell profile"} for setting JAVA_HOME
            export JAVA_HOME=${javaLocation.removeSuffix("/bin/java")}
        """.trimIndent()

        if (javaHome.isNullOrBlank()) {
            result.addInfo("JAVA_HOME is not set", javaHomeHint)
        } else {
            val javaHomeCanonical = javaHome.removeSuffix("/")
            val javaCmdLocations =
                listOf(javaHomeCanonical, "$javaHomeCanonical/bin/java", "$javaHomeCanonical/bin/jre/sh/java")
            if (javaCmdLocations.none { system.fileExists(it) }) {
                result.addFailure(
                    "JAVA_HOME is set to an invalid directory",
                    "JAVA_HOME: $javaHome",
                    javaHomeHint
                )
                return result.build()
            } else {
                result.addSuccess("JAVA_HOME: $javaHome")
                if (javaCmdLocations.none { it == javaLocation }) {
                    result.addInfo(
                        "JAVA_HOME does not match Java binary location",
                        "Java binary location found in PATH: $javaLocation",
                        "Note that, by default, Gradle will use Java environment provided by JAVA_HOME"
                    )
                }
            }
        }

        return result.build()
    }
}