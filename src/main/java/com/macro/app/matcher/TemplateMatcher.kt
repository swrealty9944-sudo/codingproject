package com.macro.app.matcher

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.macro.app.data.ImageSearchParams
import com.macro.app.data.MatchResult
import kotlin.math.abs

class TemplateMatcher {

    companion object {
        private const val TAG = "TemplateMatcher"
        private var screenshotPixels: IntArray? = null
        private var templatePixels: IntArray? = null
    }

    fun match(
        screenshot: Bitmap,
        template: Bitmap,
        threshold: Float,
        searchRegion: ImageSearchParams.SearchRegion? = null
    ): MatchResult? {
        val screenshotWidth = screenshot.width
        val screenshotHeight = screenshot.height
        val templateWidth = template.width
        val templateHeight = template.height

        if (templateWidth > screenshotWidth || templateHeight > screenshotHeight) {
            return null
        }

        val regionX = searchRegion?.x ?: 0
        val regionY = searchRegion?.y ?: 0
        val regionWidth = searchRegion?.width ?: screenshotWidth
        val regionHeight = searchRegion?.height ?: screenshotHeight

        val searchStartX = regionX.coerceAtLeast(0)
        val searchStartY = regionY.coerceAtLeast(0)
        val searchEndX = (regionX + regionWidth - templateWidth + 1).coerceAtMost(screenshotWidth - templateWidth + 1)
        val searchEndY = (regionY + regionHeight - templateHeight + 1).coerceAtMost(screenshotHeight - templateHeight + 1)

        if (searchStartX >= searchEndX || searchStartY >= searchEndY) {
            return null
        }

        if (screenshotPixels == null || screenshotPixels!!.size != screenshotWidth * screenshotHeight) {
            screenshotPixels = IntArray(screenshotWidth * screenshotHeight)
        }
        if (templatePixels == null || templatePixels!!.size != templateWidth * templateHeight) {
            templatePixels = IntArray(templateWidth * templateHeight)
        }

        screenshot.getPixels(screenshotPixels!!, 0, screenshotWidth, 0, 0, screenshotWidth, screenshotHeight)
        template.getPixels(templatePixels!!, 0, templateWidth, 0, 0, templateWidth, templateHeight)

        val searchArea = (searchEndX - searchStartX) * (searchEndY - searchStartY)

        val result = if (searchArea > 500000) {
            matchWithDownscale(screenshot, template, threshold, searchStartX, searchStartY, searchEndX, searchEndY)
        } else {
            matchDirectFast(
                screenshotPixels!!,
                screenshotWidth,
                screenshotHeight,
                templatePixels!!,
                templateWidth,
                templateHeight,
                threshold,
                searchStartX,
                searchStartY,
                searchEndX,
                searchEndY
            )
        }

        if (result != null && result.found) {
            Log.d(TAG, "매칭: ${(result.confidence * 100).toInt()}% at (${result.x}, ${result.y})")
        }

        return result
    }

