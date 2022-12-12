package org.jetbrains.kotlin.doctor.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VersionTest {

    @Test
    fun checkSemVer() {
        assertSemver("12.34.56-RC+meta", 12, 34, 56, "RC", "meta")
        assertSemver("12.34.56-RC", 12, 34, 56, "RC", null)
        assertSemver("12.34.56", 12, 34, 56, null, null)
        assertSemver("12.34", 12, 34, null, null, null)
        assertSemver("12.0", 12, 0, null, null, null)
        assertSemver("0.1", 0, 1, null, null, null)

        assertSemver("1.0.0-SNAPSHOT", 1, 0, 0, "SNAPSHOT", null)
        assertSemver("0.5.2(221)-3", 0, 5, 2, null, null)
        assertSemver("AI-222.4345.14.2221.9321504", 222, 4345, 14, null, null)
        assertSemver("openjdk version \"11.0.16\" 2022-07-19 LTS", 11, 0, 16, null, null)
        assertSemver("openjdk 17.0.4.1 2022-08-12", 17, 0, 4, null, null)
        assertSemver("ruby 3.1.3p185 (2022-11-24 revision 1a6b16756e) [arm64-darwin21]", 3, 1, 3, null, null)
        assertSemver("213-1.7.20-release-for-android-studio-AS6777.52", 1, 7, 20, "release-for-android-studio-AS6777.52", null)
    }

    @Test
    fun checkNotSemVer() {
        assertNotSemver("123")
        assertNotSemver("1-1")
        assertNotSemver("abc12..1")
        assertNotSemver("abc")
        assertNotSemver("a.b.c")
    }

    private fun assertSemver(
        rawString: String,
        major: Int?,
        minor: Int?,
        patch: Int?,
        prerelease: String?,
        meta: String?
    ) {
        val v = Version(rawString)
        assertEquals(v.rawString, rawString)
        assertEquals(v.major, major)
        assertEquals(v.minor, minor)
        assertEquals(v.patch, patch)
        assertEquals(v.prerelease, prerelease)
        assertEquals(v.meta, meta)
    }

    private fun assertNotSemver(rawString: String) {
        val v = Version(rawString)
        assertEquals(v.rawString, rawString)
        assertNull(v.semVersion)
        assertNull(v.semVersionShort)
        assertNull(v.major)
        assertNull(v.minor)
        assertNull(v.patch)
        assertNull(v.prerelease)
        assertNull(v.meta)
    }
}