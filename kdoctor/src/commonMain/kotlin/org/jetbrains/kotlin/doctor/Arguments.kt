package org.jetbrains.kotlin.doctor

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

class Arguments(args: Array<String>) {
    private val parser = ArgParser("kdoctor")

    val isShowVersion: Boolean by parser.option(
        ArgType.Boolean,
        fullName = "version",
        description = "print KDoctor version"
    ).default(false)

    val isVerbose: Boolean by parser.option(
        ArgType.Boolean,
        shortName = "v",
        fullName = "verbose",
        description = "print extended information"
    ).default(false)

    val isDebug: Boolean by parser.option(
        ArgType.Boolean,
        fullName = "debug",
        description = "debug mode"
    ).default(false)

    val localCompatibilityJson: String? by parser.option(
        ArgType.String,
        fullName = "compatibilityJson"
    )

    val projectPath: String? by parser.option(
        ArgType.String,
        shortName = "p",
        fullName = "project",
        description = "path to a Gradle project root directory"
    )

    init {
        parser.parse(args)
    }
}