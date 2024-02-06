package org.jetbrains.kotlin.doctor.diagnostics

import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.doctor.entity.*

class AndroidStudioDiagnostic(private val system: System, private val checkForPlugins: Boolean) : Diagnostic() {
    override val title = "Android Studio"

    override suspend fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder(title)

        val paths = mutableSetOf<String>()
        paths.addAll(system.spotlightFindAppPaths("com.google.android.studio*"))
        if (paths.isEmpty()) {
            paths.addAll(system.findAppsPathsInDirectory("Android Studio", "/Applications"))
            paths.addAll(system.findAppsPathsInDirectory("Android Studio", "${system.homeDir}/Applications"))
            paths.addAll(
                system.findAppsPathsInDirectory(
                    "Android Studio",
                    "${system.homeDir}/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio",
                    true
                )
            )
        }
        if (paths.isEmpty()) {
            result.addFailure(
                "No Android Studio installation found",
                "Get Android Studio from https://developer.android.com/studio"
            )
            return result.build()
        }

        val jsonFormatter = Json { ignoreUnknownKeys = true }
        val studioInstallations =
            paths.mapNotNull { findAndroidStudio(it) }.filter { app -> //filter Toolbox backup versions
                if (app.location != null && app.location.contains("Toolbox", ignoreCase = true)) {
                    val channelPath = app.location.substringBeforeLast('/').substringBeforeLast('/')
                    val historyFile = "$channelPath/.history.json"
                    val historyJson = system.readFile(historyFile) ?: return@filter true
                    val history: ToolboxHistory = try {
                        jsonFormatter.decodeFromString(historyJson)
                    } catch (e: Exception) {
                        system.logger.e("Parse Toolbox history error: $e", e)
                        return@filter true
                    }
                    val latestBuild = history.history.maxBy { it.timestamp }.item.build
                    if (app.location.contains(latestBuild)) {
                        return@filter true
                    } else {
                        system.logger.d("Skip Toolbox backup: $app")
                        return@filter false
                    }
                } else {
                    true
                }
            }

        if (studioInstallations.count() > 1) {
            result.addInfo("Multiple Android Studio installations found")
        }

        studioInstallations.forEach { app ->
            val androidStudio = AppManager(system, app)
            val kotlinPlugin = androidStudio.getPlugin(AppManager.KOTLIN_PLUGIN)
            val kmmPlugin = androidStudio.getPlugin(AppManager.KMM_PLUGIN)
            val embeddedJavaVersion = androidStudio.getEmbeddedJavaVersion()

            val message = """
                Android Studio (${app.version})
                Location: ${app.location}
                Bundled Java: ${embeddedJavaVersion ?: "not found"}
                Kotlin Plugin: ${kotlinPlugin?.version ?: "not installed"}
                Kotlin Multiplatform Mobile Plugin: ${kmmPlugin?.version ?: "not installed"}
            """.trimIndent()
            result.addEnvironment(*buildSet {
                add(EnvironmentPiece.AndroidStudio(app.version))
                if (kotlinPlugin != null) add(EnvironmentPiece.KotlinPlugin(kotlinPlugin.version))
                if (kmmPlugin != null) add(EnvironmentPiece.KmmPlugin(kmmPlugin.version))
            }.toTypedArray())

            if (checkForPlugins) {
                if (kotlinPlugin == null) {
                    result.addFailure(
                        message,
                        "Install Kotlin plugin - https://plugins.jetbrains.com/plugin/6954-kotlin"
                    )
                    return@forEach
                }
                if (!kotlinPlugin.isEnabled) {
                    result.addFailure(
                        message,
                        "Kotlin plugin is disabled. Enable Kotlin in Android Studio settings."
                    )
                    return@forEach
                }
                if (kmmPlugin == null) {
                    result.addWarning(
                        message,
                        "Install Kotlin Multiplatform Mobile plugin - https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile"
                    )
                    return@forEach
                }
                if (!kmmPlugin.isEnabled) {
                    result.addWarning(
                        message,
                        "Kotlin Multiplatform Mobile plugin is disabled. Enable Kotlin Multiplatform Mobile in Android Studio settings."
                    )
                    return@forEach
                }
            }

            result.addSuccess(message)
            result.setConclusion(DiagnosisResult.Success) //at least one AS is fine
        }

        result.addInfo(
            "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
            "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
        )

        return result.build()
    }

    private suspend fun findAndroidStudio(path: String): Application? {
        system.logger.d("findAndroidStudio($path)")
        val plist = system.parsePlist("$path/Contents/Info.plist") ?: return null
        val version = plist["CFBundleVersion"]?.toString()?.trim('"') ?: return null
        val name = plist["CFBundleName"]?.toString()?.trim('"')
            ?: path.substringAfterLast("/").substringBeforeLast(".")
        return Application(name, Version(version), path)
    }
}
