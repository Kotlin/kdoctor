package org.jetbrains.kotlin.doctor.entity

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
    val osVersion: Version?
    val cpuInfo: String?
    val homeDir: String
    val shell: Shell?

    fun getEnvVar(name: String): String?
    fun execute(command: String, vararg args: String): ProcessResult

    fun retrieveUrl(url: String): String
    fun downloadUrl(url: String, targetPath: String)
    fun find(path: String, nameFilter: String): List<String>?
    fun list(path: String): String?
    fun pwd(): String
    fun parseCert(certString: String): Map<String, String>

    fun fileExists(path: String): Boolean
    fun readFile(path: String): String?
    fun writeTempFile(content: String): String
    fun readArchivedFile(pathToArchive: String, pathToFile: String): String?
    fun findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean = false): List<String>

    fun createTempDir(): String
    fun rm(path: String): Boolean
    fun mv(from: String, to: String): Boolean
}

fun System.isUsingRosetta() =
    execute("sysctl", "sysctl.proc_translated").output
        ?.substringAfter("sysctl.proc_translated: ")
        ?.toIntOrNull() == 1

fun System.isUsingM1() =
    cpuInfo?.contains("Apple") == true

fun System.parsePlist(path: String): Map<String, Any>? {
    if (!fileExists(path)) return null
    return try {
        execute("/usr/bin/plutil", "-convert", "json", "-o", "-", path).output
            ?.let { Json.decodeFromString<JsonObject>(it) }
    } catch (e: SerializationException) {
        null
    }
}

fun System.spotlightFindAppPaths(appId: String): List<String> =
    execute("/usr/bin/mdfind", "kMDItemCFBundleIdentifier=\"$appId\"").output
        ?.split("\n")
        ?.filter { it.isNotBlank() }
        .orEmpty()
