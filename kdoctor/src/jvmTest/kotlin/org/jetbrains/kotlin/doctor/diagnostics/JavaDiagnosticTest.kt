package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.jetbrains.kotlin.doctor.entity.ProcessResult
import org.jetbrains.kotlin.doctor.entity.Version
import org.junit.Test
import kotlin.test.assertEquals


internal class JavaDiagnosticTest {

    @Test
    fun `check success`() {
        val system = object : BaseTestSystem() {}
        val diagnose = JavaDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Java").apply {
            addSuccess(
                "Java (openjdk version \"11.0.16\" 2022-07-19 LTS)",
                "Location: /Users/my/.sdkman/candidates/java/current/bin/java"
            )
            addSuccess(
                "JAVA_HOME: /Users/my/.sdkman/candidates/java/current"
            )
            addInfo(
                "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
            )
            addEnvironment(
                EnvironmentPiece.Jdk(Version("openjdk version \"11.0.16\" 2022-07-19 LTS"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check Xcode system JAVA_HOME info`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "defaults read com.apple.dt.Xcode IDEApplicationwideBuildSettings" -> {
                    ProcessResult(0, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = JavaDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Java").apply {
            addSuccess(
                "Java (openjdk version \"11.0.16\" 2022-07-19 LTS)",
                "Location: /Users/my/.sdkman/candidates/java/current/bin/java"
            )
            addSuccess(
                "JAVA_HOME: /Users/my/.sdkman/candidates/java/current"
            )
            addInfo(
                "Xcode JAVA_HOME does not match the environment variable",
                "Xcode JAVA_HOME: /Users/my/Library/Java/JavaVirtualMachines/jbr-17.0.5/Contents/Home",
                "System JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Set JAVA_HOME in Xcode -> Preferences -> Locations -> Custom Paths",
            )
            addInfo(
                "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
            )
            addEnvironment(
                EnvironmentPiece.Jdk(Version("openjdk version \"11.0.16\" 2022-07-19 LTS"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check Xcode custom JAVA_HOME info`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "defaults read com.apple.dt.Xcode IDEApplicationwideBuildSettings" -> {
                    ProcessResult(0, "\"JAVA_HOME\"=/custom/path;")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = JavaDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Java").apply {
            addSuccess(
                "Java (openjdk version \"11.0.16\" 2022-07-19 LTS)",
                "Location: /Users/my/.sdkman/candidates/java/current/bin/java"
            )
            addSuccess(
                "JAVA_HOME: /Users/my/.sdkman/candidates/java/current"
            )
            addInfo(
                "Xcode JAVA_HOME does not match the environment variable",
                "Xcode JAVA_HOME: /custom/path",
                "System JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Set JAVA_HOME in Xcode -> Preferences -> Locations -> Custom Paths",
            )
            addInfo(
                "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
            )
            addEnvironment(
                EnvironmentPiece.Jdk(Version("openjdk version \"11.0.16\" 2022-07-19 LTS"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check no java`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "which java" -> {
                    ProcessResult(0, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = JavaDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Java").apply {
            addFailure(
                "Java not found",
                "Get JDK from https://www.oracle.com/java/technologies/javase-downloads.html"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check no JAVA_HOME`() {
        val system = object : BaseTestSystem() {
            override fun getEnvVar(name: String): String? = when (name) {
                "JAVA_HOME" -> null
                else -> super.getEnvVar(name)
            }
        }
        val diagnose = JavaDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Java").apply {
            addSuccess(
                "Java (openjdk version \"11.0.16\" 2022-07-19 LTS)",
                "Location: /Users/my/.sdkman/candidates/java/current/bin/java"
            )
            addInfo(
                "JAVA_HOME is not set",
                "Consider adding the following to ~/.zprofile for setting JAVA_HOME",
                "export JAVA_HOME=/Users/my/.sdkman/candidates/java/current",
            )
            addInfo(
                "Xcode JAVA_HOME does not match the environment variable",
                "Xcode JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Set JAVA_HOME in Xcode -> Preferences -> Locations -> Custom Paths",
            )
            addInfo(
                "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
            )
            addEnvironment(
                EnvironmentPiece.Jdk(Version("openjdk version \"11.0.16\" 2022-07-19 LTS"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check invalid JAVA_HOME`() {
        val system = object : BaseTestSystem() {
            override fun getEnvVar(name: String): String? = when (name) {
                "JAVA_HOME" -> "/wrong/path"
                else -> super.getEnvVar(name)
            }
        }
        val diagnose = JavaDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Java").apply {
            addSuccess(
                "Java (openjdk version \"11.0.16\" 2022-07-19 LTS)",
                "Location: /Users/my/.sdkman/candidates/java/current/bin/java",
            )
            addFailure(
                "JAVA_HOME is set to an invalid directory",
                "JAVA_HOME: /wrong/path",
                "Consider adding the following to ~/.zprofile for setting JAVA_HOME",
                "export JAVA_HOME=/Users/my/.sdkman/candidates/java/current",
            )
            addEnvironment(
                EnvironmentPiece.Jdk(Version("openjdk version \"11.0.16\" 2022-07-19 LTS"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check JAVA_HOME != java location`() {
        val system = object : BaseTestSystem() {
            override fun getEnvVar(name: String): String? = when (name) {
                "JAVA_HOME" -> "/some/path"
                else -> super.getEnvVar(name)
            }

            override fun fileExists(path: String): Boolean = when (path) {
                "/some/path" -> true
                else -> super.fileExists(path)
            }
        }
        val diagnose = JavaDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Java").apply {
            addSuccess(
                "Java (openjdk version \"11.0.16\" 2022-07-19 LTS)",
                "Location: /Users/my/.sdkman/candidates/java/current/bin/java"
            )
            addSuccess(
                "JAVA_HOME: /some/path",
            )
            addInfo(
                "JAVA_HOME does not match Java binary location",
                "Java binary location found in PATH: /Users/my/.sdkman/candidates/java/current/bin/java",
                "Note that, by default, Gradle will use Java environment provided by JAVA_HOME",
            )
            addInfo(
                "Xcode JAVA_HOME does not match the environment variable",
                "Xcode JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "System JAVA_HOME: /some/path",
                "Set JAVA_HOME in Xcode -> Preferences -> Locations -> Custom Paths",
            )
            addInfo(
                "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
            )
            addEnvironment(
                EnvironmentPiece.Jdk(Version("openjdk version \"11.0.16\" 2022-07-19 LTS"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }
}
