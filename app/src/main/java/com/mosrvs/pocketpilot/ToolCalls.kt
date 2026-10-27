package com.mosrvs.pocketpilot

import org.json.JSONArray
import org.json.JSONObject

/** A validated, allowlisted action. Nothing here touches files; execution needs user approval. */
sealed class ToolCall {
    data class FindVideos(val minimumMb: Long, val sort: String) : ToolCall()
    data class SuggestCompression(val quality: Quality) : ToolCall()
    data class CompressSelected(val quality: Quality) : ToolCall()
}

data class NeedleResult(val calls: List<ToolCall>, val confidence: Double?, val rejected: List<String>, val raw: String)

object ToolCallParser {
    const val MIN_CONFIDENCE = 0.5
    private val sorts = setOf("largest_first", "smallest_first", "newest_first")

    fun qualityOf(s: String?): Quality? = when (s) {
        "smallest" -> Quality.SMALL
        "balanced", null -> Quality.BALANCED
        "high_quality" -> Quality.HIGH
        else -> null
    }

    /** Parses Needle's response object; unknown tools / bad arguments are rejected, never guessed. */
    fun parse(raw: String): NeedleResult {
        val root = JSONObject(raw)
        val confidence = if (root.has("confidence") && !root.isNull("confidence")) root.optDouble("confidence") else null
        val arr = root.optJSONArray("function_calls") ?: JSONArray()
        val calls = mutableListOf<ToolCall>()
        val rejected = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            val name = c.optString("name")
            val args = c.optJSONObject("arguments") ?: c.optJSONObject("parameters") ?: JSONObject()
            val call: ToolCall? = when (name) {
                "find_videos" -> {
                    val mb = if (args.has("minimum_size_mb")) args.optLong("minimum_size_mb", -1) else 0L
                    val sort = args.optString("sort", "largest_first")
                    if (mb in 0..10_000_000 && sort in sorts) ToolCall.FindVideos(mb, sort) else null
                }
                "suggest_compression" -> qualityOf(args.optString("quality").ifEmpty { null })?.let { ToolCall.SuggestCompression(it) }
                "compress_selected_video" -> qualityOf(args.optString("quality").ifEmpty { null })?.let { ToolCall.CompressSelected(it) }
                else -> null
            }
            if (call != null) calls.add(call) else rejected.add(name.ifEmpty { "?" })
        }
        return NeedleResult(calls, confidence, rejected, raw)
    }
}
