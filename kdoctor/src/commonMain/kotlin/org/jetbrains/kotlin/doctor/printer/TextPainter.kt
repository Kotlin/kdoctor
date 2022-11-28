package org.jetbrains.kotlin.doctor.printer

object TextPainter {
    const val RESET = "\u001B[0m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BOLD = "\u001B[1m"

    fun String.removeColors(): String {
        var result = this
        listOf(RESET, RED, GREEN, YELLOW, BOLD).forEach { result = result.replace(it, "") }
        return result
    }
}
