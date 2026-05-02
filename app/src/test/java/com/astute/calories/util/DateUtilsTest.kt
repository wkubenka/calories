package com.astute.calories.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `today returns the current date`() {
        // Compare against a tight window to tolerate the rare midnight rollover.
        val before = LocalDate.now()
        val today = DateUtils.today()
        val after = LocalDate.now()
        assertTrue(today == before || today == after, "today() out of range: $today")
    }

    @Test
    fun `yesterday is one day before today`() {
        assertEquals(DateUtils.today().minusDays(1), DateUtils.yesterday())
    }

    @Test
    fun `formatForDisplay produces a non-empty localized string`() {
        val formatted = DateUtils.formatForDisplay(LocalDate.of(2025, 3, 14))
        assertTrue(formatted.isNotBlank(), "formatForDisplay returned blank")
    }
}
