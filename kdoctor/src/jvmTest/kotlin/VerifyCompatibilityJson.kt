import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.doctor.entity.Compatibility
import org.jetbrains.kotlin.doctor.entity.EnvironmentPiece
import org.junit.Test

class VerifyCompatibilityJson {

    @Test
    fun verify() {
        val compatibilityJson = javaClass.getResource("compatibility.json")?.readText().orEmpty()
        val compatibility = Json.decodeFromString<Compatibility>(compatibilityJson)
        val knownEnvNames = EnvironmentPiece.allNames
        compatibility.problems.flatMap { problem ->
            problem.matrix.map { it.name }
        }.forEach { jsonEnvName ->
            if (!knownEnvNames.contains(jsonEnvName)) {
                error("Unknown env name: $jsonEnvName")
            }
        }
        println(compatibility.problems.joinToString("\n") { it.url })
    }
}