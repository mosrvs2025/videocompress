package com.mosrvs.pocketpilot

/** Deterministic offline fallback. Never presented as Needle inference. */
sealed class Command {
    data class Filter(val minimumMb: Long) : Command()
    data class Compress(val quality: Quality) : Command()
    object Help : Command()
}
enum class Quality(val height: Int) { SMALL(480), BALANCED(720), HIGH(1080) }

object CommandRouter {
    private val size = Regex("""(\d+)\s*(?:mb|megabytes?|gb|gigabytes?)""", RegexOption.IGNORE_CASE)
    fun parse(input: String): Command {
        val text = input.lowercase()
        if (text.contains("compress") || text.contains("shrink") || text.contains("reduce size")) {
            val quality = when {
                text.contains("smallest") || text.contains("low quality") -> Quality.SMALL
                text.contains("high quality") || text.contains("1080") -> Quality.HIGH
                else -> Quality.BALANCED
            }
            return Command.Compress(quality)
        }
        if (text.contains("find") || text.contains("show") || text.contains("large") || text.contains("biggest") || text.contains("over")) {
            val match = size.find(text)
            val value = match?.groupValues?.get(1)?.toLongOrNull() ?: 0
            val mult = if (match?.value?.contains("gb", ignoreCase = true) == true || match?.value?.contains("gigabyte", ignoreCase = true) == true) 1024 else 1
            return Command.Filter(value * mult)
        }
        return Command.Help
    }
}
