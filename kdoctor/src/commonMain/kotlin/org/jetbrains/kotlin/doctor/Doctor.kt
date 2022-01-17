package org.jetbrains.kotlin.doctor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.doctor.diagnostics.AndroidStudioDiagnostic
import org.jetbrains.kotlin.doctor.diagnostics.CocoapodsDiagnostic
import org.jetbrains.kotlin.doctor.diagnostics.JavaDiagnostic
import org.jetbrains.kotlin.doctor.diagnostics.SystemDiagnostic
import org.jetbrains.kotlin.doctor.diagnostics.XcodeDiagnostic

object Doctor {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val KmmDiagnostics = setOf(
        SystemDiagnostic(),
        JavaDiagnostic(),
        AndroidStudioDiagnostic(),
        XcodeDiagnostic(),
        CocoapodsDiagnostic()
    )

    suspend fun diagnoseKmmEnvironment(verbose: Boolean): String =
        KmmDiagnostics.map { d ->
            scope.async { d.diagnose(verbose) }
        }.awaitAll().joinToString("\n\n")
}

fun main() {
    runBlocking {
        print("Diagnosing Kotlin Multiplatform Mobile environment...")
        val kmmDiagnostic = Doctor.diagnoseKmmEnvironment(true)
        print("\r$kmmDiagnostic")
    }
}