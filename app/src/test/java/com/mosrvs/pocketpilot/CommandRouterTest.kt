package com.mosrvs.pocketpilot

import org.junit.Assert.*
import org.junit.Test

class CommandRouterTest {
    @Test fun filterMb() = assertEquals(Command.Filter(500), CommandRouter.parse("Show videos larger than 500 MB"))
    @Test fun filterGb() = assertEquals(Command.Filter(2048), CommandRouter.parse("find videos over 2 GB"))
    @Test fun compressHigh() = assertEquals(Command.Compress(Quality.HIGH), CommandRouter.parse("compress high quality"))
    @Test fun help() = assertEquals(Command.Help, CommandRouter.parse("hello"))
}
