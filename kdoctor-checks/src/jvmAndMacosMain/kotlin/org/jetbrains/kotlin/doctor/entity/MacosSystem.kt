package org.jetbrains.kotlin.doctor.entity

abstract class MacosSystem : System {
    override val currentOS: OS = OS.MacOS
    override val osVersion: Version? by lazy {
        execute("sw_vers", "-productVersion").output
            ?.let { Version(it) }
    }
    override val cpuInfo: String? by lazy {
        execute("sysctl", "-n", "machdep.cpu.brand_string").output
    }
    override val shell: Shell? by lazy {
        getEnvVar("SHELL")?.let { shellPath ->
            Shell.entries.firstOrNull { it.path == shellPath }
        }
    }
}
