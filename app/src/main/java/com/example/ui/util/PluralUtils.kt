package com.example.ui.util

/**
 * Consistent pluralization helper functions for WalkCrew UI strings
 */
object PluralUtils {
    fun walkers(count: Int): String {
        return if (count == 1) "1 walker" else "$count walkers"
    }

    fun friends(count: Int): String {
        return if (count == 1) "1 friend" else "$count friends"
    }

    fun days(count: Int): String {
        return if (count == 1) "1 day" else "$count days"
    }

    fun crews(count: Int): String {
        return if (count == 1) "1 crew" else "$count crews"
    }
}
