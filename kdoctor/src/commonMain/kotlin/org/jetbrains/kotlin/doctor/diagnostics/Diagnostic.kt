package org.jetbrains.kotlin.doctor.diagnostics

abstract class Diagnostic(val name: String) {
    enum class Result(val symbol: Char) {
        Success('v'),
        Info('i'),
        Warning('!'),
        Failure('x')
    }

    data class Message(val result: Result, val text: String)

    private class Paragraph(val title: String, vararg val text: String) {
        fun print() = (title + "\n" + text.joinToString("\n").prependIndent()).trim()
    }

    protected open fun getResultIndex(result: Result) = when (result) {
        Result.Failure -> 0
        Result.Warning -> 1
        Result.Success -> 2
        Result.Info -> 3
    }

    protected abstract fun runChecks(): List<Message>

    fun diagnose(verbose: Boolean): String {
        val messages = runChecks()
        val (result, text) = if (messages.isEmpty()) {
            Message(Result.Failure, "Diagnostic returned no result")
        } else {
            Message(
                messages.sortedWith { a, b -> getResultIndex(a.result) - getResultIndex(b.result) }.first().result,
                messages.joinToString("\n\n") { it.text }
            )
        }
        return Paragraph(
            "[${result.symbol}] $name",
            if (verbose) text else ""
        ).print()
    }

    fun MutableCollection<Message>.addSuccess(title: String, vararg text: String) =
        add(Message(Result.Success, Paragraph(title, *text).print()))

    fun MutableCollection<Message>.addInfo(title: String, vararg text: String) =
        add(Message(Result.Info, Paragraph(title, *text).print()))

    fun MutableCollection<Message>.addWarning(title: String, vararg text: String) =
        add(Message(Result.Warning, Paragraph(title, *text).print()))

    fun MutableCollection<Message>.addFailure(title: String, vararg text: String) =
        add(Message(Result.Failure, Paragraph(title, *text).print()))
}
