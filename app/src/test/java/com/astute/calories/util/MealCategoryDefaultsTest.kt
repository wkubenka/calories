package com.astute.calories.util

import com.astute.calories.data.local.entity.MealCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class MealCategoryDefaultsTest {

    @Test
    fun `early morning before breakfast window is snacks`() {
        assertEquals(MealCategory.SNACKS, defaultMealCategory(LocalTime.of(3, 59)))
    }

    @Test
    fun `breakfast window includes its start`() {
        assertEquals(MealCategory.BREAKFAST, defaultMealCategory(LocalTime.of(4, 0)))
    }

    @Test
    fun `mid breakfast is breakfast`() {
        assertEquals(MealCategory.BREAKFAST, defaultMealCategory(LocalTime.of(8, 30)))
    }

    @Test
    fun `lunch start is lunch`() {
        assertEquals(MealCategory.LUNCH, defaultMealCategory(LocalTime.of(10, 30)))
    }

    @Test
    fun `mid lunch is lunch`() {
        assertEquals(MealCategory.LUNCH, defaultMealCategory(LocalTime.of(12, 0)))
    }

    @Test
    fun `afternoon gap is snacks`() {
        assertEquals(MealCategory.SNACKS, defaultMealCategory(LocalTime.of(14, 30)))
        assertEquals(MealCategory.SNACKS, defaultMealCategory(LocalTime.of(15, 30)))
    }

    @Test
    fun `dinner start is dinner`() {
        assertEquals(MealCategory.DINNER, defaultMealCategory(LocalTime.of(17, 0)))
    }

    @Test
    fun `mid dinner is dinner`() {
        assertEquals(MealCategory.DINNER, defaultMealCategory(LocalTime.of(19, 0)))
    }

    @Test
    fun `late night after dinner is snacks`() {
        assertEquals(MealCategory.SNACKS, defaultMealCategory(LocalTime.of(21, 0)))
        assertEquals(MealCategory.SNACKS, defaultMealCategory(LocalTime.of(23, 30)))
    }
}
