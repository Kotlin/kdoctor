package org.jetbrains.kotlin.doctor.entity

import kotlin.math.max

class Version : Comparable<Version> {
    val rawString: String
    val major: Int?
    val minor: Int?
    val patch: Int?
    val prerelease: String?
    val meta: String?

    constructor(major: Int, minor: Int, patch: Int? = null, prerelease: String? = null, meta: String? = null) {
        this.major = major
        this.minor = minor
        this.patch = patch
        this.prerelease = prerelease
        this.meta = meta
        var version = "$major.$minor"
        version += if (patch != null) ".$patch" else ""
        version += if (prerelease != null) "-$prerelease" else ""
        version += if (meta != null) "+$meta" else ""
        rawString = version
    }

    constructor(version: String) {
        rawString = version
        val match = regex.find(this.rawString)
        major = match?.groups?.get(1)?.value?.toIntOrNull()
        minor = match?.groups?.get(2)?.value?.toIntOrNull()
        patch = match?.groups?.get(3)?.value?.toIntOrNull()
        prerelease = match?.groups?.get(4)?.value
        meta = match?.groups?.get(5)?.value
    }

    val prevMinorVersion: Version
        get() {
            if (major == null || minor == null) return unknown
            return Version(major, max(minor - 1, 0), 0)
        }

    val nextMinorVersion: Version
        get() {
            if (major == null || minor == null) return unknown
            return Version(major, minor + 1, 0)
        }

    override fun compareTo(other: Version): Int {
        if (semVersion == null && other.semVersion != null) return less
        if (semVersion != null && other.semVersion == null) return greater
        if (semVersion == other.semVersion) return equal

        if (major != null && other.major != null && major != other.major) {
            return major.compareTo(other.major)
        }
        if (minor != null && other.minor != null && minor != other.minor) {
            return minor.compareTo(other.minor)
        }
        if (patch != null && other.patch == null) return greater
        if (patch == null && other.patch != null) return less
        if (patch != null && other.patch != null && patch != other.patch) {
            return patch.compareTo(other.patch)
        }
        if (prerelease != null && other.prerelease == null) return less
        if (prerelease == null && other.prerelease != null) return greater
        if (prerelease != null && other.prerelease != null && prerelease != other.prerelease) {
            return prerelease.compareTo(other.prerelease)
        }

        return equal
    }

    override fun equals(other: Any?): Boolean {
        if (other is Version) {
            return rawString == other.rawString
        }
        return false
    }

    override fun hashCode(): Int = rawString.hashCode()

    override fun toString(): String = rawString

    val semVersionShort: String?
        get() {
            if (major == null || minor == null) return null
            var semVersionShort = "$major.$minor"
            if (patch != null) {
                semVersionShort += ".$patch"
            }
            return semVersionShort
        }

    val semVersion: String?
        get() {
            var semVersion = semVersionShort ?: return null
            if (prerelease != null) {
                semVersion += "-$prerelease"
            }
            if (meta != null) {
                semVersion += "+$meta"
            }
            return semVersion
        }

    companion object {
        private val regex = Regex("(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.?(0|[1-9]\\d*)*(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?")
        private const val less = -1
        private const val greater = 1
        private const val equal = 0

        val unknown = Version("?")
    }
}