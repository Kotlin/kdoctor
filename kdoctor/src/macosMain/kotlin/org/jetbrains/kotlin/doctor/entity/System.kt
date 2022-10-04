package org.jetbrains.kotlin.doctor.entity

import kotlinx.cinterop.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.kotlin.doctor.Log
import platform.Foundation.NSHomeDirectory
import platform.posix.*

actual fun System.getCurrentSystemType(): SystemType = SystemType.MacOS
actual fun System.getHomeDir(): String = NSHomeDirectory()
actual fun System.getEnvVar(name: String): String? = getenv(name)?.toKString()
actual fun System.fileExists(path: String): Boolean = access(path, F_OK) == 0
actual fun System.readFile(path: String): String? = memScoped {
    val fd = open(path, O_RDONLY)
    if (fd == -1) return null
    val fs: stat = alloc()
    fstat(fd, fs.ptr)

    readFd(fd)
}

actual fun System.execute(command: String, vararg args: String, verbose: Boolean): ProcessResult = memScoped {
    val readEnd = 0
    val writeEnd = 1

    //setup timer process
    val timerPid = fork()
    if (timerPid == 0) {
        sleep(5000)
        exit(EXIT_SUCCESS)
    }

    //setup pipes to read stdout and stderr of the child process
    val outputPipe = allocArray<IntVar>(2)
    val errorPipe = allocArray<IntVar>(2)
    if (pipe(outputPipe) == -1) {
        return ProcessResult(EXIT_FAILURE, null, null)
    }
    pipe(errorPipe)

    //spawn a child process to execute command
    val execPid = fork()
    if (execPid == -1) {
        return ProcessResult(EXIT_FAILURE, null, null)
    }

    //execute command in the child process
    if (execPid == 0) {
        //redirect stdout and stderr to the respective pipes
        dup2(outputPipe[writeEnd], STDOUT_FILENO)
        dup2(errorPipe[writeEnd], STDERR_FILENO)

        close(outputPipe[readEnd])
        close(errorPipe[readEnd])
        close(outputPipe[writeEnd])
        close(errorPipe[writeEnd])

        //execute command
        val newArgs = listOf(command) + args + null
        val result = execvp(command, newArgs.map { it?.cstr?.ptr }.toCValues())
        exit(result)
    }

    close(outputPipe[writeEnd])
    close(errorPipe[writeEnd])

    val output = readFd(outputPipe[readEnd])
    close(outputPipe[readEnd])
    val error = readFd(errorPipe[readEnd])
    close(errorPipe[readEnd])

    //kill command execution process if timer process has ended first
    val returnCode: Int
    val retCode = alloc<IntVar>()
    val endedPid = wait(retCode.ptr)
    returnCode = if (endedPid == timerPid || endedPid == -1) {
        kill(execPid, SIGKILL)
        Log.e { "$command SIGKILL" }
        EXIT_FAILURE
    } else {
        kill(timerPid, SIGKILL)
        retCode.value
    }

    Log.d {
        val commandWithArgs = "$command ${args.joinToString(separator = " ")}"
        "-----> Command \"$commandWithArgs\" returned with code $returnCode"
    }

    if (verbose) {
        val outputLines = output?.lines()
        if (outputLines != null && outputLines.size > 1) {
            Log.d { "<----- output:" }
            outputLines.forEach {
                Log.d { it }
            }
        } else {
            Log.d { "<----- output: $output" }
        }

        error?.let {
            Log.e { "<----- error: $it" }
        }
    }

    ProcessResult(returnCode, output, error)
}

fun System.parsePlist(path: String): Map<String, Any>? {
    if (!fileExists(path)) return null
    val output = execute("/usr/bin/plutil", "-convert", "json", "-o", "-", path).output
    return try {
        output?.let { Json.decodeFromString<JsonObject>(it) }
    } catch (e: SerializationException) {
        null
    }
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