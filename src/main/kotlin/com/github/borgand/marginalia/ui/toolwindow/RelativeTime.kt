package com.github.borgand.marginalia.ui.toolwindow

import com.github.borgand.marginalia.MarginaliaBundle

/** Short "2m ago" style age for a past epoch-millis timestamp. */
fun relativeTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val seconds = ((nowMs - timestampMs) / 1000).coerceAtLeast(0)
    return when {
        seconds < 5 -> MarginaliaBundle.message("time.just.now")
        seconds < 60 -> MarginaliaBundle.message("time.seconds.ago", seconds)
        seconds < 3600 -> MarginaliaBundle.message("time.minutes.ago", seconds / 60)
        seconds < 86_400 -> MarginaliaBundle.message("time.hours.ago", seconds / 3600)
        else -> MarginaliaBundle.message("time.days.ago", seconds / 86_400)
    }
}
