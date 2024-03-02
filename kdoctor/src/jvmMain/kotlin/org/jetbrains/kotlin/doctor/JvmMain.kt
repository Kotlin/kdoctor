package org.jetbrains.kotlin.doctor

import org.jetbrains.kotlin.doctor.entity.JvmSystem
import org.jetbrains.kotlin.doctor.entity.System

actual fun getSystem(): System = JvmSystem()