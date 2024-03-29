package org.jetbrains.kotlin.doctor.compatibility

import co.touchlab.kermit.Logger
import org.jetbrains.kotlin.doctor.KDOCTOR_VERSION
import org.jetbrains.kotlin.doctor.entity.Compatibility
import org.jetbrains.kotlin.doctor.entity.CompatibilityProblem
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.jetbrains.kotlin.doctor.entity.Version
import org.jetbrains.kotlin.doctor.printer.TextPainter

class CompatibilityAnalyse(private val compatibility: Compatibility) {
    fun check(environments: List<Set<EnvironmentPiece>>, verbose: Boolean): String {
        val userProblems = mutableSetOf<CompatibilityProblem>()
        environments.forEach { environment ->
            val problems = compatibility.problems.filter { problem ->
                Logger.d("Analyze problem: ${problem.url}")
                problem.matrix.all { app ->
                    Logger.d("Find app: ${app.name}")
                    val userAppVersion = environment.firstOrNull { it.name == app.name }?.version ?: return@all false
                    Logger.d("User app (${app.name}): ${userAppVersion.semVersion}")
                    val beforeProblem = app.from?.let { userAppVersion < Version(it) } ?: false
                    val fixedAlready = app.fixedIn?.let { userAppVersion >= Version(it) } ?: false
                    val result = !(beforeProblem || fixedAlready)
                    Logger.d("App has problem: $result")
                    return@all result
                }
            }
            userProblems.addAll(problems)
        }

        val result = buildString {
            if (Version(KDOCTOR_VERSION) < Version(compatibility.latestKdoctor)) {
                appendLine("  ➤ Update the kdoctor to the latest version: ${compatibility.latestKdoctor}")
                appendLine("    brew upgrade kdoctor")
                appendLine()
            }

            userProblems
                .filter { if (verbose) true else it.isCritical }
                .forEach { problem ->
                    problem.text.lines().forEachIndexed { index, s ->
                        if (index == 0) {
                            if (problem.isCritical) {
                                appendLine("${TextPainter.RED}  ! ${TextPainter.RESET}$s")
                            } else {
                                appendLine("  ➤ $s")
                            }
                        } else {
                            appendLine("    $s")
                        }
                    }
                    appendLine("    More details: ${problem.url}")
                    appendLine()
                }
        }

        return if (result.isNotBlank()) "Recommendations:\n$result" else ""
    }
}