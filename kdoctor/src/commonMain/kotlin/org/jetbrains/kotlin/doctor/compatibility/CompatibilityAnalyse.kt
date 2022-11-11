package org.jetbrains.kotlin.doctor.compatibility

import org.jetbrains.kotlin.doctor.KDOCTOR_VERSION
import org.jetbrains.kotlin.doctor.entity.Compatibility
import org.jetbrains.kotlin.doctor.entity.CompatibilityProblem
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.jetbrains.kotlin.doctor.entity.Version

class CompatibilityAnalyse(private val compatibility: Compatibility) {
    fun check(environments: List<Set<EnvironmentPiece>>): String {
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
                problem.matrix.all { app ->
                    val userAppVersion = environment.firstOrNull { it.name == app.name }?.version ?: return@all false
                    val beforeProblem = app.from?.let { userAppVersion < Version(it) } ?: false
                    val fixedAlready = app.fixedIn?.let { userAppVersion >= Version(it) } ?: false
                    return@all beforeProblem || fixedAlready
                }
            }
            userProblems.addAll(problems)
        }
        userProblems.forEach { problem ->
            report.add("${problem.text}\nMore details: ${problem.url}")
        }

        return report.joinToString("\n\n")
    }
}