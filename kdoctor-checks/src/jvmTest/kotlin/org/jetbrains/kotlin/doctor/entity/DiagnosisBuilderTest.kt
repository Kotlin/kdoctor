package org.jetbrains.kotlin.doctor.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class DiagnosisBuilderTest {

    @Test
    fun `check success diagnosis`() {
        val env1 = setOf(
            EnvironmentPiece.Macos(Version("1.1.1")),
            EnvironmentPiece.Jdk(Version("1.1.1")),
        )
        val env2 = setOf(
            EnvironmentPiece.Macos(Version("2.2.2"))
        )
        val diagnosis = Diagnosis.Builder("Title").apply {
            addSuccess(
                "success 1",
            )
            addInfo(
                "info 1",
                "info 2",
            )
            addEnvironment(*env1.toTypedArray())
            addEnvironment(*env2.toTypedArray())
        }.build()

        assertEquals("Title", diagnosis.title)
        assertEquals(DiagnosisResult.Success, diagnosis.conclusion)
        assertEquals(listOf(env1, env2), diagnosis.checkedEnvironments)
        assertEquals(
            listOf(
                DiagnosisEntry(DiagnosisResult.Success, "success 1"),
                DiagnosisEntry(DiagnosisResult.Info, "info 1\ninfo 2"),
            ),
            diagnosis.entries
        )
    }

    @Test
    fun `check warning diagnosis`() {
        val env1 = setOf(
            EnvironmentPiece.Macos(Version("1.1.1")),
            EnvironmentPiece.Jdk(Version("1.1.1")),
        )
        val env2 = setOf(
            EnvironmentPiece.Macos(Version("2.2.2"))
        )
        val diagnosis = Diagnosis.Builder("Title").apply {
            addSuccess(
                "success 1",
            )
            addInfo(
                "info 1",
                "info 2",
            )
            addWarning(
                "warning 1\nwarning 2",
            )
            addEnvironment(*env1.toTypedArray())
            addEnvironment(*env2.toTypedArray())
        }.build()

        assertEquals("Title", diagnosis.title)
        assertEquals(DiagnosisResult.Warning, diagnosis.conclusion)
        assertEquals(listOf(env1, env2), diagnosis.checkedEnvironments)
        assertEquals(
            listOf(
                DiagnosisEntry(DiagnosisResult.Success, "success 1"),
                DiagnosisEntry(DiagnosisResult.Info, "info 1\ninfo 2"),
                DiagnosisEntry(DiagnosisResult.Warning, "warning 1\nwarning 2"),
            ),
            diagnosis.entries
        )
    }

    @Test
    fun `check failure diagnosis`() {
        val env1 = setOf(
            EnvironmentPiece.Macos(Version("1.1.1")),
            EnvironmentPiece.Jdk(Version("1.1.1")),
        )
        val diagnosis = Diagnosis.Builder("Title").apply {
            addSuccess(
                "success 1",
            )
            addSuccess(
                "success 2",
            )
            addFailure(
                "Fail"
            )
            addEnvironment(*env1.toTypedArray())
        }.build()

        assertEquals("Title", diagnosis.title)
        assertEquals(DiagnosisResult.Failure, diagnosis.conclusion)
        assertEquals(listOf(env1), diagnosis.checkedEnvironments)
        assertEquals(
            listOf(
                DiagnosisEntry(DiagnosisResult.Success, "success 1"),
                DiagnosisEntry(DiagnosisResult.Success, "success 2"),
                DiagnosisEntry(DiagnosisResult.Failure, "Fail"),
            ),
            diagnosis.entries
        )
    }

    @Test
    fun `check custom diagnosis conclusion`() {
        val diagnosis = Diagnosis.Builder("Title").apply {
            addFailure(
                "Fail"
            )
            addSuccess(
                "success 1",
            )
            setConclusion(DiagnosisResult.Warning)
        }.build()

        assertEquals("Title", diagnosis.title)
        assertEquals(DiagnosisResult.Warning, diagnosis.conclusion)
        assertEquals(emptyList(), diagnosis.checkedEnvironments)
        assertEquals(
            listOf(
                DiagnosisEntry(DiagnosisResult.Failure, "Fail"),
                DiagnosisEntry(DiagnosisResult.Success, "success 1"),
            ),
            diagnosis.entries
        )
    }
}