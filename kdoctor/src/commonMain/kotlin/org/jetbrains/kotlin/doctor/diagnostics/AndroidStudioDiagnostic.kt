package org.jetbrains.kotlin.doctor.diagnostics

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.doctor.entity.*

class AndroidStudioDiagnostic : Diagnostic() {
    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder("Android Studio")

        val paths = mutableSetOf<String>()
        paths.addAll(System.findAppPaths("com.google.android.studio*"))
        if (paths.isEmpty()) {
            paths.addAll(System.findAppsPathsInDirectory("Android Studio", "/Applications"))
            paths.addAll(System.findAppsPathsInDirectory("Android Studio", "${System.homeDir}/Applications"))
            paths.addAll(
                System.findAppsPathsInDirectory(
                    "Android Studio",
                    "${System.homeDir}/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio",
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

        val studioInstallations = paths.mapNotNull { AppManager.findApp(it) }.filter { app -> //filter Toolbox backup versions
            if (app.location != null && app.location.contains("Toolbox", ignoreCase = true)) {
                val channelPath = app.location.substringBeforeLast('/').substringBeforeLast('/')
                val historyFile = "$channelPath/.history.json"
                val historyJson = System.readFile(historyFile)
                val jsonFormatter = Json { ignoreUnknownKeys = true }
                val history = historyJson?.let {
                    try {
                        jsonFormatter.decodeFromString<ToolboxHistory>(it)
                    } catch (e: Exception) {
                        null
                    }
                }?.history
                val currentBuild = history?.maxByOrNull { it.timestamp }?.item?.build
                currentBuild?.let { app.location.contains(currentBuild) } ?: true
            } else {
                true
            }
        }

        if (studioInstallations.count() > 1) {
            result.addInfo("Multiple Android Studio installations found")
        }

        studioInstallations.forEach { app ->
            val androidStudio = AppManager(app)
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

            val hints = mutableListOf<String>()
            if (kotlinPlugin == null || kmmPlugin == null) {
                if (kotlinPlugin == null) {
                    hints.add("Install Kotlin plugin - https://plugins.jetbrains.com/plugin/6954-kotlin")
                }

                if (kmmPlugin == null) {
                    hints.add("Install Kotlin Multiplatform Mobile plugin - https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile")
                }
                result.addFailure(message, *hints.toTypedArray())
                return@forEach
            }

            val kmmKotlinVersion = Version(kmmPlugin.version.version.substringAfter("(").substringBefore(")"))
            val minKotlinVersion = kmmKotlinVersion.prevMinorVersion
            val maxKmmKotlinVersion = kotlinPlugin.version.nextMinorVersion

            if (kotlinPlugin.version < minKotlinVersion) {
                hints.add("Update Kotlin plugin. Current version of Kotlin Multiplatform Mobile plugin requires $minKotlinVersion")
            } else if (kotlinPlugin.version >= maxKmmKotlinVersion) {
                hints.add("Update Kotlin Multiplatform Mobile plugin")
            }
            if (!kotlinPlugin.isEnabled) {
                hints.add("Kotlin plugin is disabled. Enable Kotlin in Android Studio settings.")
            }
            if (!kmmPlugin.isEnabled) {
                hints.add("Kotlin Multiplatform Mobile plugin is disabled. Enable Kotlin Multiplatform Mobile in Android Studio settings.")
            }

            if (hints.isNotEmpty()) {
                result.addFailure(message, *hints.toTypedArray())
            } else {
                result.addSuccess(message)
                result.setConclusion(DiagnosisResult.Success) //at least one AS is fine
            }
        }

        return result.build()
    }
}
