package com.macro.app.manager

import com.macro.app.core.FlowType
import com.macro.app.data.ActionItem

class ActionFlowController {
    private var currentIndex = 0
    private val history = mutableListOf<Int>()

    fun reset() {
        currentIndex = 0
        history.clear()
    }

    fun getNextIndex(currentItem: ActionItem, totalSize: Int): Int? {
        history.add(currentIndex)

        return when (currentItem.flowControl.type) {
            FlowType.NONE -> {  // ← NORMAL을 NONE으로
                if (currentIndex + 1 < totalSize) currentIndex + 1 else null
            }
            FlowType.GOTO -> {
                val target = currentItem.flowControl.targetIndex
                if (target != null && target in 0 until totalSize) target else null
            }
            FlowType.BACK -> {
                if (history.size > 1) {
                    history.removeAt(history.size - 1)
                    history.lastOrNull()
                } else null
            }
            FlowType.SKIP -> {
                if (currentIndex + 2 < totalSize) currentIndex + 2 else null
            }
            FlowType.RESET -> 0
        }.also { nextIndex ->
            if (nextIndex != null) {
                currentIndex = nextIndex
            }
        }
    }

    fun getCurrentIndex(): Int = currentIndex

    fun setCurrentIndex(index: Int) {
        currentIndex = index
    }

    fun getHistory(): List<Int> = history.toList()

    fun clearHistory() {
        history.clear()
    }
}