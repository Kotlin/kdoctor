import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.doctor.entity.Compatibility
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.jetbrains.kotlin.doctor.entity.Version
import kotlin.test.Test

class VerifyCompatibilityJson {

    @Test
    fun verify() {
        val compatibilityJson = javaClass.getResource("compatibility.json")?.readText().orEmpty()
        val compatibility = Json.decodeFromString<Compatibility>(compatibilityJson)
        val knownEnvNames = EnvironmentPiece.allNames
        compatibility.problems.flatMap { problem ->
            problem.matrix.map { it.name }
        }.forEach { jsonEnvName ->
            if (!knownEnvNames.contains(jsonEnvName) && !jsonEnvName.startsWith("GradlePlugin(")) {
                error("Unknown env name: $jsonEnvName")
            }
        }
        val invalidVersions = mutableListOf<String>()
        compatibility.problems.forEach { problem ->
            problem.matrix.forEach { range ->
                range.from?.let {  str ->
                    val version = Version(str)
                    if (version.semVersion == null) {
                        invalidVersions.add(str)
                    }
                }
                range.fixedIn?.let {  str ->
                    val version = Version(str)
                    if (version.semVersion == null) {
                        invalidVersions.add(str)
                    }
                }
            }
        }
        if (invalidVersions.isNotEmpty()) {
            error("Invalid semantic versions: $invalidVersions")
        }

        println(compatibility.problems.joinToString("\n") { it.url })
    }
}