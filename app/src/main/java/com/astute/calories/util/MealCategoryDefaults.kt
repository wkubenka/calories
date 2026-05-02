package com.astute.calories.util

import com.astute.calories.data.local.entity.MealCategory
import java.time.LocalTime

private val BREAKFAST_START = LocalTime.of(4, 0)
private val LUNCH_START = LocalTime.of(10, 30)
private val LUNCH_END = LocalTime.of(14, 30)
private val DINNER_START = LocalTime.of(17, 0)
private val DINNER_END = LocalTime.of(21, 0)

fun defaultMealCategory(time: LocalTime = LocalTime.now()): MealCategory = when {
    time >= BREAKFAST_START && time < LUNCH_START -> MealCategory.BREAKFAST
    time >= LUNCH_START && time < LUNCH_END -> MealCategory.LUNCH
    time >= DINNER_START && time < DINNER_END -> MealCategory.DINNER
    else -> MealCategory.SNACKS
}
