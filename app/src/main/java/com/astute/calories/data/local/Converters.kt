package com.astute.calories.data.local

import androidx.room.TypeConverter
import com.astute.calories.data.local.entity.MealCategory
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromInstant(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun fromMealCategory(category: MealCategory): String = category.name

    @TypeConverter
    fun toMealCategory(value: String): MealCategory = MealCategory.valueOf(value)
}
