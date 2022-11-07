package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class XcodeDiagnostic : Diagnostic() {
    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder("Xcode")

        val paths = mutableSetOf<String>()
        paths.addAll(System.findAppPaths("com.apple.dt.Xcode"))
        if (paths.isEmpty()) {
            paths.addAll(System.findAppsPathsInDirectory("Xcode", "/Applications"))
            paths.addAll(System.findAppsPathsInDirectory("Xcode", "${System.homeDir}/Applications"))
        }
        if (paths.isEmpty()) {
            result.addFailure(
                "No Xcode installation found",
                "Get Xcode from https://developer.apple.com/xcode/"
            )
        }

        val xcodeInstallations = paths.mapNotNull { AppManager.findApp(it) }

        if (xcodeInstallations.count() > 1) {
            result.addInfo("Multiple Xcode installations found")
        }

        xcodeInstallations.forEach { xcode ->
            result.addSuccess(
                "${xcode.name} (${xcode.version})\nLocation: ${xcode.location}"
            )
        }

        if (xcodeInstallations.count() > 1) {
            val tools = System.execute("xcode-select", "-p").output
            if (tools != null) {
                result.addSuccess("Current command line tools: $tools")
            }
        }

        val xcrun = System.execute("xcrun", "cc").output
        if (xcrun?.contains("license") == true) {
            result.addFailure(
                "Xcode license is not accepted",
                "Accept Xcode license on the first Xcode launch or execute 'sudo xcodebuild -license' in the terminal"
            )
        }

        if (xcodeInstallations.isNotEmpty()) {
            val launchStatus = System.execute("xcodebuild", "-checkFirstLaunchStatus")
            if (launchStatus.code != 0) {
                result.addFailure(
                    "Xcode requires to perform the First Launch tasks",
                    "Launch Xcode or execute 'xcodebuild -runFirstLaunch' in terminal"
                )
            }
        }
        return result.build()
    }
}