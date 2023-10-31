package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*
import org.jetbrains.kotlin.doctor.entity.OS.*

class JavaDiagnostic(private val system: System) : Diagnostic() {
    override val title = "Java"

    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder(title)

        var javaLocation = when(system.currentOS) {
            Linux, MacOS -> system.execute("which", "java").output
//            Windows -> system.execute("(gcm java).Path").output
            Windows -> "${system.getEnvVar("java.home")}\\"
            UNKNOWN -> throw UnsupportedOperationException()
        }
        println("\n Value is $javaLocation")
        val javaVersion = system.execute("java", "--version").output?.lineSequence()?.firstOrNull()

        val systemJavaHome = when(system.currentOS) {
            Linux, MacOS -> system.execute("/usr/libexec/java_home").output
            Windows -> system.execute("\$Env:JAVA_HOME").output
            UNKNOWN -> throw UnsupportedOperationException()
        }
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

        val javaHomeHint = when(system.currentOS) {
            Linux, MacOS -> """
            Consider adding the following to ${system.shell?.profile ?: "your shell profile"} for setting JAVA_HOME
            export JAVA_HOME=${javaLocation.removeSuffix("/bin/java")}
        """.trimIndent()
            Windows -> "Consider adding the `$javaLocation` to your system environment variables, search for 'env' " +
                    "in the Windows search bar. Click on the environment variables and add it in the System variables."
            UNKNOWN -> "Consider adding the `$javaLocation` to your environment variable."
        }

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