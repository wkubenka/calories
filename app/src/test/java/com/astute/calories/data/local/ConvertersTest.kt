package com.astute.calories.data.local

import com.astute.calories.data.local.entity.MealCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `LocalDate round trips through ISO-8601 string`() {
        val date = LocalDate.of(2025, 3, 14)
        val encoded = converters.fromLocalDate(date)
        assertEquals("2025-03-14", encoded)
        assertEquals(date, converters.toLocalDate(encoded))
    }

    @Test
    fun `Instant round trips at millisecond precision`() {
        val instant = Instant.ofEpochMilli(1_700_000_000_123L)
        val encoded = converters.fromInstant(instant)
        assertEquals(1_700_000_000_123L, encoded)
        assertEquals(instant, converters.toInstant(encoded))
    }

    @Test
    fun `Instant conversion truncates sub-millisecond precision`() {
        // Storage is epoch millis, so nanos finer than 1ms are dropped.
        val withNanos = Instant.ofEpochSecond(1_700_000_000L, 123_456_789L)
        val recovered = converters.toInstant(converters.fromInstant(withNanos))
        assertEquals(withNanos.toEpochMilli(), recovered.toEpochMilli())
    }

    @Test
    fun `MealCategory round trips through enum name`() {
        MealCategory.entries.forEach { category ->
            val encoded = converters.fromMealCategory(category)
            assertEquals(category.name, encoded)
            assertEquals(category, converters.toMealCategory(encoded))
        }
    }

    @Test
    fun `toMealCategory throws on unknown value`() {
        assertThrows(IllegalArgumentException::class.java) {
            converters.toMealCategory("BRUNCH")
        }
    }
}
