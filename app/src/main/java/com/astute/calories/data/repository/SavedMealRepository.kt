package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.SavedMealDao
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.SavedMeal
import com.astute.calories.data.local.entity.SavedMealItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedMealRepository @Inject constructor(
    private val savedMealDao: SavedMealDao,
    private val moshi: Moshi
) {
    private val listType = Types.newParameterizedType(List::class.java, SavedMealItem::class.java)
    private val adapter = moshi.adapter<List<SavedMealItem>>(listType)

    fun getAll(): Flow<List<SavedMeal>> =
        savedMealDao.getAll()

    fun getByCategory(category: String): Flow<List<SavedMeal>> =
        savedMealDao.getByCategory(category)

    suspend fun getById(id: Long): SavedMeal? =
        savedMealDao.getById(id)

    suspend fun save(meal: SavedMeal): Long =
        savedMealDao.insert(meal)

    suspend fun update(meal: SavedMeal) =
        savedMealDao.update(meal)

    suspend fun delete(meal: SavedMeal) =
        savedMealDao.delete(meal)

    fun parseItems(json: String): List<SavedMealItem> =
        adapter.fromJson(json) ?: emptyList()

    fun toJson(items: List<SavedMealItem>): String =
        adapter.toJson(items)

    fun logEntriesToSavedItems(entries: List<LogEntry>): List<SavedMealItem> =
        entries.map { entry ->
            SavedMealItem(
                foodName = entry.foodName,
                calories = entry.calories,
                proteinG = entry.proteinG,
                carbsG = entry.carbsG,
                fatG = entry.fatG,
                servingSize = entry.servingSize,
                quantity = entry.quantity,
                barcode = entry.barcode
            )
        }

    fun savedItemsToLogEntries(
        items: List<SavedMealItem>,
        category: MealCategory,
        date: java.time.LocalDate
    ): List<LogEntry> =
        items.map { item ->
            LogEntry(
                date = date,
                mealCategory = category,
                foodName = item.foodName,
                calories = item.calories,
                proteinG = item.proteinG,
                carbsG = item.carbsG,
                fatG = item.fatG,
                servingSize = item.servingSize,
                quantity = item.quantity,
                barcode = item.barcode,
                addedAt = java.time.Instant.now()
            )
        }
}
