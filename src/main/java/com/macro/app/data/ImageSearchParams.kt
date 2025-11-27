package com.macro.app.data

data class ImageSearchParams(
    val templateId: String,
    val threshold: Float = 0.8f,
    val searchMode: SearchMode = SearchMode.WAIT_UNTIL_FOUND,
    val timeoutMs: Long = 30000L,
    val tapOnFound: Boolean = false,
    val tapOffsetX: Int = 0,
    val tapOffsetY: Int = 0,
    val searchRegion: SearchRegion? = null
) {
    enum class SearchMode {
        WAIT_UNTIL_FOUND,
        CHECK_ONCE,
        WAIT_UNTIL_NOT_FOUND
    }

    data class SearchRegion(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
}