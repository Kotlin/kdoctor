package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.doctor.entity.System

internal const val KDOCTOR_VERSION = "0.0.6"

fun main(args: Array<String>) {
    val parser = ArgParser("kdoctor")
    val versionFlag by parser.option(
        ArgType.Boolean,
        fullName = "version",
        description = "print KDoctor version"
    )
    val verboseFlag by parser.option(
        ArgType.Boolean,
        shortName = "v",
        fullName = "verbose",
        description = "print extended information"
    )
    val debugFlag by parser.option(ArgType.Boolean, fullName = "debug", description = "debug mode")
    parser.parse(args)

    Logger.setLogWriters(object : CommonWriter() {
        override fun isLoggable(severity: Severity) = debugFlag == true
    })

    when (versionFlag) {
        true -> println(KDOCTOR_VERSION)
        else -> run(verboseFlag == true)
    }
}

private fun run(verbose: Boolean): Unit = runBlocking {
    val progressMsg = "Diagnosing Kotlin Multiplatform Mobile environment..."
    val system = getSystem()

    print(progressMsg)
    val kmmDiagnostic = Doctor(system).diagnoseKmmEnvironment(verbose)
    print("\r" + " ".repeat(progressMsg.length))

    system.print("\r${kmmDiagnostic.trim()}\n")
}

//get platform implementation
expect fun getSystem(): System