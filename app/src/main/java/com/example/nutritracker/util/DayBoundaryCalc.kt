package com.example.nutritracker.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

class DayBoundaryCalc @Inject constructor() {

    /**
     * Returns the logical day for a given moment, respecting a configurable day-start offset.
     * E.g., with offsetMinutes = 270 (04:30), a timestamp at 02:00 belongs to the previous day.
     */
    fun logicalDayOf(moment: LocalDateTime, offsetMinutes: Int = 0): LocalDate {
        val boundary = LocalTime.of(offsetMinutes / 60, offsetMinutes % 60)
        return if (moment.toLocalTime().isBefore(boundary)) {
            moment.toLocalDate().minusDays(1)
        } else {
            moment.toLocalDate()
        }
    }

    fun currentLogicalDay(offsetMinutes: Int = 0): LocalDate =
        logicalDayOf(LocalDateTime.now(), offsetMinutes)

    /**
     * Returns the start (inclusive) and end (exclusive) LocalDateTime for a logical day.
     */
    fun logicalDayRange(date: LocalDate, offsetMinutes: Int = 0): Pair<LocalDateTime, LocalDateTime> {
        val boundary = LocalTime.of(offsetMinutes / 60, offsetMinutes % 60)
        val start = LocalDateTime.of(date, boundary)
        val end = start.plusDays(1)
        return start to end
    }

    fun isSameLogicalDay(a: LocalDateTime, b: LocalDateTime, offsetMinutes: Int = 0): Boolean =
        logicalDayOf(a, offsetMinutes) == logicalDayOf(b, offsetMinutes)
}
