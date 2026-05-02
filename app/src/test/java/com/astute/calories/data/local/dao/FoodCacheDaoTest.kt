package com.astute.calories.data.local.dao

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astute.calories.data.local.AppDatabase
import com.astute.calories.data.local.entity.CachedFood
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class FoodCacheDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FoodCacheDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.foodCacheDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun food(
        barcode: String,
        name: String = "Food $barcode",
        searchQuery: String? = null,
        lastAccessed: Instant = Instant.parse("2025-01-15T08:00:00Z")
    ) = CachedFood(
        barcode = barcode,
        name = name,
        calories = 100,
        proteinG = 0f,
        carbsG = 0f,
        fatG = 0f,
        servingSizeG = null,
        servingSizeLabel = null,
        imageUrl = null,
        searchQuery = searchQuery,
        lastAccessed = lastAccessed
    )

    @Test
    fun `getByBarcode returns null when not present`() = runBlocking {
        assertNull(dao.getByBarcode("000"))
    }

    @Test
    fun `getByBarcode returns the matching row`() = runBlocking {
        dao.upsert(food("123", name = "Banana"))
        assertEquals("Banana", dao.getByBarcode("123")?.name)
    }

    @Test
    fun `searchByName matches only exact searchQuery and ignores nulls`() = runBlocking {
        dao.upsertAll(listOf(
            food("1", name = "Apple", searchQuery = "apple"),
            food("2", name = "Apple Juice", searchQuery = "apple"),
            food("3", name = "Apricot", searchQuery = "apricot"),
            food("4", name = "Looked up by barcode only", searchQuery = null)
        ))

        val results = dao.searchByName("apple")

        assertEquals(setOf("Apple", "Apple Juice"), results.map { it.name }.toSet())
    }

    @Test
    fun `searchByName orders by lastAccessed descending`() = runBlocking {
        dao.upsertAll(listOf(
            food("1", name = "Older", searchQuery = "x", lastAccessed = Instant.parse("2025-01-01T00:00:00Z")),
            food("2", name = "Newer", searchQuery = "x", lastAccessed = Instant.parse("2025-02-01T00:00:00Z"))
        ))

        val results = dao.searchByName("x")

        assertEquals(listOf("Newer", "Older"), results.map { it.name })
    }

    @Test
    fun `searchByName respects the limit parameter`() = runBlocking {
        repeat(5) { i -> dao.upsert(food(i.toString(), searchQuery = "x")) }
        assertEquals(2, dao.searchByName("x", limit = 2).size)
    }

    @Test
    fun `upsert replaces an existing row by primary key`() = runBlocking {
        dao.upsert(food("123", name = "First"))
        dao.upsert(food("123", name = "Second"))
        assertEquals("Second", dao.getByBarcode("123")?.name)
    }

    @Test
    fun `deleteOlderThan removes rows strictly older than the cutoff`() = runBlocking {
        val cutoff = Instant.parse("2025-02-01T00:00:00Z")
        dao.upsertAll(listOf(
            food("old", lastAccessed = cutoff.minusSeconds(1)),
            food("equal", lastAccessed = cutoff),
            food("newer", lastAccessed = cutoff.plusSeconds(1))
        ))

        dao.deleteOlderThan(cutoff.toEpochMilli())

        assertNull(dao.getByBarcode("old"))
        assertEquals("Food equal", dao.getByBarcode("equal")?.name)
        assertEquals("Food newer", dao.getByBarcode("newer")?.name)
    }
}
