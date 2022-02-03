package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Application
import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.entity.Version
import org.jetbrains.kotlin.doctor.entity.execute
import org.jetbrains.kotlin.doctor.entity.getEnvVar

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
            messages.addInfo(
                "System ruby is currently used",
                "Consider installing ruby via Homebrew, rvm or other package manager in case of issues with cocoapods installation"
            )
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

        val gemList = System.execute("gem", "list", "--local", "cocoapods").output
        val cocoapodsGems = gemList?.split("\n")?.mapNotNull {
            val nameAndVersion = it.split(" ", limit = 2)
            val name = nameAndVersion.firstOrNull() ?: return@mapNotNull null
            val versionString = nameAndVersion.lastOrNull()?.trim('(', ')')
            val version = if (versionString != null) Version(versionString) else Version.unknown
            Application(name, version)
        }

        val cocoapods = cocoapodsGems?.firstOrNull { it.name == "cocoapods" }
        if (cocoapods == null) {
            messages.addFailure(
                "cocoapods not found",
                "Get cocoapods from https://guides.cocoapods.org/using/getting-started.html#installation"
            )
            return messages
        }

        messages.addSuccess("${cocoapods.name} (${cocoapods.version})")

        val cocoapodsGenerate = cocoapodsGems.firstOrNull { it.name == "cocoapods-generate" }
        if (cocoapodsGenerate == null) {
            messages.addFailure(
                "cocoapods-generate plugin not found",
                "Get cocoapods-generate from https://github.com/square/cocoapods-generate#installation"
            )
            return messages
        }
        if (cocoapodsGenerate.version < Version(2, 2, 2)) {
            messages.addFailure(
                "Cocoapods-generate version ${cocoapodsGenerate.version} is not supported",
                "Get the latest version of cocoapods-generate plugin from https://github.com/square/cocoapods-generate#installation"
            )
            return messages
        }

        messages.addSuccess("${cocoapodsGenerate.name} (${cocoapodsGenerate.version})")

        val locale = System.getEnvVar("LC_CTYPE")
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