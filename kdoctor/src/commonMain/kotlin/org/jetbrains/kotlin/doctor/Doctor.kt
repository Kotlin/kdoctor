package org.jetbrains.kotlin.doctor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.doctor.diagnostics.*

object Doctor {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val KmmDiagnostics = setOf(
        SystemDiagnostic(),
        JavaDiagnostic(),
        AndroidStudioDiagnostic(),
        XcodeDiagnostic(),
        CocoapodsDiagnostic()
    )

    suspend fun diagnoseKmmEnvironment(verbose: Boolean): String {
        val results = KmmDiagnostics.map { d ->
            scope.async { d.diagnose(verbose) }
        }.awaitAll()
        val failures = results.count { it.resultType == Diagnostic.ResultType.Failure }
        val warnings = results.count { it.resultType == Diagnostic.ResultType.Warning }
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
        return results.joinToString("\n") { it.text }.plus("\n$conclusion")
    }
}

fun main() {
    runBlocking {
        val progressMsg = "Diagnosing Kotlin Multiplatform Mobile environment..."
        print(progressMsg)
        val kmmDiagnostic = Doctor.diagnoseKmmEnvironment(true)
        print("\r")
        print(" ".repeat(progressMsg.length))
        println("\r$kmmDiagnostic")
    }
}