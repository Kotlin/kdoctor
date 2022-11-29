package org.jetbrains.kotlin.doctor

import io.ktor.client.*
import org.jetbrains.kotlin.doctor.entity.*

open class BaseTestSystem : System {
    override val currentOS: OS = OS.MacOS
    override val osVersion: Version? = Version("42.777")
    override val cpuInfo: String? = "test_cpu"
    override val homeDir: String = "/Users/my"
    override val shell: Shell? = Shell.Zsh

    override fun getEnvVar(name: String): String? = when (name) {
        "JAVA_HOME" -> "$homeDir/.sdkman/candidates/java/current"
        else -> null
    }

    override fun execute(command: String, vararg args: String): ProcessResult =
        executeCmd((command + " " + args.joinToString(" ")).trim())

    open fun executeCmd(cmd: String): ProcessResult {
        val output = when (cmd) {
            "which java" -> "$homeDir/.sdkman/candidates/java/current/bin/java"
            "java -version" -> """
                openjdk version "11.0.16" 2022-07-19 LTS
                OpenJDK Runtime Environment Corretto-11.0.16.8.1 (build 11.0.16+8-LTS)
                OpenJDK 64-Bit Server VM Corretto-11.0.16.8.1 (build 11.0.16+8-LTS, mixed mode)
            """.trimIndent()

            "/usr/libexec/java_home" -> "$homeDir/Library/Java/JavaVirtualMachines/jbr-17.0.5/Contents/Home"
            "defaults read com.apple.dt.Xcode IDEApplicationwideBuildSettings" -> "\"JAVA_HOME\"=$homeDir/.sdkman/candidates/java/current;"
            else -> null
        }
        return output?.let { ProcessResult(0, output) } ?: ProcessResult(-1, null)
    }

    override fun findAppPaths(appId: String): List<String> = emptyList()

    override fun findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean): List<String> =
        emptyList()

    override fun print(text: String) {
        TODO("Not yet implemented")
    }

    override fun creteHttpClient(): HttpClient {
        TODO("Not yet implemented")
    }

    override fun fileExists(path: String): Boolean = when (path) {
        "/Users/my/.sdkman/candidates/java/current/bin/java" -> true
        else -> false
    }

    override fun readFile(path: String): String? = null

    override fun readArchivedFile(pathToArchive: String, pathToFile: String): String? = null
}