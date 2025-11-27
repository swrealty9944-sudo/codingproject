package com.macro.app.registry

import android.content.Context
import com.macro.app.core.Action
import com.macro.app.core.ActionExecutor
import com.macro.app.core.ActionType
import com.macro.app.executor.TouchExecutor
import com.macro.app.executor.SwipeExecutor
import com.macro.app.executor.ImageSearchExecutor
import com.macro.app.executor.ConditionExecutor
import com.macro.app.service.MacroService
import com.macro.app.capture.ScreenCapture
import com.macro.app.condition.ConditionEvaluator

object ExecutorRegistry {
    private lateinit var context: Context
    private lateinit var screenCapture: ScreenCapture
    private var touchExecutor: TouchExecutor? = null
    private var swipeExecutor: SwipeExecutor? = null
    private var imageSearchExecutor: ImageSearchExecutor? = null
    private var conditionExecutor: ConditionExecutor? = null

    fun initialize(context: Context, screenCapture: ScreenCapture) {
        this.context = context
        this.screenCapture = screenCapture
    }

    fun getExecutor(type: ActionType): ActionExecutor {
        return when(type) {
            ActionType.TOUCH -> {
                if (touchExecutor == null) {
                    touchExecutor = TouchExecutor()
                    MacroService.instance?.let { touchExecutor?.setService(it) }
                }
                touchExecutor!!
            }
            ActionType.SWIPE -> {
                if (swipeExecutor == null) {
                    swipeExecutor = SwipeExecutor()
                    MacroService.instance?.let { swipeExecutor?.setService(it) }
                }
                swipeExecutor!!
            }
            ActionType.IMAGE_SEARCH -> {
                if (imageSearchExecutor == null) {
                    MacroService.instance?.let { service ->
                        imageSearchExecutor = ImageSearchExecutor(context, service, screenCapture)
                    } ?: throw IllegalStateException("MacroService not initialized")
                }
                imageSearchExecutor!!
            }
            ActionType.CONDITION -> {
                if (conditionExecutor == null) {
                    val imageExecutor = getExecutor(ActionType.IMAGE_SEARCH) as ImageSearchExecutor
                    val evaluator = ConditionEvaluator(imageExecutor)
                    conditionExecutor = ConditionExecutor(evaluator)
                }
                conditionExecutor!!
            }
            ActionType.DELAY -> DelayExecutor()
            else -> throw IllegalArgumentException("Unknown action type: $type")
        }
    }

    fun reset() {
        touchExecutor?.stop()
        swipeExecutor?.stop()
        imageSearchExecutor?.stop()
        conditionExecutor?.stop()
        
        touchExecutor = null
        swipeExecutor = null
        imageSearchExecutor = null
        conditionExecutor = null
    }

    private class DelayExecutor : ActionExecutor {
        override fun execute(action: Action): Boolean {
            val delayMs = action.params["delayMs"] as? Long ?: 0L
            if (delayMs > 0) {
                Thread.sleep(delayMs)
            }
            return true
        }

        override fun stop() {}
    }
}