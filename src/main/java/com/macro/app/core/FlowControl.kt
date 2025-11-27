package com.macro.app.core

data class FlowControl(
    val type: FlowType,
    val targetIndex: Int? = null,
    val steps: Int? = 1
)