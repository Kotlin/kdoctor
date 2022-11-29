package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class CocoapodsDiagnostic(private val system: System) : Diagnostic() {
    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder("Cocoapods")

        val rubyVersion = system.execute("ruby", "-v").output
        val rubyLocation = system.execute("which", "ruby").output
        if (rubyLocation == null || rubyVersion == null) {
            result.addFailure(
                "ruby not found",
                "Get ruby from https://www.ruby-lang.org/en/documentation/installation/"
            )
            return result.build()
        }

        val ruby = Application("ruby", Version(rubyVersion), rubyLocation)
        result.addSuccess("${ruby.name} (${ruby.version})")

        if (ruby.location == "/usr/bin/ruby") {
            var title = "System ruby is currently used"
            if (system.isUsingM1()) {
                result.addFailure(
                    title,
                    "CocoaPods is not compatible with system ruby installation on Apple M1 computers.",
                    "Please install ruby via Homebrew, rvm, rbenv or other tool and make it default"
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
            return result.build()
        }

        val gems = Application("ruby gems", Version(rubyGemsVersion))
        result.addSuccess("${gems.name} (${gems.version})")

        var cocoapods: Application? = null
        val cocoapodsVersionOutput = system.execute("pod", "--version").output
        if (cocoapodsVersionOutput != null) {
            val cocoapodsVersion = Version(cocoapodsVersionOutput)
            cocoapods = Application("cocoapods", cocoapodsVersion)
        }
        if (cocoapods == null) {
            //check if installed via brew but not linked to /usr/bin
            val cocoapodsBrewInstallation = system.execute("brew", "list", "cocoapods", "--versions").output
            if (cocoapodsBrewInstallation?.isNotBlank() == true) {
                result.addFailure(
                    "Cocoapods are installed via Homebrew but not linked to /usr/local/bin",
                    "Execute 'brew link --overwrite cocoapods'"
                )
            } else {
                result.addFailure(
                    "cocoapods not found",
                    "Get cocoapods from https://guides.cocoapods.org/using/getting-started.html#installation"
                )
            }
            return result.build()
        }
        result.addSuccess("${cocoapods.name} (${cocoapods.version})")

        val locale = system.execute("/usr/bin/locale", "-k", "LC_CTYPE").output
        if (locale == null || !locale.contains("UTF-8")) {
            val hint = """
                Consider adding the following to ${system.shell?.profile ?: "shell profile"}
                export LC_ALL=en_US.UTF-8
            """.trimIndent()
            if (cocoapods.version > Version(1, 10, 2)) {
                result.addFailure("CocoaPods requires your terminal to be using UTF-8 encoding.", hint)
            } else {
                result.addWarning("CocoaPods requires your terminal to be using UTF-8 encoding.", hint)
            }
        }

        result.addEnvironment(
            EnvironmentPiece.Ruby(ruby.version),
            EnvironmentPiece.RubyGems(gems.version),
            EnvironmentPiece.Cocoapods(cocoapods.version)
        )
        return result.build()
    }
}