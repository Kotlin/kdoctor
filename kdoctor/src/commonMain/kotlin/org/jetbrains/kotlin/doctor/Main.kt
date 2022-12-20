package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.doctor.entity.System

internal const val KDOCTOR_VERSION = "0.0.6"

fun main(args: Array<String>) {
    val arguments = Arguments(args)

    Logger.setLogWriters(object : CommonWriter() {
        override fun isLoggable(severity: Severity) = arguments.isDebug
    })

    when {
        arguments.isShowVersion -> {
            println(KDOCTOR_VERSION)
        }

        else -> {
            diagnoseKmmEnvironment(
                arguments.isVerbose,
                arguments.projectPath,
                arguments.localCompatibilityJson.takeIf { arguments.isDebug }
            )
        }
    }
}

private fun diagnoseKmmEnvironment(
    verbose: Boolean,
    projectPath: String?,
    localCompatibilityJson: String?
): Unit = runBlocking {
    val progressMsg = "Diagnosing Kotlin Multiplatform Mobile environment..."
    val system = getSystem()

    print(progressMsg)
    val kmmDiagnostic = Doctor(system).diagnoseKmmEnvironment(verbose, projectPath, localCompatibilityJson)
    print("\r" + " ".repeat(progressMsg.length))

    system.print("\r${kmmDiagnostic.trim()}\n")
}

//get platform implementation
expect fun getSystem(): System