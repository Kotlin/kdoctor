package org.jetbrains.kotlin.doctor

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.kotlin.doctor.compatibility.CompatibilityAnalyse
import org.jetbrains.kotlin.doctor.diagnostics.*
import org.jetbrains.kotlin.doctor.entity.Compatibility
import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.DiagnosisResult
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.jetbrains.kotlin.doctor.printer.TextPainter
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

    suspend fun diagnoseKmmEnvironment(verbose: Boolean): String = coroutineScope {
        val compatibility = async { Compatibility.download() }
        val results = KmmDiagnostics.map { d -> async { d.asyncDiagnose() } }.awaitAll()
        val failures = results.count { it.conclusion == DiagnosisResult.Failure }

        val checkedEnvironments = results.map { it.checkedEnvironments }
        val allUserEnvironments = allCombinations(checkedEnvironments).map { combination ->
            val environment = mutableSetOf<EnvironmentPiece>()
            combination.forEach { environment.addAll(it) }
            environment
        }
        val compatibilityReport = CompatibilityAnalyse(compatibility.await()).check(allUserEnvironments)

        buildString {
            results.forEach { diagnosis ->
                appendLine(diagnosis.getText(verbose))
                if (verbose) appendLine()
            }
            if (compatibilityReport.isNotBlank()) {
                appendLine()
                appendLine("Recommendations:")
                appendLine(compatibilityReport.prependIndent("    "))
            }

            appendLine()
            appendLine("Conclusion:")
            if (failures > 0) {
                val prefix = "  ${DiagnosisResult.Failure.color}${DiagnosisResult.Failure.symbol}${TextPainter.RESET} "
                appendLine("${prefix}KDoctor has diagnosed one or more problems while checking your environment.")
                appendLine("    Please check the output for problem description and possible solutions.")
            } else {
                val prefix = "  ${DiagnosisResult.Success.color}${DiagnosisResult.Success.symbol}${TextPainter.RESET} "
                appendLine("${prefix}Your system is ready for Kotlin Multiplatform Mobile Development!")
            }
        }
    }

    //input: [[@], [a, b], [1, 2], [#]]
    //output: [[@, a, 1, #], [@, a, 2, #], [@, b, 1, #], [@, b, 2, #]]
    private fun <T> allCombinations(input: List<List<T>>): List<List<T>> {
        val list = input.filter { it.isNotEmpty() }
        return when {
            list.isEmpty() -> emptyList()
            list.size == 1 -> list.first().map { listOf(it) }
            else -> {
                val head: List<T> = list.first()
                val tail: List<List<T>> = list.drop(1)
                val tailResult = allCombinations(tail)
                head.flatMap { h ->
                    tailResult.map { j -> listOf(h) + j }
                }
            }
        }
    }
}