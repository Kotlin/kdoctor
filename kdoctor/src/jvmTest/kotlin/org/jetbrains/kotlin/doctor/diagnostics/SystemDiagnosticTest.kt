package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*
import org.junit.Test
import kotlin.test.assertEquals


internal class SystemDiagnosticTest {

    @Test
    fun `check success`() {
        val system = object : BaseTestSystem() {}
        val diagnose = SystemDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Operation System").apply {
            addSuccess(
                "Version OS: macOS 42.777",
                "CPU: test_cpu"
            )
            addEnvironment(
                EnvironmentPiece.Macos(Version("42.777"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check success info with Rosetta`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "sysctl sysctl.proc_translated" -> {
                    ProcessResult(0, "sysctl.proc_translated: 1")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = SystemDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Operation System").apply {
            addSuccess(
                "Version OS: macOS 42.777",
                "CPU: test_cpu"
            )
            addInfo(
                "You are currently using Rosetta 2.",
                "It may cause some issues while trying to install packages using Homebrew.",
                "Consider switching off Rosetta 2 or ignore this message in case you actually need it.",
            )
            addEnvironment(
                EnvironmentPiece.Macos(Version("42.777"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check invalid OS`() {
        val system = object : BaseTestSystem() {
            override val currentOS = OS.UNKNOWN
        }
        val diagnose = SystemDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Operation System").apply {
            addFailure(
                "OS: Unknown 42.777"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check invalid OS version`() {
        val system = object : BaseTestSystem() {
            override val osVersion = null
        }
        val diagnose = SystemDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Operation System").apply {
            addFailure(
                "OS: macOS null"
            )
        }.build()

        assertEquals(expected, diagnose)
    }
}
