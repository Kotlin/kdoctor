package org.jetbrains.kotlin.doctor

import org.jetbrains.kotlin.doctor.diagnostics.SystemDiagnostic
import org.jetbrains.kotlin.doctor.entity.OS
import org.jetbrains.kotlin.doctor.entity.ProcessResult
import org.jetbrains.kotlin.doctor.printer.TextPainter.removeColors
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals


internal class SystemDiagnosticReportTest {

    @Test
    fun `check success`() {
        val system = object : BaseTestSystem() {}
        val diagnostic = SystemDiagnostic(system)
        val doctor = Doctor(system)
        val report = doctor.makeDiagnosticsReport(listOf(diagnostic.diagnose()), true)
        assertEquals(
            """
            [✓] Operation System
              ➤ Version OS: macOS 42.777
                CPU: test_cpu
        """.trimIndent(), report.removeColors()
        )
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
        val diagnostic = SystemDiagnostic(system)
        val doctor = Doctor(system)
        val report = doctor.makeDiagnosticsReport(listOf(diagnostic.diagnose()), true)
        assertEquals(
            """
            [✓] Operation System
              ➤ Version OS: macOS 42.777
                CPU: test_cpu
              i You are currently using Rosetta 2.
                It may cause some issues while trying to install packages using Homebrew.
                Consider switching off Rosetta 2 or ignore this message in case you actually need it.
        """.trimIndent(), report.removeColors()
        )
    }

    @Test
    fun `check invalid OS`() {
        val system = object : BaseTestSystem() {
            override val currentOS = OS.Linux
        }

        val diagnostic = SystemDiagnostic(system)
        val doctor = Doctor(system)
        val report = doctor.makeDiagnosticsReport(listOf(diagnostic.diagnose()), true)
        assertEquals(
            """
            [✖] Operation System
              ✖ OS: Linux 42.777
        """.trimIndent(), report.removeColors()
        )
    }

    @Test
    fun `check invalid OS version`() {
        val system = object : BaseTestSystem() {
            override val osVersion = null
        }

        val diagnostic = SystemDiagnostic(system)
        val doctor = Doctor(system)
        val report = doctor.makeDiagnosticsReport(listOf(diagnostic.diagnose()), true)
        assertEquals(
            """
            [✖] Operation System
              ✖ OS: macOS null
        """.trimIndent(), report.removeColors()
        )
    }
}
