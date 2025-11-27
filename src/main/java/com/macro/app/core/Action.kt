package com.macro.app.core

data class Action(
    val id: Int,
    val type: ActionType,
    val params: Map<String, Any>
)