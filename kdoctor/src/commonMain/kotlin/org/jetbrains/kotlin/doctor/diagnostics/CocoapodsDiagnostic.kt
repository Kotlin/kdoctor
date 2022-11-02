package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Application
import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.entity.Version
import org.jetbrains.kotlin.doctor.entity.execute

class CocoapodsDiagnostic : Diagnostic("Cocoapods") {
    override fun runChecks(): List<Message> {
        val messages = mutableListOf<Message>()

        val rubyVersion = System.execute("ruby", "-v").output
        val rubyLocation = System.execute("which", "ruby").output
        if (rubyLocation == null || rubyVersion == null) {
            messages.addFailure(
                "ruby not found",
                "Get ruby from https://www.ruby-lang.org/en/documentation/installation/"
            )
            return messages
        }

        val ruby = Application("ruby", Version(rubyVersion), rubyLocation)

        if (ruby.version >= Version(3, 0, 0)) {
            messages.addFailure(
                """
                ruby version ${ruby.version} is not compatible with cocoapods-generate
                More details: https://github.com/rubygems/rubygems/issues/4338
            """.trimIndent(),
                "Downgrade ruby to version < 3.0.0"
            )
        }

        messages.addSuccess("${ruby.name} (${ruby.version})")

        if (ruby.location == "/usr/bin/ruby") {
            var title = "System ruby is currently used"
            if (System.isUsingM1) {
                messages.addFailure(
                    title,
                    "CocoaPods is not compatible with system ruby installation on Apple M1 computers.",
                    "Please install ruby 2.7 via Homebrew, rvm, rbenv or other tool and make it default"
                )
            } else {
                messages.addInfo(
                    title,
                    "Consider installing ruby 2.7 via Homebrew, rvm or other package manager in case of issues with CocoaPods installation"
                )
            }
        }

        val rubyGemsVersion = System.execute("gem", "-v").output
        if (rubyGemsVersion == null) {
            messages.addFailure(
                "ruby gems not found",
                "Get ruby gems from https://rubygems.org/pages/download"
            )
            return messages
        }

        val gems = Application("ruby gems", Version(rubyGemsVersion))
        messages.addSuccess("${gems.name} (${gems.version})")

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
                messages.addFailure(
                    "Cocoapods are installed via Homebrew but not linked to /usr/local/bin",
                    "Execute 'brew link --overwrite cocoapods'"
                )
            } else {
                messages.addFailure(
                    "cocoapods not found",
                    "Get cocoapods from https://guides.cocoapods.org/using/getting-started.html#installation"
                )
            }
            return messages
        } else if (cocoapods.version < Version(1, 8, 0)) {
            messages.addFailure(
                "cocoapods version ${cocoapods.version.version} is outdated",
                "Update your cocoapods installation to the latest available version"
            )
            return messages
        }
        messages.addSuccess("${cocoapods.name} (${cocoapods.version})")

        var cocoapodsGenerate: Application? = null
        val cocoapodsGenerateName = "cocoapods-generate"
        val plugins = System.execute("pod", "plugins", "installed", "--no-ansi").output
        val cocoaPodsGenerateEntry = plugins?.lines()?.find { it.contains(cocoapodsGenerateName) }
        if (cocoaPodsGenerateEntry != null) {
            val version = cocoaPodsGenerateEntry.split(":").lastOrNull()?.let { Version(it.trim()) }
            if (version != null) {
                cocoapodsGenerate = Application(cocoapodsGenerateName, version)
            }
        }

        if (cocoapodsGenerate == null) {
            messages.addInfo(
                "cocoapods-generate plugin not found",
                "Before Kotlin 1.7.0 you have to use cocoapods-generate plugin",
                "Get cocoapods-generate from https://github.com/square/cocoapods-generate#installation"
            )
        } else {
            if (cocoapodsGenerate.version < Version(2, 2, 2)) {
                messages.addFailure(
                    "Cocoapods-generate version ${cocoapodsGenerate.version} is not supported",
                    "Get the latest version of cocoapods-generate plugin from https://github.com/square/cocoapods-generate#installation"
                )
            } else {
                messages.addSuccess("${cocoapodsGenerate.name} (${cocoapodsGenerate.version})")
            }
        }

        val locale = System.execute("/usr/bin/locale", "-k", "LC_CTYPE").output
        if (locale == null || !locale.contains("UTF-8")) {
            val hint = """
                Consider adding the following to ${System.getShell()?.profile ?: "shell profile"}
                export LC_ALL=en_US.UTF-8
            """.trimIndent()
            if (cocoapods.version > Version(1, 10, 2)) {
                messages.addFailure("CocoaPods requires your terminal to be using UTF-8 encoding.", hint)
            } else {
                messages.addWarning("CocoaPods requires your terminal to be using UTF-8 encoding.", hint)
            }
        }

        return messages
    }
}