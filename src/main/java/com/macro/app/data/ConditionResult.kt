package com.macro.app.data

data class ConditionResult(
    val success: Boolean,
    val message: String = "",
    val value: Any? = null
)