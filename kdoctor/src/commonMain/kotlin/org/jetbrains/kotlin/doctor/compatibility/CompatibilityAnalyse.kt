package org.jetbrains.kotlin.doctor.compatibility

import org.jetbrains.kotlin.doctor.KDOCTOR_VERSION
import org.jetbrains.kotlin.doctor.entity.Compatibility
import org.jetbrains.kotlin.doctor.entity.CompatibilityProblem
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.jetbrains.kotlin.doctor.entity.Version

class CompatibilityAnalyse(private val compatibility: Compatibility) {
    fun check(environments: List<Map<EnvironmentPiece, Version>>): String {
        val report = mutableListOf<String>()
        if (Version(KDOCTOR_VERSION) < Version(compatibility.latestKdoctor)) {
           report.add("""
               Update the kdoctor to the latest version: ${compatibility.latestKdoctor}
                 `brew upgrade kdoctor`
           """.trimIndent())
        }

        val userProblems = mutableSetOf<CompatibilityProblem>()
        environments.forEach { environment ->
            val problems = compatibility.problems.filter { problem ->
                problem.matrix.entries.all { (problemApp, problemAppRange) ->
                    val userAppVersion = environment[problemApp] ?: return@all false
                    val less = problemAppRange.from?.let { userAppVersion < Version(it) } ?: false
                    val more = problemAppRange.to?.let { userAppVersion >= Version(it) } ?: false
                    return@all less || more
                }
            }
            userProblems.addAll(problems)
        }
        userProblems.forEach { problem ->
            report.add("${problem.text}\nMore info: ${problem.url}")
        }

        return report.joinToString("\n\n")
    }
}