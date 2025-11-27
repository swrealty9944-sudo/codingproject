package com.macro.app.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.*
import com.macro.app.R
import com.macro.app.core.Action
import com.macro.app.core.ActionType
import com.macro.app.data.ActionItem
import com.macro.app.data.ImageSearchParams
import com.macro.app.repository.ImageRepository

class ImageSearchEditDialog(
    private val context: Context,
    private val actionItem: ActionItem,
    private val onActionUpdated: (ActionItem) -> Unit
) {

    private val repository = ImageRepository(context)
    private var selectedTemplateId: String? = null
    private var selectedRegion: ImageSearchParams.SearchRegion? = null

    fun show() {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(
            R.layout.image_search_edit_dialog,
            null
        )

        val templateSpinner = view.findViewById<Spinner>(R.id.template_spinner)
        val changeTemplateButton = view.findViewById<Button>(R.id.change_template_button)
        val thresholdSeek = view.findViewById<SeekBar>(R.id.threshold_seekbar)
        val thresholdText = view.findViewById<TextView>(R.id.threshold_text)
        val searchModeSpinner = view.findViewById<Spinner>(R.id.search_mode_spinner)
        val regionButton = view.findViewById<Button>(R.id.select_region_button)
        val regionText = view.findViewById<TextView>(R.id.region_text)
        val clearRegionButton = view.findViewById<Button>(R.id.clear_region_button)

        val onFoundSpinner = view.findViewById<Spinner>(R.id.on_found_action_spinner)
        val onFoundJumpLayout = view.findViewById<LinearLayout>(R.id.on_found_jump_layout)
        val onFoundJumpEdit = view.findViewById<EditText>(R.id.on_found_jump_edit)

        val onNotFoundSpinner = view.findViewById<Spinner>(R.id.on_not_found_action_spinner)
        val onNotFoundJumpLayout = view.findViewById<LinearLayout>(R.id.on_not_found_jump_layout)
        val onNotFoundJumpEdit = view.findViewById<EditText>(R.id.on_not_found_jump_edit)

        val tapOnFoundCheck = view.findViewById<CheckBox>(R.id.tap_on_found_checkbox)
        val saveButton = view.findViewById<Button>(R.id.save_button)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        val templates = repository.listTemplates()
        val templateNames = templates.map { it.name }
        val templateIds = templates.map { it.id }

        val currentTemplateId = actionItem.action.params["templateId"] as? String
        selectedTemplateId = currentTemplateId

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, templateNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        templateSpinner.adapter = adapter

        val currentIndex = templateIds.indexOf(currentTemplateId)
        if (currentIndex >= 0) {
            templateSpinner.setSelection(currentIndex)
        }

        templateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedTemplateId = templateIds[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        changeTemplateButton.setOnClickListener {
        }

        val currentThreshold = ((actionItem.action.params["threshold"] as? Number)?.toFloat() ?: 0.85f)
        thresholdSeek.progress = (currentThreshold * 100).toInt()
        thresholdText.text = "${(currentThreshold * 100).toInt()}%"

        thresholdSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                thresholdText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val searchModes = arrayOf("한 번만 확인", "발견할 때까지", "사라질 때까지")
        val searchModeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, searchModes)
        searchModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        searchModeSpinner.adapter = searchModeAdapter

        val currentSearchMode = actionItem.action.params["searchMode"] as? String ?: "WAIT_UNTIL_FOUND"
        val searchModeIndex = when (currentSearchMode) {
            "CHECK_ONCE" -> 0
            "WAIT_UNTIL_FOUND" -> 1
            "WAIT_UNTIL_NOT_FOUND" -> 2
            else -> 1
        }
        searchModeSpinner.setSelection(searchModeIndex)

        if (actionItem.action.params.containsKey("searchRegionX")) {
            selectedRegion = ImageSearchParams.SearchRegion(
                x = (actionItem.action.params["searchRegionX"] as? Number)?.toInt() ?: 0,
                y = (actionItem.action.params["searchRegionY"] as? Number)?.toInt() ?: 0,
                width = (actionItem.action.params["searchRegionWidth"] as? Number)?.toInt() ?: 1080,
                height = (actionItem.action.params["searchRegionHeight"] as? Number)?.toInt() ?: 1920
            )
            regionText.text = "영역: (${selectedRegion?.x}, ${selectedRegion?.y}) ${selectedRegion?.width}x${selectedRegion?.height}"
        } else {
            regionText.text = "전체 화면"
        }

        regionButton.setOnClickListener {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "오버레이 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                context.startActivity(intent)
                return@setOnClickListener
            }

            dialog.dismiss()
            val picker = RegionPicker(context) { region ->
                selectedRegion = region
                regionText.text = "영역: (${region.x}, ${region.y}) ${region.width}x${region.height}"
                show()
            }
            picker.show()
        }

        clearRegionButton.setOnClickListener {
            selectedRegion = null
            regionText.text = "전체 화면"
        }

        val actionOptions = arrayOf("계속", "중지", "점프")
        val actionAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, actionOptions)
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        onFoundSpinner.adapter = actionAdapter
        onNotFoundSpinner.adapter = actionAdapter

        val currentOnFoundAction = actionItem.action.params["onFoundAction"] as? String ?: "CONTINUE"
        onFoundSpinner.setSelection(when (currentOnFoundAction) {
            "STOP" -> 1
            "JUMP" -> 2
            else -> 0
        })

        val currentOnNotFoundAction = actionItem.action.params["onNotFoundAction"] as? String ?: "CONTINUE"
        onNotFoundSpinner.setSelection(when (currentOnNotFoundAction) {
            "STOP" -> 1
            "JUMP" -> 2
            else -> 0
        })

        (actionItem.action.params["onFoundJumpTo"] as? Number)?.toInt()?.let {
            onFoundJumpEdit.setText((it + 1).toString())
        }

        (actionItem.action.params["onNotFoundJumpTo"] as? Number)?.toInt()?.let {
            onNotFoundJumpEdit.setText((it + 1).toString())
        }

        onFoundSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                onFoundJumpLayout.visibility = if (position == 2) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        onNotFoundSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                onNotFoundJumpLayout.visibility = if (position == 2) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        tapOnFoundCheck.isChecked = actionItem.action.params["tapOnFound"] as? Boolean ?: false

        saveButton.setOnClickListener {
            val newParams = mutableMapOf<String, Any>()

            selectedTemplateId?.let { newParams["templateId"] = it }
            newParams["threshold"] = thresholdSeek.progress / 100f

            val searchMode = when (searchModeSpinner.selectedItemPosition) {
                0 -> "CHECK_ONCE"
                1 -> "WAIT_UNTIL_FOUND"
                2 -> "WAIT_UNTIL_NOT_FOUND"
                else -> "WAIT_UNTIL_FOUND"
            }
            newParams["searchMode"] = searchMode

            newParams["timeoutMs"] = actionItem.action.params["timeoutMs"] ?: 30000L
            newParams["tapOnFound"] = tapOnFoundCheck.isChecked
            newParams["playSound"] = actionItem.action.params["playSound"] ?: true
            newParams["vibrate"] = actionItem.action.params["vibrate"] ?: true
            newParams["tapOffsetX"] = actionItem.action.params["tapOffsetX"] ?: 0
            newParams["tapOffsetY"] = actionItem.action.params["tapOffsetY"] ?: 0

            selectedRegion?.let { region ->
                newParams["searchRegionX"] = region.x
                newParams["searchRegionY"] = region.y
                newParams["searchRegionWidth"] = region.width
                newParams["searchRegionHeight"] = region.height
            }

            val onFoundAction = when (onFoundSpinner.selectedItemPosition) {
                1 -> "STOP"
                2 -> "JUMP"
                else -> "CONTINUE"
            }
            newParams["onFoundAction"] = onFoundAction
            if (onFoundAction == "JUMP") {
                val jumpTo = onFoundJumpEdit.text.toString().toIntOrNull()
                if (jumpTo != null && jumpTo > 0) {
                    newParams["onFoundJumpTo"] = jumpTo - 1
                }
            }

            val onNotFoundAction = when (onNotFoundSpinner.selectedItemPosition) {
                1 -> "STOP"
                2 -> "JUMP"
                else -> "CONTINUE"
            }
            newParams["onNotFoundAction"] = onNotFoundAction
            if (onNotFoundAction == "JUMP") {
                val jumpTo = onNotFoundJumpEdit.text.toString().toIntOrNull()
                if (jumpTo != null && jumpTo > 0) {
                    newParams["onNotFoundJumpTo"] = jumpTo - 1
                }
            }

            val newAction = Action(
                id = actionItem.action.id,
                type = ActionType.IMAGE_SEARCH,
                params = newParams
            )

            val newItem = actionItem.copy(action = newAction)
            onActionUpdated(newItem)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }
}