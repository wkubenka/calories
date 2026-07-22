package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.WeightDao
import com.astute.calories.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val weightDao: WeightDao
) {
    fun getForDate(date: LocalDate): Flow<WeightEntry?> =
        weightDao.getForDate(date)

    fun getRecent(): Flow<List<WeightEntry>> =
        weightDao.getRecent()

    suspend fun upsert(entry: WeightEntry) =
        weightDao.upsert(entry)

    suspend fun delete(date: LocalDate) =
        weightDao.delete(date)
}
