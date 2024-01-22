package org.jetbrains.kotlin.doctor

import org.jetbrains.kotlin.doctor.entity.NativeMacosSystem

internal actual fun initMain(): Main = Main(NativeMacosSystem())