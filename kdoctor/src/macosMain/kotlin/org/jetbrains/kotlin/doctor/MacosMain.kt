package org.jetbrains.kotlin.doctor

import org.jetbrains.kotlin.doctor.entity.MacosSystem
import org.jetbrains.kotlin.doctor.entity.System

actual fun getSystem(): System = MacosSystem()