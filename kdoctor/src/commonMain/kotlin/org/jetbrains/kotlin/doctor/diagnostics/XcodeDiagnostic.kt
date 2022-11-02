package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class XcodeDiagnostic : Diagnostic("Xcode") {
    override fun runChecks(): List<Message> {
        val messages = mutableListOf<Message>()

        val paths = mutableSetOf<String>()
        paths.addAll(System.findAppPaths("com.apple.dt.Xcode"))
        if (paths.isEmpty()) {
            paths.addAll(System.findAppsPathsInDirectory("Xcode", "/Applications"))
            paths.addAll(System.findAppsPathsInDirectory("Xcode", "${System.homeDir}/Applications"))
        }
        if (paths.isEmpty()) {
            messages.addFailure(
                "No Xcode installation found",
                "Get Xcode from https://developer.apple.com/xcode/"
            )
        }

        val xcodeInstallations = paths.mapNotNull { AppManager.findApp(it) }

        if (xcodeInstallations.count() > 1) {
            messages.addInfo("Multiple Xcode installations found")
        }

        xcodeInstallations.forEach { xcode ->
            messages.addSuccess(
                "${xcode.name} (${xcode.version})\nLocation: ${xcode.location}"
            )
        }

        if (xcodeInstallations.count() > 1) {
            val tools = System.execute("xcode-select", "-p").output
            if (tools != null) {
                messages.addSuccess("Current command line tools: $tools")
            }
        }

        val xcrun = System.execute("xcrun", "cc").output
        if (xcrun?.contains("license") == true) {
            messages.addFailure(
                "Xcode license is not accepted",
                "Accept Xcode license on the first Xcode launch or execute 'sudo xcodebuild -license' in the terminal"
            )
        }

        if (xcodeInstallations.isNotEmpty()) {
            val result = System.execute("xcodebuild", "-checkFirstLaunchStatus")
            if (result.code != 0) {
                messages.addFailure(
                    "Xcode requires to perform the First Launch tasks",
                    "Launch Xcode or execute 'xcodebuild -runFirstLaunch' in terminal"
                )
            }
        }
        return messages
    }
}