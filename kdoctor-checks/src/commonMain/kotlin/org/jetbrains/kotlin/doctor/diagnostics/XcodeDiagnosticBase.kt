package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

abstract class XcodeDiagnosticBase(protected val system: System) : Diagnostic() {
    final override val title = "Xcode"

    final override suspend fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder(title)

        val paths = mutableSetOf<String>()
        paths.addAll(system.spotlightFindAppPaths("com.apple.dt.Xcode"))
        if (paths.isEmpty()) {
            paths.addAll(system.findAppsPathsInDirectory("Xcode", "/Applications"))
            paths.addAll(system.findAppsPathsInDirectory("Xcode", "${system.homeDir}/Applications"))
        }
        if (paths.isEmpty()) {
            result.addFailure(
                "No Xcode installation found",
                "Get Xcode from https://developer.apple.com/xcode/"
            )
        }

        val xcodeInstallations = paths.mapNotNull { findXcode(it) }
        diagnoseInstallations(xcodeInstallations, result)

        return result.build()
    }

    protected open suspend fun diagnoseInstallations(xcodeInstallations: List<Application>, result: Diagnosis.Builder) {
        if (xcodeInstallations.count() > 1) {
            result.addInfo("Multiple Xcode installations found")
        }

        xcodeInstallations.forEach { xcode ->
            result.addSuccess(
                "${xcode.name} (${xcode.version})\nLocation: ${xcode.location}"
            )
            result.addEnvironment(EnvironmentPiece.Xcode(xcode.version))
        }

        val xcrun = system.execute("xcrun", "cc").output
        if (xcrun?.contains("license") == true) {
            result.addFailure(
                "Xcode license is not accepted",
                "Accept Xcode license on the first Xcode launch or execute 'sudo xcodebuild -license' in the terminal"
            )
        }

        if (xcodeInstallations.isNotEmpty()) {
            val tools = system.execute("xcode-select", "-p").output
            if (tools != null) {
                if (xcodeInstallations.none { tools.startsWith(it.location!!) }) {
                    activeXcodeInstallationNotFound(result, tools)
                } else {
                    if (xcodeInstallations.size > 1) {
                        result.addSuccess("Current command line tools: $tools")
                    }

                    val launchStatus = system.execute("xcodebuild", "-checkFirstLaunchStatus")
                    if (launchStatus.code != 0) {
                        result.addFailure(
                            "Xcode requires to perform the First Launch tasks",
                            "Launch Xcode and complete setup"
                        )
                    }
                }
            } else {
                noXcodeInstallationConfigured(result)
            }
        }
    }

    protected open fun activeXcodeInstallationNotFound(result: Diagnosis.Builder, activeInstallation: String?) {
        result.addFailure(
            "Current command line tools: $activeInstallation",
            "You have to select command line tools bundled to Xcode",
            "Command line tools can be configured in Xcode -> Settings -> Locations -> Locations"
        )
    }

    protected open fun noXcodeInstallationConfigured(result: Diagnosis.Builder) {
        result.addFailure(
            "Command line tools are not configured",
            "Command line tools can be configured in Xcode -> Settings -> Locations -> Locations"
        )
    }

    private suspend fun findXcode(path: String): Application? {
        system.logger.d("findXcode($path)")
        val plist = system.parsePlist("$path/Contents/Info.plist") ?: return null
        val version = plist["CFBundleShortVersionString"]?.toString()?.trim('"') ?: return null
        val name = plist["CFBundleName"]?.toString()
            ?.trim('"')
            ?: path.substringAfterLast("/").substringBeforeLast(".")
        return Application(name, Version(version), path)
    }
}