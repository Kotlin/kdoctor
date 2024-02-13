package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.doctor.diagnostics.*
import org.jetbrains.kotlin.doctor.entity.Compatibility
import org.jetbrains.kotlin.doctor.entity.DiagnosisResult
import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.printer.TextPainter

internal expect fun initMain(): Main
fun main(args: Array<String>): Unit = initMain().main(args)

internal class Main(val system: System) : CliktCommand(name = "kdoctor") {
    val showVersion: Boolean by option(
        "--version",
        help = "Report a version of KDoctor"
    ).flag()
    val isVerbose: Boolean by option(
        "--verbose", "-v",
        help = "Report an extended information"
    ).flag()
    val isExtraDiagnostics: Boolean by option(
        "--all", "-a",
        help = "Run extra diagnostics such as a build of a synthetic project and an analysis of a project in the current directory"
    ).flag()

    val showDevelopmentTeams: Boolean by option(
        "--team-ids",
        help = "Report all available Apple dev team ids"
    ).flag()

    val isDebug: Boolean by option("--debug", hidden = true).flag()
    val localCompatibilityJson: String? by option("--compatibilityJson", hidden = true)
    val templateProjectTag: String? by option("--templateProject", hidden = true)

    override fun run(): Unit = runBlocking {
        val logFormatter = object : MessageStringFormatter {
            override fun formatSeverity(severity: Severity) = severity.name.first() + ":"
            override fun formatTag(tag: Tag) = ""
        }
        Logger.setLogWriters(object : CommonWriter(logFormatter) {
            override fun isLoggable(tag: String, severity: Severity) = if (isDebug) {
                if (isVerbose) true else severity != Severity.Verbose
            } else false
        })

        when {
            showVersion -> {
                println(KDOCTOR_VERSION)
            }

            showDevelopmentTeams -> {
                val teams = DevelopmentTeams(system).getTeams()
                if (teams.isEmpty()) {
                    println("Certificates are not found")
                } else {
                    println(teams.joinToString("\n"))
                }
            }

            else -> {
                if (isVerbose) {
                    println("Environment diagnose:")
                } else {
                    println("Environment diagnose (to see all details, use -v option):")
                }

                val compatibility = async {
                    localCompatibilityJson?.let {
                        Compatibility.from(system, system.readFile(it).orEmpty())
                    } ?: Compatibility.download(system)
                }

                val output = Channel<String>()
                launch(Dispatchers.Default) {
                    output.consumeEach { line -> print(line) }
                }

                val diagnoses = Doctor(system)
                    .diagnoseEnvironment(
                        isVerbose,
                        isDebug,
                        flowOf(
                            SystemDiagnostic(system),
                            JavaDiagnostic(system),
                            AndroidStudioDiagnostic(system, true),
                            XcodeDiagnosticStandlone(system),
                        ),
                        flowOf(CocoapodsDiagnostic(system)),
                        when {
                            isExtraDiagnostics -> flowOf(
                                templateProjectTag?.let { TemplateProjectDiagnostic(system, it) }
                                    ?: TemplateProjectDiagnostic(system),
                                GradleProjectDiagnostic(system, system.pwd())
                            )

                            else -> emptyFlow()
                        },
                        compatibility,
                        output
                    ).toList()

                println("Conclusion:")

                val hasFailures = diagnoses.any { it.conclusion == DiagnosisResult.Failure }
                if (hasFailures) {
                    val prefix = "  ${DiagnosisResult.Failure.color}${DiagnosisResult.Failure.symbol}${TextPainter.RESET} "
                    println("${prefix}KDoctor has diagnosed one or more problems while checking your environment.")
                    println("    Please check the output for problem description and possible solutions.")
                } else {
                    val prefix = "  ${DiagnosisResult.Success.color}${DiagnosisResult.Success.symbol}${TextPainter.RESET} "
                    println("${prefix}Your operating system is ready for Kotlin Multiplatform Mobile Development!")
                }
            }
        }
    }
}
