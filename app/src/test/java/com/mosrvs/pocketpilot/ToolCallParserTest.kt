package com.mosrvs.pocketpilot

import org.junit.Assert.*
import org.junit.Test

class ToolCallParserTest {
    @Test fun parsesFindVideos() {
        val r = ToolCallParser.parse("""{"function_calls":[{"name":"find_videos","arguments":{"minimum_size_mb":500,"sort":"largest_first"}}],"confidence":0.93}""")
        assertEquals(listOf<ToolCall>(ToolCall.FindVideos(500, "largest_first")), r.calls)
        assertEquals(0.93, r.confidence!!, 1e-9)
    }
    @Test fun parsesCompressDefaultsToBalanced() {
        val r = ToolCallParser.parse("""{"function_calls":[{"name":"compress_selected_video","arguments":{}}]}""")
        assertEquals(listOf<ToolCall>(ToolCall.CompressSelected(Quality.BALANCED)), r.calls)
    }
    @Test fun rejectsUnknownToolAndBadArgs() {
        val r = ToolCallParser.parse("""{"function_calls":[{"name":"delete_file","arguments":{}},{"name":"find_videos","arguments":{"minimum_size_mb":-5}},{"name":"compress_selected_video","arguments":{"quality":"ultra"}}]}""")
        assertTrue(r.calls.isEmpty())
        assertEquals(3, r.rejected.size)
    }
    @Test fun emptyCallsIsNotAGuess() {
        assertTrue(ToolCallParser.parse("""{"function_calls":[],"confidence":0.8}""").calls.isEmpty())
    }
}
