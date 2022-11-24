package org.jetbrains.kotlin.doctor.entity

import org.jetbrains.kotlin.doctor.printer.TextPainter

enum class DiagnosisResult(val symbol: Char, val color: String) {
    Success('✓', TextPainter.GREEN),
    Info('i', TextPainter.BLUE),
    Warning('!', TextPainter.YELLOW),
    Failure('✖', TextPainter.RED)
}

data class DiagnosisEntry(val result: DiagnosisResult, val text: String)

data class Diagnosis(
    val title: String,
    val entries: List<DiagnosisEntry>,
    val checkedEnvironments: List<Set<EnvironmentPiece>>, //there maybe e.g. several AS with installed plugins
    val conclusion: DiagnosisResult
) {

    val text = buildString {
        val prefix = with(conclusion) {
            "$color[$symbol]${TextPainter.RESET}"
        }
        appendLine("$prefix $title")
        entries.forEach { entry ->
            val mark = when (entry.result) {
                DiagnosisResult.Success -> "  ➤ "
                else -> "${entry.result.color}  ${entry.result.symbol} "
            }
            appendLine(entry.text.markLines(mark, "     "))
        }
    }

    private fun String.markLines(first: String, other: String) =
        lines().mapIndexed { index, s ->
            if (index == 0) {
                "${TextPainter.BOLD}$first$s${TextPainter.RESET}"
            } else {
                "$other$s"
            }
        }.joinToString("\n")

    class Builder(private val title: String) {
        private val entries = mutableListOf<DiagnosisEntry>()
        private var conclusion: DiagnosisResult? = null
        private val checkedEnvironments: MutableList<Set<EnvironmentPiece>> = mutableListOf()

        fun addSuccess(vararg text: String) {
            entries.add(DiagnosisEntry(DiagnosisResult.Success, text.joinToString("\n")))
        }

        fun addInfo(vararg text: String) {
            entries.add(DiagnosisEntry(DiagnosisResult.Info, text.joinToString("\n")))
        }

        fun addWarning(vararg text: String) {
            entries.add(DiagnosisEntry(DiagnosisResult.Warning, text.joinToString("\n")))
        }

        fun addFailure(vararg text: String) {
            entries.add(DiagnosisEntry(DiagnosisResult.Failure, text.joinToString("\n")))
        }

        fun setConclusion(conclusion: DiagnosisResult) {
            this.conclusion = conclusion
        }

        fun addEnvironment(environment: Set<EnvironmentPiece>) {
            checkedEnvironments.add(environment)
        }

        fun build() = Diagnosis(
            title,
            entries,
            checkedEnvironments,
            conclusion
                ?: entries.firstOrNull { it.result == DiagnosisResult.Failure }?.result
                ?: DiagnosisResult.Success
        )
    }
}