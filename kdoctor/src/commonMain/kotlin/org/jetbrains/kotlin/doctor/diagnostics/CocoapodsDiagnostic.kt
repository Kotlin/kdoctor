package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Application
import org.jetbrains.kotlin.doctor.entity.System
import org.jetbrains.kotlin.doctor.entity.Version
import org.jetbrains.kotlin.doctor.entity.execute

class CocoapodsDiagnostic : Diagnostic("Cocoapods") {

    override fun runChecks(): List<Message> {
        val messages = mutableListOf<Message>()

        val cocoapodsFormulae = System.execute("brew", "list", "cocoapods").output
            ?.lines()

        val cocoapodsFromHomebrew = cocoapodsFormulae?.let { lines ->
            lines.find { it.contains("/bin/pod") }
                ?.split("/")
                ?.find { it.matches(Regex(COCOAPODS_VERSION_PATTERN)) }
                ?.substringBefore("_")
                ?.let { version ->
                    Application(name = "cocoapods", version = Version(version))
                }
        }

        if (cocoapodsFromHomebrew != null) {
            messages.addSuccess("Found cocoapods in Homebrew: ${cocoapodsFromHomebrew.name} (${cocoapodsFromHomebrew.version})")
        }

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

        val cocoapodsFromGem = cocoapodsGems?.firstOrNull { it.name == "cocoapods" }

        if (cocoapodsFromGem != null) {
            messages.addSuccess("Found cocoapods in Gem: ${cocoapodsFromGem.name} (${cocoapodsFromGem.version})")
        }

        if (cocoapodsFromGem == null && cocoapodsFromHomebrew == null) {
            messages.addFailure(
                "cocoapods not found",
                "Get cocoapods from https://guides.cocoapods.org/using/getting-started.html#installation"
            )
            return messages
        }

        if (cocoapodsFromGem == null) {
            return messages
        }

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

        val locale = System.execute("/usr/bin/locale", "-k", "LC_CTYPE", "") //trailing empty arg is required
        if (locale.output == null || !locale.output.contains("UTF-8")) {
            val hint = """
            Consider adding the following to ${System.getShell()?.profile ?: "shell profile"}
            export LC_ALL=en_US.UTF-8
        """.trimIndent()
            if (cocoapodsFromGem.version > Version(1, 10, 2)) {
                messages.addFailure("CocoaPods requires your terminal to be using UTF-8 encoding.", hint)
            } else {
                messages.addWarning("CocoaPods requires your terminal to be using UTF-8 encoding.", hint)
            }
        }

        return messages
    }

    companion object {
        private const val COCOAPODS_VERSION_PATTERN = "^\\d\\.\\d{1,2}.\\d{1,2}(_\\d)?$"
    }
}