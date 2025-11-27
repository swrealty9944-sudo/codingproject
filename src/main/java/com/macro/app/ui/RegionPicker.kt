package com.macro.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.macro.app.data.ImageSearchParams

class RegionPicker(
    private val context: Context,
    private val onRegionSelected: (ImageSearchParams.SearchRegion) -> Unit
) {
    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private var infoText: TextView? = null
    private var regionView: RegionSelectionView? = null

    fun show() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        overlayView = FrameLayout(context).apply {
            setBackgroundColor(0x80000000.toInt())
        }

        regionView = RegionSelectionView(context)
        overlayView?.addView(regionView)

        val controlLayout = LayoutInflater.from(context).inflate(
            context.resources.getIdentifier("region_picker_controls", "layout", context.packageName),
            null
        ) as? FrameLayout ?: FrameLayout(context).apply {
            setPadding(32, 32, 32, 32)
        }

        infoText = TextView(context).apply {
            text = "영역을 드래그하여 선택하세요"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xCC000000.toInt())
        }

        val buttonLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
        }

        val confirmButton = Button(context).apply {
            text = "확인"
            setOnClickListener {
                regionView?.getSelectedRegion()?.let { region ->
                    onRegionSelected(region)
                }
                dismiss()
            }
        }

        val cancelButton = Button(context).apply {
            text = "취소"
            setOnClickListener {
                dismiss()
            }
        }

        val fullScreenButton = Button(context).apply {
            text = "전체화면"
            setOnClickListener {
                onRegionSelected(ImageSearchParams.SearchRegion(0, 0, 1080, 2400))
                dismiss()
            }
        }

        buttonLayout.addView(fullScreenButton)
        buttonLayout.addView(confirmButton)
        buttonLayout.addView(cancelButton)

        controlLayout.addView(infoText, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        })

        controlLayout.addView(buttonLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        })

        overlayView?.addView(controlLayout)

        regionView?.onRegionChanged = { region ->
            infoText?.text = "영역: (${region.x}, ${region.y}) ${region.width}x${region.height}"
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager?.addView(overlayView, params)
    }

    fun dismiss() {
        overlayView?.let { view ->
            windowManager?.removeView(view)
        }
        overlayView = null
        regionView = null
        windowManager = null
    }

    private class RegionSelectionView(context: Context) : View(context) {
        private val paint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        private val fillPaint = Paint().apply {
            color = 0x3000FF00
            style = Paint.Style.FILL
        }

        private var startX = 0f
        private var startY = 0f
        private var currentX = 0f
        private var currentY = 0f
        private var isDrawing = false

        var onRegionChanged: ((ImageSearchParams.SearchRegion) -> Unit)? = null

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    currentX = event.x
                    currentY = event.y
                    isDrawing = true
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentX = event.x
                    currentY = event.y
                    invalidate()
                    getSelectedRegion()?.let { region ->
                        onRegionChanged?.invoke(region)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    currentX = event.x
                    currentY = event.y
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            if (isDrawing) {
                val left = minOf(startX, currentX)
                val top = minOf(startY, currentY)
                val right = maxOf(startX, currentX)
                val bottom = maxOf(startY, currentY)

                canvas.drawRect(left, top, right, bottom, fillPaint)
                canvas.drawRect(left, top, right, bottom, paint)

                paint.textSize = 40f
                paint.style = Paint.Style.FILL
                canvas.drawText("${(right - left).toInt()} x ${(bottom - top).toInt()}",
                    left, top - 10, paint)
                paint.style = Paint.Style.STROKE
            }
        }

        fun getSelectedRegion(): ImageSearchParams.SearchRegion? {
            if (!isDrawing) return null

            val x = minOf(startX, currentX).toInt()
            val y = minOf(startY, currentY).toInt()
            val width = (maxOf(startX, currentX) - minOf(startX, currentX)).toInt()
            val height = (maxOf(startY, currentY) - minOf(startY, currentY)).toInt()

            if (width < 10 || height < 10) return null

            return ImageSearchParams.SearchRegion(x, y, width, height)
        }
    }
}