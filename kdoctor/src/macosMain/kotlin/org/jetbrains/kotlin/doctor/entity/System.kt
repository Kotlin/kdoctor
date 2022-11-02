package org.jetbrains.kotlin.doctor.entity

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import platform.Foundation.NSHomeDirectory
import platform.posix.*

actual val System.currentOS: OS get() = OS.MacOS
actual val System.homeDir: String get() = NSHomeDirectory()
actual fun System.getEnvVar(name: String): String? = getenv(name)?.toKString()
actual fun System.fileExists(path: String): Boolean = access(path, F_OK) == 0
actual fun System.readFile(path: String): String? = memScoped {
    Logger.d("readFile($path)")
    val fd = open(path, O_RDONLY)
    if (fd == -1) return null
    val fs: stat = alloc()
    fstat(fd, fs.ptr)

    readFd(fd)
}

actual fun System.execute(command: String, vararg args: String): ProcessResult = memScoped {
    val cmd = mutableListOf<String>().apply {
        add(command)
        addAll(args)
    }.joinToString(separator = " ") { it.replace(" ", "\\ ") }
    Logger.d("Execute '$cmd'")

    val buffer = ByteArray(128)
    val exitCode: Int
    var result = ""
    val pipe = popen("$cmd 2>&1", "r") //more info https://stackoverflow.com/a/44680326
        ?: error("popen('$cmd 2>&1', 'r') error")

    try {
        while (true) {
            val input = fgets(buffer.refTo(0), buffer.size, pipe) ?: break
            result += input.toKString()
        }
    } finally {
        exitCode = pclose(pipe) / 256 //more info https://stackoverflow.com/a/808995
        Logger.d("Exit code '$cmd' = $exitCode")
    }

    ProcessResult(exitCode, result.trim().takeIf { it.isNotBlank() })
}

actual fun System.findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean): List<String> {
    val paths = mutableListOf<String>()
    val dp = opendir(directory) ?: return paths
    do {
        val result = readdir(dp)
        if (result != null) {
            val name = result.pointed.d_name.toKString()
            val type = result.pointed.d_type.toInt()
            if (type == DT_DIR && name.startsWith(prefix) && name.endsWith(".app")) {
                paths.add("$directory/$name")
            } else if (recursively && type == DT_DIR && name.trim('.').isNotEmpty() /*exclude . & .. child paths*/) {
                paths.addAll(findAppsPathsInDirectory(prefix, "$directory/$name", recursively))
            }
        }
    } while (result != null)
    return paths
}

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