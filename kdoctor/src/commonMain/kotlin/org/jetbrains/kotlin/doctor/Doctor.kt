package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.kotlin.doctor.compatibility.CompatibilityAnalyse
import org.jetbrains.kotlin.doctor.diagnostics.*
import org.jetbrains.kotlin.doctor.entity.*
import org.jetbrains.kotlin.doctor.printer.TextPainter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Doctor(private val system: System) {
    private val KmmDiagnostics = setOf(
        SystemDiagnostic(system),
        JavaDiagnostic(system),
        AndroidStudioDiagnostic(system),
        XcodeDiagnostic(system),
        CocoapodsDiagnostic(system)
    )

    private suspend fun Diagnostic.asyncDiagnose(): Diagnosis = suspendCoroutine {
        it.resume(diagnose())
    }

    private suspend fun runDiagnostics(diagnostics: Set<Diagnostic>) = coroutineScope {
        diagnostics.map { d -> async { d.asyncDiagnose() } }.awaitAll()
    }

    private fun makeDiagnosticsReport(diagnosis: List<Diagnosis>, verbose: Boolean) = buildString {
        diagnosis.forEach { diagnosis ->
            appendLine(diagnosis.getText(verbose))
            if (verbose) appendLine()
        }
    }.trim()


    suspend fun diagnoseKmmEnvironment(
        verbose: Boolean,
        projectPath: String?,
        localCompatibilityJson: String?
    ): String = coroutineScope {
        val compatibility = async {
            if (localCompatibilityJson == null) {
                Compatibility.download(system.creteHttpClient())
            } else {
                Compatibility.from(system.readFile(localCompatibilityJson).orEmpty())
            }
        }
        val diagnostics = buildSet {
            addAll(KmmDiagnostics)
            if (projectPath != null) {
                add(GradleProjectDiagnostic(system, projectPath))
            }
        }

        val diagnosis = runDiagnostics(diagnostics)
        val diagnosticsResult = makeDiagnosticsReport(diagnosis, verbose)

        val checkedEnvironments = diagnosis.map { it.checkedEnvironments }
        Logger.d("Environments: \n${checkedEnvironments.joinToString("\n")}")

        val allUserEnvironments = allCombinations(checkedEnvironments).map { combination ->
            val environment = mutableSetOf<EnvironmentPiece>()
            combination.forEach { environment.addAll(it) }
            environment
        }
        val compatibilityReport = CompatibilityAnalyse(compatibility.await())
            .check(allUserEnvironments, verbose).trim()

        val hasFailures = diagnosis.any { it.conclusion == DiagnosisResult.Failure }
        buildString {
            if (diagnosticsResult.isNotBlank()) {
                appendLine(diagnosticsResult)
            }
            if (compatibilityReport.isNotBlank()) {
                appendLine()
                appendLine(compatibilityReport)
            }

            appendLine()
            appendLine("Conclusion:")
            if (hasFailures) {
                val prefix = "  ${DiagnosisResult.Failure.color}${DiagnosisResult.Failure.symbol}${TextPainter.RESET} "
                appendLine("${prefix}KDoctor has diagnosed one or more problems while checking your environment.")
                appendLine("    Please check the output for problem description and possible solutions.")
            } else {
                val prefix = "  ${DiagnosisResult.Success.color}${DiagnosisResult.Success.symbol}${TextPainter.RESET} "
                appendLine("${prefix}Your system is ready for Kotlin Multiplatform Mobile Development!")
            }
        }
    }
}