    private fun matchWithDownscale(
        screenshot: Bitmap,
        template: Bitmap,
        threshold: Float,
        searchStartX: Int,
        searchStartY: Int,
        searchEndX: Int,
        searchEndY: Int
    ): MatchResult? {
        val scale = 0.4f
        val scaledScreenshot = scaleBitmap(screenshot, scale)
        val scaledTemplate = scaleBitmap(template, scale)

        val scaledWidth = scaledScreenshot.width
        val scaledHeight = scaledScreenshot.height
        val scaledTemplateWidth = scaledTemplate.width
        val scaledTemplateHeight = scaledTemplate.height

        val scaledScreenPixels = IntArray(scaledWidth * scaledHeight)
        val scaledTemplatePixels = IntArray(scaledTemplateWidth * scaledTemplateHeight)

        scaledScreenshot.getPixels(scaledScreenPixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)
        scaledTemplate.getPixels(scaledTemplatePixels, 0, scaledTemplateWidth, 0, 0, scaledTemplateWidth, scaledTemplateHeight)

        val scaledStartX = (searchStartX * scale).toInt()
        val scaledStartY = (searchStartY * scale).toInt()
        val scaledEndX = (searchEndX * scale).toInt().coerceAtMost(scaledWidth - scaledTemplateWidth)
        val scaledEndY = (searchEndY * scale).toInt().coerceAtMost(scaledHeight - scaledTemplateHeight)

        val coarseMatch = matchDirectFast(
            scaledScreenPixels,
            scaledWidth,
            scaledHeight,
            scaledTemplatePixels,
            scaledTemplateWidth,
            scaledTemplateHeight,
            threshold - 0.1f,
            scaledStartX,
            scaledStartY,
            scaledEndX,
            scaledEndY
        ) ?: return null

        val refineStartX = ((coarseMatch.x / scale).toInt() - 20).coerceAtLeast(searchStartX)
        val refineStartY = ((coarseMatch.y / scale).toInt() - 20).coerceAtLeast(searchStartY)
        val refineEndX = ((coarseMatch.x / scale).toInt() + 20 + template.width).coerceAtMost(searchEndX)
        val refineEndY = ((coarseMatch.y / scale).toInt() + 20 + template.height).coerceAtMost(searchEndY)

        if (screenshotPixels == null || screenshotPixels!!.size != screenshot.width * screenshot.height) {
            screenshotPixels = IntArray(screenshot.width * screenshot.height)
        }
        if (templatePixels == null || templatePixels!!.size != template.width * template.height) {
            templatePixels = IntArray(template.width * template.height)
        }

        screenshot.getPixels(screenshotPixels!!, 0, screenshot.width, 0, 0, screenshot.width, screenshot.height)
        template.getPixels(templatePixels!!, 0, template.width, 0, 0, template.width, template.height)

        return matchDirectFast(
            screenshotPixels!!,
            screenshot.width,
            screenshot.height,
            templatePixels!!,
            template.width,
            template.height,
            threshold,
            refineStartX,
            refineStartY,
            refineEndX,
            refineEndY
        )
    }

    private fun matchDirectFast(
        screenshotPixels: IntArray,
        screenshotWidth: Int,
        screenshotHeight: Int,
        templatePixels: IntArray,
        templateWidth: Int,
        templateHeight: Int,
        threshold: Float,
        searchStartX: Int,
        searchStartY: Int,
        searchEndX: Int,
        searchEndY: Int
    ): MatchResult? {
        val searchWidth = searchEndX - searchStartX
        val searchHeight = searchEndY - searchStartY
        val searchArea = searchWidth * searchHeight

        val initialStep = when {
            searchArea > 500000 -> 8
            searchArea > 300000 -> 6
            searchArea > 100000 -> 4
            else -> 3
        }

        var bestX = -1
        var bestY = -1
        var bestConfidence = 0f

        for (y in searchStartY until searchEndY step initialStep) {
            for (x in searchStartX until searchEndX step initialStep) {
                val confidence = calculateConfidenceUltraFast(
                    screenshotPixels, screenshotWidth, screenshotHeight,
                    templatePixels, templateWidth, templateHeight,
                    x, y
                )

                if (confidence > bestConfidence) {
                    bestConfidence = confidence
                    bestX = x
                    bestY = y
                }

                if (confidence >= 0.96f) {
                    val refined = refineMatchFast(
                        screenshotPixels, screenshotWidth, screenshotHeight,
                        templatePixels, templateWidth, templateHeight,
                        x, y, threshold
                    )
                    if (refined != null && refined.found) {
                        return refined
                    }
                }
            }
        }

        if (bestX >= 0 && bestConfidence >= threshold * 0.75f) {
            val refined = refineMatchFast(
                screenshotPixels, screenshotWidth, screenshotHeight,
                templatePixels, templateWidth, templateHeight,
                bestX, bestY, threshold
            )
            if (refined != null) {
                return refined
            }
        }

        if (bestX >= 0) {
            return MatchResult(
                found = bestConfidence >= threshold,
                confidence = bestConfidence,
                x = bestX,
                y = bestY,
                width = templateWidth,
                height = templateHeight
            )
        }

        return null
    }

