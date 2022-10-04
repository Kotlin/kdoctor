package org.jetbrains.kotlin.doctor

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal object Log {
    private val isEnabled get() = isDebug

    private val currentTime get() = NSDate().timeIntervalSince1970().toULong()

    fun d(message: () -> String) {
        if (isEnabled) println("[$currentTime] debug: ${message()}")
    }

    fun e(message: () -> String) {
        if (isEnabled) println("[$currentTime] error: ${message()}")
    }
}