package org.jetbrains.kotlin.doctor.entity

abstract class NativeMacosSystemBase : MacosSystem() {
    override suspend fun retrieveUrl(url: String): String {
        val curlResult = execute("curl", "--location", "--silent", "--fail", url)
        if (curlResult.code != 0) {
            error("curl error code = ${curlResult.code}")
        }
        return curlResult.output.orEmpty()
    }

    override suspend fun downloadUrl(url: String, targetPath: String) {
        val curlResult = execute(
            "curl",
            "--location", //for redirects
            "--silent", //hide progress output
            "--show-error", //show errors in silent mode
            "--fail", //return error code for HTTP errors
            "--output", targetPath,
            url
        )
        if (curlResult.code != 0) {
            error("curl error code = ${curlResult.code}\n${curlResult.rawOutput.orEmpty()}")
        }
    }

    override suspend fun find(path: String, nameFilter: String): List<String>? =
        execute("find", path, "-name", "\"$nameFilter\"").output?.split("\n")

    override suspend fun list(path: String) = execute("ls", path).output?.trim()
    override suspend fun pwd() = execute("pwd").output?.trim().orEmpty()
    override suspend fun parseCert(certString: String): Map<String, String> {
        val file = writeTempFile(certString)
        val subject = execute(
            "openssl",
            "x509",
            "-noout",
            "-subject",
            "-nameopt", "sep_multiline",
            "-nameopt", "utf8",
            "-in", file
        ).output ?: error("Couldn't read certificate!")
        val info = subject.lines().associate { line ->
            line.trim().substringBefore("=") to line.substringAfter("=")
        }
        return info
    }

    override suspend fun createTempDir(): String = execute("mktemp", "-d").output?.trim().orEmpty()
    override suspend fun rm(path: String): Boolean = execute("rm", path).code == 0
    override suspend fun mv(from: String, to: String): Boolean = execute("mv", from, to).code == 0
}