package com.astute.calories.data.local.dao

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astute.calories.data.local.AppDatabase
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class WeightDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WeightDao

    private val recordedAt = Instant.parse("2025-01-15T08:00:00Z")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.weightDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun weight(date: LocalDate, lbs: Float) =
        WeightEntry(date = date, weightLbs = lbs, recordedAt = recordedAt)

    @Test
    fun `upsert then getForDate returns the entry`() = runBlocking {
        val date = LocalDate.of(2025, 1, 15)
        dao.upsert(weight(date, 178.4f))

        val result = dao.getForDate(date).first()

        assertEquals(178.4f, result?.weightLbs)
        assertEquals(date, result?.date)
    }

    @Test
    fun `re-upserting the same date overwrites and keeps one row`() = runBlocking {
        val date = LocalDate.of(2025, 1, 15)
        dao.upsert(weight(date, 178.4f))
        dao.upsert(weight(date, 176.0f))

        val all = dao.getRecent().first()
        assertEquals(1, all.size)
        assertEquals(176.0f, all.single().weightLbs)
    }

    @Test
    fun `getForDate returns null when no entry exists`() = runBlocking {
        assertNull(dao.getForDate(LocalDate.of(2025, 1, 15)).first())
    }

    @Test
    fun `getRecent returns entries ordered by date descending`() = runBlocking {
        dao.upsert(weight(LocalDate.of(2025, 1, 13), 180f))
        dao.upsert(weight(LocalDate.of(2025, 1, 15), 178f))
        dao.upsert(weight(LocalDate.of(2025, 1, 14), 179f))

        val dates = dao.getRecent().first().map { it.date }

        assertEquals(
            listOf(
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 1, 14),
                LocalDate.of(2025, 1, 13)
            ),
            dates
        )
    }

    @Test
    fun `delete removes the entry for that date`() = runBlocking {
        val date = LocalDate.of(2025, 1, 15)
        dao.upsert(weight(date, 178.4f))

        dao.delete(date)

        assertNull(dao.getForDate(date).first())
    }

    @Test
    fun `weight survives a daily-log purge (exemption guard)`() = runBlocking {
        // DailyResetWorker calls DailyLogDao.deleteEntriesBefore, which must NOT touch weight.
        val twoDaysAgo = LocalDate.of(2025, 1, 13)
        val yesterday = LocalDate.of(2025, 1, 14)
        dao.upsert(weight(twoDaysAgo, 180f))
        db.dailyLogDao().insert(
            LogEntry(
                date = twoDaysAgo,
                mealCategory = MealCategory.SNACKS,
                foodName = "Old",
                calories = 100,
                proteinG = 0f,
                carbsG = 0f,
                fatG = 0f,
                servingSize = 0f,
                quantity = 1f,
                addedAt = recordedAt
            )
        )

        db.dailyLogDao().deleteEntriesBefore(yesterday)

        // Old log entry is gone, but the weight from the same day survives.
        assertEquals(0, db.dailyLogDao().getEntriesForDate(twoDaysAgo).first().size)
        assertEquals(180f, dao.getForDate(twoDaysAgo).first()?.weightLbs)
    }
}
