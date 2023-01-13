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
                arguments.isFull,
                arguments.projectPath,
                arguments.localCompatibilityJson.takeIf { arguments.isDebug }
            )
        }
    }
}

private fun diagnoseKmmEnvironment(
    verbose: Boolean,
    full: Boolean,
    projectPath: String?,
    localCompatibilityJson: String?
): Unit = runBlocking {
    val system = getSystem()
    Doctor(system).diagnoseKmmEnvironment(verbose, full, projectPath, localCompatibilityJson).collect { line ->
        system.print(line)
    }
}

//get platform implementation
expect fun getSystem(): System