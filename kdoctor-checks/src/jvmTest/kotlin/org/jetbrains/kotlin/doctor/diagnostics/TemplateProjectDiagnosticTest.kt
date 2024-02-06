package org.jetbrains.kotlin.doctor.diagnostics

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.ProcessResult
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TemplateProjectDiagnosticTest {

    @Test
    fun `check success`() = runTest {
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
    fun `temp dir error`() = runTest {
        val system = object : BaseTestSystem() {
            override suspend fun executeCmd(cmd: String): ProcessResult = when (cmd) {
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
    fun `download template error`() = runTest {
        val system = object : BaseTestSystem() {
            override suspend fun executeCmd(cmd: String): ProcessResult = when (cmd) {
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
                "curl error code = -1",
                "500: Server error"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `unzip template error`() = runTest {
        val system = object : BaseTestSystem() {
            override suspend fun executeCmd(cmd: String): ProcessResult = when (cmd) {
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
    fun `template project build error`() = runTest {
        val system = object : BaseTestSystem() {
            override suspend fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "$tempDir/kdoctor-template/template/gradlew -p $tempDir/kdoctor-template/template clean linkReleaseFrameworkIosArm64 jvmJar --info" -> {
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
    fun `custom template project success`() = runTest {
        val customTag = "test-tag"
        val system = object : BaseTestSystem() {
            override suspend fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "curl --location --silent --show-error --fail --output $tempDir/archive.zip https://github.com/Kotlin/kdoctor/archive/refs/tags/$customTag.zip" -> {
                    ProcessResult(0, "")
                }
                "$tempDir/kdoctor-$customTag/template/gradlew -p $tempDir/kdoctor-$customTag/template clean linkReleaseFrameworkIosArm64 jvmJar --info" -> {
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