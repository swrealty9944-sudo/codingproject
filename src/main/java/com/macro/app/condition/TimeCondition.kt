package com.macro.app.condition

import com.macro.app.data.Condition
import com.macro.app.data.ConditionResult

class TimeCondition {
    private var startTime: Long = 0
    
    fun start() {
        startTime = System.currentTimeMillis()
    }
    
    fun checkElapsed(condition: Condition): ConditionResult {
        val elapsedMs = condition.params["elapsedMs"] as? Long ?: return ConditionResult(false)
        val currentElapsed = System.currentTimeMillis() - startTime
        return ConditionResult(
            success = currentElapsed >= elapsedMs,
            message = "Elapsed: ${currentElapsed}ms / ${elapsedMs}ms",
            value = currentElapsed
        )
    }
    
    fun checkUntil(condition: Condition): ConditionResult {
        val targetHour = condition.params["hour"] as? Int ?: return ConditionResult(false)
        val targetMinute = condition.params["minute"] as? Int ?: return ConditionResult(false)
        
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        
        val targetMinutes = targetHour * 60 + targetMinute
        val currentMinutes = currentHour * 60 + currentMinute
        
        return ConditionResult(
            success = currentMinutes >= targetMinutes,
            message = "$currentHour:$currentMinute / $targetHour:$targetMinute"
        )
    }
}