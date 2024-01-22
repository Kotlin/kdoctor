package org.jetbrains.kotlin.doctor.logging

class KermitKdoctorLogger : KdoctorLogger {
    override fun v(throwable: Throwable?, message: () -> String): Unit =
        co.touchlab.kermit.Logger.v(throwable, message = message)

    override fun d(throwable: Throwable?, message: () -> String): Unit =
        co.touchlab.kermit.Logger.d(throwable, message = message)

    override fun i(throwable: Throwable?, message: () -> String): Unit =
        co.touchlab.kermit.Logger.i(throwable, message = message)

    override fun e(throwable: Throwable?, message: () -> String): Unit =
        co.touchlab.kermit.Logger.e(throwable, message = message)

    override fun v(messageString: String, throwable: Throwable?): Unit =
        co.touchlab.kermit.Logger.v(messageString, throwable)

    override fun d(messageString: String, throwable: Throwable?): Unit =
        co.touchlab.kermit.Logger.d(messageString, throwable)

    override fun i(messageString: String, throwable: Throwable?): Unit =
        co.touchlab.kermit.Logger.i(messageString, throwable)

    override fun e(messageString: String, throwable: Throwable?): Unit =
        co.touchlab.kermit.Logger.e(messageString, throwable)
}