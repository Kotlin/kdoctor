package org.jetbrains.kotlin.doctor

import org.jetbrains.kotlin.doctor.entity.System

class DevelopmentTeam(val teamId: String, val teamName: String) {
    override fun toString(): String = "$teamId ($teamName)"
}

class DevelopmentTeams(private val system: System) {
    fun getTeams(): List<DevelopmentTeam> {
        val allCertsResult = system.execute(
            "security",
            "find-certificate",
            "-c",
            "Apple Development",
            "-p",
            "-a"
        )
        val allCerts = allCertsResult.output ?: error("Could not find certificates! ${allCertsResult.rawOutput}")
        val certs = splitCerts(allCerts)
        return certs.map {
            getDevelopmentTeamForCert(it)
        }
    }


    private fun getDevelopmentTeamForCert(s: String): DevelopmentTeam {
        val file = system.writeTempFile(s)
        return getDevelopmentTeamIdFromOpenSslOutput(
            system.execute(
                "openssl",
                "x509",
                "-noout",
                "-text",
                "-in",
                file
            ).output ?: error("Couldn't read development certificate!")
        )
    }

    private fun getDevelopmentTeamIdFromOpenSslOutput(o: String): DevelopmentTeam {
        val regex = """OU=(\w{3,}), O=([^,]+)""".toRegex()
        val res = regex.find(o) ?: error("Could not find development cert!")
        val (organizationalUnit, organization) = res.destructured
        return DevelopmentTeam(organizationalUnit, organization)
    }

    private fun splitCerts(allCertificates: String): List<String> {
        val certs = mutableListOf<String>()
        val currentCert = StringBuilder()
        for (l in allCertificates.lines()) {
            currentCert.appendLine(l)
            if ("-----END CERTIFICATE-----" in l) {
                certs += currentCert.toString()
                currentCert.clear()
            }
        }
        return certs
    }
}