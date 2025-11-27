package com.macro.app.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.macro.app.core.Action
import com.macro.app.core.ActionType
import com.macro.app.core.FlowControl
import com.macro.app.core.FlowType
import com.macro.app.data.ActionItem
import org.json.JSONArray
import org.json.JSONObject

data class ActionList(
    val id: String,
    val name: String,
    val actions: MutableList<ActionItem> = mutableListOf(),
    val isInfiniteLoop: Boolean = false
)

object ActionListManager {
    private const val TAG = "ActionListManager"
    private const val PREFS_NAME = "action_lists"
    private const val KEY_CURRENT_LIST = "current_list_id"
    private const val KEY_LISTS = "lists"

    private val actionLists = mutableMapOf<String, ActionList>()
    private var currentListId: String = "default"
    private val listeners = mutableListOf<OnActionListChangeListener>()

    @Volatile
    private var appContext: Context? = null
    private val prefs: SharedPreferences?
        get() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var isInitialized = false

    fun initialize(ctx: Context) {
        if (isInitialized) return

        appContext = ctx.applicationContext

        if (actionLists.isEmpty()) {
            actionLists["default"] = ActionList(
                id = "default",
                name = "기본 목록",
                actions = mutableListOf()
            )
        }

        loadFromStorage()
        isInitialized = true
        Log.i(TAG, "초기화 완료: ${actionLists.size}개 목록")
    }

    var isInfiniteLoop: Boolean
        get() = getCurrentList().isInfiniteLoop
        set(value) {
            val list = getCurrentList()
            actionLists[currentListId] = list.copy(isInfiniteLoop = value)
            saveToStorage()
            notifyListeners()
        }

    fun createList(name: String): String {
        val id = "list_${System.currentTimeMillis()}"
        actionLists[id] = ActionList(
            id = id,
            name = name,
            actions = mutableListOf()
        )
        saveToStorage()
        notifyListeners()
        Log.i(TAG, "목록 생성: $name")
        return id
    }

    fun deleteList(listId: String) {
        if (listId == "default") return
        if (currentListId == listId) {
            currentListId = "default"
            prefs?.edit()?.putString(KEY_CURRENT_LIST, "default")?.apply()
        }
        actionLists.remove(listId)
        saveToStorage()
        notifyListeners()
        Log.i(TAG, "목록 삭제: $listId")
    }

    fun renameList(listId: String, newName: String) {
        actionLists[listId]?.let { list ->
            actionLists[listId] = list.copy(name = newName)
            saveToStorage()
            notifyListeners()
            Log.i(TAG, "목록 이름 변경: $listId → $newName")
        }
    }

    fun switchToList(listId: String) {
        if (actionLists.containsKey(listId)) {
            currentListId = listId
            prefs?.edit()?.putString(KEY_CURRENT_LIST, listId)?.apply()
            notifyListeners()
            Log.i(TAG, "목록 전환: $listId (${actionLists[listId]?.actions?.size ?: 0}개 액션)")
        }
    }

    fun getCurrentListId(): String = currentListId

    fun getCurrentList(): ActionList {
        return actionLists[currentListId] ?: actionLists["default"]!!
    }

    fun getAllLists(): List<ActionList> {
        return actionLists.values.sortedBy {
            if (it.id == "default") "0" else it.id
        }
    }

    fun addAction(action: ActionItem) {
        val list = getCurrentList()
        list.actions.add(action)
        saveToStorage()
        notifyListeners()
        Log.d(TAG, "액션 추가 to [$currentListId]: ${action.description} (총 ${list.actions.size}개)")
    }

    fun removeAction(position: Int) {
        val actions = getCurrentList().actions
        if (position in actions.indices) {
            val removed = actions.removeAt(position)
            saveToStorage()
            notifyListeners()
            Log.d(TAG, "액션 삭제: $position - ${removed.description}")
        }
    }

    fun updateAction(position: Int, action: ActionItem) {
        val actions = getCurrentList().actions
        if (position in actions.indices) {
            actions[position] = action
            saveToStorage()
            notifyListeners()
            Log.d(TAG, "액션 수정: $position")
        }
    }

    fun moveAction(from: Int, to: Int) {
        val actions = getCurrentList().actions
        if (from in actions.indices && to in actions.indices) {
            val item = actions.removeAt(from)
            actions.add(to, item)
            saveToStorage()
            notifyListeners()
            Log.d(TAG, "액션 이동: $from → $to")
        }
    }

    fun clearAll() {
        getCurrentList().actions.clear()
        saveToStorage()
        notifyListeners()
        Log.i(TAG, "전체 삭제")
    }

    fun getActions(): List<ActionItem> {
        val actions = getCurrentList().actions.toList()
        Log.d(TAG, "getActions() 호출: [$currentListId] ${actions.size}개")
        return actions
    }

    fun getActionCount(): Int {
        return getCurrentList().actions.size
    }

    fun addListener(listener: OnActionListChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: OnActionListChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val currentList = getCurrentList()
        listeners.forEach { it.onActionListChanged(currentList.actions) }
    }

