package org.jetbrains.kotlin.doctor.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EnvironmentPiece {
    @SerialName("macos") Macos,
    @SerialName("jdk") Jdk,
    @SerialName("androidStudio") AndroidStudio,
    @SerialName("kotlinPlugin") KotlinPlugin,
    @SerialName("cocoapods") Cocoapods,
    @SerialName("kmmPlugin") KmmPlugin,
    @SerialName("xcode") Xcode;
}

@Serializable
enum class CompatibilityPriority {
    @SerialName("error") Error,
    @SerialName("warning") Warning,
    @SerialName("info") Info
}

@Serializable
data class CompatibilityRange(
    @SerialName("from") val from: String? = null,
    @SerialName("to") val to: String? = null
)

@Serializable
data class CompatibilityProblem(
    @SerialName("trackerId") val trackerId: String,
    @SerialName("title") val title: String,
    @SerialName("priority") val priority: CompatibilityPriority,
    @SerialName("matrix") val matrix: Map<EnvironmentPiece, CompatibilityRange>
)

@Serializable
data class Compatibility(
    @SerialName("problems") val problems: List<CompatibilityProblem>
)