package com.macro.app.manager

import com.macro.app.data.Condition

object ConditionManager {
    private val conditions = mutableListOf<Condition>()
    
    fun addCondition(condition: Condition) {
        conditions.add(condition)
    }
    
    fun removeCondition(id: Int) {
        conditions.removeAll { it.id == id }
    }
    
    fun getConditions(): MutableList<Condition> = conditions
    
    fun getCondition(id: Int): Condition? = conditions.find { it.id == id }
    
    fun clear() {
        conditions.clear()
    }
}