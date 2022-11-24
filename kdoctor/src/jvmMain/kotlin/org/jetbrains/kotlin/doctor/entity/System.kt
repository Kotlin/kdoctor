package org.jetbrains.kotlin.doctor.entity

import io.ktor.client.*

actual val System.currentOS: OS get() = TODO("JVM target is only for unit tests")
actual val System.homeDir: String get() = TODO("JVM target is only for unit tests")
actual val System.httpClient: HttpClient get() = TODO("JVM target is only for unit tests")
actual fun System.getEnvVar(name: String): String? = TODO("JVM target is only for unit tests")
actual fun System.fileExists(path: String): Boolean = TODO("JVM target is only for unit tests")
actual fun System.readFile(path: String): String? = TODO("JVM target is only for unit tests")
actual fun System.execute(command: String, vararg args: String): ProcessResult = TODO("JVM target is only for unit tests")
actual fun System.findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean): List<String> = TODO("JVM target is only for unit tests")
actual fun System.print(text: String): Unit = TODO("JVM target is only for unit tests")