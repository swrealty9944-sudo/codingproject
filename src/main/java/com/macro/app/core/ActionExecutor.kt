package com.macro.app.core

interface ActionExecutor {
    fun execute(action: Action): Boolean
    fun stop()
}