package org.jetbrains.kotlin.doctor.diagnostics

import co.touchlab.kermit.Logger
import org.jetbrains.kotlin.doctor.entity.*

private const val KDOCTOR_PREFIX = "kdoctor >>> "

class GradleProjectDiagnostic(
    private val system: System,
    path: String
) : Diagnostic() {
    private val projectPath: String = path.removeSuffix("/")

    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder("Project: $projectPath")

        val projectPathFiles = system.execute("ls", projectPath)
        val projectPathExists = projectPathFiles.code == 0
        if (!projectPathExists) {
            result.addFailure("Path is not found")
            return result.build()
        }
        if (!projectPathFiles.output.orEmpty().contains("settings.gradle")) {
            result.addFailure(
                "${projectPath}/settings.gradle[.kts] is not found!",
                "Check project directory path"
            )
            return result.build()
        }

        var gradleBin = "$projectPath/gradlew"
        var gradleVersion = system.execute(gradleBin, "-v")
        if (gradleVersion.code != 0) {
            gradleBin = "gradle"
            gradleVersion = system.execute(gradleBin, "-v")
            if (gradleVersion.code != 0) {
                result.addFailure(
                    "Gradle is not found",
                    "For better experience we recommend to use a Gradle wrapper"
                )
                return result.build()
            } else {
                result.addInfo(
                    "Project doesn't use a Gradle wrapper",
                    "System Gradle was used for the analysis",
                    "For better experience we recommend to use a Gradle wrapper"
                )
            }
        }

        val gradle = gradleVersion.output.orEmpty().lines()
            .firstOrNull { it.startsWith("Gradle ") }
            ?.let { Application("Gradle", Version(it.substringAfter("Gradle "))) }

        if (gradle == null) {
            result.addFailure(
                "Gradle is not found",
                "For better experience we recommend to use a Gradle wrapper"
            )
            return result.build()
        } else {
            result.addSuccess(
                """${gradle.name} (${gradle.version})"""
            )
        }

        val tempFile = try {
            system.writeTempFile(printPluginsInitScript())
        } catch (e: Exception) {
            result.addFailure(
                "Error: impossible to create temporary file",
                "Check your file system write permissions",
                "'${e.message}'"
            )
            return result.build()
        }
        val initScript = tempFile.substringBeforeLast("/") + "/init.gradle.kts"

        system.execute("rm", initScript) //result code doesn't matter

        val initScriptRenamed = system.execute("mv", tempFile, initScript).code
        if (initScriptRenamed != 0) {
            result.addFailure(
                "Error $initScriptRenamed: impossible to prepare $initScript file",
                "Check your file system write permissions"
            )
            return result.build()
        }

        Logger.d("Created $initScript")

        val runGradle = system.execute(gradleBin, "-p", projectPath, "-I", initScript)

        if (runGradle.code != 0) {
            result.addFailure(
                "Gradle error",
                "Run in terminal '$gradleBin -p $projectPath --info'"
            )
            return result.build()
        }

        val gradleOutput = runGradle.output.orEmpty()
        val pluginArtifacts = mutableListOf<PluginArtifact>()
        val appliedPluginsMap = mutableMapOf<String, String>()
        gradleOutput.lineSequence().forEach { l ->
            if (l.contains(KDOCTOR_PREFIX)) {
                val line = l.substringAfter(KDOCTOR_PREFIX)
                if (line.startsWith("DEP ")) {
                    //build script dependency
                    val (group, name, version) = line.substringAfter("DEP ").split(":")
                    pluginArtifacts.add(PluginArtifact(group, name, version))
                } else {
                    val (pluginId, pluginVersion) = line.split("=")
                    //a plugin can be applied without a version
                    if (!appliedPluginsMap.containsKey(pluginId) || pluginVersion != "null") {
                        appliedPluginsMap[pluginId] = pluginVersion
                    }
                }
            }
        }

        val appliedPlugins = findPluginVersions(appliedPluginsMap, pluginArtifacts).sortedBy { it.name }
        if (appliedPlugins.isNotEmpty()) {
            result.addSuccess(
                "Gradle plugins:",
                *appliedPlugins.map { it.name + ":" + it.version }.toTypedArray()
            )
        } else {
            result.addWarning(
                "Gradle plugins are not found"
            )
        }

        result.addEnvironment(
            EnvironmentPiece.Gradle(gradle.version),
            *appliedPlugins.map { EnvironmentPiece.GradlePlugin(it.name, it.version) }.toTypedArray()
        )

        system.execute("rm", initScript) //result code doesn't matter

        return result.build()
    }

    private fun printPluginsInitScript() = """
        gradle.settingsEvaluated {
            settings.pluginManagement.resolutionStrategy.eachPlugin {
                println("$KDOCTOR_PREFIX${'$'}{requested.id}=${'$'}{requested.version}")
            }
        }

        allprojects {
            project.afterEvaluate {
                val deps = buildscript.configurations.flatMap { it.dependencies }
                deps.forEach { d ->
                    println("${KDOCTOR_PREFIX}DEP ${'$'}{d.group}:${'$'}{d.name}:${'$'}{d.version}")
                }
            }
        }
    """.trimIndent()

    data class PluginArtifact(val group: String, val name: String, val version: String)

    private fun findPluginVersions(plugins: Map<String, String>, artifacts: List<PluginArtifact>) =
        plugins.map { (id, version) ->
            if (version == "null") {
                val pluginArtifact = artifacts.firstOrNull { it.name == "$id.gradle.plugin" }
                    ?: if (id.startsWith("org.jetbrains.kotlin.")) {
                        artifacts.firstOrNull {
                            (it.group == "org.jetbrains.kotlin" && it.name == "kotlin-gradle-plugin")
                                    //plugin "org.jetbrains.kotlin.native.cocoapods"
                                    // is bundled in "org.jetbrains.kotlin.multiplatform.gradle.plugin" artifact
                                    || it.name == "org.jetbrains.kotlin.multiplatform.gradle.plugin"
                        }
                    } else if (id == "com.android.application" || id == "com.android.library") {
                        artifacts.firstOrNull { it.group == "com.android.tools.build" && it.name == "gradle" }
                    } else {
                        Logger.d("Unknown plugin $id")
                        null
                    }

                Application(id, Version(pluginArtifact?.version ?: "null"))
            } else {
                Application(id, Version(version))
            }
        }
}