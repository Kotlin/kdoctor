package org.jetbrains.kotlin.doctor

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.doctor.diagnostics.BaseTestSystem
import org.jetbrains.kotlin.doctor.entity.ProcessResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class DevelopmentTeamsTest {

    @Test
    fun `check success`() = runTest {
        val testCert = """
            -----BEGIN CERTIFICATE-----
            xxx
            -----END CERTIFICATE-----
        """.trimIndent()
        val testTeam = DevelopmentTeam("777", "Test Team")

        val system = object : BaseTestSystem() {

            override suspend fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "security find-certificate -c Apple Development -p -a" -> {
                    ProcessResult(0, testCert)
                }

                "openssl x509 -noout -subject -nameopt sep_multiline -nameopt utf8 -in $testTempFile" -> {
                    ProcessResult(
                        0, """
                        subject=
                            OU=${testTeam.id}
                            O=${testTeam.name}
                        """.trimIndent()
                    )
                }

                else -> super.executeCmd(cmd)
            }
        }

        val teams = DevelopmentTeams(system).getTeams()

        assertEquals(testTeam, teams.single())
    }

    @Test
    fun `check two certificates success`() = runTest {
        val testCert1 = """
            -----BEGIN CERTIFICATE-----
            yyy
            -----END CERTIFICATE-----
        """.trimIndent()
        val testTeam1 = DevelopmentTeam("777", "Test Team")
        val f1 = "tmp1.file"

        val testCert2 = """
            -----BEGIN CERTIFICATE-----
            xxx
            -----END CERTIFICATE-----
        """.trimIndent()
        val testTeam2 = DevelopmentTeam("888", "Test Team")
        val f2 = "tmp2.file"

        val system = object : BaseTestSystem() {
            override suspend fun writeTempFile(content: String): String = when (content.trim()) {
                testCert1 -> f1
                testCert2 -> f2
                else -> super.writeTempFile(content)
            }

            override suspend fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "security find-certificate -c Apple Development -p -a" -> {
                    ProcessResult(0, "$testCert1\n$testCert2")
                }

                "openssl x509 -noout -subject -nameopt sep_multiline -nameopt utf8 -in $f1" -> {
                    ProcessResult(
                        0, """
                        subject=
                            OU=${testTeam1.id}
                            O=${testTeam1.name}
                        """.trimIndent()
                    )
                }

                "openssl x509 -noout -subject -nameopt sep_multiline -nameopt utf8 -in $f2" -> {
                    ProcessResult(
                        0, """
                        subject=
                            OU=${testTeam2.id}
                            O=${testTeam2.name}
                        """.trimIndent()
                    )
                }

                else -> super.executeCmd(cmd)
            }
        }

        val expected = listOf(testTeam1, testTeam2)
        val teams = DevelopmentTeams(system).getTeams()

        assertContentEquals(expected, teams)
    }

    @Test
    fun `check wrong find-certificate output`() = runTest {
        val system = object : BaseTestSystem() {

            override suspend fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "security find-certificate -c Apple Development -p -a" -> {
                    ProcessResult(0, "wrong format")
                }

                "openssl x509 -noout -subject -nameopt sep_multiline -nameopt utf8 -in $testTempFile" -> {
                    ProcessResult(-1, "unable to load certificate")
                }

                else -> super.executeCmd(cmd)
            }
        }

        val teams = DevelopmentTeams(system).getTeams()

        assert(teams.isEmpty())
    }

    @Test
    fun `check wrong openssl output`() = runTest {
        val testCert = """
            -----BEGIN CERTIFICATE-----
            xxx
            -----END CERTIFICATE-----
        """.trimIndent()

        val system = object : BaseTestSystem() {

            override suspend fun executeCmd(cmd: String): ProcessResult = when (cmd) {
                "security find-certificate -c Apple Development -p -a" -> {
                    ProcessResult(0, testCert)
                }

                "openssl x509 -noout -subject -nameopt sep_multiline -nameopt utf8 -in $testTempFile" -> {
                    ProcessResult(0, "wrong output")
                }

                else -> super.executeCmd(cmd)
            }
        }

        val teams = DevelopmentTeams(system).getTeams()

        assert(teams.isEmpty())
    }
}