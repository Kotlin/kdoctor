package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.jetbrains.kotlin.doctor.entity.ProcessResult
import org.jetbrains.kotlin.doctor.entity.Version
import org.junit.Test
import kotlin.test.assertEquals

class GradleProjectDiagnosticTest {

    @Test
    fun `check success`() {
        val projectPath = "./test/my_test_project"
        val system = object : BaseTestSystem(projectPath) {}
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addSuccess(
                "Gradle (7.6)"
            )
            addSuccess(
                "Gradle plugins:",
                "pluginA:1.2.3",
                "pluginB:4.5.6"
            )
            addEnvironment(
                EnvironmentPiece.Gradle(Version("7.6")),
                EnvironmentPiece.GradlePlugin("pluginA", Version("1.2.3")),
                EnvironmentPiece.GradlePlugin("pluginB", Version("4.5.6"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `invalid project path`() {
        val projectPath = "./broken"
        val system = object : BaseTestSystem(projectPath) {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "ls $projectPath" -> {
                    ProcessResult(-1, "ls: $projectPath: No such file or directory")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addFailure("Path is not found")
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `invalid gradle project`() {
        val projectPath = "./test/my_test_project"
        val system = object : BaseTestSystem(projectPath) {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "ls $projectPath" -> {
                    ProcessResult(0, "some.txt")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addFailure(
                "${projectPath}/settings.gradle[.kts] is not found!",
                "Check project directory path"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `system gradle info`() {
        val projectPath = "./test/my_test_project"
        val system = object : BaseTestSystem(projectPath) {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "$projectPath/gradlew -v" -> {
                    ProcessResult(-1, null)
                }

                "gradle -v" -> {
                    ProcessResult(0, "Gradle 7.0")
                }

                "gradle -p $projectPath -I $projectPath/init.gradle.kts" -> {
                    ProcessResult(
                        0,
                        """
                            kdoctor >>> pluginA=1.2
                            kdoctor >>> pluginB=4.5
                        """.trimIndent()
                    )
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addInfo(
                "Project doesn't use a Gradle wrapper",
                "System Gradle was used for the analysis",
                "For better experience we recommend to use a Gradle wrapper"
            )
            addSuccess(
                "Gradle (7.0)"
            )
            addSuccess(
                "Gradle plugins:",
                "pluginA:1.2",
                "pluginB:4.5"
            )
            addEnvironment(
                EnvironmentPiece.Gradle(Version("7.0")),
                EnvironmentPiece.GradlePlugin("pluginA", Version("1.2")),
                EnvironmentPiece.GradlePlugin("pluginB", Version("4.5"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `no gradle error`() {
        val projectPath = "./test/my_test_project"
        val system = object : BaseTestSystem(projectPath) {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "$projectPath/gradlew -v" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addFailure(
                "Gradle is not found",
                "For better experience we recommend to use a Gradle wrapper"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `temp file error`() {
        val projectPath = "./test/my_test_project"
        val system = object : BaseTestSystem(projectPath) {
            override fun writeTempFile(content: String): String {
                error("Temp file error")
            }
        }
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addSuccess(
                "Gradle (7.6)"
            )
            addFailure(
                "Error: impossible to create temporary file",
                "Check your file system write permissions",
                "'Temp file error'"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `rename init script error`() {
        val projectPath = "./test/my_test_project"
        val system = object : BaseTestSystem(projectPath) {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "mv $projectPath/temp.file $projectPath/init.gradle.kts" -> {
                    ProcessResult(-77, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addSuccess(
                "Gradle (7.6)"
            )
            addFailure(
                "Error -77: impossible to prepare $projectPath/init.gradle.kts file",
                "Check your file system write permissions"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `gradle error`() {
        val projectPath = "./test/my_test_project"
        val system = object : BaseTestSystem(projectPath) {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "$projectPath/gradlew -p $projectPath -I $projectPath/init.gradle.kts" -> {
                    ProcessResult(-1, null)
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addSuccess(
                "Gradle (7.6)"
            )
            addFailure(
                "Gradle error",
                "Run in terminal '$projectPath/gradlew -p $projectPath --info'"
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `gradle unknown output`() {
        val projectPath = "./test/my_test_project"
        val system = object : BaseTestSystem(projectPath) {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "$projectPath/gradlew -p $projectPath -I $projectPath/init.gradle.kts" -> {
                    ProcessResult(0, "some unknown text")
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addSuccess(
                "Gradle (7.6)"
            )
            addWarning(
                "Gradle plugins are not found"
            )
            addEnvironment(
                EnvironmentPiece.Gradle(Version("7.6"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }

    @Test
    fun `get right gradle plugin versions`() {
        val projectPath = "./test/my_test_project"
        val system = object : BaseTestSystem(projectPath) {
            override fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "$projectPath/gradlew -p $projectPath -I $projectPath/init.gradle.kts" -> {
                    ProcessResult(
                        0,
                        """
                            kdoctor >>> pluginA=1.2
                            kdoctor >>> pluginB=4.5
                            kdoctor >>> pluginB=null
                            kdoctor >>> pluginC=null
                            kdoctor >>> pluginD=null
                            kdoctor >>> DEP group:pluginC.gradle.plugin:7.7.7
                        """.trimIndent()
                    )
                }

                else -> super.executeCmd(cmd)
            }
        }
        val diagnose = GradleProjectDiagnostic(system, projectPath).diagnose()

        val expected = Diagnosis.Builder("Project: $projectPath").apply {
            addSuccess(
                "Gradle (7.6)"
            )
            addSuccess(
                "Gradle plugins:",
                "pluginA:1.2",
                "pluginB:4.5",
                "pluginC:7.7.7",
                "pluginD:null"
            )
            addEnvironment(
                EnvironmentPiece.Gradle(Version("7.6")),
                EnvironmentPiece.GradlePlugin("pluginA", Version("1.2")),
                EnvironmentPiece.GradlePlugin("pluginB", Version("4.5")),
                EnvironmentPiece.GradlePlugin("pluginC", Version("7.7.7")),
                EnvironmentPiece.GradlePlugin("pluginD", Version("null"))
            )
        }.build()

        assertEquals(expected, diagnose)
    }
}