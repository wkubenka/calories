package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.DailyLogDao
import com.astute.calories.data.local.entity.LogEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao
) {
    fun getEntriesForDate(date: LocalDate): Flow<List<LogEntry>> =
        dailyLogDao.getEntriesForDate(date)

    fun getEntriesForDateAndCategory(date: LocalDate, category: String): Flow<List<LogEntry>> =
        dailyLogDao.getEntriesForDateAndCategory(date, category)

    fun getTotalCaloriesForDate(date: LocalDate): Flow<Int?> =
        dailyLogDao.getTotalCaloriesForDate(date)

    suspend fun addEntry(entry: LogEntry): Long =
        dailyLogDao.insert(entry)

    suspend fun updateEntry(entry: LogEntry) =
        dailyLogDao.update(entry)

    suspend fun removeEntry(entry: LogEntry) =
        dailyLogDao.delete(entry)

    suspend fun clearDate(date: LocalDate) =
        dailyLogDao.deleteAllForDate(date)

    suspend fun deleteEntriesBefore(date: LocalDate) =
        dailyLogDao.deleteEntriesBefore(date)
}
