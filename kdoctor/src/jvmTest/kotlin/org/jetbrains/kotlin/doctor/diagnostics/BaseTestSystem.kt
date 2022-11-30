package org.jetbrains.kotlin.doctor.diagnostics

import co.touchlab.kermit.Logger
import io.ktor.client.*
import org.jetbrains.kotlin.doctor.entity.*

open class BaseTestSystem : System {
    override val currentOS: OS = OS.MacOS
    override val osVersion: Version? = Version("42.777")
    override val cpuInfo: String? = "test_cpu"
    override val homeDir: String = "/Users/my"
    override val shell: Shell? = Shell.Zsh

    override fun getEnvVar(name: String): String? = when (name) {
        "JAVA_HOME" -> "$homeDir/.sdkman/candidates/java/current"
        else -> null
    }

    override fun execute(command: String, vararg args: String): ProcessResult {
        val cmd = (command + " " + args.joinToString(" ")).trim()
        Logger.d("exec '$cmd'")
        return executeCmd(cmd)
    }

    open fun executeCmd(cmd: String): ProcessResult {
        val output = when (cmd) {
            "which java" -> {
                "$homeDir/.sdkman/candidates/java/current/bin/java"
            }
            "java -version" -> {
                """
                    openjdk version "11.0.16" 2022-07-19 LTS
                    OpenJDK Runtime Environment Corretto-11.0.16.8.1 (build 11.0.16+8-LTS)
                    OpenJDK 64-Bit Server VM Corretto-11.0.16.8.1 (build 11.0.16+8-LTS, mixed mode)
                """.trimIndent()
            }

            "/usr/libexec/java_home" -> {
                "$homeDir/Library/Java/JavaVirtualMachines/jbr-17.0.5/Contents/Home"
            }
            "defaults read com.apple.dt.Xcode IDEApplicationwideBuildSettings" -> {
                "\"JAVA_HOME\"=$homeDir/.sdkman/candidates/java/current;"
            }
            "/usr/bin/mdfind kMDItemCFBundleIdentifier=\"com.apple.dt.Xcode\"" -> {
                "/Applications/Xcode.app"
            }
            "/usr/bin/plutil -convert json -o - /Applications/Xcode.app/Contents/Info.plist" -> {
                "{\"CFBundleShortVersionString\":\"13.4.1\", \"CFBundleName\":\"Xcode\"}"
            }
            "/usr/bin/mdfind kMDItemCFBundleIdentifier=\"com.google.android.studio*\"" -> {
                "$homeDir/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/222.4345.14.2221.9252092/Android Studio Preview.app"
            }
            "/usr/bin/plutil -convert json -o - $homeDir/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/222.4345.14.2221.9252092/Android Studio Preview.app/Contents/Info.plist" -> {
                """
                    {
                      "CFBundleVersion": "AI-222.4345.14.2221.9252092",
                      "CFBundleName": "Android Studio",
                      "JVMOptions": {
                        "Properties": {
                          "idea.paths.selector": "data/Directory/Name"
                        }
                      }
                    }
                """.trimIndent()
            }
            "xcrun cc" -> {
                "clang: error: no input files"
            }
            "xcodebuild -checkFirstLaunchStatus" -> {
                return ProcessResult(0, null)
            }
            "ruby -v" -> {
                "ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]"
            }
            "which ruby" -> {
                "$homeDir/.rbenv/shims/ruby"
            }
            "gem -v" -> {
                "3.3.26"
            }
            "pod --version" -> {
                "1.11.3"
            }
            "/usr/bin/locale -k LC_CTYPE" -> {
                "charmap=\"UTF-8\""
            }
            "find $homeDir/Library/Application Support/Google/data/Directory/Name/plugins/Kotlin/lib -name \"*.jar\"" -> {
                "$homeDir/Library/Application Support/Google/data/Directory/Name/plugins/Kotlin/lib/kotlin.jar"
            }
            "find $homeDir/Library/Application Support/Google/data/Directory/Name/plugins/kmm/lib -name \"*.jar\"" -> {
                "$homeDir/Library/Application Support/Google/data/Directory/Name/plugins/kmm/lib/kmm.jar"
            }
            "$homeDir/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/222.4345.14.2221.9252092/Android Studio Preview.app/Contents/jre/Contents/Home/bin/java --version" -> {
                "openjdk version \"11.0.16\" 2022-07-19 LTS"
            }
            else -> {
                null
            }
        }
        return output?.let { ProcessResult(0, output) } ?: ProcessResult(-1, null)
    }

    override fun findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean): List<String> =
        emptyList()

    override fun print(text: String) {
        TODO("Not yet implemented")
    }

    override fun creteHttpClient(): HttpClient {
        TODO("Not yet implemented")
    }

    override fun fileExists(path: String): Boolean = true

    override fun readFile(path: String): String? = null

    override fun readArchivedFile(pathToArchive: String, pathToFile: String): String? {
        if (pathToArchive == "/Users/my/Library/Application Support/Google/data/Directory/Name/plugins/Kotlin/lib/kotlin.jar" && pathToFile == "META-INF/plugin.xml") {
            return """
                <version>1.7.20</version>
                <id>org.jetbrains.kotlin</id>
            """.trimIndent()
        }
        if (pathToArchive == "/Users/my/Library/Application Support/Google/data/Directory/Name/plugins/kmm/lib/kmm.jar" && pathToFile == "META-INF/plugin.xml") {
            return """
                <version>0.5.0</version>
                <id>org.jetbrains.kmm</id>
            """.trimIndent()
        }
        return null
    }
}