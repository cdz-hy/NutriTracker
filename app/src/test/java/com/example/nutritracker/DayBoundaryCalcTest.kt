package com.example.nutritracker

import com.example.nutritracker.util.DayBoundaryCalc
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DayBoundaryCalcTest {

    private val calc = DayBoundaryCalc()

    @Test
    fun `logical day respects offset before and after boundary`() {
        val offset = 270 // 04:30
        val beforeBoundary = LocalDateTime.of(2026, 6, 2, 3, 15)
        val atBoundary = LocalDateTime.of(2026, 6, 2, 4, 30)

        assertEquals(LocalDate.of(2026, 6, 1), calc.logicalDayOf(beforeBoundary, offset))
        assertEquals(LocalDate.of(2026, 6, 2), calc.logicalDayOf(atBoundary, offset))
    }

    @Test
    fun `out of range offset is safely clamped instead of throwing`() {
        val moment = LocalDateTime.of(2026, 6, 2, 12, 0)
        assertEquals(LocalDate.of(2026, 6, 2), calc.logicalDayOf(moment, -50))
        assertEquals(LocalDate.of(2026, 6, 1), calc.logicalDayOf(moment, 9999)) // clamped to 23:59
    }

    @Test
    fun `logical day range spans exactly 24 hours`() {
        val (start, end) = calc.logicalDayRange(LocalDate.of(2026, 6, 2), 180)
        assertEquals(LocalDateTime.of(2026, 6, 2, 3, 0), start)
        assertEquals(LocalDateTime.of(2026, 6, 3, 3, 0), end)
    }
}
