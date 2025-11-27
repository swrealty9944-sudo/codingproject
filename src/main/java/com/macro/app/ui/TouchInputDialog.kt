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

class TouchInputDialog(
    context: Context,
    private val onActionCreated: (Action) -> Unit,
    private val existingAction: Action? = null
) : Dialog(context) {

    private lateinit var xInput: EditText
    private lateinit var yInput: EditText
    private lateinit var pickButton: Button
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val layoutId = context.resources.getIdentifier(
            "coordinate_input_dialog",
            "layout",
            context.packageName
        )
        setContentView(layoutId)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        xInput = findViewById(
            context.resources.getIdentifier("part3_x_input", "id", context.packageName)
        )
        yInput = findViewById(
            context.resources.getIdentifier("part3_y_input", "id", context.packageName)
        )
        pickButton = findViewById(
            context.resources.getIdentifier("part3_pick_button", "id", context.packageName)
        )
        confirmButton = findViewById(
            context.resources.getIdentifier("part3_dialog_confirm", "id", context.packageName)
        )
        cancelButton = findViewById(
            context.resources.getIdentifier("part3_dialog_cancel", "id", context.packageName)
        )

        existingAction?.let { action ->
            val x = action.params["x"] as? Int
            val y = action.params["y"] as? Int
            if (x != null) xInput.setText(x.toString())
            if (y != null) yInput.setText(y.toString())
        }

        pickButton.setOnClickListener {
            dismiss()
            showCoordinatePicker()
        }

        confirmButton.setOnClickListener {
            val x = xInput.text.toString().toIntOrNull() ?: 0
            val y = yInput.text.toString().toIntOrNull() ?: 0

            val action = Action(
                id = existingAction?.id ?: System.currentTimeMillis().toInt(),
                type = ActionType.TOUCH,
                params = mapOf(
                    "x" to x,
                    "y" to y
                )
            )

            onActionCreated(action)
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun showCoordinatePicker() {
        val picker = CoordinatePicker(context) { x, y ->
            xInput.setText(x.toString())
            yInput.setText(y.toString())
            show()
        }
        picker.show()
    }
}