package org.jetbrains.kotlin.doctor.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.doctor.KDOCTOR_VERSION

private const val COMPATIBILITY_JSON = "https://github.com/Kotlin/kdoctor/raw/master/kdoctor/src/commonMain/resources/compatibility.json"

@Suppress("FunctionName")
data class EnvironmentPiece(
    val name: String,
    val version: Version
) {
    companion object {
        private const val Macos = "Macos"
        private const val Jdk = "Jdk"
        private const val AndroidStudio = "AndroidStudio"
        private const val KotlinPlugin = "KotlinPlugin"
        private const val Cocoapods = "CocoaPods"
        private const val KmmPlugin = "KmmPlugin"
        private const val Ruby = "Ruby"
        private const val RubyGems = "RubyGems"
        private const val Xcode = "Xcode"
        private const val Gradle = "Gradle"
        private const val GradlePlugin = "GradlePlugin"
        val allNames get() = setOf(
            Macos, Jdk, AndroidStudio, KotlinPlugin, Cocoapods, KmmPlugin, Ruby, RubyGems, Xcode, Gradle, GradlePlugin
        )

        fun Macos(version: Version) = EnvironmentPiece(Macos, version)
        fun Jdk(version: Version) = EnvironmentPiece(Jdk, version)
        fun AndroidStudio(version: Version) = EnvironmentPiece(AndroidStudio, version)
        fun KotlinPlugin(version: Version) = EnvironmentPiece(KotlinPlugin, version)
        fun Cocoapods(version: Version) = EnvironmentPiece(Cocoapods, version)
        fun KmmPlugin(version: Version) = EnvironmentPiece(KmmPlugin, version)
        fun Ruby(version: Version) = EnvironmentPiece(Ruby, version)
        fun RubyGems(version: Version) = EnvironmentPiece(RubyGems, version)
        fun Xcode(version: Version) = EnvironmentPiece(Xcode, version)
        fun Gradle(version: Version) = EnvironmentPiece(Gradle, version)
        fun GradlePlugin(id: String, version: Version) = EnvironmentPiece("$GradlePlugin($id)", version)
    }
}

@Serializable
data class EnvironmentRange(
    @SerialName("name") val name: String,
    @SerialName("from") val from: String? = null,
    @SerialName("fixedIn") val fixedIn: String? = null
)

@Serializable
data class CompatibilityProblem(
    @SerialName("url") val url: String,
    @SerialName("text") val text: String,
    @SerialName("isCritical") val isCritical: Boolean = false,
    @SerialName("matrix") val matrix: Set<EnvironmentRange>
)

@Serializable
data class Compatibility(
    @SerialName("latestKdoctor") val latestKdoctor: String,
    @SerialName("problems") val problems: List<CompatibilityProblem>
) {
    companion object {
        suspend fun download(system: System) = try {
            system.logger.d("Compatibility.download: $COMPATIBILITY_JSON")
            from(system, system.retrieveUrl(COMPATIBILITY_JSON))
        } catch (e: Exception) {
            system.logger.e("Compatibility.download error ${e.message}", e)
            Compatibility(KDOCTOR_VERSION, emptyList())
        }

        fun from(system: System, compatibilityJson: String) = try {
            system.logger.d(compatibilityJson)
            Json.decodeFromString(compatibilityJson)
        } catch (e: Exception) {
            system.logger.e("Compatibility json error ${e.message}", e)
            Compatibility(KDOCTOR_VERSION, emptyList())
        }
    }
}