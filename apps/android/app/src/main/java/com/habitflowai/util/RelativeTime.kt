package com.habitflowai.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val CLOCK_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Compact relative time for posts: seconds/minutes/hours, "Yesterday", days up to a
 * week, then a plain date (e.g. "Aug 9, 2026") — posts don't get a "months ago" tier.
 * Every tier from "Yesterday" onward also gets a trailing clock time (e.g. "Yesterday
 * 16:53"), since the word/day alone no longer pins down when in the day it happened.
 */
fun formatPostTime(isoDate: String?): String = formatRelativeTime(isoDate, includeMonths = false)

/**
 * Compact relative time for comments: same as posts but with an extra "months ago"
 * tier (e.g. "3mo") between "days ago" and falling back to a plain date.
 */
fun formatCommentTime(isoDate: String?): String = formatRelativeTime(isoDate, includeMonths = true)

private fun formatRelativeTime(isoDate: String?, includeMonths: Boolean): String {
    if (isoDate.isNullOrBlank()) return "Just now"
    val then = try {
        Instant.parse(isoDate)
    } catch (e: DateTimeParseException) {
        return isoDate
    }

    val seconds = (Instant.now().epochSecond - then.epochSecond).coerceAtLeast(0)
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    // Seconds/minutes/hours already read as "just now"-ish and precise relative to
    // now — a clock time alongside them would just be noise. Everything coarser than
    // that (Yesterday, Xd, Xmo, a full date) benefits from knowing the time of day too.
    return when {
        seconds < 60 -> "${seconds}s"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days == 1L -> "Yesterday ${formatClockTime(then)}"
        !includeMonths && days < 7 -> "${days}d ${formatClockTime(then)}"
        !includeMonths -> formatFullDateTime(then)
        days < 30 -> "${days}d ${formatClockTime(then)}"
        days / 30 < 12 -> "${days / 30}mo ${formatClockTime(then)}"
        else -> formatFullDateTime(then)
    }
}

private fun formatClockTime(instant: Instant): String =
    CLOCK_TIME_FORMATTER.withZone(ZoneId.systemDefault()).format(instant)

private fun formatFullDateTime(instant: Instant): String =
    "${FULL_DATE_FORMATTER.withZone(ZoneId.systemDefault()).format(instant)} ${formatClockTime(instant)}"
