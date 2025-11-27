package com.macro.app.data

import com.macro.app.core.Action
import com.macro.app.core.FlowControl
import com.macro.app.core.FlowType

data class ActionItem(
    val action: Action,
    val delayMs: Long = 0,
    val flowControl: FlowControl = FlowControl(FlowType.NONE),
    val description: String = "",
    val nextActionIndex: Int? = null
)