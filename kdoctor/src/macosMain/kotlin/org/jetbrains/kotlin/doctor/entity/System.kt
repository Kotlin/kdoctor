package org.jetbrains.kotlin.doctor.entity

import kotlinx.cinterop.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import platform.Foundation.NSHomeDirectory
import platform.posix.*

actual fun System.getCurrentSystemType(): SystemType = SystemType.MacOS
actual fun System.getHomeDir(): String = NSHomeDirectory()
actual fun System.getEnvVar(name: String): String? = getenv(name)?.toKString()
actual fun System.fileExists(path: String): Boolean = access(path, F_OK) == 0
actual fun System.readFile(path: String): String? = memScoped {
    var output = ""
    val fd = open(path, O_RDONLY)
    if (fd == -1) return null
    val fs: stat = alloc()
    fstat(fd, fs.ptr)

    var dataRead: Long
    val bufferSize: ULong = 4096u;
    val buff = allocArray<ByteVar>(bufferSize.toInt())
    do {
        dataRead = read(fd, buff, bufferSize)
        output += buff.toKString()
        memset(buff, 0, bufferSize)
    } while (dataRead > 0)
    output
}

actual fun System.execute(command: String, vararg args: String): ProcessResult = memScoped {
    val pipeRead = 0
    val pipeWrite = 1

    //setup timer process
    val timerPid = fork()
    if (timerPid == 0) {
        sleep(5000)
        exit(EXIT_SUCCESS)
    }


    val pipeFromChild = allocArray<IntVar>(2)
    if (pipe(pipeFromChild) == -1) {
        return ProcessResult(EXIT_FAILURE, null)
    }

    //spawn a child process to execute command
    val execPid = fork()
    if (execPid == -1) {
        return ProcessResult(EXIT_FAILURE, null)
    }
    if (execPid == 0) {
        //redirect stdout and stderr to the pipe
        dup2(pipeFromChild[pipeWrite], STDOUT_FILENO)
        dup2(pipeFromChild[pipeWrite], STDERR_FILENO)

        close(pipeFromChild[pipeRead])
        close(pipeFromChild[pipeWrite])

        //execute command
        val newArgs = listOf(command) + args
        val result = execvp(command, newArgs.map { it.cstr.ptr }.toCValues())
        exit(result)
    }

    close(pipeFromChild[pipeWrite])

    var output = ""
    //check pipe for readiness
    val fdSet = alloc<fd_set>()
    posix_FD_ZERO(fdSet.ptr)
    posix_FD_SET(pipeFromChild[pipeRead], fdSet.ptr)
    val timeout = alloc<timeval> {
        tv_sec = 5
        tv_usec = 0
    }
    val pipeReady = select(pipeFromChild[pipeRead] + 1, fdSet.ptr, null, null, timeout.ptr)
    if (pipeReady > 0) {
        val bufferSize: ULong = 4096u;
        val out = allocArray<ByteVar>(bufferSize.toInt())
        var dataRead: Long
        do {
            dataRead = read(pipeFromChild[pipeRead], out, bufferSize)
            output += out.toKString()
            memset(out, 0, bufferSize)
        } while (dataRead > 0)
    }

    close(pipeFromChild[pipeRead])

    //kill command execution process if timer process has ended first
    val returnCode: Int
    val retCode = alloc<IntVar>()
    val endedPid = wait(retCode.ptr)
    if (endedPid == timerPid || endedPid == -1) {
        kill(execPid, SIGKILL)
        returnCode = EXIT_FAILURE
    } else {
        kill(timerPid, SIGKILL)
        returnCode = retCode.value
    }

    ProcessResult(returnCode, output.trim())
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

actual fun System.findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean) : List<String> {
    val paths = mutableListOf<String>()
    val dp = opendir(directory) ?: return paths
    do {
        val result = readdir(dp)
        if  (result != null) {
            val name = result.pointed.d_name.toKString()
            val type = result.pointed.d_type.toInt()
            if (type == DT_DIR && name.startsWith(prefix) && name.endsWith(".app")) {
                paths.add("$directory/$name")
            } else if (recursively && type == DT_DIR && name.trim('.').isNotEmpty()){
                paths.addAll(findAppsPathsInDirectory(prefix, "$directory/$name", recursively))
            }
        }
    } while (result != null)
    return paths
}