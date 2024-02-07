package org.jetbrains.kotlin.doctor.diagnostics

import org.jetbrains.kotlin.doctor.entity.Diagnosis
import org.jetbrains.kotlin.doctor.entity.System
import kotlin.coroutines.cancellation.CancellationException

class TemplateProjectDiagnostic(
    private val system: System,
    private val tag: String = "template"
) : Diagnostic() {
    override val title = "Synthetic generated project"

    override suspend fun diagnose(): Diagnosis {
        val result = Diagnosis.Builder(title)

        val dir = system.createTempDir()
        if (dir.isEmpty()) {
            result.addFailure(
                "Error: impossible to create temporary directory",
                "Check your file system write permissions"
            )
            return result.build()
        }

        val zip = "$dir/archive.zip"

        try {
            system.downloadUrl("https://github.com/Kotlin/kdoctor/archive/refs/tags/$tag.zip", zip)
        } catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
            result.addFailure("Error: impossible to download a template project", e.message.orEmpty())
            return result.build()
        }

        val unzip = system.execute(
            "unzip",
            zip,
            "-d", dir
        )
        if (unzip.code != 0) {
            result.addFailure(
                "Error: impossible to unzip a template project"
            )
            return result.build()
        }

        val project = "$dir/kdoctor-$tag/template"
        val gradlew = "$project/gradlew"

        val gradleExecution = system.execute(
            gradlew,
            "-p", project,
            "clean", "linkReleaseFrameworkIosArm64", "jvmJar",
            "--info"
        )
        if (gradleExecution.code != 0) {
            result.addFailure(
                "Template project build has problems:",
                gradleExecution.rawOutput.orEmpty()
            )
            return result.build()
        }

        result.addSuccess(
            "Template project build was successful"
        )
        return result.build()
    }
}