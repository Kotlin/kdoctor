package org.jetbrains.kotlin.doctor

import org.jetbrains.kotlin.doctor.diagnostics.JavaDiagnostic
import org.jetbrains.kotlin.doctor.entity.ProcessResult
import org.jetbrains.kotlin.doctor.printer.TextPainter.removeColors
import org.junit.Test
import kotlin.test.assertEquals


internal class JavaDiagnosticReportTest {

    @Test
    fun `check success`() {
        val system = object : BaseTestSystem() {}
        val diagnostic = JavaDiagnostic(system)
        val doctor = Doctor(system)
        val report = doctor.makeDiagnosticsReport(listOf(diagnostic.diagnose()), true)
        assertEquals(
            """
            [✓] Java
              ➤ Java (openjdk version "11.0.16" 2022-07-19 LTS)
                Location: /Users/my/.sdkman/candidates/java/current/bin/java
              ➤ JAVA_HOME: /Users/my/.sdkman/candidates/java/current
              i Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.
                Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section
        """.trimIndent(), report.removeColors()
        )
    }

    @Test
    fun `check Xcode java info`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "defaults read com.apple.dt.Xcode IDEApplicationwideBuildSettings" -> {
                    ProcessResult(0, null)
                }

                else -> super.executeCmd(cmd)
            }
        }

        val diagnostic = JavaDiagnostic(system)
        val doctor = Doctor(system)
        val report = doctor.makeDiagnosticsReport(listOf(diagnostic.diagnose()), true)
        assertEquals(
            """
            [✓] Java
              ➤ Java (openjdk version "11.0.16" 2022-07-19 LTS)
                Location: /Users/my/.sdkman/candidates/java/current/bin/java
              ➤ JAVA_HOME: /Users/my/.sdkman/candidates/java/current
              i Xcode JAVA_HOME does not match the environment variable
                Xcode JAVA_HOME: /Users/my/Library/Java/JavaVirtualMachines/jbr-17.0.5/Contents/Home
                System JAVA_HOME: /Users/my/.sdkman/candidates/java/current
                Set JAVA_HOME in Xcode -> Preferences -> Locations -> Custom Paths
              i Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.
                Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section
        """.trimIndent(), report.removeColors()
        )
    }
}
