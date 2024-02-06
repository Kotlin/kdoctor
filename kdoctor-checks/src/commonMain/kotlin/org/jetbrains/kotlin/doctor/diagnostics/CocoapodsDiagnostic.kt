package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class CocoapodsDiagnostic(private val system: System) : Diagnostic() {
    override val title = "CocoaPods"

    override suspend fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder(title)

        fun Diagnosis.Builder.buildWithRecommendation(): Diagnosis {
            val original = build()
            if (original.conclusion == DiagnosisResult.Failure) {
                val info = DiagnosisEntry(
                    DiagnosisResult.Warning,
                    "CocoaPods configuration is not required, but highly recommended for full-fledged development"
                )
                return original.copy(
                    conclusion = DiagnosisResult.Warning,
                    entries = listOf(info) + original.entries
                )
            } else {
                return original
            }
        }

        val rubyVersion = system.execute("ruby", "-v").output
        val rubyLocation = system.execute("which", "ruby").output
        if (rubyLocation == null || rubyVersion == null) {
            result.addFailure(
                "ruby not found",
                "Get ruby from https://www.ruby-lang.org/en/documentation/installation/"
            )
            return result.buildWithRecommendation()
        }

        val ruby = Application("ruby", Version(rubyVersion), rubyLocation)
        result.addSuccess("${ruby.name} (${ruby.version})")

        if (ruby.location == "/usr/bin/ruby") {
            val title = "System ruby is currently used"
            if (system.isUsingM1()) {
                result.addFailure(
                    title,
                    "CocoaPods is not compatible with system ruby installation on Apple M1 computers.",
                    "Please install ruby via Homebrew, rvm, rbenv or other tool and make it default",
                    "Detailed information: https://stackoverflow.com/questions/64901180/how-to-run-cocoapods-on-apple-silicon-m1/66556339#66556339"
                )
            } else {
                result.addInfo(
                    title,
                    "Consider installing ruby via Homebrew, rvm or other package manager in case of issues with CocoaPods installation"
                )
            }
        }

        val rubyGemsVersion = system.execute("gem", "-v").output
        if (rubyGemsVersion == null) {
            result.addFailure(
                "ruby gems not found",
                "Get ruby gems from https://rubygems.org/pages/download"
            )
            return result.buildWithRecommendation()
        }

        val gems = Application("ruby gems", Version(rubyGemsVersion))
        result.addSuccess("${gems.name} (${gems.version})")

        var cocoapods: Application? = null
        val cocoapodsVersionOutput = system.execute("pod", "--version").output?.lines()?.last()
        if (cocoapodsVersionOutput != null) {
            val cocoapodsVersion = Version(cocoapodsVersionOutput)
            cocoapods = Application("cocoapods", cocoapodsVersion)
        }
        if (cocoapods == null) {
            //check if installed via brew but not linked to /usr/bin
            val cocoapodsBrewInstallation = system.execute("brew", "list", "cocoapods", "--versions").output
            if (cocoapodsBrewInstallation?.isNotBlank() == true) {
                result.addFailure(
                    "CocoaPods are installed via Homebrew but not linked to /usr/local/bin",
                    "Execute 'brew link --overwrite cocoapods'"
                )
            } else {
                result.addFailure(
                    "cocoapods not found",
                    "Get cocoapods from https://guides.cocoapods.org/using/getting-started.html#installation"
                )
            }
            return result.buildWithRecommendation()
        }
        result.addSuccess("${cocoapods.name} (${cocoapods.version})")

        val locale = system.execute("/usr/bin/locale").output
        val lang = locale?.lines()?.firstOrNull { it.startsWith("LANG=") }?.substringAfter("=")
        val lcAll = locale?.lines()?.firstOrNull { it.startsWith("LC_ALL=") }?.substringAfter("=")
        val exportHint = buildString {
            if (lang.isNullOrEmpty() || !lang.contains("UTF-8")) {
                appendLine("export LANG=en_US.UTF-8")
            }
            if (lcAll.isNullOrEmpty() || !lcAll.contains("UTF-8")) {
                appendLine("export LC_ALL=en_US.UTF-8")
            }
        }.trim()
        if (exportHint.isNotEmpty()) {
            val hint = "Consider adding the following to ${system.shell.await()?.profile ?: "shell profile"}"
            if (cocoapods.version > Version(1, 10, 2)) {
                result.addFailure("CocoaPods requires your terminal to be using UTF-8 encoding.", hint, exportHint)
            } else {
                result.addWarning("CocoaPods requires your terminal to be using UTF-8 encoding.", hint, exportHint)
            }
        }

        result.addEnvironment(
            EnvironmentPiece.Ruby(ruby.version),
            EnvironmentPiece.RubyGems(gems.version),
            EnvironmentPiece.Cocoapods(cocoapods.version)
        )
        return result.buildWithRecommendation()
    }
}