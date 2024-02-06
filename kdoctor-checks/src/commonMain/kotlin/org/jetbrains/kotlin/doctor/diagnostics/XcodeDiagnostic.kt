package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class XcodeDiagnostic(private val system: System, private val checkCliTools: Boolean) : Diagnostic() {
    override val title = "Xcode"

    override suspend fun diagnose(): Diagnosis {
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

        if (checkCliTools && xcodeInstallations.isNotEmpty()) {
            val tools = system.execute("xcode-select", "-p").output
            if (tools != null) {
                if (xcodeInstallations.none { tools.startsWith(it.location!!) }) {
                    result.addFailure(
                        "Current command line tools: $tools",
                        "You have to select command line tools bundled to Xcode",
                        "Command line tools can be configured in Xcode -> Settings -> Locations -> Locations"
                    )
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
                result.addFailure(
                    "Command line tools are not configured",
                    "Command line tools can be configured in Xcode -> Settings -> Locations -> Locations"
                )
            }

            val xcodeJavaHome =
                system.execute("defaults", "read", "com.apple.dt.Xcode", "IDEApplicationwideBuildSettings").output
                    ?.lines()
                    ?.lastOrNull { it.contains("\"JAVA_HOME\"") }
                    ?.split("=")
                    ?.lastOrNull()
                    ?.trim(' ', '"', ';')
                    ?: system.execute("/usr/libexec/java_home").output
            if (xcodeJavaHome != null) {
                result.addInfo(
                    "Xcode JAVA_HOME: $xcodeJavaHome",
                    "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
                )
            }
        }

        return result.build()
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