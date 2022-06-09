package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class JavaDiagnostic : Diagnostic("Java") {
    override fun runChecks(): List<Message> {
        val messages = mutableListOf<Message>()
        var javaLocation = System.execute("which", "java").output
        val javaVersionCmd = System.execute("java", "-version")
        val javaVersion = if (javaVersionCmd.code == 0) javaVersionCmd.error?.lineSequence()?.firstOrNull() else null
        val systemJavaHome = System.execute("/usr/libexec/java_home").output
        val javaHome = System.getEnvVar("JAVA_HOME")
        if (javaLocation == "/usr/bin/java") {
            javaLocation =
                when {
                    javaHome?.isNotBlank() == true -> javaHome.plus("/bin/java")
                    systemJavaHome?.isNotBlank() == true -> systemJavaHome.plus("/bin/java")
                    else -> javaLocation
                }
        }
        if (javaLocation.isNullOrBlank() || javaVersion.isNullOrBlank()
        ) {
            messages.addFailure(
                "Java not found",
                "Get JDK from https://www.oracle.com/java/technologies/javase-downloads.html"
            )
            return messages
        }

        val java = Application("Java", Version(javaVersion), javaLocation)
        messages.addSuccess("${java.name} (${java.version})\nLocation: ${java.location}")

        if (javaHome.isNullOrBlank()) {
            messages.addInfo("JAVA_HOME is not set", javaHomeHint(javaLocation))
        } else {
            val javaHomeCanonical = javaHome.removeSuffix("/")
            val javaCmdLocations = listOf("$javaHomeCanonical/bin/java", "$javaHomeCanonical/bin/jre/sh/java")
            if (javaCmdLocations.none { System.fileExists(it) }) {
                messages.addFailure(
                    "JAVA_HOME is set to an invalid directory: $javaHome",
                    javaHomeHint(javaLocation)
                )
            } else {
                messages.addSuccess("JAVA_HOME=$javaHome")
                if (javaCmdLocations.none { it == javaLocation }) {
                    messages.addInfo(
                        """
                            JAVA_HOME does not match Java binary location found in PATH: $javaLocation
                            Note that, by default, Gradle will use Java environment provided by JAVA_HOME 
                        """.trimIndent()
                    )
                }
                if (javaHome != systemJavaHome) {
                    val xcodeJavaHome =
                        System.execute(
                            "defaults",
                            "read",
                            "com.apple.dt.Xcode",
                            "IDEApplicationwideBuildSettings"
                        ).output?.lines()?.lastOrNull { it.contains("\"JAVA_HOME\"") }?.split("=")?.lastOrNull()
                            ?.trim(' ', '"', ';')
                    if (xcodeJavaHome == null) {
                        messages.addInfo(
                            """
                            Note that, by default, Xcode uses Java environment returned by /usr/libexec/java_home:
                            $systemJavaHome
                            It does not match current JAVA_HOME environment variable:
                            $javaHome
                        """.trimIndent(),
                            """
                            Set JAVA_HOME in Xcode -> Preferences -> Locations -> Custom Paths to
                            $javaHome
                        """.trimIndent()
                        )
                    } else if (javaHome != xcodeJavaHome) {
                        messages.addInfo(
                            """
                            Xcode JAVA_HOME is set to
                            $xcodeJavaHome
                            It does not match current JAVA_HOME environment variable:
                            $javaHome 
                            """.trimIndent(),
                            "Set JAVA_HOME in Xcode -> Preferences -> Locations -> Custom Paths to $javaHome"
                        )
                    }

                }

                messages.addInfo(
                    "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                    "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
                )
            }
        }

        return messages
    }

    private fun javaHomeHint(javaLocation: String?) = """
        Consider adding the following to ${System.getShell()?.profile ?: "your shell profile"} for setting JAVA_HOME
        export JAVA_HOME=${javaLocation?.removeSuffix("/bin/java") ?: "<path to java>"}
    """.trimIndent()
}