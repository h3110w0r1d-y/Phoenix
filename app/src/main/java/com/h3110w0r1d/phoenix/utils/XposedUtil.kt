package com.h3110w0r1d.phoenix.utils

import androidx.annotation.Keep

@Keep
object XposedUtil {
    fun isModuleEnabled(): Boolean = false

    fun getModuleVersion(): Int = -1
}
