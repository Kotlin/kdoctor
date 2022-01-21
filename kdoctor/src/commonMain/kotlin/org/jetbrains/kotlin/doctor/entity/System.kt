package org.jetbrains.kotlin.doctor.entity

enum class SystemType(private val str: String) {
    MacOS("macOS"),
    Windows("Windows"),
    Linux("Linux");

    override fun toString() = str
}

enum class Shell(val path: String, val profile: String) {
    Bash("/bin/bash", "~/.bash_profile"),
    Zsh("/bin/zsh", "~/.zprofile")
}

data class ProcessResult(val code: Int, val output: String?)

object System {
    val type: SystemType = getCurrentSystemType()
    val homeDir: String = getHomeDir()

    fun getVersion() =
        System.execute("sw_vers", "-productVersion").output?.let { Version(it) }

    fun getHardwareInfo(): String? =
        System.execute("system_profiler", "SPHardwareDataType").output?.lines()
            ?.firstOrNull { it.contains("Processor Name") || it.contains("Chip") }
            ?.split(":")?.lastOrNull()?.let { "CPU: $it" }

    fun findAppPaths(appId: String): List<String> =
        System.execute("/usr/bin/mdfind", "kMDItemCFBundleIdentifier=\"$appId\"").output
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            .orEmpty()

    fun readArchivedFile(pathToArchive: String, pathToFile: String): String? {
        val output = System.execute("/usr/bin/unzip", "-p", pathToArchive, pathToFile).output
        return if (output?.contains("filename not matched") == true) null else output
    }

    fun getShell(): Shell? {
        val shellPath = getEnvVar("SHELL")
        return Shell.values().singleOrNull { it.path == shellPath }
    }
}

expect fun System.getCurrentSystemType(): SystemType
expect fun System.getHomeDir(): String
expect fun System.getEnvVar(name: String): String?
expect fun System.fileExists(path: String): Boolean
expect fun System.readFile(path: String): String?
expect fun System.execute(command: String, vararg args: String): ProcessResult
expect fun System.findAppsPathsInDirectory(prefix: String, directory: String) : List<String>