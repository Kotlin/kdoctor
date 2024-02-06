package org.jetbrains.kotlin.doctor.entity

import kotlinx.coroutines.Deferred
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.kotlin.doctor.logging.KdoctorLogger

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

interface System {
    val logger: KdoctorLogger

    val currentOS: OS
    val osVersion: Deferred<Version?>
    val cpuInfo: Deferred<String?>
    val homeDir: String
    val shell: Deferred<Shell?>

    suspend fun getEnvVar(name: String): String?
    suspend fun execute(command: String, vararg args: String): ProcessResult

    suspend fun retrieveUrl(url: String): String
    suspend fun downloadUrl(url: String, targetPath: String)
    suspend fun find(path: String, nameFilter: String): List<String>?
    suspend fun list(path: String): String?
    suspend fun pwd(): String
    suspend fun parseCert(certString: String): Map<String, String>

    suspend fun fileExists(path: String): Boolean
    suspend fun readFile(path: String): String?
    suspend fun writeTempFile(content: String): String
    suspend fun readArchivedFile(pathToArchive: String, pathToFile: String): String?
    suspend fun findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean = false): List<String>

    suspend fun createTempDir(): String
    suspend fun rm(path: String): Boolean
    suspend fun mv(from: String, to: String): Boolean
}

suspend fun System.isUsingRosetta() =
    execute("sysctl", "sysctl.proc_translated").output
        ?.substringAfter("sysctl.proc_translated: ")
        ?.toIntOrNull() == 1

suspend fun System.isUsingM1() =
    cpuInfo.await()?.contains("Apple") == true

suspend fun System.parsePlist(path: String): Map<String, Any>? {
    if (!fileExists(path)) return null
    return try {
        execute("/usr/bin/plutil", "-convert", "json", "-o", "-", path).output
            ?.let { Json.decodeFromString<JsonObject>(it) }
    } catch (e: SerializationException) {
        null
    }
}

suspend fun System.spotlightFindAppPaths(appId: String): List<String> =
    execute("/usr/bin/mdfind", "kMDItemCFBundleIdentifier=\"$appId\"").output
        ?.split("\n")
        ?.filter { it.isNotBlank() }
        .orEmpty()
