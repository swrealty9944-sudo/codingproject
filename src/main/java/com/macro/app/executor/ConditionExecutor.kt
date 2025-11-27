package com.macro.app.executor

import com.macro.app.core.Action
import com.macro.app.core.ActionExecutor
import com.macro.app.condition.ConditionEvaluator
import com.macro.app.data.Condition
import com.macro.app.core.ConditionType
import kotlinx.coroutines.runBlocking

class ConditionExecutor(private val evaluator: ConditionEvaluator) : ActionExecutor {
    private var isRunning = false
    
    override fun execute(action: Action): Boolean {
        isRunning = true
        
        val condition = Condition(
            id = action.id,
            type = action.params["conditionType"] as? ConditionType ?: return false,
            params = action.params,
            description = action.params["description"] as? String ?: ""
        )
        
        val timeoutMs = action.params["timeoutMs"] as? Long ?: 30000L
        
        return runBlocking {
            try {
                val result = evaluator.evaluate(condition, timeoutMs)
                result.success
            } catch (e: Exception) {
                false
            }
        }
    }
    
    override fun stop() {
        isRunning = false
    }
}