package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.*
import org.junit.Test
import kotlin.test.assertEquals

class AndroidStudioDiagnosticTest {

    @Test
    fun `check success`() {
        val system = object : BaseTestSystem() {}
        val diagnose = AndroidStudioDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Android Studio").apply {
            addSuccess(
                "Android Studio (AI-222.4345.14.2221.9252092)",
                "Location: /Users/my/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/222.4345.14.2221.9252092/Android Studio Preview.app",
                "Bundled Java: openjdk version \"11.0.16\" 2022-07-19 LTS",
                "Kotlin Plugin: 1.7.20",
                "Kotlin Multiplatform Mobile Plugin: 0.5.0"
            )
            addInfo(
                "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
            )
            addEnvironment(
                EnvironmentPiece.AndroidStudio(Version("AI-222.4345.14.2221.9252092")),
                EnvironmentPiece.KotlinPlugin(Version("1.7.20")),
                EnvironmentPiece.KmmPlugin(Version("0.5.0"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check no Android Studio`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "/usr/bin/mdfind kMDItemCFBundleIdentifier=\"com.google.android.studio*\"" -> {
                    ProcessResult(0, "")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = AndroidStudioDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Android Studio").apply {
            addFailure(
                "No Android Studio installation found",
                "Get Android Studio from https://developer.android.com/studio"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check disabled Kotlin plugin`() {
        val system = object : BaseTestSystem() {
            override fun readFile(path: String): String? = when (path) {
                "/Users/my/Library/Application Support/Google/data/Directory/Name/disabled_plugins.txt" -> "org.jetbrains.kotlin"
                else -> super.readFile(path)
            }
        }
        val diagnose = AndroidStudioDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Android Studio").apply {
            addFailure(
                "Android Studio (AI-222.4345.14.2221.9252092)",
                "Location: /Users/my/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/222.4345.14.2221.9252092/Android Studio Preview.app",
                "Bundled Java: openjdk version \"11.0.16\" 2022-07-19 LTS",
                "Kotlin Plugin: 1.7.20",
                "Kotlin Multiplatform Mobile Plugin: 0.5.0",
                "Kotlin plugin is disabled. Enable Kotlin in Android Studio settings."
            )
            addInfo(
                "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
            )
            addEnvironment(
                EnvironmentPiece.AndroidStudio(Version("AI-222.4345.14.2221.9252092")),
                EnvironmentPiece.KotlinPlugin(Version("1.7.20")),
                EnvironmentPiece.KmmPlugin(Version("0.5.0"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check no KMM plugin`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "find $homeDir/Library/Application Support/Google/data/Directory/Name/plugins/kmm/lib -name \"*.jar\"" -> {
                    ProcessResult(0, "")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = AndroidStudioDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Android Studio").apply {
            addWarning(
                "Android Studio (AI-222.4345.14.2221.9252092)",
                "Location: /Users/my/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/222.4345.14.2221.9252092/Android Studio Preview.app",
                "Bundled Java: openjdk version \"11.0.16\" 2022-07-19 LTS",
                "Kotlin Plugin: 1.7.20",
                "Kotlin Multiplatform Mobile Plugin: not installed",
                "Install Kotlin Multiplatform Mobile plugin - https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile"
            )
            addInfo(
                "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
            )
            addEnvironment(
                EnvironmentPiece.AndroidStudio(Version("AI-222.4345.14.2221.9252092")),
                EnvironmentPiece.KotlinPlugin(Version("1.7.20"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check multiple Android Studio installations`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "/usr/bin/mdfind kMDItemCFBundleIdentifier=\"com.google.android.studio*\"" -> {
                    val origin = super.executeCmd(cmd).output
                    ProcessResult(
                        0,
                        """
                            $origin
                            $homeDir/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-1/213.7172.25.2113.9123335/Android Studio.app
                        """.trimIndent()
                    )
                }
                "find $homeDir/Library/Application Support/Google/data/Directory/Name/plugins/Kotlin/lib -name \"*.jar\"" -> {
                    ProcessResult(0, "")
                }
                "find $homeDir/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-1/213.7172.25.2113.9123335/Android Studio.app/Contents/plugins/Kotlin/lib -name \"*.jar\"" -> {
                    ProcessResult(0, "$homeDir/Library/Application Support/Google/data/Directory/Name/plugins/Kotlin/lib/kotlin.jar")
                }

                "/usr/bin/plutil -convert json -o - $homeDir/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-1/213.7172.25.2113.9123335/Android Studio.app/Contents/Info.plist" -> {
                    ProcessResult(
                        0,
                        """
                            {
                              "CFBundleVersion": "AI-213.7172.25.2113.9123335",
                              "CFBundleName": "Android Studio",
                              "JVMOptions": {
                                "Properties": {
                                  "idea.paths.selector": "data/Directory/Name"
                                }
                              }
                            }
                        """.trimIndent()
                    )
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = AndroidStudioDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Android Studio").apply {
            addInfo(
                "Multiple Android Studio installations found"
            )
            addFailure(
                "Android Studio (AI-222.4345.14.2221.9252092)",
                "Location: /Users/my/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/222.4345.14.2221.9252092/Android Studio Preview.app",
                "Bundled Java: openjdk version \"11.0.16\" 2022-07-19 LTS",
                "Kotlin Plugin: not installed",
                "Kotlin Multiplatform Mobile Plugin: 0.5.0",
                "Install Kotlin plugin - https://plugins.jetbrains.com/plugin/6954-kotlin"
            )
            addEnvironment(
                EnvironmentPiece.AndroidStudio(Version("AI-222.4345.14.2221.9252092")),
                EnvironmentPiece.KmmPlugin(Version("0.5.0"))
            )
            addSuccess(
                "Android Studio (AI-213.7172.25.2113.9123335)",
                "Location: /Users/my/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-1/213.7172.25.2113.9123335/Android Studio.app",
                "Bundled Java: not found",
                "Kotlin Plugin: 1.7.20",
                "Kotlin Multiplatform Mobile Plugin: 0.5.0"
            )
            addEnvironment(
                EnvironmentPiece.AndroidStudio(Version("AI-213.7172.25.2113.9123335")),
                EnvironmentPiece.KotlinPlugin(Version("1.7.20")),
                EnvironmentPiece.KmmPlugin(Version("0.5.0"))
            )
            addInfo(
                "Note that, by default, Android Studio uses bundled JDK for Gradle tasks execution.",
                "Gradle JDK can be configured in Android Studio Preferences under Build, Execution, Deployment -> Build Tools -> Gradle section"
            )
            setConclusion(DiagnosisResult.Success)
        }.build()

        assertEquals(expected, diagnose)
    }
}