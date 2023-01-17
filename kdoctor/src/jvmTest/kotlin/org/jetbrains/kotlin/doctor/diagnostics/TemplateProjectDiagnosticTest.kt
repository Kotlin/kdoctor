package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.ProcessResult
import org.junit.Test
import kotlin.test.assertEquals

internal class TemplateProjectDiagnosticTest {

    @Test
    fun `check success`() {
        val system = object : BaseTestSystem() {}
        val diagnose = TemplateProjectDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Synthetic generated project").apply {
            addSuccess(
                "Template project build was successful"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `temp dir error`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "mktemp -d" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = TemplateProjectDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Synthetic generated project").apply {
            addFailure(
                "Error: impossible to create temporary directory",
                "Check your file system write permissions"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `download template error`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "curl --location --silent --show-error --fail --output $tempDir/archive.zip https://github.com/Kotlin/kdoctor/archive/refs/tags/template.zip" -> {
                    ProcessResult(-1, "500: Server error")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = TemplateProjectDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Synthetic generated project").apply {
            addFailure(
                "Error: impossible to download a template project",
                "500: Server error"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `unzip template error`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "unzip $tempDir/archive.zip -d $tempDir" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = TemplateProjectDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Synthetic generated project").apply {
            addFailure(
                "Error: impossible to unzip a template project"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `template project build error`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "$tempDir/kdoctor-template/template/gradlew -p $tempDir/kdoctor-template/template clean linkReleaseFrameworkIosArm64 jvmJar" -> {
                    ProcessResult(-1, "BUILD FAIL")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = TemplateProjectDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Synthetic generated project").apply {
            addFailure(
                "Template project build has problems:",
                "BUILD FAIL"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `custom template project success`() {
        val customTag = "test-tag"
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "curl --location --silent --show-error --fail --output $tempDir/archive.zip https://github.com/Kotlin/kdoctor/archive/refs/tags/$customTag.zip" -> {
                    ProcessResult(0, "")
                }
                "$tempDir/kdoctor-$customTag/template/gradlew -p $tempDir/kdoctor-$customTag/template clean linkReleaseFrameworkIosArm64 jvmJar" -> {
                    ProcessResult(0, "BUILD SUCCESSFUL")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = TemplateProjectDiagnostic(system, customTag).diagnose()

        val expected = Diagnosis.Builder("Synthetic generated project").apply {
            addSuccess(
                "Template project build was successful"
            )
        }.build()

        assertEquals(expected, diagnose)
    }
}