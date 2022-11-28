package org.jetbrains.kotlin.doctor.diagnostics

import io.mockative.*
import org.jetbrains.kotlin.doctor.entity.*
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SystemDiagnosticTest {
    @Mock
    private val system = mock(System::class)
    private val title = "Operation System"
    private val version = Version(42, 777)

    @Test
    fun `check success non Rosetta`() {
        val diagnostic = SystemDiagnostic(system)

        given(system).invocation { currentOS }.then { OS.MacOS }
        given(system).invocation { osVersion }.then { version }
        given(system).invocation { cpuInfo }.then { "test_cpu" }
        given(system).function(system::execute).whenInvokedWith(
            matching {  it == "sysctl" },
            matching {  it == listOf("sysctl.proc_translated") }
        ).then { _, _ -> ProcessResult(0, "sysctl.proc_translated: 0") }

        val diagnose = diagnostic.diagnose()

        assertEquals(title, diagnose.title)
        assertEquals(DiagnosisResult.Success, diagnose.conclusion)
        assertContentEquals(
            listOf(setOf(EnvironmentPiece.Macos(version))),
            diagnose.checkedEnvironments
        )
        assertContentEquals(
            listOf(DiagnosisEntry(DiagnosisResult.Success, "Version OS: macOS 42.777\nCPU: test_cpu")),
            diagnose.entries
        )
    }

    @Test
    fun `check success with Rosetta`() {
        val diagnostic = SystemDiagnostic(system)

        given(system).invocation { currentOS }.then { OS.MacOS }
        given(system).invocation { osVersion }.then { version }
        given(system).invocation { cpuInfo }.then { "test_cpu" }
        given(system).function(system::execute).whenInvokedWith(
            matching {  it == "sysctl" },
            matching {  it == listOf("sysctl.proc_translated") }
        ).then { _, _ -> ProcessResult(0, "sysctl.proc_translated: 1") }

        val diagnose = diagnostic.diagnose()

        assertEquals(title, diagnose.title)
        assertEquals(DiagnosisResult.Success, diagnose.conclusion)
        assertContentEquals(
            listOf(setOf(EnvironmentPiece.Macos(version))),
            diagnose.checkedEnvironments
        )
        assertEquals(2, diagnose.entries.size)
        assert(
            diagnose.entries
                .first { it.result == DiagnosisResult.Info }.text
                .contains("You are currently using Rosetta 2.")
        )
    }

    @Test
    fun `non mac OS is not supported`() {
        val diagnostic = SystemDiagnostic(system)

        given(system).invocation { currentOS }.then { OS.Linux }
        given(system).invocation { osVersion }.then { Version(42, 777) }

        val diagnose = diagnostic.diagnose()

        assertEquals(title, diagnose.title)
        assertEquals(DiagnosisResult.Failure, diagnose.conclusion)
        assertContentEquals(emptyList(), diagnose.checkedEnvironments)
        diagnose.entries.single().let {
            assertEquals(DiagnosisResult.Failure, it.result)
            assertEquals("OS: Linux", it.text)
        }
    }

    @Test
    fun `check invalid os version`() {
        val diagnostic = SystemDiagnostic(system)

        given(system).invocation { currentOS }.then { OS.MacOS }
        given(system).invocation { osVersion }.then { null }

        val diagnose = diagnostic.diagnose()

        assertEquals(title, diagnose.title)
        assertEquals(DiagnosisResult.Failure, diagnose.conclusion)
        assertContentEquals(emptyList(), diagnose.checkedEnvironments)
        diagnose.entries.single().let {
            assertEquals(DiagnosisResult.Failure, it.result)
            assertEquals("OS: macOS", it.text)
        }
    }
}