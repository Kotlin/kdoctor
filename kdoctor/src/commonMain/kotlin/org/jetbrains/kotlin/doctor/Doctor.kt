package org.jetbrains.kotlin.doctor

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.coroutines.*
import org.jetbrains.kotlin.doctor.diagnostics.*

object Doctor {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val KmmDiagnostics = setOf(
        SystemDiagnostic(),
        JavaDiagnostic(),
        AndroidStudioDiagnostic(),
        XcodeDiagnostic(),
        CocoapodsDiagnostic()
    )

    suspend fun diagnoseKmmEnvironment(verbose: Boolean): String {
        val results = KmmDiagnostics.map { d ->
            scope.async { d.diagnose(verbose) }
        }.awaitAll()
        val failures = results.count { it.resultType == Diagnostic.ResultType.Failure }
        val warnings = results.count { it.resultType == Diagnostic.ResultType.Warning }
        val conclusion = buildString {
            if (failures > 0) {
                appendLine("Failures: $failures")
            }
            if (warnings > 0) {
                appendLine("Warnings: $warnings")
            }
            if (failures > 0)
                appendLine(
                    """
                    |KDoctor has diagnosed one or more problems while checking your environment.
                    |Please check the output for problem description and possible solutions.
                    """.trimMargin()
                )
            else
                appendLine("Your system is ready for Kotlin Multiplatform Mobile Development!")
        }
        return results.joinToString("\n") { it.text }.plus("\n$conclusion")
    }
}

internal var isDebug = false
fun main(args: Array<String>) {
    val parser = ArgParser("kdoctor")
    val versionFlag by parser.option(ArgType.Boolean, shortName = "v", fullName = "version", description = "print KDoctor version")
    val debugFlag by parser.option(ArgType.Boolean, fullName = "debug", description = "debug mode")
    parser.parse(args)

    debugFlag?.let { isDebug = it }
    when (versionFlag) {
        true -> println(DoctorVersion.VERSION)
        else -> run()
    }
}

private fun run() {
    runBlocking {
        val progressMsg = "Diagnosing Kotlin Multiplatform Mobile environment..."
        print(progressMsg)
        val kmmDiagnostic = Doctor.diagnoseKmmEnvironment(true)
        print("\r")
        print(" ".repeat(progressMsg.length))
        println("\r$kmmDiagnostic")
    }
}