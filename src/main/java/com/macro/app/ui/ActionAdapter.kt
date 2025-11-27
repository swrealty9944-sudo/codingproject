package com.macro.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.macro.app.R
import com.macro.app.data.ActionItem
import com.macro.app.core.FlowType

class ActionAdapter(
    private val items: MutableList<ActionItem>,
    private val onItemClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ActionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val indexText: TextView = view.findViewById(R.id.part2_item_index)
        val titleText: TextView = view.findViewById(R.id.part2_item_title)
        val descriptionText: TextView = view.findViewById(R.id.part2_item_description)
        val delayText: TextView = view.findViewById(R.id.part2_item_delay)
        val flowText: TextView = view.findViewById(R.id.part2_item_flow)
        val deleteButton: ImageButton = view.findViewById(R.id.part2_item_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.action_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.indexText.text = (position + 1).toString()
        holder.titleText.text = item.action.type.name
        holder.descriptionText.text = item.description.ifEmpty {
            item.action.params.entries.joinToString(", ") { "${it.key}=${it.value}" }
        }
        holder.delayText.text = if (item.delayMs > 0) "${item.delayMs}ms" else ""

        val flowTexts = mutableListOf<String>()

        if (item.flowControl.type != FlowType.NONE) {
            val targetText = item.flowControl.targetIndex?.let { target ->
                if (target >= 0) " → ${target + 1}" else ""
            } ?: ""
            flowTexts.add(item.flowControl.type.name + targetText)
        }

        if (item.nextActionIndex != null && item.nextActionIndex >= 0) {
            flowTexts.add("다음→${item.nextActionIndex + 1}")
        }

        holder.flowText.text = flowTexts.joinToString(" | ")

        holder.itemView.setOnClickListener { onItemClick(position) }
        holder.deleteButton.setOnClickListener { onDeleteClick(position) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ActionItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }
}