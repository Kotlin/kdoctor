package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.doctor.entity.System

internal const val KDOCTOR_VERSION = "0.0.6"

fun main(args: Array<String>) = Main().main(args)

private class Main : CliktCommand() {
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

    val isDebug: Boolean by option("--debug", hidden = true).flag()
    val localCompatibilityJson: String? by option("--compatibilityJson", hidden = true)
    val templateProjectTag: String? by option("--templateProject", hidden = true)

    override fun run() {
        Logger.setLogWriters(object : CommonWriter() {
            override fun isLoggable(severity: Severity) = if (isDebug) {
                if (isVerbose) true
                else severity != Severity.Verbose
            } else false

            override fun formatMessage(
                severity: Severity,
                message: String,
                tag: String,
                throwable: Throwable?
            ) = "${severity.name.first().lowercaseChar()}: $message"
        })

        when {
            showVersion -> {
                println(KDOCTOR_VERSION)
            }

            else -> runBlocking {
                Doctor(getSystem())
                    .diagnoseKmmEnvironment(isVerbose, isDebug, isExtraDiagnostics, localCompatibilityJson, templateProjectTag)
                    .collect { line -> print(line) }
            }
        }
    }
}

//get platform implementation
expect fun getSystem(): System