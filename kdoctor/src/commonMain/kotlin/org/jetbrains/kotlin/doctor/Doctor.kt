package org.jetbrains.kotlin.doctor

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.kotlin.doctor.diagnostics.*
import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.DiagnosisResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Doctor {
    private val KmmDiagnostics = setOf(
        SystemDiagnostic(),
        JavaDiagnostic(),
        AndroidStudioDiagnostic(),
        XcodeDiagnostic(),
        CocoapodsDiagnostic()
    )

    private suspend fun Diagnostic.asyncDiagnose(): Diagnosis = suspendCoroutine {
        it.resume(diagnose())
    }

    suspend fun diagnoseKmmEnvironment(): String = coroutineScope {
        val results = KmmDiagnostics.map { d -> async { d.asyncDiagnose() } }.awaitAll()
        val failures = results.count { it.conclusion == DiagnosisResult.Failure }
        val warnings = results.count { it.conclusion == DiagnosisResult.Warning }
        val conclusion = buildString {
            if (failures > 0) {
                appendLine("Failures: $failures")
            }
            if (warnings > 0) {
                appendLine("Warnings: $warnings")
            }
            if (failures > 0)
                appendLine(
                    """
                    |KDoctor has diagnosed one or more problems while checking your environment.
                    |Please check the output for problem description and possible solutions.
                    """.trimMargin()
                )
            else
                appendLine("Your system is ready for Kotlin Multiplatform Mobile Development!")
        }
        results.joinToString("\n") { it.text }.plus("\n$conclusion")
    }
}