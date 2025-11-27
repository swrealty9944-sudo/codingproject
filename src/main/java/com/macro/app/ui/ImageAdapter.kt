package com.macro.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.macro.app.repository.ImageRepository
import java.text.SimpleDateFormat
import java.util.*

class ImageAdapter(
    private val onDeleteClick: (ImageRepository.TemplateInfo) -> Unit,
    private val onSelectClick: ((ImageRepository.TemplateInfo) -> Unit)? = null
) : ListAdapter<ImageRepository.TemplateInfo, ImageAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private var selectedTemplateId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = parent.context.resources.getIdentifier(
            "image_item_layout",
            "layout",
            parent.context.packageName
        )
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val template = getItem(position)
        holder.bind(template, template.id == selectedTemplateId)
    }

    fun setSelectedTemplate(templateId: String?) {
        selectedTemplateId = templateId
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardView: MaterialCardView = view.findViewById(
            view.context.resources.getIdentifier("part4_item_card", "id", view.context.packageName)
        )
        private val templateImage: ImageView = view.findViewById(
            view.context.resources.getIdentifier("part4_template_image", "id", view.context.packageName)
        )
        private val templateName: TextView = view.findViewById(
            view.context.resources.getIdentifier("part4_template_name", "id", view.context.packageName)
        )
        private val templateSize: TextView = view.findViewById(
            view.context.resources.getIdentifier("part4_template_size", "id", view.context.packageName)
        )
        private val templateDate: TextView = view.findViewById(
            view.context.resources.getIdentifier("part4_template_date", "id", view.context.packageName)
        )
        private val deleteButton: ImageButton = view.findViewById(
            view.context.resources.getIdentifier("part4_delete_button", "id", view.context.packageName)
        )

        fun bind(template: ImageRepository.TemplateInfo, isSelected: Boolean) {
            templateName.text = template.name
            templateSize.text = "${template.width}x${template.height}"
            templateDate.text = dateFormat.format(Date(template.createdAt))

            val bitmap = android.graphics.BitmapFactory.decodeFile(template.filePath)
            if (bitmap != null) {
                templateImage.setImageBitmap(bitmap)
            } else {
                templateImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            if (isSelected) {
                cardView.setCardBackgroundColor(0xFFE3F2FD.toInt())
                cardView.strokeWidth = 4
                cardView.strokeColor = 0xFF2196F3.toInt()
            } else {
                cardView.setCardBackgroundColor(0xFFFFFFFF.toInt())
                cardView.strokeWidth = 0
            }

            cardView.setOnClickListener {
                onSelectClick?.invoke(template)
                selectedTemplateId = if (isSelected) null else template.id
                notifyDataSetChanged()
            }

            deleteButton.setOnClickListener {
                onDeleteClick(template)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ImageRepository.TemplateInfo>() {
        override fun areItemsTheSame(
            oldItem: ImageRepository.TemplateInfo,
            newItem: ImageRepository.TemplateInfo
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ImageRepository.TemplateInfo,
            newItem: ImageRepository.TemplateInfo
        ): Boolean {
            return oldItem == newItem
        }
    }
}