package org.jetbrains.kotlin.doctor.diagnostics

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class Diagnostic(val name: String) {
    enum class ResultType(val symbol: Char) {
        Success('v'),
        Info('i'),
        Warning('!'),
        Failure('x')
    }

    data class Message(val resultType: ResultType, val text: String)

    inner class Result(private val title: String, messages: List<Message>) {

        private val messages: List<Message> = messages.ifEmpty {
            listOf(Message(ResultType.Failure, "Diagnostic returned no result"))
        }

        val resultType = this.messages.minByOrNull { getResultTypeSeverity(it.resultType) }?.resultType ?: ResultType.Failure

        val text = buildString {
            appendLine("[${resultType.symbol}] $title")
            append(messages.joinToString("\n") { it.text.prependIndent() })
        }
    }

    private class Paragraph(val title: String, vararg val text: String) {
        fun print(prefix: String = "") = buildString {
            appendLine(
                title.lines()
                    .mapIndexed { i: Int, s: String -> if (i == 0) s.prependIndent(prefix) else s.prependIndent(" ".repeat(prefix.length)) }
                    .joinToString("\n")
            )
            if (text.isEmpty().not()) appendLine(text.joinToString("\n") { it.prependIndent().prependIndent(" ".repeat(prefix.length)) })
        }
    }

    protected open fun getResultTypeSeverity(resultType: ResultType) = when (resultType) {
        ResultType.Failure -> 0
        ResultType.Warning -> 1
        ResultType.Success -> 2
        ResultType.Info -> 3
    }

    protected abstract fun runChecks(): List<Message>

    suspend fun diagnose(verbose: Boolean): Result = suspendCoroutine {
        it.resume(Result(name, runChecks()))
    }

    private val attentionPrefix = "* "

    fun MutableCollection<Message>.addSuccess(title: String, vararg text: String) =
        add(Message(ResultType.Success, Paragraph(title, *text).print()))

    fun MutableCollection<Message>.addInfo(title: String, vararg text: String) =
        add(Message(ResultType.Info, Paragraph(title, *text).print(attentionPrefix)))

    fun MutableCollection<Message>.addWarning(title: String, vararg text: String) =
        add(Message(ResultType.Warning, Paragraph(title, *text).print(attentionPrefix)))

    fun MutableCollection<Message>.addFailure(title: String, vararg text: String) =
        add(Message(ResultType.Failure, Paragraph(title, *text).print(attentionPrefix)))
}
