package com.astute.calories.data.local.dao

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astute.calories.data.local.AppDatabase
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
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
class DailyLogDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DailyLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dailyLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entry(
        date: LocalDate,
        category: MealCategory = MealCategory.SNACKS,
        name: String = "Apple",
        calories: Int = 100,
        addedAt: Instant = Instant.parse("2025-01-15T08:00:00Z")
    ) = LogEntry(
        date = date,
        mealCategory = category,
        foodName = name,
        calories = calories,
        proteinG = 0f,
        carbsG = 0f,
        fatG = 0f,
        servingSize = 0f,
        quantity = 1f,
        addedAt = addedAt
    )

    @Test
    fun `getEntriesForDate returns only that date's entries ordered by addedAt`() = runBlocking {
        val today = LocalDate.of(2025, 1, 15)
        val yesterday = today.minusDays(1)
        dao.insert(entry(yesterday, name = "Old"))
        dao.insert(entry(today, name = "Second", addedAt = Instant.parse("2025-01-15T12:00:00Z")))
        dao.insert(entry(today, name = "First", addedAt = Instant.parse("2025-01-15T08:00:00Z")))

        val results = dao.getEntriesForDate(today).first()

        assertEquals(listOf("First", "Second"), results.map { it.foodName })
    }

    @Test
    fun `getEntriesForDateAndCategory filters by both date and category`() = runBlocking {
        val today = LocalDate.of(2025, 1, 15)
        dao.insert(entry(today, MealCategory.BREAKFAST, name = "Toast"))
        dao.insert(entry(today, MealCategory.LUNCH, name = "Salad"))
        dao.insert(entry(today.minusDays(1), MealCategory.BREAKFAST, name = "Old toast"))

        val results = dao.getEntriesForDateAndCategory(today, MealCategory.BREAKFAST.name).first()

        assertEquals(listOf("Toast"), results.map { it.foodName })
    }

    @Test
    fun `getTotalCaloriesForDate returns null for an empty day`() = runBlocking {
        val today = LocalDate.of(2025, 1, 15)
        assertNull(dao.getTotalCaloriesForDate(today).first())
    }

    @Test
    fun `getTotalCaloriesForDate sums calories on the given day only`() = runBlocking {
        val today = LocalDate.of(2025, 1, 15)
        dao.insert(entry(today, calories = 200))
        dao.insert(entry(today, calories = 350))
        dao.insert(entry(today.minusDays(1), calories = 999))

        assertEquals(550, dao.getTotalCaloriesForDate(today).first())
    }

    @Test
    fun `deleteAllForDate removes only that date's entries`() = runBlocking {
        val today = LocalDate.of(2025, 1, 15)
        val yesterday = today.minusDays(1)
        dao.insert(entry(today, name = "Today"))
        dao.insert(entry(yesterday, name = "Yesterday"))

        dao.deleteAllForDate(today)

        assertEquals(0, dao.getEntriesForDate(today).first().size)
        assertEquals(listOf("Yesterday"), dao.getEntriesForDate(yesterday).first().map { it.foodName })
    }

    @Test
    fun `deleteEntriesBefore preserves entries on the cutoff date`() = runBlocking {
        // This is the load-bearing invariant: DailyResetWorker passes yesterday as the
        // cutoff to preserve yesterday for "Copy yesterday".
        val today = LocalDate.of(2025, 1, 15)
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        dao.insert(entry(today, name = "Today"))
        dao.insert(entry(yesterday, name = "Yesterday"))
        dao.insert(entry(twoDaysAgo, name = "Old"))

        dao.deleteEntriesBefore(yesterday)

        val survivors = (dao.getEntriesForDate(today).first() +
            dao.getEntriesForDate(yesterday).first() +
            dao.getEntriesForDate(twoDaysAgo).first()).map { it.foodName }
        assertEquals(setOf("Today", "Yesterday"), survivors.toSet())
    }

    @Test
    fun `update modifies fields without changing the row identity`() = runBlocking {
        val today = LocalDate.of(2025, 1, 15)
        val id = dao.insert(entry(today, name = "Apple", calories = 100))
        val original = dao.getEntriesForDate(today).first().single()

        dao.update(original.copy(foodName = "Pear", calories = 60))

        val updated = dao.getEntriesForDate(today).first().single()
        assertEquals(id, updated.id)
        assertEquals("Pear", updated.foodName)
        assertEquals(60, updated.calories)
    }
}
