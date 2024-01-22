package org.jetbrains.kotlin.doctor

import org.jetbrains.kotlin.doctor.entity.JvmMacosSystem
import org.jetbrains.kotlin.doctor.logging.KermitKdoctorLogger

internal actual fun initMain() = Main(JvmMacosSystem(KermitKdoctorLogger()))