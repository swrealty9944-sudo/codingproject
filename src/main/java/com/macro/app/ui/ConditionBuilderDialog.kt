package com.macro.app.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.macro.app.R
import com.macro.app.core.ConditionType
import com.macro.app.data.Condition
import com.macro.app.manager.ConditionManager

class ConditionBuilderDialog : DialogFragment() {
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.condition_builder_dialog, null)
        
        val typeSpinner = view.findViewById<Spinner>(R.id.part5_type_spinner)
        val value1Edit = view.findViewById<EditText>(R.id.part5_value1)
        val value2Edit = view.findViewById<EditText>(R.id.part5_value2)
        val descriptionEdit = view.findViewById<EditText>(R.id.part5_description)
        
        val types = ConditionType.values().map { it.name }
        typeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            types
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        return AlertDialog.Builder(requireContext())
            .setTitle("조건 생성")
            .setView(view)
            .setPositiveButton("추가") { _, _ ->
                val type = ConditionType.values()[typeSpinner.selectedItemPosition]
                val params = buildParams(type, value1Edit.text.toString(), value2Edit.text.toString())
                val description = descriptionEdit.text.toString()
                
                val condition = Condition(
                    id = System.currentTimeMillis().toInt(),
                    type = type,
                    params = params,
                    description = description
                )
                
                ConditionManager.addCondition(condition)
            }
            .setNegativeButton("취소", null)
            .create()
    }
    
    private fun buildParams(type: ConditionType, value1: String, value2: String): Map<String, Any> {
        return when (type) {
            ConditionType.TIME_ELAPSED -> mapOf("elapsedMs" to (value1.toLongOrNull() ?: 0L))
            ConditionType.TIME_UNTIL -> mapOf(
                "hour" to (value1.toIntOrNull() ?: 0),
                "minute" to (value2.toIntOrNull() ?: 0)
            )
            ConditionType.IMAGE_FOUND, ConditionType.IMAGE_NOT_FOUND -> mapOf(
                "templateId" to value1,
                "threshold" to (value2.toFloatOrNull() ?: 0.8f),
                "checkIntervalMs" to 500L
            )
            ConditionType.COUNT_REACHED -> mapOf("count" to (value1.toIntOrNull() ?: 1))
            else -> emptyMap()
        }
    }
}