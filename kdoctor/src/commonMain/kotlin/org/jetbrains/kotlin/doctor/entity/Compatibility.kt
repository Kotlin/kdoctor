package org.jetbrains.kotlin.doctor.entity

import co.touchlab.kermit.Logger
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
) {
    companion object {
        suspend fun download() = try {
            Logger.d("Compatibility.download")
            //todo fix json url!
            val compatibilityJson = System.httpClient.get(
                "https://github.com/Kotlin/kdoctor/raw/wip/kdoctor/src/commonMain/resources/compatibility.json"
            ).bodyAsText()
            Logger.d(compatibilityJson)
            Json.decodeFromString<Compatibility>(compatibilityJson)
        } catch (e: Exception) {
            Logger.e("Compatibility.download error ${e.message}", e)
            Compatibility(emptyList())
        }
    }
}