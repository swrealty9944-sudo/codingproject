package com.macro.app.condition

import com.macro.app.data.Condition
import com.macro.app.data.ConditionResult
import com.macro.app.executor.ImageSearchExecutor

class ImageCondition(private val imageSearchExecutor: ImageSearchExecutor) {
    
    fun checkImageFound(condition: Condition): ConditionResult {
        val templateId = condition.params["templateId"] as? String 
            ?: return ConditionResult(false, "Template ID missing")
        val threshold = condition.params["threshold"] as? Float ?: 0.8f
        
        val matchResult = imageSearchExecutor.searchImage(templateId, threshold)
        
        return ConditionResult(
            success = matchResult.found,
            message = "Match: ${matchResult.confidence}%",
            value = matchResult
        )
    }
    
    fun checkImageNotFound(condition: Condition): ConditionResult {
        val result = checkImageFound(condition)
        return result.copy(success = !result.success)
    }
}