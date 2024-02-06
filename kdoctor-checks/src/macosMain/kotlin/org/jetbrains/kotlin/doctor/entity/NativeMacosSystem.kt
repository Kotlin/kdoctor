package org.jetbrains.kotlin.doctor.entity

import kotlinx.cinterop.*
import kotlinx.coroutines.DelicateCoroutinesApi
import org.jetbrains.kotlin.doctor.logging.KdoctorLogger
import org.jetbrains.kotlin.doctor.logging.KermitKdoctorLogger
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.*

class NativeMacosSystem : NativeMacosSystemBase() {
    override val logger: KdoctorLogger = KermitKdoctorLogger()
    override val homeDir: String by lazy { NSHomeDirectory() }

    override suspend fun getEnvVar(name: String): String? = getenv(name)?.toKString()

    override suspend fun execute(command: String, vararg args: String): ProcessResult = memScoped {
        val cmd = mutableListOf<String>().apply {
            add(command)
            addAll(args)
        }.joinToString(separator = " ") { it.replace(" ", "\\ ") }
        logger.d("Execute '$cmd'")

        val buffer = ByteArray(128)
        val exitCode: Int
        var result = ""
        val pipe = popen("$cmd 2>&1", "r") //write stderr together with stdout: https://stackoverflow.com/a/44680326
            ?: error("popen('$cmd 2>&1', 'r') error")

        try {
            while (true) {
                val input = fgets(buffer.refTo(0), buffer.size, pipe) ?: break
                val inputString = input.toKString()
                logger.v(inputString.removeSuffix("\n"))
                result += inputString
            }
        } finally {
            exitCode = pclose(pipe) / 256 //get error code from a child process: https://stackoverflow.com/a/808995
            if (exitCode != 0) logger.d("Error code '$cmd' = $exitCode")
        }

        val rawOutput = result.trim().takeIf { it.isNotBlank() }
        ProcessResult(exitCode, rawOutput)
    }

    override suspend fun findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean): List<String> {
        val paths = mutableListOf<String>()
        val dp = opendir(directory) ?: return paths
        do {
            val result = readdir(dp)
            if (result != null) {
                val name = result.pointed.d_name.toKString()
                val type = result.pointed.d_type.toInt()
                if (type == DT_DIR && name.startsWith(prefix) && name.endsWith(".app")) {
                    paths.add("$directory/$name")
                } else if (recursively && type == DT_DIR && name.trim('.')
                        .isNotEmpty() /*exclude . & .. child paths*/) {
                    paths.addAll(findAppsPathsInDirectory(prefix, "$directory/$name", recursively))
                }
            }
        } while (result != null)
        return paths
    }

    override suspend fun fileExists(path: String): Boolean = access(path, F_OK) == 0

    override suspend fun readFile(path: String): String? =
        NSString.stringWithContentsOfFile(path)?.toString()

    @Suppress("CAST_NEVER_SUCCEEDS")
    override suspend fun writeTempFile(content: String): String {
        logger.d("writeTempFile")

        val tempFile = execute("mktemp").output?.trim().orEmpty()
        if (tempFile.isBlank()) error("writeFile error: couldn't make temp file")
        logger.d("tempFile = $tempFile")

        (content as NSString).writeToFile(tempFile, true)

        return tempFile
    }

    override suspend fun readArchivedFile(pathToArchive: String, pathToFile: String): String? =
        execute("/usr/bin/unzip", "-p", pathToArchive, pathToFile).output

    //checks if file descriptor is ready
    private fun fdReady(fd: Int): Boolean = memScoped {
        val fdSet = alloc<fd_set>()
        posix_FD_ZERO(fdSet.ptr)
        posix_FD_SET(fd, fdSet.ptr)
        val to = alloc<timeval> {
            tv_sec = 5
            tv_usec = 0
        }
        select(fd + 1, fdSet.ptr, null, null, to.ptr) > 0
    }

    //read from file descriptor
    private fun readFd(fd: Int): String? = memScoped {
        var output = ""
        if (fdReady(fd)) {
            val bufferSize: ULong = 4096u
            val out = allocArray<ByteVar>(bufferSize.toInt())
            var dataRead: Long
            do {
                dataRead = read(fd, out, bufferSize)
                output += out.toKString()
                memset(out, 0, bufferSize)
            } while (dataRead > 0)
        }
        output.trim().takeIf { it.isNotEmpty() }
    }
}