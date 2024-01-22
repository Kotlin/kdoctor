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

            override fun fileExists(path: String): Boolean = when (path) {
                "/wrong/path",
                "/wrong/path/bin/java",
                "/wrong/path/bin/jre/sh/java" -> false
                else -> super.fileExists(path)
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
            addEnvironment(
                EnvironmentPiece.Jdk(Version("openjdk version \"11.0.16\" 2022-07-19 LTS"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }
}
