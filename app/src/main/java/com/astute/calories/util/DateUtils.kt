package com.astute.calories.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    fun today(): LocalDate = LocalDate.now()

    fun yesterday(): LocalDate = LocalDate.now().minusDays(1)

    fun formatForDisplay(date: LocalDate): String = date.format(displayFormatter)
}