    private fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val matrix = Matrix().apply {
            postScale(scale, scale)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun calculateConfidenceUltraFast(
        screenshotPixels: IntArray,
        screenshotWidth: Int,
        screenshotHeight: Int,
        templatePixels: IntArray,
        templateWidth: Int,
        templateHeight: Int,
        offsetX: Int,
        offsetY: Int
    ): Float {
        val sampleStep = when {
            templateWidth * templateHeight > 10000 -> 4
            templateWidth * templateHeight > 5000 -> 3
            else -> 2
        }

        var totalDiff = 0
        var sampleCount = 0
        val maxDiff = 255

        for (ty in 0 until templateHeight step sampleStep) {
            val sy = offsetY + ty
            if (sy >= screenshotHeight) break

            val templateRowStart = ty * templateWidth
            val screenRowStart = sy * screenshotWidth + offsetX

            for (tx in 0 until templateWidth step sampleStep) {
                val sx = offsetX + tx
                if (sx >= screenshotWidth) break

                val templatePixel = templatePixels[templateRowStart + tx]
                val screenshotPixel = screenshotPixels[screenRowStart + tx]

                val tr = (templatePixel shr 16) and 0xFF
                val tg = (templatePixel shr 8) and 0xFF
                val tb = templatePixel and 0xFF

                val sr = (screenshotPixel shr 16) and 0xFF
                val sg = (screenshotPixel shr 8) and 0xFF
                val sb = screenshotPixel and 0xFF

                val diff = abs(tr - sr) + abs(tg - sg) + abs(tb - sb)
                totalDiff += diff

                sampleCount++

                if (sampleCount > 50 && totalDiff > sampleCount * maxDiff * 0.5) {
                    return 0f
                }
            }
        }

        if (sampleCount == 0) return 0f

        val avgDiff = totalDiff.toFloat() / (sampleCount * 3)
        val confidence = 1.0f - (avgDiff / 255.0f)

        return confidence.coerceIn(0f, 1f)
    }

    private fun refineMatchFast(
        screenshotPixels: IntArray,
        screenshotWidth: Int,
        screenshotHeight: Int,
        templatePixels: IntArray,
        templateWidth: Int,
        templateHeight: Int,
        coarseX: Int,
        coarseY: Int,
        threshold: Float
    ): MatchResult? {
        val searchRange = 3
        val startX = (coarseX - searchRange).coerceAtLeast(0)
        val startY = (coarseY - searchRange).coerceAtLeast(0)
        val endX = (coarseX + searchRange).coerceAtMost(screenshotWidth - templateWidth)
        val endY = (coarseY + searchRange).coerceAtMost(screenshotHeight - templateHeight)

        var bestX = coarseX
        var bestY = coarseY
        var bestConfidence = 0f

        for (y in startY..endY) {
            for (x in startX..endX) {
                val confidence = calculateConfidencePreciseFast(
                    screenshotPixels, screenshotWidth, screenshotHeight,
                    templatePixels, templateWidth, templateHeight,
                    x, y
                )
                if (confidence > bestConfidence) {
                    bestConfidence = confidence
                    bestX = x
                    bestY = y
                }

                if (confidence >= 0.98f) {
                    return MatchResult(
                        found = true,
                        confidence = confidence,
                        x = x,
                        y = y,
                        width = templateWidth,
                        height = templateHeight
                    )
                }
            }
        }

        return MatchResult(
            found = bestConfidence >= threshold,
            confidence = bestConfidence,
            x = bestX,
            y = bestY,
            width = templateWidth,
            height = templateHeight
        )
    }

    private fun calculateConfidencePreciseFast(
        screenshotPixels: IntArray,
        screenshotWidth: Int,
        screenshotHeight: Int,
        templatePixels: IntArray,
        templateWidth: Int,
        templateHeight: Int,
        offsetX: Int,
        offsetY: Int
    ): Float {
        var totalDiff = 0
        var pixelCount = 0

        for (ty in 0 until templateHeight) {
            val sy = offsetY + ty
            if (sy >= screenshotHeight) break

            val templateRowStart = ty * templateWidth
            val screenRowStart = sy * screenshotWidth + offsetX

            for (tx in 0 until templateWidth) {
                val sx = offsetX + tx
                if (sx >= screenshotWidth) break

                val templatePixel = templatePixels[templateRowStart + tx]
                val screenshotPixel = screenshotPixels[screenRowStart + tx]

                val tr = (templatePixel shr 16) and 0xFF
                val tg = (templatePixel shr 8) and 0xFF
                val tb = templatePixel and 0xFF

                val sr = (screenshotPixel shr 16) and 0xFF
                val sg = (screenshotPixel shr 8) and 0xFF
                val sb = screenshotPixel and 0xFF

                totalDiff += abs(tr - sr) + abs(tg - sg) + abs(tb - sb)
                pixelCount++
            }
        }

        if (pixelCount == 0) return 0f

        val avgDiff = totalDiff.toFloat() / (pixelCount * 3)
        val confidence = 1.0f - (avgDiff / 255.0f)

        return confidence.coerceIn(0f, 1f)
    }
}