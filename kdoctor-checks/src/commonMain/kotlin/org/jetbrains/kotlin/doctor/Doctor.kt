package org.jetbrains.kotlin.doctor

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.jetbrains.kotlin.doctor.compatibility.CompatibilityAnalyse
import org.jetbrains.kotlin.doctor.diagnostics.Diagnostic
import org.jetbrains.kotlin.doctor.entity.*
import org.jetbrains.kotlin.doctor.printer.TextPainter

class Doctor(private val system: System) {
    fun diagnoseEnvironment(
        verbose: Boolean,
        debug: Boolean,
        diagnostics: Flow<Diagnostic>,
        supplementalDiagnostics: Flow<Diagnostic>,
        extraDiagnostics: Flow<Diagnostic>,
        localCompatibilityJson: String?,
        output: SendChannel<String>
    ): Flow<Conclusive> = channelFlow {
        try {
            val compatibility = async {
                if (localCompatibilityJson == null) {
                    Compatibility.download(system)
                } else {
                    Compatibility.from(system, system.readFile(localCompatibilityJson).orEmpty())
                }
            }

            fun CoroutineScope.showDiagnosticProgress(title: String) = launch {
                var progressIndex = 0
                while (isActive) {
                    progressIndex = (progressIndex + 1) % progressSymbols.size
                    val symbol = progressSymbols[progressIndex]
                    output.send("\r[$symbol] $title")
                    delay(60)
                }
            }

            suspend fun Diagnostic.run(): Diagnosis {
                val progress = if (!debug) showDiagnosticProgress(title) else null
                val diagnose = diagnose()
                send(diagnose)
                val text = "\r" + diagnose.getText(verbose) + if (verbose) "\n\n" else "\n"
                progress?.cancel()
                output.send(text)
                return diagnose
            }

            val checkedEnvironments = buildList {
                var mainEnvironmentIsNotReady = false

                diagnostics.collect { diagnostic ->
                    val diagnosis = diagnostic.run()
                    if (diagnosis.conclusion == DiagnosisResult.Failure) mainEnvironmentIsNotReady = true
                    add(diagnosis.checkedEnvironments)
                }

                supplementalDiagnostics.collect { diagnostic ->
                    add(diagnostic.run().checkedEnvironments)
                }

                var isFirstExtraDiagnostic = true
                extraDiagnostics.collect { diagnostic ->
                    when {
                        mainEnvironmentIsNotReady -> when {
                            isFirstExtraDiagnostic -> {
                                isFirstExtraDiagnostic = false
                                val msg = with(DiagnosisResult.Failure) {
                                    "$color[$symbol]${TextPainter.RESET} To run extra diagnostics setup your environment before."
                                } + if (verbose) "\n\n" else "\n"
                                output.send(msg)
                            }
                        }
                        else -> add(diagnostic.run().checkedEnvironments)
                    }
                }
            }

            if (!verbose) {
                output.send("\n")
            }

            system.logger.d("Environments: \n${checkedEnvironments.joinToString("\n")}")

            val allUserEnvironments = allCombinations(checkedEnvironments).map { combination ->
                val environment = mutableSetOf<EnvironmentPiece>()
                combination.forEach { environment.addAll(it) }
                environment
            }

            val compatibilityReport =
                CompatibilityAnalyse(system, compatibility.await(), true)
                    .check(allUserEnvironments, verbose)

            send(compatibilityReport)
            if (compatibilityReport.conclusion != DiagnosisResult.Success) {
                output.send(compatibilityReport.text)
            }
        } finally {
            output.close()
        }
    }

    private val progressSymbols = listOf("⣷", "⣯", "⣟", "⡿", "⢿", "⣻", "⣽", "⣾")
}