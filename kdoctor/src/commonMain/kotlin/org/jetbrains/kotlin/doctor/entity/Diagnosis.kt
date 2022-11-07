package org.jetbrains.kotlin.doctor.entity

enum class DiagnosisResult(val symbol: Char) {
    Success('v'),
    Info('i'),
    Warning('!'),
    Failure('x')
}

data class DiagnosisEntry(val result: DiagnosisResult, val text: String)

data class Diagnosis(
    val title: String,
    val entries: List<DiagnosisEntry>,
    val conclusion: DiagnosisResult
) {

    val text = buildString {
        appendLine("[${conclusion.symbol}] $title")
        append(entries.joinToString("\n") { it.text.prependIndent() })
    }

    class Builder(private val title: String) {
        private val attentionPrefix = "* "
        private val entries = mutableListOf<DiagnosisEntry>()
        private var conclusion: DiagnosisResult? = null

        private fun paragraph(
            prefix: String,
            title: String,
            vararg text: String
        ) = buildString {
            appendLine(
                title.lines()
                    .mapIndexed { i: Int, s: String ->
                        if (i == 0) s.prependIndent(prefix) else s.prependIndent(" ".repeat(prefix.length))
                    }
                    .joinToString("\n")
            )
            if (text.isEmpty().not()) {
                appendLine(
                    text.joinToString("\n") {
                        it.prependIndent().prependIndent(" ".repeat(prefix.length))
                    }
                )
            }
        }

        private fun addEntry(result: DiagnosisResult, prefix: String, title: String, vararg text: String) {
            entries.add(DiagnosisEntry(result, paragraph(prefix, title, *text)))
        }

        fun addSuccess(title: String, vararg text: String) {
            addEntry(DiagnosisResult.Success, "", title, *text)
        }

        fun addInfo(title: String, vararg text: String) {
            addEntry(DiagnosisResult.Info, attentionPrefix, title, *text)
        }

        fun addWarning(title: String, vararg text: String) {
            addEntry(DiagnosisResult.Warning, attentionPrefix, title, *text)
        }

        fun addFailure(title: String, vararg text: String) {
            addEntry(DiagnosisResult.Failure, attentionPrefix, title, *text)
        }

        fun addPlainMessage(result: DiagnosisResult, text: String) {
            entries.add(DiagnosisEntry(result, text))
        }

        fun setConclusion(conclusion: DiagnosisResult) {
            this.conclusion = conclusion
        }

        fun build() = Diagnosis(
            title,
            entries,
            conclusion ?: entries.minByOrNull { it.result }?.result ?: DiagnosisResult.Failure
        )
    }
}