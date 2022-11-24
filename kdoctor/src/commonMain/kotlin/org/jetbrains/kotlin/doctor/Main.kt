package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.entity.print

internal const val KDOCTOR_VERSION = "0.0.5"

fun main(args: Array<String>) {
    val parser = ArgParser("kdoctor")
    val versionFlag by parser.option(
        ArgType.Boolean,
        shortName = "v",
        fullName = "version",
        description = "print KDoctor version"
    )
    val debugFlag by parser.option(ArgType.Boolean, fullName = "debug", description = "debug mode")
    parser.parse(args)

    Logger.setLogWriters(object : CommonWriter() {
        override fun isLoggable(severity: Severity) = debugFlag == true
    })

    when (versionFlag) {
        true -> println(KDOCTOR_VERSION)
        else -> run()
    }
}

private fun run(): Unit = runBlocking {
    val progressMsg = "Diagnosing Kotlin Multiplatform Mobile environment..."

    print(progressMsg)
    val kmmDiagnostic = Doctor.diagnoseKmmEnvironment()
    print("\r" + " ".repeat(progressMsg.length))

    System.print("\r${kmmDiagnostic.trim()}\n")
}