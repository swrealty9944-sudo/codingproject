package com.macro.app.executor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.macro.app.core.Action
import com.macro.app.core.ActionExecutor
import com.macro.app.service.MacroService

class SwipeExecutor : ActionExecutor {
    private var service: AccessibilityService? = null
    
    fun setService(service: AccessibilityService) {
        this.service = service
    }
    
    override fun execute(action: Action): Boolean {
        val startX = action.params["startX"] as? Int ?: return false
        val startY = action.params["startY"] as? Int ?: return false
        val endX = action.params["endX"] as? Int ?: return false
        val endY = action.params["endY"] as? Int ?: return false
        val duration = action.params["duration"] as? Long ?: 500L
        
        val currentService = service ?: MacroService.instance ?: return false
        
        return performSwipe(
            currentService,
            startX.toFloat(),
            startY.toFloat(),
            endX.toFloat(),
            endY.toFloat(),
            duration
        )
    }
    
    private fun performSwipe(
        service: AccessibilityService,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        
        val gestureBuilder = GestureDescription.Builder()
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, duration)
        gestureBuilder.addStroke(strokeDescription)
        
        var success = false
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                success = true
            }
            
            override fun onCancelled(gestureDescription: GestureDescription?) {
                success = false
            }
        }
        
        service.dispatchGesture(gestureBuilder.build(), callback, null)
        
        Thread.sleep(duration + 50)
        return success
    }
    
    override fun stop() {
        service = null
    }
}