package com.habitflowai.util

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativeTimeTest {

    private fun agoIso(seconds: Long): String = Instant.now().minusSeconds(seconds).toString()

    private val clockTimePattern = Regex("""\d{2}:\d{2}$""")

    @Test
    fun `null or blank date falls back to Just now`() {
        assertEquals("Just now", formatPostTime(null))
        assertEquals("Just now", formatPostTime(""))
        assertEquals("Just now", formatCommentTime(null))
    }

    @Test
    fun `seconds minutes and hours are compact with no clock time suffix`() {
        assertEquals("30s", formatPostTime(agoIso(30)))
        assertEquals("20m", formatPostTime(agoIso(20 * 60)))
        assertEquals("5h", formatPostTime(agoIso(5 * 3600)))

        assertEquals("30s", formatCommentTime(agoIso(30)))
        assertEquals("20m", formatCommentTime(agoIso(20 * 60)))
        assertEquals("5h", formatCommentTime(agoIso(5 * 3600)))
    }

    @Test
    fun `exactly one day ago shows Yesterday plus a clock time`() {
        val postResult = formatPostTime(agoIso(24 * 3600 + 30))
        val commentResult = formatCommentTime(agoIso(24 * 3600 + 30))

        assertTrue(postResult.startsWith("Yesterday "))
        assertTrue(clockTimePattern.containsMatchIn(postResult))
        assertTrue(commentResult.startsWith("Yesterday "))
        assertTrue(clockTimePattern.containsMatchIn(commentResult))
    }

    @Test
    fun `post shows days with a clock time up to a week then falls back to a full date`() {
        assertTrue(formatPostTime(agoIso(3 * 24 * 3600)).startsWith("3d "))
        assertTrue(formatPostTime(agoIso(6 * 24 * 3600)).startsWith("6d "))
        // 7+ days: no "d" tier for posts — straight to a plain date (still with a clock time).
        val farResult = formatPostTime(agoIso(9 * 24 * 3600))
        assertFalse(farResult.startsWith("9d"))
        assertTrue(clockTimePattern.containsMatchIn(farResult))
    }

    @Test
    fun `comment shows days then months then a full date, each with a clock time`() {
        assertTrue(formatCommentTime(agoIso(3 * 24 * 3600)).startsWith("3d "))
        assertTrue(formatCommentTime(agoIso(29 * 24 * 3600)).startsWith("29d "))
        assertTrue(formatCommentTime(agoIso(65 * 24 * 3600)).startsWith("2mo "))
        // 12+ months: falls back to a plain date, not "12mo".
        val farResult = formatCommentTime(agoIso(400 * 24 * 3600))
        assertFalse(farResult.contains("mo"))
        assertTrue(clockTimePattern.containsMatchIn(farResult))
    }
}
