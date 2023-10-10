package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.jetbrains.kotlin.doctor.entity.ProcessResult
import org.jetbrains.kotlin.doctor.entity.Version
import org.junit.Test
import kotlin.test.assertEquals

class XcodeDiagnosticTest {

    @Test
    fun `check success`() {
        val system = object : BaseTestSystem() {}
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addSuccess(
                "Xcode (13.4.1)",
                "Location: /Applications/Xcode.app",
            )
            addInfo(
                "Xcode JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
            )
            addEnvironment(
                EnvironmentPiece.Xcode(Version("13.4.1"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check no Xcode`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "/usr/bin/mdfind kMDItemCFBundleIdentifier=\"com.apple.dt.Xcode\"" -> {
                    ProcessResult(0, "")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addFailure(
                "No Xcode installation found",
                "Get Xcode from https://developer.apple.com/xcode/",
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check Xcode custom installation`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "/usr/bin/mdfind kMDItemCFBundleIdentifier=\"com.apple.dt.Xcode\"" -> {
                    ProcessResult(0, "")
                }

                "/usr/bin/plutil -convert json -o - $homeDir/Applications/Xcode.app/Contents/Info.plist" -> {
                    ProcessResult(0, "{\"CFBundleShortVersionString\":\"77.7.7\", \"CFBundleName\":\"Xcode\"}")
                }

                "xcode-select -p" -> {
                    ProcessResult(0, "$homeDir/Applications/Xcode.app/Contents/Developer")
                }

                else -> super.executeCmd(cmd)
            }

            override fun findAppsPathsInDirectory(
                prefix: String,
                directory: String,
                recursively: Boolean
            ): List<String> {
                if (prefix == "Xcode" && directory == "$homeDir/Applications") {
                    return listOf("$homeDir/Applications/Xcode.app")
                }
                return super.findAppsPathsInDirectory(prefix, directory, recursively)
            }
        }
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addSuccess(
                "Xcode (77.7.7)",
                "Location: /Users/my/Applications/Xcode.app",
            )
            addEnvironment(
                EnvironmentPiece.Xcode(Version("77.7.7"))
            )
            addInfo(
                "Xcode JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check multiple Xcode installations`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "/usr/bin/mdfind kMDItemCFBundleIdentifier=\"com.apple.dt.Xcode\"" -> {
                    ProcessResult(0, "/Applications/Xcode.app\n/Applications/XcodeBeta.app")
                }

                "/usr/bin/plutil -convert json -o - /Applications/Xcode.app/Contents/Info.plist" -> {
                    ProcessResult(0, "{\"CFBundleShortVersionString\":\"11.1\", \"CFBundleName\":\"Xcode\"}")
                }

                "/usr/bin/plutil -convert json -o - /Applications/XcodeBeta.app/Contents/Info.plist" -> {
                    ProcessResult(0, "{\"CFBundleShortVersionString\":\"22.2\", \"CFBundleName\":\"Xcode Beta\"}")
                }

                "xcode-select -p" -> {
                    ProcessResult(0, "/Applications/Xcode.app/Contents/Developer")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addInfo(
                "Multiple Xcode installations found"
            )
            addSuccess(
                "Xcode (11.1)",
                "Location: /Applications/Xcode.app",
            )
            addSuccess(
                "Xcode Beta (22.2)",
                "Location: /Applications/XcodeBeta.app",
            )
            addSuccess(
                "Current command line tools: /Applications/Xcode.app/Contents/Developer",
            )
            addEnvironment(
                EnvironmentPiece.Xcode(Version("11.1"))
            )
            addInfo(
                "Xcode JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check Xcode requires the license`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "xcrun cc" -> {
                    ProcessResult(0, "Xcode requires the license")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addSuccess(
                "Xcode (13.4.1)",
                "Location: /Applications/Xcode.app",
            )
            addEnvironment(
                EnvironmentPiece.Xcode(Version("13.4.1"))
            )
            addFailure(
                "Xcode license is not accepted",
                "Accept Xcode license on the first Xcode launch or execute 'sudo xcodebuild -license' in the terminal"
            )
            addInfo(
                "Xcode JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check external Command line tools`() {
        val cltPath = "/Library/Developer/CommandLineTools"
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "xcode-select -p" -> {
                    ProcessResult(0, cltPath)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addSuccess(
                "Xcode (13.4.1)",
                "Location: /Applications/Xcode.app",
            )
            addFailure(
                "Current command line tools: $cltPath",
                "You have to select command line tools bundled to Xcode",
                "Command line tools can be configured in Xcode -> Settings -> Locations -> Locations"
            )
            addInfo(
                "Xcode JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check misconfigured Command line tools`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "xcode-select -p" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addSuccess(
                "Xcode (13.4.1)",
                "Location: /Applications/Xcode.app",
            )
            addFailure(
                "Command line tools are not configured",
                "Command line tools can be configured in Xcode -> Settings -> Locations -> Locations"
            )
            addInfo(
                "Xcode JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check Xcode requires first launch`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "xcodebuild -checkFirstLaunchStatus" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addSuccess(
                "Xcode (13.4.1)",
                "Location: /Applications/Xcode.app",
            )
            addEnvironment(
                EnvironmentPiece.Xcode(Version("13.4.1"))
            )
            addFailure(
                "Xcode requires to perform the First Launch tasks",
                "Launch Xcode and complete setup"
            )
            addInfo(
                "Xcode JAVA_HOME: /Users/my/.sdkman/candidates/java/current",
                "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check Xcode custom JAVA_HOME`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "defaults read com.apple.dt.Xcode IDEApplicationwideBuildSettings" -> {
                    ProcessResult(0, "\"JAVA_HOME\"=/custom/path;")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addSuccess(
                "Xcode (13.4.1)",
                "Location: /Applications/Xcode.app",
            )
            addInfo(
                "Xcode JAVA_HOME: /custom/path",
                "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
            )
            addEnvironment(
                EnvironmentPiece.Xcode(Version("13.4.1"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check Xcode system JAVA_HOME info`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "defaults read com.apple.dt.Xcode IDEApplicationwideBuildSettings" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = XcodeDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Xcode").apply {
            addSuccess(
                "Xcode (13.4.1)",
                "Location: /Applications/Xcode.app",
            )
            addInfo(
                "Xcode JAVA_HOME: /Users/my/Library/Java/JavaVirtualMachines/jbr-17.0.5/Contents/Home",
                "Xcode JAVA_HOME can be configured in Xcode -> Settings -> Locations -> Custom Paths"
            )
            addEnvironment(
                EnvironmentPiece.Xcode(Version("13.4.1"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }
}