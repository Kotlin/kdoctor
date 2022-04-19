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

data class ProcessResult(val code: Int, val output: String?, val error: String?)

object System {
    val type: SystemType = getCurrentSystemType()
    val homeDir: String = getHomeDir()

    val isUsingRosetta by lazy { isUsingRosetta() }

    val isUsingM1 by lazy { isUsingM1() }

    fun getVersion() =
        System.execute("sw_vers", "-productVersion").output?.let { Version(it) }

    fun getCPUInfo(): String? = System.execute("sysctl", "-n", "machdep.cpu.brand_string", "").output
        ?.let { "CPU: $it" }

    fun findAppPaths(appId: String): List<String> =
        System.execute("/usr/bin/mdfind", "kMDItemCFBundleIdentifier=\"$appId\"").output
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            .orEmpty()

    fun readArchivedFile(pathToArchive: String, pathToFile: String): String? =
        System.execute("/usr/bin/unzip", "-p", pathToArchive, pathToFile).output

    fun getShell(): Shell? {
        val shellPath = getEnvVar("SHELL")
        return Shell.values().singleOrNull { it.path == shellPath }
    }

    private fun isUsingRosetta(): Boolean = System.execute("sysctl", "sysctl.proc_translated").output
        ?.substringAfter("sysctl.proc_translated: ")
        ?.toIntOrNull() == 1

    private fun isUsingM1(): Boolean = getCPUInfo()?.contains("Apple") == true
}

expect fun System.getCurrentSystemType(): SystemType
expect fun System.getHomeDir(): String
expect fun System.getEnvVar(name: String): String?
expect fun System.fileExists(path: String): Boolean
expect fun System.readFile(path: String): String?
expect fun System.execute(command: String, vararg args: String, verbose: Boolean = false): ProcessResult
expect fun System.findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean = false): List<String>