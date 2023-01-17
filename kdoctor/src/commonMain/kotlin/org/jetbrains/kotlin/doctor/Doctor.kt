package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import org.jetbrains.kotlin.doctor.compatibility.CompatibilityAnalyse
import org.jetbrains.kotlin.doctor.diagnostics.*
import org.jetbrains.kotlin.doctor.entity.*
import org.jetbrains.kotlin.doctor.printer.TextPainter

class Doctor(private val system: System) {
    fun diagnoseKmmEnvironment(
        verbose: Boolean,
        debug: Boolean,
        extraDiagnostics: Boolean,
        localCompatibilityJson: String?,
        templateProjectTag: String?
    ): Flow<String> = channelFlow {
        if (verbose) {
            send("Environment diagnose:\n")
        } else {
            send("Environment diagnose (to see all details, run kdoctor -v):\n")
        }

        val compatibility = async {
            if (localCompatibilityJson == null) {
                Compatibility.download(system)
            } else {
                Compatibility.from(system.readFile(localCompatibilityJson).orEmpty())
            }
        }

        val diagnostics = buildSet {
            add(SystemDiagnostic(system))
            add(JavaDiagnostic(system))
            add(AndroidStudioDiagnostic(system))
            add(XcodeDiagnostic(system))
            add(CocoapodsDiagnostic(system))
            if (extraDiagnostics) {
                if (templateProjectTag != null) {
                    add(TemplateProjectDiagnostic(system, templateProjectTag))
                } else {
                    add(TemplateProjectDiagnostic(system))
                }

                val path = system.execute("pwd").output?.trim().orEmpty()
                add(GradleProjectDiagnostic(system, path))
            }
        }

        val diagnosis = diagnostics.map { diagnostic ->
            val progress = if (!debug) showDiagnosticProgress(diagnostic.title) else null
            val diagnose = diagnostic.diagnose()
            val text = "\r" + diagnose.getText(verbose) + if (verbose) "\n\n" else "\n"
            progress?.cancel()
            send(text)
            diagnose
        }
        if (!verbose) {
            send("\n")
        }

        val checkedEnvironments = diagnosis.map { it.checkedEnvironments }
        Logger.d("Environments: \n${checkedEnvironments.joinToString("\n")}")

        val allUserEnvironments = allCombinations(checkedEnvironments).map { combination ->
            val environment = mutableSetOf<EnvironmentPiece>()
            combination.forEach { environment.addAll(it) }
            environment
        }

        val compatibilityReport =
            CompatibilityAnalyse(compatibility.await()).check(allUserEnvironments, verbose).trim()

        if (compatibilityReport.isNotBlank()) {
            send(compatibilityReport + "\n")
        }

        send("Conclusion:\n")

        val hasFailures = diagnosis.any { it.conclusion == DiagnosisResult.Failure }
        if (hasFailures) {
            val prefix = "  ${DiagnosisResult.Failure.color}${DiagnosisResult.Failure.symbol}${TextPainter.RESET} "
            send("${prefix}KDoctor has diagnosed one or more problems while checking your environment.\n")
            send("    Please check the output for problem description and possible solutions.\n")
        } else {
            val prefix = "  ${DiagnosisResult.Success.color}${DiagnosisResult.Success.symbol}${TextPainter.RESET} "
            send("${prefix}Your system is ready for Kotlin Multiplatform Mobile Development!\n")
        }
    }.buffer(0).flowOn(Dispatchers.Default)

    private val progressSymbols = listOf("⣷", "⣯", "⣟", "⡿", "⢿", "⣻", "⣽", "⣾")
    private fun ProducerScope<String>.showDiagnosticProgress(title: String) = launch {
        var progressIndex = 0
        while (isActive) {
            progressIndex = (progressIndex + 1) % progressSymbols.size
            val symbol = progressSymbols[progressIndex]
            send("\r[$symbol] $title")
            delay(60)
        }
    }
}