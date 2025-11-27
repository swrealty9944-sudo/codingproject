package com.macro.app.condition

import com.macro.app.data.Condition
import com.macro.app.data.ConditionResult

class CountCondition {
    private val counters = mutableMapOf<Int, Int>()
    
    fun increment(conditionId: Int) {
        counters[conditionId] = (counters[conditionId] ?: 0) + 1
    }
    
    fun check(condition: Condition): ConditionResult {
        val targetCount = condition.params["count"] as? Int ?: return ConditionResult(false)
        val currentCount = counters[condition.id] ?: 0
        
        return ConditionResult(
            success = currentCount >= targetCount,
            message = "Count: $currentCount / $targetCount",
            value = currentCount
        )
    }
    
    fun reset(conditionId: Int) {
        counters[conditionId] = 0
    }
    
    fun resetAll() {
        counters.clear()
    }
}