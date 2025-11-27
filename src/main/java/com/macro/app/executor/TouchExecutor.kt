package com.macro.app.executor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.macro.app.core.Action
import com.macro.app.core.ActionExecutor
import com.macro.app.service.MacroService

class TouchExecutor : ActionExecutor {
    private var service: AccessibilityService? = null
    
    fun setService(service: AccessibilityService) {
        this.service = service
    }
    
    override fun execute(action: Action): Boolean {
        val x = action.params["x"] as? Int ?: return false
        val y = action.params["y"] as? Int ?: return false
        val duration = action.params["duration"] as? Long ?: 100L
        
        val currentService = service ?: MacroService.instance ?: return false
        
        return performTouch(currentService, x.toFloat(), y.toFloat(), duration)
    }
    
    private fun performTouch(
        service: AccessibilityService,
        x: Float,
        y: Float,
        duration: Long
    ): Boolean {
        val path = Path().apply {
            moveTo(x, y)
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