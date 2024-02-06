package org.jetbrains.kotlin.doctor

import org.jetbrains.kotlin.doctor.entity.System

data class DevelopmentTeam(val id: String, val name: String) {
    override fun toString(): String = "$id ($name)"
}

class DevelopmentTeams(private val system: System) {
    suspend fun getTeams(): Collection<DevelopmentTeam> {
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
                system.logger.e("Read certificate error: $e", e)
                null
            }
        }.toSet()
    }

    private suspend fun getDevelopmentTeamForCert(s: String): DevelopmentTeam {
        val info = system.parseCert(s)
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