    private fun saveToStorage() {
        try {
            val json = JSONObject()
            val listsArray = JSONArray()

            actionLists.values.forEach { list ->
                val listObj = JSONObject().apply {
                    put("id", list.id)
                    put("name", list.name)
                    put("isInfiniteLoop", list.isInfiniteLoop)

                    val actionsArray = JSONArray()
                    list.actions.forEach { item ->
                        actionsArray.put(actionItemToJson(item))
                    }
                    put("actions", actionsArray)
                }
                listsArray.put(listObj)
            }

            json.put("lists", listsArray)

            prefs?.edit()?.apply {
                putString(KEY_LISTS, json.toString())
                putString(KEY_CURRENT_LIST, currentListId)
                apply()
            }

            Log.d(TAG, "저장 완료: ${actionLists.size}개 목록, 현재 [$currentListId] ${getCurrentList().actions.size}개 액션")
        } catch (e: Exception) {
            Log.e(TAG, "저장 실패", e)
        }
    }

    private fun loadFromStorage() {
        try {
            val jsonStr = prefs?.getString(KEY_LISTS, null)
            if (jsonStr.isNullOrEmpty()) {
                Log.d(TAG, "저장된 데이터 없음 - 기본 목록만 사용")
                return
            }

            val json = JSONObject(jsonStr)
            val listsArray = json.getJSONArray("lists")

            actionLists.clear()

            for (i in 0 until listsArray.length()) {
                val listObj = listsArray.getJSONObject(i)
                val listId = listObj.getString("id")
                val listName = listObj.getString("name")
                val isInfiniteLoop = listObj.optBoolean("isInfiniteLoop", false)

                val actions = mutableListOf<ActionItem>()
                val actionsArray = listObj.getJSONArray("actions")

                for (j in 0 until actionsArray.length()) {
                    try {
                        val actionObj = actionsArray.getJSONObject(j)
                        val actionItem = jsonToActionItem(actionObj)
                        if (actionItem != null) {
                            actions.add(actionItem)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "액션 로드 실패: $j", e)
                    }
                }

                actionLists[listId] = ActionList(
                    id = listId,
                    name = listName,
                    actions = actions,
                    isInfiniteLoop = isInfiniteLoop
                )
                Log.d(TAG, "목록 로드: [$listId] $listName (${actions.size}개 액션)")
            }

            currentListId = prefs?.getString(KEY_CURRENT_LIST, "default") ?: "default"

            if (!actionLists.containsKey(currentListId)) {
                Log.w(TAG, "현재 목록 [$currentListId] 없음 - default로 전환")
                currentListId = "default"
            }

            if (!actionLists.containsKey("default")) {
                actionLists["default"] = ActionList(
                    id = "default",
                    name = "기본 목록",
                    actions = mutableListOf()
                )
            }

            Log.i(TAG, "로드 완료: ${actionLists.size}개 목록, 현재: [$currentListId] ${getCurrentList().actions.size}개 액션")
        } catch (e: Exception) {
            Log.e(TAG, "로드 실패", e)
            actionLists.clear()
            actionLists["default"] = ActionList(
                id = "default",
                name = "기본 목록",
                actions = mutableListOf()
            )
            currentListId = "default"
        }
    }

    private fun actionItemToJson(item: ActionItem): JSONObject {
        return JSONObject().apply {
            put("description", item.description)
            put("delayMs", item.delayMs)

            val actionObj = JSONObject().apply {
                put("id", item.action.id)
                put("type", item.action.type.name)

                val paramsObj = JSONObject()
                item.action.params.forEach { (key, value) ->
                    paramsObj.put(key, value)
                }
                put("params", paramsObj)
            }
            put("action", actionObj)

            if (item.flowControl.type != FlowType.NONE) {
                val flowObj = JSONObject().apply {
                    put("type", item.flowControl.type.name)
                    item.flowControl.targetIndex?.let { put("targetIndex", it) }
                    item.flowControl.steps?.let { put("steps", it) }
                }
                put("flowControl", flowObj)
            }

            item.nextActionIndex?.let { put("nextActionIndex", it) }
        }
    }

    private fun jsonToActionItem(json: JSONObject): ActionItem? {
        return try {
            val description = json.getString("description")
            val delayMs = json.getLong("delayMs")

            val actionObj = json.getJSONObject("action")
            val actionId = actionObj.getInt("id")
            val actionType = ActionType.valueOf(actionObj.getString("type"))

            val paramsObj = actionObj.getJSONObject("params")
            val params = mutableMapOf<String, Any>()
            paramsObj.keys().forEach { key ->
                params[key] = paramsObj.get(key)
            }

            val action = Action(
                id = actionId,
                type = actionType,
                params = params
            )

            val flowControl = if (json.has("flowControl")) {
                val flowObj = json.getJSONObject("flowControl")
                FlowControl(
                    type = FlowType.valueOf(flowObj.getString("type")),
                    targetIndex = if (flowObj.has("targetIndex")) flowObj.getInt("targetIndex") else null,
                    steps = if (flowObj.has("steps")) flowObj.getInt("steps") else 1
                )
            } else {
                FlowControl(FlowType.NONE)
            }

            val nextActionIndex = if (json.has("nextActionIndex")) {
                json.getInt("nextActionIndex")
            } else {
                null
            }

            ActionItem(
                action = action,
                delayMs = delayMs,
                flowControl = flowControl,
                description = description,
                nextActionIndex = nextActionIndex
            )
        } catch (e: Exception) {
            Log.e(TAG, "ActionItem 변환 실패", e)
            null
        }
    }

    interface OnActionListChangeListener {
        fun onActionListChanged(actions: List<ActionItem>)
    }
}