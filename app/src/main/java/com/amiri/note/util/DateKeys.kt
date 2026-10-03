package com.amiri.note.util

import java.util.Calendar
import java.util.Locale

/** Date helpers using local time. dayKey = yyyy-MM-dd, weekKey = yyyy-'W'ww. */
object DateKeys {

    fun dayKey(millis: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun dayKey(year: Int, month0: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month0 + 1, day)

    /** ISO-like week key. Uses Monday as first day of week. */
    fun weekKey(millis: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance(Locale.US).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = millis
        }
        val week = c.get(Calendar.WEEK_OF_YEAR)
        var year = c.get(Calendar.YEAR)
        // December week 1 belongs to next year
        if (c.get(Calendar.MONTH) == Calendar.DECEMBER && week == 1) year += 1
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    /** Returns the 7 dayKeys (Mon..Sun) of the week containing [millis]. */
    fun weekDays(millis: Long = System.currentTimeMillis()): List<String> {
        val c = Calendar.getInstance(Locale.US).apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = millis
        }
        // move to Monday
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            c.add(Calendar.DAY_OF_MONTH, -1)
        }
        return (0 until 7).map {
            val key = dayKey(c.timeInMillis)
            c.add(Calendar.DAY_OF_MONTH, 1)
            key
        }
    }

    fun startOfMonthKey(millis: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return dayKey(c.timeInMillis)
    }

    fun endOfMonthKey(millis: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        return dayKey(c.timeInMillis)
    }
}
