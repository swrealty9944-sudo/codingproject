package com.macro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.macro.app.R
import com.macro.app.core.ConditionType
import com.macro.app.data.Condition
import com.macro.app.manager.ConditionManager

class ConditionFragment : Fragment() {
    private lateinit var conditionListView: ListView
    private lateinit var addConditionButton: FloatingActionButton
    private lateinit var adapter: ConditionAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.condition_fragment, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        conditionListView = view.findViewById(R.id.part5_condition_list)
        addConditionButton = view.findViewById(R.id.part5_add_condition)
        
        adapter = ConditionAdapter(requireContext(), ConditionManager.getConditions())
        conditionListView.adapter = adapter
        
        addConditionButton.setOnClickListener {
            showConditionBuilder()
        }
    }
    
    private fun showConditionBuilder() {
        val dialog = ConditionBuilderDialog()
        dialog.show(childFragmentManager, "ConditionBuilder")
    }
}