package com.macro.app.executor

import android.content.Context
import android.media.RingtoneManager
import android.os.Vibrator
import android.util.Log
import com.macro.app.capture.ScreenCapture
import com.macro.app.core.Action
import com.macro.app.core.ActionExecutor
import com.macro.app.data.ImageSearchParams
import com.macro.app.data.MatchResult
import com.macro.app.matcher.TemplateMatcher
import com.macro.app.repository.ImageRepository
import com.macro.app.service.MacroService

class ImageSearchExecutor(
    private val context: Context,
    private val service: MacroService,
    private val screenCapture: ScreenCapture
) : ActionExecutor {

    private val repository = ImageRepository(context)
    private val matcher = TemplateMatcher()
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var isRunning = false

    companion object {
        private const val TAG = "ImageSearchExecutor"
    }

    data class TestResult(
        val screenshotCaptured: Boolean,
        val templateLoaded: Boolean,
        val matchFound: Boolean,
        val confidence: Float,
        val position: String,
        val errorMessage: String?
    )

    fun testSearch(
        templateId: String,
        threshold: Float = 0.8f,
        searchRegion: ImageSearchParams.SearchRegion? = null
    ): TestResult {
        Log.i(TAG, "=== Test Start ===")
        if (searchRegion != null) {
            Log.i(TAG, "Search region: (${searchRegion.x}, ${searchRegion.y}) ${searchRegion.width}x${searchRegion.height}")
        } else {
            Log.i(TAG, "Search region: Full screen")
        }

        try {
            val screenshot = screenCapture.captureScreen()
            Log.i(TAG, "Screenshot: ${if (screenshot != null) "Success (${screenshot.width}x${screenshot.height})" else "Failed!"}")

            if (screenshot == null) {
                return TestResult(
                    screenshotCaptured = false,
                    templateLoaded = false,
                    matchFound = false,
                    confidence = 0f,
                    position = "",
                    errorMessage = "Screenshot capture failed! MediaProjection may not be initialized."
                )
            }

            val template = repository.loadTemplate(templateId)
            Log.i(TAG, "Template load: ${if (template != null) "Success (${template.width}x${template.height})" else "Failed!"}")

            if (template == null) {
                return TestResult(
                    screenshotCaptured = true,
                    templateLoaded = false,
                    matchFound = false,
                    confidence = 0f,
                    position = "",
                    errorMessage = "Template load failed! Template ID: $templateId"
                )
            }

            val startTime = System.currentTimeMillis()
            val result = matcher.match(screenshot, template, threshold, searchRegion)
            val elapsedTime = System.currentTimeMillis() - startTime

            Log.i(TAG, "Matching time: ${elapsedTime}ms")
            Log.i(TAG, "Matching result: ${if (result != null) "Found (confidence: ${(result.confidence * 100).toInt()}%)" else "Not found"}")

            if (result == null) {
                return TestResult(
                    screenshotCaptured = true,
                    templateLoaded = true,
                    matchFound = false,
                    confidence = 0f,
                    position = "",
                    errorMessage = "Image not found. Try lowering the threshold."
                )
            }

            return TestResult(
                screenshotCaptured = true,
                templateLoaded = true,
                matchFound = result.found,
                confidence = result.confidence,
                position = "(${result.x}, ${result.y})",
                errorMessage = if (result.found) null else "Confidence below threshold (${(result.confidence * 100).toInt()}% < ${(threshold * 100).toInt()}%)"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Test error", e)
            return TestResult(
                screenshotCaptured = false,
                templateLoaded = false,
                matchFound = false,
                confidence = 0f,
                position = "",
                errorMessage = "Error: ${e.message}"
            )
        }
    }

    override fun execute(action: Action): Boolean {
        val templateId = action.params["templateId"] as? String ?: return false
        val threshold = (action.params["threshold"] as? Number)?.toFloat() ?: 0.85f
        val searchModeStr = action.params["searchMode"] as? String ?: "WAIT_UNTIL_FOUND"
        val timeoutMs = (action.params["timeoutMs"] as? Number)?.toLong() ?: 30000L
        val tapOnFound = action.params["tapOnFound"] as? Boolean ?: false
        val playSound = action.params["playSound"] as? Boolean ?: true
        val vibrate = action.params["vibrate"] as? Boolean ?: true

        val searchRegion = if (action.params.containsKey("searchRegionX")) {
            ImageSearchParams.SearchRegion(
                x = (action.params["searchRegionX"] as? Number)?.toInt() ?: 0,
                y = (action.params["searchRegionY"] as? Number)?.toInt() ?: 0,
                width = (action.params["searchRegionWidth"] as? Number)?.toInt() ?: 1080,
                height = (action.params["searchRegionHeight"] as? Number)?.toInt() ?: 1920
            )
        } else null

        val searchMode = ImageSearchParams.SearchMode.valueOf(searchModeStr)

        val params = ImageSearchParams(
            templateId = templateId,
            threshold = threshold,
            searchMode = searchMode,
            timeoutMs = timeoutMs,
            tapOnFound = tapOnFound,
            tapOffsetX = (action.params["tapOffsetX"] as? Number)?.toInt() ?: 0,
            tapOffsetY = (action.params["tapOffsetY"] as? Number)?.toInt() ?: 0,
            searchRegion = searchRegion
        )

        Log.i(TAG, "Image search start - Template: $templateId, Threshold: ${(threshold * 100).toInt()}%, Mode: $searchMode")
        val found = searchImageInternal(params)

        if (found) {
            Log.i(TAG, "Image FOUND!")

            if (playSound) {
                playNotificationSound()
            }
            if (vibrate) {
                vibrator.vibrate(500)
            }

            return true
        } else {
            Log.i(TAG, "Image NOT FOUND")
            return false
        }
    }

    fun searchImage(
        templateId: String,
        threshold: Float,
        searchRegion: ImageSearchParams.SearchRegion? = null
    ): MatchResult {
        val template = repository.loadTemplate(templateId)
        val screenshot = screenCapture.captureScreen()

        if (template == null || screenshot == null) {
            return MatchResult(
                found = false,
                confidence = 0f,
                x = 0,
                y = 0,
                width = 0,
                height = 0
            )
        }

        val result = matcher.match(screenshot, template, threshold, searchRegion)
        return result ?: MatchResult(
            found = false,
            confidence = 0f,
            x = 0,
            y = 0,
            width = 0,
            height = 0
        )
    }

    private fun searchImageInternal(params: ImageSearchParams): Boolean {
        isRunning = true
        val startTime = System.currentTimeMillis()

        val template = repository.loadTemplate(params.templateId)
        if (template == null) {
            Log.e(TAG, "Template load failed: ${params.templateId}")
            return false
        }

        val checkDelay = if (params.searchMode == ImageSearchParams.SearchMode.CHECK_ONCE) {
            0L
        } else {
            50L
        }

        var captureFailCount = 0
        val maxCaptureFails = 5

        while (isRunning && (System.currentTimeMillis() - startTime) < params.timeoutMs) {
            val screenshot = screenCapture.captureScreen()
            if (screenshot == null) {
                captureFailCount++
                if (captureFailCount >= maxCaptureFails) {
                    Log.e(TAG, "Screen capture failed $captureFailCount times - Abort")
                    return false
                }
                Log.w(TAG, "Screen capture failed ($captureFailCount/$maxCaptureFails)")
                Thread.sleep(10)
                continue
            }
            captureFailCount = 0

            val result = matcher.match(screenshot, template, params.threshold, params.searchRegion)

            if (result != null) {
                Log.d(TAG, "Match result - Confidence: ${(result.confidence * 100).toInt()}%")

                when (params.searchMode) {
                    ImageSearchParams.SearchMode.WAIT_UNTIL_FOUND -> {
                        if (result.found) {
                            if (params.tapOnFound) {
                                performTap(result.x + params.tapOffsetX, result.y + params.tapOffsetY)
                            }
                            return true
                        }
                    }
                    ImageSearchParams.SearchMode.CHECK_ONCE -> {
                        return result.found
                    }
                    ImageSearchParams.SearchMode.WAIT_UNTIL_NOT_FOUND -> {
                        if (!result.found) {
                            return true
                        }
                    }
                }
            }

            if (params.searchMode == ImageSearchParams.SearchMode.CHECK_ONCE) {
                return false
            }

            if (checkDelay > 0) {
                Thread.sleep(checkDelay)
            }
        }

        Log.i(TAG, "Timeout - Image not found")
        return false
    }

    private fun performTap(x: Int, y: Int) {
        Log.i(TAG, "Auto tap: ($x, $y)")
    }

    private fun playNotificationSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notification)
            ringtone?.play()
            Log.i(TAG, "Notification sound played")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play notification", e)
        }
    }

    override fun stop() {
        isRunning = false
    }
}