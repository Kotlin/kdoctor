package org.jetbrains.kotlin.doctor

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
            send("Environment diagnose (to see all details, use -v option):\n")
        }

        val compatibility = async {
            if (localCompatibilityJson == null) {
                Compatibility.download(system)
            } else {
                Compatibility.from(system, system.readFile(localCompatibilityJson).orEmpty())
            }
        }

        suspend fun Diagnostic.run(): Diagnosis {
            val progress = if (!debug) showDiagnosticProgress(title) else null
            val diagnose = diagnose()
            val text = "\r" + diagnose.getText(verbose) + if (verbose) "\n\n" else "\n"
            progress?.cancel()
            send(text)
            return diagnose
        }

        val diagnosis = buildList<Diagnosis> {
            add(SystemDiagnostic(system).run())
            add(JavaDiagnostic(system).run())
            add(AndroidStudioDiagnostic(system, true).run())
            add(XcodeDiagnostic(system, true).run())

            val mainEnvironmentIsNotReady = this.any { it.conclusion == DiagnosisResult.Failure }

            add(CocoapodsDiagnostic(system).run())

            if (extraDiagnostics) {
                if (mainEnvironmentIsNotReady) {
                    val msg = with(DiagnosisResult.Failure) {
                        "$color[$symbol]${TextPainter.RESET} To run extra diagnostics setup your environment before."
                    } + if (verbose) "\n\n" else "\n"
                    send(msg)
                } else {
                    if (templateProjectTag != null) {
                        add(TemplateProjectDiagnostic(system, templateProjectTag).run())
                    } else {
                        add(TemplateProjectDiagnostic(system).run())
                    }

                    val path = system.pwd()
                    add(GradleProjectDiagnostic(system, path).run())
                }
            }
        }
        if (!verbose) {
            send("\n")
        }

        val checkedEnvironments = diagnosis.map { it.checkedEnvironments }
        system.logger.d("Environments: \n${checkedEnvironments.joinToString("\n")}")

        val allUserEnvironments = allCombinations(checkedEnvironments).map { combination ->
            val environment = mutableSetOf<EnvironmentPiece>()
            combination.forEach { environment.addAll(it) }
            environment
        }

        val compatibilityReport =
            CompatibilityAnalyse(system, compatibility.await(), true)
                .check(allUserEnvironments, verbose).trim()

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
            send("${prefix}Your operation system is ready for Kotlin Multiplatform Mobile Development!\n")
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