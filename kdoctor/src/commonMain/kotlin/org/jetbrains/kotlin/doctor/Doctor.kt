package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.kotlin.doctor.compatibility.CompatibilityAnalyse
import org.jetbrains.kotlin.doctor.diagnostics.*
import org.jetbrains.kotlin.doctor.entity.*
import org.jetbrains.kotlin.doctor.printer.TextPainter
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Doctor(private val system: System) {
    private val doctorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val KmmDiagnostics = setOf(
        SystemDiagnostic(system),
        JavaDiagnostic(system),
        AndroidStudioDiagnostic(system),
        XcodeDiagnostic(system),
        CocoapodsDiagnostic(system)
    )

    fun diagnoseKmmEnvironment(
        verbose: Boolean,
        full: Boolean,
        projectPath: String?,
        localCompatibilityJson: String?
    ): Flow<String> = flow {
        emit("Environment diagnose (to see all details, run kdoctor -v):\n")

        val compatibility = doctorScope.async {
            if (localCompatibilityJson == null) {
                Compatibility.download(system)
            } else {
                Compatibility.from(system.readFile(localCompatibilityJson).orEmpty())
            }
        }

        val diagnostics = buildSet {
            addAll(KmmDiagnostics)
            if (full) {
                add(TemplateProjectDiagnostic(system))
            }
            if (projectPath != null) {
                add(GradleProjectDiagnostic(system, projectPath))
            }
        }

        val diagnosis = diagnostics.map { diagnostic ->
            emit("${TextPainter.GREEN}[…]${TextPainter.RESET} ${diagnostic.title}")
            val diagnose = diagnostic.diagnose()
            val text = "\r" + diagnose.getText(verbose) + if (verbose) "\n\n" else "\n"
            emit(text)
            diagnose
        }
        if (!verbose) {
            emit("\n")
        }

        val checkedEnvironments = diagnosis.map { it.checkedEnvironments }
        Logger.d("Environments: \n${checkedEnvironments.joinToString("\n")}")

        val allUserEnvironments = allCombinations(checkedEnvironments).map { combination ->
            val environment = mutableSetOf<EnvironmentPiece>()
            combination.forEach { environment.addAll(it) }
            environment
        }

        val compatibilityReport = CompatibilityAnalyse(compatibility.await()).check(allUserEnvironments, verbose).trim()

        if (compatibilityReport.isNotBlank()) {
            emit(compatibilityReport + "\n")
        }

        emit("Conclusion:\n")

        val hasFailures = diagnosis.any { it.conclusion == DiagnosisResult.Failure }
        if (hasFailures) {
            val prefix = "  ${DiagnosisResult.Failure.color}${DiagnosisResult.Failure.symbol}${TextPainter.RESET} "
            emit("${prefix}KDoctor has diagnosed one or more problems while checking your environment.\n")
            emit("    Please check the output for problem description and possible solutions.\n")
        } else {
            val prefix = "  ${DiagnosisResult.Success.color}${DiagnosisResult.Success.symbol}${TextPainter.RESET} "
            emit("${prefix}Your system is ready for Kotlin Multiplatform Mobile Development!\n")
        }
    }
}