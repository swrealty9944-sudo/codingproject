package com.macro.app.runner

import android.content.Context
import android.util.Log
import com.macro.app.data.ActionItem
import com.macro.app.manager.ActionListManager
import com.macro.app.manager.ActionFlowController
import com.macro.app.registry.ExecutorRegistry
import com.macro.app.core.FlowType
import kotlinx.coroutines.*

class MacroRunner(private val context: Context) {
    private var isRunning = false
    private var job: Job? = null
    private val flowController = ActionFlowController()

    var onStatusChanged: ((Boolean, Int, Int) -> Unit)? = null
    var onJumpToAction: ((Int) -> Unit)? = null

    companion object {
        private const val TAG = "MacroRunner"
    }

    fun start() {
        if (isRunning) {
            Log.w(TAG, "매크로가 이미 실행 중입니다")
            return
        }

        val actions = ActionListManager.getActions()
        if (actions.isEmpty()) {
            Log.w(TAG, "실행할 액션이 없습니다")
            return
        }

        isRunning = true
        onStatusChanged?.invoke(true, 0, actions.size)

        job = CoroutineScope(Dispatchers.Default).launch {
            try {
                if (ActionListManager.isInfiniteLoop) {
                    Log.i(TAG, "무한반복 모드로 시작")
                    executeInfiniteLoop(actions)
                } else {
                    Log.i(TAG, "일반 모드로 시작")
                    executeActions(actions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "매크로 실행 중 오류 발생", e)
            } finally {
                isRunning = false
                withContext(Dispatchers.Main) {
                    onStatusChanged?.invoke(false, 0, 0)
                }
            }
        }
    }

    fun stop() {
        if (!isRunning) return

        Log.i(TAG, "매크로 중지")
        isRunning = false
        job?.cancel()
        ExecutorRegistry.reset()
        flowController.reset()
        onStatusChanged?.invoke(false, 0, 0)
    }

    fun isRunning(): Boolean = isRunning

    private suspend fun executeInfiniteLoop(actions: List<ActionItem>) {
        var loopCount = 0

        while (isRunning) {
            loopCount++
            Log.i(TAG, "=== 반복 #$loopCount 시작 ===")

            val shouldContinue = executeActions(actions)

            if (!shouldContinue || !isRunning) {
                Log.i(TAG, "무한반복 중단됨 (loopCount: $loopCount)")
                break
            }

            delay(500)
        }

        Log.i(TAG, "무한반복 완료. 총 반복 횟수: $loopCount")
    }

    private suspend fun executeActions(actions: List<ActionItem>): Boolean {
        flowController.reset()
        var index = 0
        var executedCount = 0
        val maxExecutions = actions.size * 1000

        while (index in actions.indices && isRunning && executedCount < maxExecutions) {
            executedCount++
            val actionItem = actions[index]

            withContext(Dispatchers.Main) {
                onStatusChanged?.invoke(true, index + 1, actions.size)
            }

            Log.d(TAG, "액션 실행 [${index + 1}/${actions.size}]: ${actionItem.description}")

            if (actionItem.delayMs > 0) {
                Log.d(TAG, "딜레이: ${actionItem.delayMs}ms")
                delay(actionItem.delayMs)
            }

            try {
                val executor = ExecutorRegistry.getExecutor(actionItem.action.type)
                val success = executor.execute(actionItem.action)

                Log.d(TAG, "실행 결과: ${if (success) "성공" else "실패"}")

                if (success) {
                    val jumpTo = actionItem.action.params["onFoundJumpTo"] as? Int
                    if (jumpTo != null && jumpTo >= 0 && jumpTo < actions.size) {
                        Log.i(TAG, "성공 → 점프: ${jumpTo + 1}번 액션으로")
                        index = jumpTo
                        withContext(Dispatchers.Main) {
                            onJumpToAction?.invoke(jumpTo)
                        }
                        continue
                    }
                } else {
                    val jumpTo = actionItem.action.params["onNotFoundJumpTo"] as? Int
                    if (jumpTo != null && jumpTo >= 0 && jumpTo < actions.size) {
                        Log.i(TAG, "실패 → 점프: ${jumpTo + 1}번 액션으로")
                        index = jumpTo
                        withContext(Dispatchers.Main) {
                            onJumpToAction?.invoke(jumpTo)
                        }
                        continue
                    }
                    
                    Log.w(TAG, "액션 실행 실패, 점프 설정 없음 - 다음 액션으로")
                }

                if (actionItem.flowControl.type != FlowType.NONE) {
                    when (actionItem.flowControl.type) {
                        FlowType.GOTO -> {
                            val targetIndex = actionItem.flowControl.targetIndex
                            if (targetIndex != null && targetIndex >= 0 && targetIndex < actions.size) {
                                Log.i(TAG, "GOTO → ${targetIndex + 1}번 액션으로")
                                index = targetIndex
                                withContext(Dispatchers.Main) {
                                    onJumpToAction?.invoke(targetIndex)
                                }
                                continue
                            } else {
                                Log.w(TAG, "GOTO 대상 인덱스 오류: $targetIndex")
                            }
                        }
                        FlowType.BACK -> {
                            val steps = actionItem.flowControl.steps ?: 1
                            val newIndex = maxOf(0, index - steps)
                            Log.i(TAG, "BACK $steps 단계 → ${newIndex + 1}번 액션으로")
                            index = newIndex
                            continue
                        }
                        FlowType.SKIP -> {
                            val steps = actionItem.flowControl.steps ?: 1
                            val newIndex = index + steps + 1
                            Log.i(TAG, "SKIP $steps 단계 → ${newIndex + 1}번 액션으로")
                            index = newIndex
                            continue
                        }
                        FlowType.RESET -> {
                            Log.i(TAG, "RESET → 1번 액션으로")
                            index = 0
                            continue
                        }
                        FlowType.NONE -> {}
                    }
                }

                if (actionItem.nextActionIndex != null) {
                    val nextIndex = actionItem.nextActionIndex
                    if (nextIndex >= 0 && nextIndex < actions.size) {
                        Log.i(TAG, "다음 액션 지정 → ${nextIndex + 1}번 액션으로")
                        index = nextIndex
                        withContext(Dispatchers.Main) {
                            onJumpToAction?.invoke(nextIndex)
                        }
                        continue
                    } else {
                        Log.w(TAG, "다음 액션 인덱스 오류: $nextIndex")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "액션 실행 중 예외 발생", e)
                return false
            }

            index++
        }

        if (executedCount >= maxExecutions) {
            Log.w(TAG, "최대 실행 횟수 도달 - 무한 루프 방지")
            return false
        }

        if (isRunning && !ActionListManager.isInfiniteLoop) {
            Log.i(TAG, "매크로 정상 완료")
        }

        return true
    }
}