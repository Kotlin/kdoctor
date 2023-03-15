package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.jetbrains.kotlin.doctor.entity.ProcessResult
import org.jetbrains.kotlin.doctor.entity.Version
import org.junit.Test
import kotlin.test.assertEquals

class CocoapodsDiagnosticTest {

    @Test
    fun `check success`() {
        val system = object : BaseTestSystem() {}
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addSuccess(
                "cocoapods (1.11.3)"
            )
            addEnvironment(
                EnvironmentPiece.Ruby(Version("ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]")),
                EnvironmentPiece.RubyGems(Version("3.3.26")),
                EnvironmentPiece.Cocoapods(Version("1.11.3"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check no Ruby`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "ruby -v" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addFailure(
                "ruby not found",
                "Get ruby from https://www.ruby-lang.org/en/documentation/installation/"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check system Ruby info`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "which ruby" -> {
                    ProcessResult(0, "/usr/bin/ruby")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addInfo(
                "System ruby is currently used",
                "Consider installing ruby via Homebrew, rvm or other package manager in case of issues with CocoaPods installation"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addSuccess(
                "cocoapods (1.11.3)"
            )
            addEnvironment(
                EnvironmentPiece.Ruby(Version("ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]")),
                EnvironmentPiece.RubyGems(Version("3.3.26")),
                EnvironmentPiece.Cocoapods(Version("1.11.3"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check system Ruby on M1`() {
        val system = object : BaseTestSystem() {
            override val cpuInfo: String? = "Apple M1_test"
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "which ruby" -> {
                    ProcessResult(0, "/usr/bin/ruby")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addFailure(
                "System ruby is currently used",
                "CocoaPods is not compatible with system ruby installation on Apple M1 computers.",
                "Please install ruby via Homebrew, rvm, rbenv or other tool and make it default",
                "Detailed information: https://stackoverflow.com/questions/64901180/how-to-run-cocoapods-on-apple-silicon-m1/66556339#66556339"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addSuccess(
                "cocoapods (1.11.3)"
            )
            addEnvironment(
                EnvironmentPiece.Ruby(Version("ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]")),
                EnvironmentPiece.RubyGems(Version("3.3.26")),
                EnvironmentPiece.Cocoapods(Version("1.11.3"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check custom Ruby on M1`() {
        val system = object : BaseTestSystem() {
            override val cpuInfo: String? = "Apple M1_test"
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "which ruby" -> {
                    ProcessResult(0, "/custom/ruby")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addSuccess(
                "cocoapods (1.11.3)"
            )
            addEnvironment(
                EnvironmentPiece.Ruby(Version("ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]")),
                EnvironmentPiece.RubyGems(Version("3.3.26")),
                EnvironmentPiece.Cocoapods(Version("1.11.3"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check no Ruby Gems`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "gem -v" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addFailure(
                "ruby gems not found",
                "Get ruby gems from https://rubygems.org/pages/download"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check no Cocoapods`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "pod --version" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addFailure(
                "cocoapods not found",
                "Get cocoapods from https://guides.cocoapods.org/using/getting-started.html#installation"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check not linked Cocoapods`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "pod --version" -> {
                    ProcessResult(-1, null)
                }

                "brew list cocoapods --versions" -> {
                    ProcessResult(0, "cocoapods 1.11.3")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addFailure(
                "Cocoapods are installed via Homebrew but not linked to /usr/local/bin",
                "Execute 'brew link --overwrite cocoapods'"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check wrong Locale with old Cocoapods`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "pod --version" -> {
                    ProcessResult(0, "1.10.2")
                }

                "/usr/bin/locale" -> {
                    ProcessResult(0, "LANG=WTF\nLC_ALL=")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addSuccess(
                "cocoapods (1.10.2)"
            )
            addWarning(
                "CocoaPods requires your terminal to be using UTF-8 encoding.",
                "Consider adding the following to ${system.shell?.profile ?: "shell profile"}",
                "export LANG=en_US.UTF-8",
                "export LC_ALL=en_US.UTF-8"
            )
            addEnvironment(
                EnvironmentPiece.Ruby(Version("ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]")),
                EnvironmentPiece.RubyGems(Version("3.3.26")),
                EnvironmentPiece.Cocoapods(Version("1.10.2"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check wrong Locale with new Cocoapods`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "/usr/bin/locale" -> {
                    ProcessResult(0, "LC_ALL=\"WTF\"")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addSuccess(
                "cocoapods (1.11.3)"
            )
            addFailure(
                "CocoaPods requires your terminal to be using UTF-8 encoding.",
                "Consider adding the following to ${system.shell?.profile ?: "shell profile"}",
                "export LANG=en_US.UTF-8",
                "export LC_ALL=en_US.UTF-8"
            )
            addEnvironment(
                EnvironmentPiece.Ruby(Version("ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]")),
                EnvironmentPiece.RubyGems(Version("3.3.26")),
                EnvironmentPiece.Cocoapods(Version("1.11.3"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check wrong LANG environment with new Cocoapods`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "/usr/bin/locale" -> {
                    ProcessResult(0, "LC_ALL=\"en_US.UTF-8\"")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addSuccess(
                "cocoapods (1.11.3)"
            )
            addFailure(
                "CocoaPods requires your terminal to be using UTF-8 encoding.",
                "Consider adding the following to ${system.shell?.profile ?: "shell profile"}",
                "export LANG=en_US.UTF-8"
            )
            addEnvironment(
                EnvironmentPiece.Ruby(Version("ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]")),
                EnvironmentPiece.RubyGems(Version("3.3.26")),
                EnvironmentPiece.Cocoapods(Version("1.11.3"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `check wrong LC_ALL environment with new Cocoapods`() {
        val system = object : BaseTestSystem() {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "/usr/bin/locale" -> {
                    ProcessResult(0, "LANG=\"en_US.UTF-8\"\nLC_ALL=\"WAT\"")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = CocoapodsDiagnostic(system).diagnose()

        val expected = Diagnosis.Builder("Cocoapods").apply {
            addSuccess(
                "ruby (ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21])"
            )
            addSuccess(
                "ruby gems (3.3.26)"
            )
            addSuccess(
                "cocoapods (1.11.3)"
            )
            addFailure(
                "CocoaPods requires your terminal to be using UTF-8 encoding.",
                "Consider adding the following to ${system.shell?.profile ?: "shell profile"}",
                "export LC_ALL=en_US.UTF-8"
            )
            addEnvironment(
                EnvironmentPiece.Ruby(Version("ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]")),
                EnvironmentPiece.RubyGems(Version("3.3.26")),
                EnvironmentPiece.Cocoapods(Version("1.11.3"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }
}