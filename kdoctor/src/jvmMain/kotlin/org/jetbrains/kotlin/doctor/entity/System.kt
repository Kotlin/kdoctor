package org.jetbrains.kotlin.doctor.entity

import co.touchlab.kermit.Logger
import org.jetbrains.kotlin.doctor.entity.OS.*
import java.io.File

class JvmSystem : System {
    override val osVersion: Version?
        get() {
            val osVersion = java.lang.System.getProperty("os.version") ?: return null
            return Version(
                version = osVersion,
            )
        }
    override val currentOS: OS
        get() {
            val osName = java.lang.System.getProperty("os.name").lowercase()
            if (osName.startsWith("win")) {
                return Windows
            }
            if (osName.startsWith("mac")) {
                return MacOS
            }
            if (osName.startsWith("linux")) {
                return Linux
            }
//            throw NotImplementedError(
//                "The operating system $osName is not supported. please send this report on Github",
//            )
            return UNKNOWN
        }
    override val cpuInfo: String?
        get() = java.lang.System.getProperty("sun.cpu.isalist")
    override val homeDir: String
        get() = java.lang.System.getProperty("user.home")
    override val shell: Shell?
        get() = null
    override val hasXcodeSupport: Boolean
        get() = false

    override fun getEnvVar(name: String): String? {
        return java.lang.System.getProperty(name) ?: null
    }

    override fun execute(command: String, vararg args: String): ProcessResult {
        val cmd = mutableListOf<String>().apply {
            add(command)
            addAll(args)
        }.joinToString(separator = " ") { it.replace(" ", "\\ ") }
        Logger.d("Execute '$cmd'")
        val runtime = Runtime.getRuntime()
        return try {
            val processResult = runtime.exec(cmd)
            ProcessResult(processResult.waitFor() , processResult.inputStream.reader().readText())
        } catch (e: Exception) {
            Logger.d("Error while execute command: '$cmd'\n with exception $e")
            ProcessResult(-1 ,e.message)
        }
    }

    override fun fileExists(path: String): Boolean {
        return File(path).exists()
    }

    override fun readFile(path: String): String? {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            null
        }
    }

    override fun writeTempFile(content: String): String {
        return ""
    }

    override fun readArchivedFile(pathToArchive: String, pathToFile: String): String? {
        return null
    }

    override fun findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean): List<String> {
        val paths = mutableListOf<String>()
        val filePaths = File(directory).listFiles() ?: return paths
        for (filePath in filePaths) {
            paths.add(filePath.path)
        }
        return paths
    }

}