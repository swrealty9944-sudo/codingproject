package com.macro.app.data

data class MatchResult(
    val found: Boolean,
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val centerX: Int get() = x + width / 2
    val centerY: Int get() = y + height / 2
}
