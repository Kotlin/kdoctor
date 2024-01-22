package org.jetbrains.kotlin.doctor.entity

import org.jetbrains.kotlin.doctor.logging.KdoctorLogger
import java.io.IOException
import java.lang.System
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipInputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.streams.asSequence

class JvmMacosSystem(override val logger: KdoctorLogger, private val envOverride: Map<String, String> = emptyMap()) : MacosSystem() {
    override val homeDir: String by lazy { System.getProperty("user.home") }

    override fun getEnvVar(name: String): String? = envOverride[name] ?: System.getenv(name)

    override fun execute(command: String, vararg args: String): ProcessResult {
        val cmdArgs = listOf(command, *args)
        val cmd by lazy(LazyThreadSafetyMode.NONE) {
            cmdArgs.joinToString(separator = " ") { it.replace(" ", "\\ ") }
        }

        return try {
            logger.i { "Execute '$cmd'" }
            val process = ProcessBuilder(cmdArgs).apply {
                if (envOverride.isNotEmpty()) environment().putAll(envOverride)
            }.redirectErrorStream(true).start()

            var output = ""
            process.inputStream.reader().useLines {
                it.forEachIndexed { index, s ->
                    if (index > 0) output += "\n"
                    logger.v { s }
                    output += s
                }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) logger.e { "Error code '$cmd' = $exitCode" }

            ProcessResult(exitCode, output.ifBlank { null })
        } catch (e: IOException) {
            logger.d(e) { "Error executing '$cmd'" }
            ProcessResult(-1, null)
        } catch (e: Exception) {
            logger.e(e) { "Error executing '$cmd'" }
            ProcessResult(-1, null)
        }
    }

    override fun retrieveUrl(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP error code = ${connection.responseCode}")
            }

            return runCatching { connection.inputStream.bufferedReader().use { it.readText() } }.getOrNull().orEmpty()
        } finally {
            connection.disconnect()
        }
    }
    override fun downloadUrl(url: String, targetPath: String) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP error code = ${connection.responseCode}")
            }

            Paths.get(targetPath).outputStream().buffered().use { out ->
                connection.inputStream.use { it.transferTo(out) }
            }
        } finally {
            connection.disconnect()
        }
    }

    override fun pwd(): String = Paths.get("").toAbsolutePath().toString()
    @OptIn(ExperimentalEncodingApi::class)
    override fun parseCert(certString: String): Map<String, String> {
        val factory = CertificateFactory.getInstance("X.509")

        val cleanCert = certString
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\\s+".toRegex(), "")
        val decodedCert = Base64.decode(cleanCert)

        val cert = factory.generateCertificate(decodedCert.inputStream()) as X509Certificate
        val dn = cert.subjectX500Principal.name
        val info = dn.split(",").associate {
            it.substringBefore("=") to it.substringAfter("=")
        }
        return info
    }

    override fun findAppsPathsInDirectory(prefix: String, directory: String, recursively: Boolean): List<String> =
        buildList {
            findAppsPathsInDirectoryImpl(directory, prefix, recursively)
        }

    private fun MutableList<String>.findAppsPathsInDirectoryImpl(
        directory: String, prefix: String, recursively: Boolean
    ) {
        Files.walk(Paths.get(directory)).forEach {
            val isDirectory = Files.isDirectory(it)
            val name = it.fileName.toString()
            if (isDirectory && name.startsWith(prefix) && name.endsWith(".app")) {
                add(it.toString())
            } else if (recursively && isDirectory && name.trim('.') != "") { //exclude . & .. child paths
                findAppsPathsInDirectoryImpl(prefix, it.toString(), true)
            }
        }
    }

    override fun fileExists(path: String): Boolean = Files.exists(Paths.get(path))

    override fun readFile(path: String): String? = Paths.get(path).takeIf { Files.isReadable(it) }?.readText()

    override fun writeTempFile(content: String): String {
        logger.d("writeTempFile")

        val tempFile = Files.createTempFile(null, null)
        logger.d { "tempFile = $tempFile" }

        tempFile.writeText(content)
        return tempFile.toString()
    }

    override fun readArchivedFile(pathToArchive: String, pathToFile: String): String? {
        ZipInputStream(Paths.get(pathToArchive).inputStream()).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                if (zipEntry.name == pathToFile) {
                    return zis.reader().readText()
                }
                zipEntry = zis.nextEntry
            }
        }
        return null
    }

    override fun createTempDir(): String = Files.createTempDirectory(null).toString()

    override fun find(path: String, nameFilter: String): List<String>? = runCatching {
        val start = Paths.get(path)
        val globMatcher = start.fileSystem.getPathMatcher("glob:$nameFilter")
        Files.find(start, 10, { p, attr -> attr.isRegularFile && globMatcher.matches(p.fileName) }).use { files ->
            files.map(Path::toString).toList()
        }
    }.getOrNull()

    override fun list(path: String): String? = runCatching {
        Files.list(Paths.get(path)).asSequence().joinToString("\n")
    }.getOrNull()

    override fun rm(path: String): Boolean = runCatching {
        Files.deleteIfExists(Paths.get(path))
    }.getOrElse { false }

    override fun mv(from: String, to: String): Boolean = runCatching {
        Files.move(Paths.get(from), Paths.get(to)); true
    }.getOrElse { false }
}