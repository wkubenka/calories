package com.astute.calories.data.local.dao

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astute.calories.data.local.AppDatabase
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.SavedMeal
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class SavedMealDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SavedMealDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.savedMealDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun meal(name: String, category: MealCategory) =
        SavedMeal(name = name, category = category, itemsJson = "[]")

    @Test
    fun `getAll orders by category then name`() = runBlocking {
        dao.insert(meal("Weekend", MealCategory.BREAKFAST))
        dao.insert(meal("Workday", MealCategory.LUNCH))
        dao.insert(meal("Default", MealCategory.BREAKFAST))

        val results = dao.getAll().first()

        assertEquals(
            listOf("Default" to MealCategory.BREAKFAST, "Weekend" to MealCategory.BREAKFAST, "Workday" to MealCategory.LUNCH),
            results.map { it.name to it.category }
        )
    }

    @Test
    fun `getByCategory filters and orders by name`() = runBlocking {
        dao.insert(meal("Big", MealCategory.DINNER))
        dao.insert(meal("Small", MealCategory.DINNER))
        dao.insert(meal("Lunch combo", MealCategory.LUNCH))

        val results = dao.getByCategory(MealCategory.DINNER.name).first()

        assertEquals(listOf("Big", "Small"), results.map { it.name })
    }

    @Test
    fun `getById returns null for missing id`() = runBlocking {
        assertNull(dao.getById(9999L))
    }

    @Test
    fun `insert update and delete round-trip`() = runBlocking {
        val id = dao.insert(meal("Original", MealCategory.SNACKS))
        val saved = dao.getById(id)!!

        dao.update(saved.copy(name = "Renamed"))
        assertEquals("Renamed", dao.getById(id)?.name)

        dao.delete(saved.copy(name = "Renamed"))
        assertNull(dao.getById(id))
    }
}
