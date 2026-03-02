package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.SavedMealDao
import com.astute.calories.data.local.entity.SavedMeal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedMealRepository @Inject constructor(
    private val savedMealDao: SavedMealDao
) {
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
}
