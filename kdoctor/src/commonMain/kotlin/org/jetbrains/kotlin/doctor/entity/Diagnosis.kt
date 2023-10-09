package org.jetbrains.kotlin.doctor.entity

import org.jetbrains.kotlin.doctor.printer.TextPainter

enum class DiagnosisResult(val symbol: Char, val color: String) {
    Success('✓', TextPainter.GREEN),
    Info('i', TextPainter.YELLOW),
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

    fun getText(verbose: Boolean) = buildString {
        val prefix = with(conclusion) {
            "$color[$symbol]${TextPainter.RESET}"
        }
        appendLine("$prefix $title")
        val paragraphs = if (verbose) entries else entries.filter { it.result == DiagnosisResult.Failure }

        paragraphs.forEach { entry ->
            entry.text.lines().forEachIndexed { index, s ->
                if (index == 0) {
                    when (entry.result) {
                        DiagnosisResult.Success -> appendLine(
                            "${TextPainter.BOLD}  ➤ $s${TextPainter.RESET}"
                        )
                        else -> appendLine(
                            "${TextPainter.BOLD}${entry.result.color}  ${entry.result.symbol} $s${TextPainter.RESET}"
                        )
                    }
                } else {
                    appendLine("    $s")
                }
            }
        }
    }.trim()

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

        fun addEnvironment(vararg environment: EnvironmentPiece) {
            checkedEnvironments.add(environment.toSet())
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