package org.jetbrains.kotlin.doctor.entity

import io.ktor.client.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

enum class OS(private val str: String) {
    MacOS("macOS"),
    Windows("Windows"),
    Linux("Linux");

    override fun toString() = str
}

enum class Shell(val path: String, val profile: String) {
    Bash("/bin/bash", "~/.bash_profile"),
    Zsh("/bin/zsh", "~/.zprofile")
}

data class ProcessResult(val code: Int, val rawOutput: String?) {
    val output get() = if (code == 0) rawOutput else null
}

object System {
    val isUsingRosetta: Boolean by lazy {
        System.execute("sysctl", "sysctl.proc_translated").output
            ?.substringAfter("sysctl.proc_translated: ")
            ?.toIntOrNull() == 1
    }

    val isUsingM1: Boolean by lazy {
        getCPUInfo()?.contains("Apple") == true
    }

    fun getVersion() =
        System.execute("sw_vers", "-productVersion").output?.let { Version(it) }

    fun getCPUInfo(): String? =
        System.execute("sysctl", "-n", "machdep.cpu.brand_string", "").output?.let { "CPU: $it" }

    fun findAppPaths(appId: String): List<String> =
        System.execute("/usr/bin/mdfind", "kMDItemCFBundleIdentifier=\"$appId\"").output
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            .orEmpty()

    fun readArchivedFile(pathToArchive: String, pathToFile: String): String? =
        System.execute("/usr/bin/unzip", "-p", pathToArchive, pathToFile).output

    fun getShell(): Shell? =
        getEnvVar("SHELL")?.let { shellPath ->
            Shell.values().firstOrNull { it.path == shellPath }
        }

    fun parsePlist(path: String): Map<String, Any>? {
        if (!fileExists(path)) return null
        return try {
            execute("/usr/bin/plutil", "-convert", "json", "-o", "-", path).output
                ?.let { Json.decodeFromString<JsonObject>(it) }
        } catch (e: SerializationException) {
            null
        }
    }
}

expect val System.currentOS: OS
expect val System.homeDir: String
expect val System.httpClient: HttpClient
expect fun System.getEnvVar(name: String): String?
expect fun System.fileExists(path: String): Boolean
expect fun System.readFile(path: String): String?
expect fun System.execute(command: String, vararg args: String): ProcessResult
expect fun System.findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean = false): List<String>