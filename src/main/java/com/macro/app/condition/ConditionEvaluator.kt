package com.macro.app.condition

import com.macro.app.core.ConditionType
import com.macro.app.data.Condition
import com.macro.app.data.ConditionResult
import com.macro.app.executor.ImageSearchExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

class ConditionEvaluator(private val imageSearchExecutor: ImageSearchExecutor) {
    private val timeCondition = TimeCondition()
    private val imageCondition = ImageCondition(imageSearchExecutor)
    private val countCondition = CountCondition()

    init {
        timeCondition.start()
    }

    suspend fun evaluate(condition: Condition, timeoutMs: Long = 30000L): ConditionResult {
        return try {
            withTimeout(timeoutMs) {
                when (condition.type) {
                    ConditionType.TIME_ELAPSED -> timeCondition.checkElapsed(condition)
                    ConditionType.TIME_UNTIL -> timeCondition.checkUntil(condition)
                    ConditionType.IMAGE_FOUND -> waitForImage(condition, true)
                    ConditionType.IMAGE_NOT_FOUND -> waitForImage(condition, false)
                    ConditionType.COUNT_REACHED -> countCondition.check(condition)
                    ConditionType.AND -> evaluateAnd(condition, timeoutMs)
                    ConditionType.OR -> evaluateOr(condition, timeoutMs)
                }
            }
        } catch (e: Exception) {
            ConditionResult(false, "Timeout or error: ${e.message}")
        }
    }

    private suspend fun waitForImage(condition: Condition, shouldFind: Boolean): ConditionResult {
        val checkIntervalMs = condition.params["checkIntervalMs"] as? Long ?: 500L

        while (true) {
            val result = if (shouldFind) {
                imageCondition.checkImageFound(condition)
            } else {
                imageCondition.checkImageNotFound(condition)
            }

            if (result.success) return result

            delay(checkIntervalMs)
        }
    }

    private suspend fun evaluateAnd(condition: Condition, timeoutMs: Long): ConditionResult {
        val subConditions = condition.params["conditions"] as? List<Condition>
            ?: return ConditionResult(false, "No sub-conditions")

        for (subCondition in subConditions) {
            val result = evaluate(subCondition, timeoutMs)
            if (!result.success) {
                return ConditionResult(false, "AND failed: ${result.message}")
            }
        }

        return ConditionResult(true, "AND success")
    }

    private suspend fun evaluateOr(condition: Condition, timeoutMs: Long): ConditionResult {
        val subConditions = condition.params["conditions"] as? List<Condition>
            ?: return ConditionResult(false, "No sub-conditions")

        for (subCondition in subConditions) {
            val result = evaluate(subCondition, timeoutMs)
            if (result.success) {
                return ConditionResult(true, "OR success: ${result.message}")
            }
        }

        return ConditionResult(false, "OR failed")
    }

    fun incrementCount(conditionId: Int) {
        countCondition.increment(conditionId)
    }

    fun resetCount(conditionId: Int) {
        countCondition.reset(conditionId)
    }

    fun resetAllCounts() {
        countCondition.resetAll()
    }
}