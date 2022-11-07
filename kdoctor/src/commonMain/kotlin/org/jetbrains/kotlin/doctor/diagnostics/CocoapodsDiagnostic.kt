package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*

class CocoapodsDiagnostic : Diagnostic() {
    override fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder("Cocoapods")

        val rubyVersion = System.execute("ruby", "-v").output
        val rubyLocation = System.execute("which", "ruby").output
        if (rubyLocation == null || rubyVersion == null) {
            result.addFailure(
                "ruby not found",
                "Get ruby from https://www.ruby-lang.org/en/documentation/installation/"
            )
            return result.build()
        }

        val ruby = Application("ruby", Version(rubyVersion), rubyLocation)

        if (ruby.version >= Version(3, 0, 0)) {
            result.addFailure(
                """
                ruby version ${ruby.version} is not compatible with cocoapods-generate
                More details: https://github.com/rubygems/rubygems/issues/4338
            """.trimIndent(),
                "Downgrade ruby to version < 3.0.0"
            )
        }

        result.addSuccess("${ruby.name} (${ruby.version})")

        if (ruby.location == "/usr/bin/ruby") {
            var title = "System ruby is currently used"
            if (System.isUsingM1) {
                result.addFailure(
                    title,
                    "CocoaPods is not compatible with system ruby installation on Apple M1 computers.",
                    "Please install ruby 2.7 via Homebrew, rvm, rbenv or other tool and make it default"
                )
            } else {
                result.addInfo(
                    title,
                    "Consider installing ruby 2.7 via Homebrew, rvm or other package manager in case of issues with CocoaPods installation"
                )
            }
        }

        val rubyGemsVersion = System.execute("gem", "-v").output
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
        val cocoapodsVersionOutput = System.execute("pod", "--version").output
        if (cocoapodsVersionOutput != null) {
            val cocoapodsVersion = Version(cocoapodsVersionOutput)
            cocoapods = Application("cocoapods", cocoapodsVersion)
        }
        if (cocoapods == null) {
            //check if installed via brew but not linked to /usr/bin
            val cocoapodsBrewInstallation = System.execute("brew", "list", "cocoapods", "--versions").output
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
        } else if (cocoapods.version < Version(1, 8, 0)) {
            result.addFailure(
                "cocoapods version ${cocoapods.version.version} is outdated",
                "Update your cocoapods installation to the latest available version"
            )
            return result.build()
        }
        result.addSuccess("${cocoapods.name} (${cocoapods.version})")

        val locale = System.execute("/usr/bin/locale", "-k", "LC_CTYPE").output
        if (locale == null || !locale.contains("UTF-8")) {
            val hint = """
                Consider adding the following to ${System.getShell()?.profile ?: "shell profile"}
                export LC_ALL=en_US.UTF-8
            """.trimIndent()
            if (cocoapods.version > Version(1, 10, 2)) {
                result.addFailure("CocoaPods requires your terminal to be using UTF-8 encoding.", hint)
            } else {
                result.addWarning("CocoaPods requires your terminal to be using UTF-8 encoding.", hint)
            }
        }

        return result.build()
    }
}