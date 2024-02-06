package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.doctor.entity.System

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
                Doctor(system)
                    .diagnoseKmmEnvironment(
                        isVerbose,
                        isDebug,
                        isExtraDiagnostics,
                        localCompatibilityJson,
                        templateProjectTag
                    )
                    .collect { line -> print(line) }
            }
        }
    }
}
