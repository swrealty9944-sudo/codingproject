package com.macro.app.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.*
import com.macro.app.R
import com.macro.app.core.Action
import com.macro.app.core.ActionType
import com.macro.app.data.ImageSearchParams
import com.macro.app.repository.ImageRepository

class ImageSearchAddDialog(
    private val context: Context,
    private val template: ImageRepository.TemplateInfo,
    private val onActionCreated: (Action) -> Unit
) {

    private var selectedRegion: ImageSearchParams.SearchRegion? = null

    fun show() {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(
            R.layout.image_search_add_dialog,
            null
        )

        val templateNameText = view.findViewById<TextView>(R.id.template_name_text)
        val thresholdSeek = view.findViewById<SeekBar>(R.id.threshold_seekbar)
        val thresholdText = view.findViewById<TextView>(R.id.threshold_text)
        val regionButton = view.findViewById<Button>(R.id.select_region_button)
        val regionText = view.findViewById<TextView>(R.id.region_text)
        val clearRegionButton = view.findViewById<Button>(R.id.clear_region_button)
        val searchModeSpinner = view.findViewById<Spinner>(R.id.search_mode_spinner)
        val tapOnFoundCheck = view.findViewById<CheckBox>(R.id.tap_on_found_checkbox)

        val onFoundSpinner = view.findViewById<Spinner>(R.id.on_found_action_spinner)
        val onFoundJumpLayout = view.findViewById<LinearLayout>(R.id.on_found_jump_layout)
        val onFoundJumpEdit = view.findViewById<EditText>(R.id.on_found_jump_edit)

        val onNotFoundSpinner = view.findViewById<Spinner>(R.id.on_not_found_action_spinner)
        val onNotFoundJumpLayout = view.findViewById<LinearLayout>(R.id.on_not_found_jump_layout)
        val onNotFoundJumpEdit = view.findViewById<EditText>(R.id.on_not_found_jump_edit)

        val addButton = view.findViewById<Button>(R.id.add_button)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        templateNameText.text = "템플릿: ${template.name}"

        thresholdSeek.progress = 85
        thresholdText.text = "85%"

        thresholdSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                thresholdText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        regionText.text = "전체 화면"

        regionButton.setOnClickListener {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "오버레이 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                context.startActivity(intent)
                return@setOnClickListener
            }

            try {
                dialog.dismiss()
                val picker = RegionPicker(context) { region ->
                    selectedRegion = region
                    regionText.text = "영역: (${region.x}, ${region.y}) ${region.width}x${region.height}"
                    show()
                }
                picker.show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                show()
            }
        }


        clearRegionButton.setOnClickListener {
            selectedRegion = null
            regionText.text = "전체 화면"
        }

        val searchModes = arrayOf("한 번만 확인", "발견할 때까지", "사라질 때까지")
        val searchModeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, searchModes)
        searchModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        searchModeSpinner.adapter = searchModeAdapter
        searchModeSpinner.setSelection(1)

        val actionOptions = arrayOf("계속", "중지", "점프")
        val actionAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, actionOptions)
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        onFoundSpinner.adapter = actionAdapter
        onNotFoundSpinner.adapter = actionAdapter

        onFoundSpinner.setSelection(0)
        onNotFoundSpinner.setSelection(1)

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

        addButton.setOnClickListener {
            val params = mutableMapOf<String, Any>()

            params["templateId"] = template.id
            params["threshold"] = thresholdSeek.progress / 100f

            val searchMode = when (searchModeSpinner.selectedItemPosition) {
                0 -> "CHECK_ONCE"
                1 -> "WAIT_UNTIL_FOUND"
                2 -> "WAIT_UNTIL_NOT_FOUND"
                else -> "WAIT_UNTIL_FOUND"
            }
            params["searchMode"] = searchMode
            params["timeoutMs"] = 30000L
            params["tapOnFound"] = tapOnFoundCheck.isChecked
            params["playSound"] = true
            params["vibrate"] = true
            params["tapOffsetX"] = 0
            params["tapOffsetY"] = 0

            selectedRegion?.let { region ->
                params["searchRegionX"] = region.x
                params["searchRegionY"] = region.y
                params["searchRegionWidth"] = region.width
                params["searchRegionHeight"] = region.height
            }

            val onFoundAction = when (onFoundSpinner.selectedItemPosition) {
                1 -> "STOP"
                2 -> "JUMP"
                else -> "CONTINUE"
            }
            params["onFoundAction"] = onFoundAction
            if (onFoundAction == "JUMP") {
                val jumpTo = onFoundJumpEdit.text.toString().toIntOrNull()
                if (jumpTo != null && jumpTo > 0) {
                    params["onFoundJumpTo"] = jumpTo - 1
                }
            }

            val onNotFoundAction = when (onNotFoundSpinner.selectedItemPosition) {
                1 -> "STOP"
                2 -> "JUMP"
                else -> "CONTINUE"
            }
            params["onNotFoundAction"] = onNotFoundAction
            if (onNotFoundAction == "JUMP") {
                val jumpTo = onNotFoundJumpEdit.text.toString().toIntOrNull()
                if (jumpTo != null && jumpTo > 0) {
                    params["onNotFoundJumpTo"] = jumpTo - 1
                }
            }

            val action = Action(
                id = System.currentTimeMillis().toInt(),
                type = ActionType.IMAGE_SEARCH,
                params = params
            )

            onActionCreated(action)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}