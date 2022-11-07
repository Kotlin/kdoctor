import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.doctor.entity.Compatibility
import org.junit.Test

class VerifyCompatibilityJson {

    @Test
    fun verify() {
        val compatibilityJson = javaClass.getResource("compatibility.json")?.readText().orEmpty()
        val compatibility = Json.decodeFromString<Compatibility>(compatibilityJson)
        println(compatibility.problems.joinToString("\n") { it.issueUrl })
    }
}