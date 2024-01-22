package org.jetbrains.kotlin.doctor.logging

interface KdoctorLogger {
    fun v(throwable: Throwable? = null, message: () -> String)
    fun d(throwable: Throwable? = null, message: () -> String)
    fun i(throwable: Throwable? = null, message: () -> String)
    fun e(throwable: Throwable? = null, message: () -> String)

    fun v(messageString: String, throwable: Throwable? = null)
    fun d(messageString: String, throwable: Throwable? = null)
    fun i(messageString: String, throwable: Throwable? = null)
    fun e(messageString: String, throwable: Throwable? = null)
}
