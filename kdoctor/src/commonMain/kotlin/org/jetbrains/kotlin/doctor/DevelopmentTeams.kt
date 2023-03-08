package org.jetbrains.kotlin.doctor

import co.touchlab.kermit.Logger
import org.jetbrains.kotlin.doctor.entity.System

data class DevelopmentTeam(val id: String, val name: String) {
    override fun toString(): String = "$id ($name)"
}

class DevelopmentTeams(private val system: System) {
    fun getTeams(): List<DevelopmentTeam> {
        val allCertsResult = system.execute(
            "security",
            "find-certificate",
            "-c", "Apple Development",
            "-p",
            "-a"
        )
        val allCerts = allCertsResult.output.orEmpty().splitCerts()
        return allCerts.mapNotNull {
            try {
                getDevelopmentTeamForCert(it)
            } catch (e: Exception) {
                Logger.e("Read certificate error: $e", e)
                null
            }
        }
    }

    private fun getDevelopmentTeamForCert(s: String): DevelopmentTeam {
        val file = system.writeTempFile(s)
        val subject = system.execute(
            "openssl",
            "x509",
            "-noout",
            "-subject",
            "-nameopt", "sep_multiline",
            "-nameopt", "utf8",
            "-in", file
        ).output ?: error("Couldn't read development certificate!")
        val info = subject.lines().associate { line ->
            line.trim().substringBefore("=") to line.substringAfter("=")
        }
        return DevelopmentTeam(info["OU"]!!, info["O"]!!)
    }

    private fun String.splitCerts(): List<String> {
        val certs = mutableListOf<String>()
        val currentCert = StringBuilder()
        lineSequence().forEach { line ->
            currentCert.appendLine(line)
            if ("-----END CERTIFICATE-----" in line) {
                certs += currentCert.toString()
                currentCert.clear()
            }
        }
        return certs
    }
}