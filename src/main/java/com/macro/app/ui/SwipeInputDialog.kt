package com.macro.app.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import com.macro.app.core.Action
import com.macro.app.core.ActionType

class SwipeInputDialog(
    context: Context,
    private val onActionCreated: (Action) -> Unit,
    private val existingAction: Action? = null
) : Dialog(context) {

    private lateinit var startXInput: EditText
    private lateinit var startYInput: EditText
    private lateinit var endXInput: EditText
    private lateinit var endYInput: EditText
    private lateinit var durationInput: EditText
    private lateinit var pickStartButton: Button
    private lateinit var pickEndButton: Button
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val layoutId = context.resources.getIdentifier(
            "swipe_input_dialog",
            "layout",
            context.packageName
        )
        setContentView(layoutId)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        startXInput = findViewById(
            context.resources.getIdentifier("part3_start_x_input", "id", context.packageName)
        )
        startYInput = findViewById(
            context.resources.getIdentifier("part3_start_y_input", "id", context.packageName)
        )
        endXInput = findViewById(
            context.resources.getIdentifier("part3_end_x_input", "id", context.packageName)
        )
        endYInput = findViewById(
            context.resources.getIdentifier("part3_end_y_input", "id", context.packageName)
        )
        durationInput = findViewById(
            context.resources.getIdentifier("part3_swipe_duration_input", "id", context.packageName)
        )
        pickStartButton = findViewById(
            context.resources.getIdentifier("part3_pick_start_button", "id", context.packageName)
        )
        pickEndButton = findViewById(
            context.resources.getIdentifier("part3_pick_end_button", "id", context.packageName)
        )
        confirmButton = findViewById(
            context.resources.getIdentifier("part3_swipe_confirm", "id", context.packageName)
        )
        cancelButton = findViewById(
            context.resources.getIdentifier("part3_swipe_cancel", "id", context.packageName)
        )

        existingAction?.let { action ->
            val startX = action.params["startX"] as? Int
            val startY = action.params["startY"] as? Int
            val endX = action.params["endX"] as? Int
            val endY = action.params["endY"] as? Int
            val duration = action.params["duration"] as? Long

            if (startX != null) startXInput.setText(startX.toString())
            if (startY != null) startYInput.setText(startY.toString())
            if (endX != null) endXInput.setText(endX.toString())
            if (endY != null) endYInput.setText(endY.toString())
            if (duration != null) durationInput.setText(duration.toString())
        } ?: run {
            durationInput.setText("500")
        }

        pickStartButton.setOnClickListener {
            dismiss()
            showCoordinatePicker(true)
        }

        pickEndButton.setOnClickListener {
            dismiss()
            showCoordinatePicker(false)
        }

        confirmButton.setOnClickListener {
            val startX = startXInput.text.toString().toIntOrNull() ?: 0
            val startY = startYInput.text.toString().toIntOrNull() ?: 0
            val endX = endXInput.text.toString().toIntOrNull() ?: 0
            val endY = endYInput.text.toString().toIntOrNull() ?: 0
            val duration = durationInput.text.toString().toLongOrNull() ?: 500L

            val action = Action(
                id = existingAction?.id ?: System.currentTimeMillis().toInt(),
                type = ActionType.SWIPE,
                params = mapOf(
                    "startX" to startX,
                    "startY" to startY,
                    "endX" to endX,
                    "endY" to endY,
                    "duration" to duration
                )
            )

            onActionCreated(action)
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun showCoordinatePicker(isStart: Boolean) {
        val picker = CoordinatePicker(context) { x, y ->
            if (isStart) {
                startXInput.setText(x.toString())
                startYInput.setText(y.toString())
            } else {
                endXInput.setText(x.toString())
                endYInput.setText(y.toString())
            }
            show()
        }
        picker.show()
    }
}