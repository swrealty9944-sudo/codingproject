package com.macro.app.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast

class ImageRegionSelector(
    context: Context,
    private val templateBitmap: Bitmap,
    private val onRegionSelected: (left: Int, top: Int, right: Int, bottom: Int) -> Unit
) : Dialog(context) {

    private lateinit var imageView: ImageView
    private lateinit var overlayView: RegionOverlayView
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val container = FrameLayout(context)
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        imageView = ImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        imageView.adjustViewBounds = true
        imageView.setImageBitmap(templateBitmap)

        overlayView = RegionOverlayView(context)
        overlayView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val buttonContainer = android.widget.LinearLayout(context)
        buttonContainer.orientation = android.widget.LinearLayout.HORIZONTAL
        buttonContainer.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.BOTTOM
            setMargins(16, 16, 16, 16)
        }

        resetButton = Button(context)
        resetButton.text = "초기화"
        resetButton.layoutParams = android.widget.LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            setMargins(0, 0, 8, 0)
        }
        resetButton.setOnClickListener {
            overlayView.reset()
        }

        cancelButton = Button(context)
        cancelButton.text = "취소"
        cancelButton.layoutParams = android.widget.LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            setMargins(8, 0, 8, 0)
        }
        cancelButton.setOnClickListener {
            dismiss()
        }

        confirmButton = Button(context)
        confirmButton.text = "확인"
        confirmButton.layoutParams = android.widget.LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            setMargins(8, 0, 0, 0)
        }
        confirmButton.setOnClickListener {
            val region = overlayView.getSelectedRegion()
            if (region != null) {
                val scaleX = templateBitmap.width.toFloat() / imageView.width
                val scaleY = templateBitmap.height.toFloat() / imageView.height

                val left = (region.left * scaleX).toInt()
                val top = (region.top * scaleY).toInt()
                val right = (region.right * scaleX).toInt()
                val bottom = (region.bottom * scaleY).toInt()

                onRegionSelected(left, top, right, bottom)
                dismiss()
            } else {
                Toast.makeText(context, "영역을 선택하세요", Toast.LENGTH_SHORT).show()
            }
        }

        buttonContainer.addView(resetButton)
        buttonContainer.addView(cancelButton)
        buttonContainer.addView(confirmButton)

        container.addView(imageView)
        container.addView(overlayView)
        container.addView(buttonContainer)

        setContentView(container)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private class RegionOverlayView(context: Context) : View(context) {
        private val paint = Paint().apply {
            color = Color.parseColor("#4CAF50")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        private val fillPaint = Paint().apply {
            color = Color.parseColor("#404CAF50")
            style = Paint.Style.FILL
        }

        private var startX = 0f
        private var startY = 0f
        private var endX = 0f
        private var endY = 0f
        private var isDrawing = false

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    endX = event.x
                    endY = event.y
                    isDrawing = true
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDrawing) {
                        endX = event.x
                        endY = event.y
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    endX = event.x
                    endY = event.y
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            if (isDrawing || (startX != endX && startY != endY)) {
                val left = minOf(startX, endX)
                val top = minOf(startY, endY)
                val right = maxOf(startX, endX)
                val bottom = maxOf(startY, endY)

                canvas.drawRect(left, top, right, bottom, fillPaint)
                canvas.drawRect(left, top, right, bottom, paint)

                val cornerSize = 20f
                canvas.drawLine(left, top, left + cornerSize, top, paint)
                canvas.drawLine(left, top, left, top + cornerSize, paint)

                canvas.drawLine(right, top, right - cornerSize, top, paint)
                canvas.drawLine(right, top, right, top + cornerSize, paint)

                canvas.drawLine(left, bottom, left + cornerSize, bottom, paint)
                canvas.drawLine(left, bottom, left, bottom - cornerSize, paint)

                canvas.drawLine(right, bottom, right - cornerSize, bottom, paint)
                canvas.drawLine(right, bottom, right, bottom - cornerSize, paint)
            }
        }

        fun getSelectedRegion(): RectF? {
            return if (startX != endX && startY != endY) {
                RectF(
                    minOf(startX, endX),
                    minOf(startY, endY),
                    maxOf(startX, endX),
                    maxOf(startY, endY)
                )
            } else {
                null
            }
        }

        fun reset() {
            startX = 0f
            startY = 0f
            endX = 0f
            endY = 0f
            isDrawing = false
            invalidate()
        }
    }
}