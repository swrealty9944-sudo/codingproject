package com.macro.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.RelativeLayout
import android.widget.Toast
import android.app.AlertDialog

class CoordinatePicker(
    private val context: Context,
    private val onCoordinateSelected: (Int, Int) -> Unit
) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var coordinateText: TextView? = null
    private var targetMarker: View? = null

    fun show() {
        if (!checkOverlayPermission()) {
            showPermissionDialog()
            return
        }

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val inflater = LayoutInflater.from(context)
            overlayView = inflater.inflate(
                context.resources.getIdentifier(
                    "coordinate_picker_overlay",
                    "layout",
                    context.packageName
                ),
                null
            )

            coordinateText = overlayView?.findViewById(
                context.resources.getIdentifier(
                    "part3_coordinate_text",
                    "id",
                    context.packageName
                )
            )

            val confirmButton = overlayView?.findViewById<Button>(
                context.resources.getIdentifier(
                    "part3_confirm_button",
                    "id",
                    context.packageName
                )
            )

            val cancelButton = overlayView?.findViewById<Button>(
                context.resources.getIdentifier(
                    "part3_cancel_button",
                    "id",
                    context.packageName
                )
            )

            targetMarker = View(context).apply {
                layoutParams = RelativeLayout.LayoutParams(40, 40).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
                setBackgroundColor(0xFFFF0000.toInt())
            }

            (overlayView as? RelativeLayout)?.addView(targetMarker)

            var selectedX = 0
            var selectedY = 0

            coordinateText?.text = "화면을 터치하여 좌표를 선택하세요"

            overlayView?.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    selectedX = event.rawX.toInt()
                    selectedY = event.rawY.toInt()
                    coordinateText?.text = "X: $selectedX, Y: $selectedY"

                    targetMarker?.let { marker ->
                        val location = IntArray(2)
                        view.getLocationOnScreen(location)
                        val viewX = location[0]
                        val viewY = location[1]

                        val relativeX = selectedX - viewX
                        val relativeY = selectedY - viewY

                        val params = marker.layoutParams as RelativeLayout.LayoutParams
                        params.removeRule(RelativeLayout.CENTER_IN_PARENT)
                        params.leftMargin = relativeX - 20
                        params.topMargin = relativeY - 20
                        marker.layoutParams = params
                    }
                }
                true
            }

            confirmButton?.setOnClickListener {
                if (selectedX > 0 || selectedY > 0) {
                    onCoordinateSelected(selectedX, selectedY)
                    dismiss()
                } else {
                    Toast.makeText(context, "좌표를 먼저 선택하세요", Toast.LENGTH_SHORT).show()
                }
            }

            cancelButton?.setOnClickListener {
                dismiss()
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            Toast.makeText(context, "오버레이 표시 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun dismiss() {
        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            overlayView = null
            targetMarker = null
            windowManager = null
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(context)
            .setTitle("권한 필요")
            .setMessage("다른 앱 위에 표시 권한이 필요합니다.\n설정으로 이동하시겠습니까?")
            .setPositiveButton("설정") { _, _ ->
                openOverlaySettings()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }
}