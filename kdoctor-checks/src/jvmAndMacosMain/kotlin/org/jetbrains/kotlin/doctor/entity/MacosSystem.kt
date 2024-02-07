package org.jetbrains.kotlin.doctor.entity

import kotlinx.coroutines.*

abstract class MacosSystem(scope: CoroutineScope) : System {
    override val currentOS: OS = OS.MacOS
    override val osVersion: Deferred<Version?> = scope.async(start = CoroutineStart.LAZY) {
        execute("sw_vers", "-productVersion").output
            ?.let { Version(it) }
    }
    override val cpuInfo: Deferred<String?> = scope.async(start = CoroutineStart.LAZY) {
        execute("sysctl", "-n", "machdep.cpu.brand_string").output
    }
    override val shell: Deferred<Shell?> = scope.async(start = CoroutineStart.LAZY) {
        getEnvVar("SHELL")?.let { shellPath ->
            Shell.entries.firstOrNull { it.path == shellPath }
        }
    }
}
