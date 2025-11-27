package com.macro.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.macro.app.R
import com.macro.app.data.Condition
import com.macro.app.manager.ConditionManager

class ConditionAdapter(
    context: Context,
    private val conditions: MutableList<Condition>
) : ArrayAdapter<Condition>(context, 0, conditions) {
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.condition_item_layout, parent, false)
        
        val condition = getItem(position) ?: return view
        
        val titleText = view.findViewById<TextView>(R.id.part5_condition_title)
        val descriptionText = view.findViewById<TextView>(R.id.part5_condition_description)
        val deleteButton = view.findViewById<ImageButton>(R.id.part5_condition_delete)
        
        titleText.text = condition.type.name
        descriptionText.text = condition.description
        
        deleteButton.setOnClickListener {
            ConditionManager.removeCondition(condition.id)
            remove(condition)
            notifyDataSetChanged()
        }
        
        return view
    }
}