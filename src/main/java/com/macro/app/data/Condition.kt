package com.macro.app.data

import com.macro.app.core.ConditionType

data class Condition(
    val id: Int,
    val type: ConditionType,
    val params: Map<String, Any>,
    val description: String = ""
)