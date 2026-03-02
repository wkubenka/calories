package com.astute.calories.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.astute.calories.data.local.dao.DailyLogDao
import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.dao.SavedMealDao
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.SavedMeal

@Database(
    entities = [LogEntry::class, CachedFood::class, SavedMeal::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun foodCacheDao(): FoodCacheDao
    abstract fun savedMealDao(): SavedMealDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_foods ADD COLUMN servingSizeLabel TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_foods ADD COLUMN searchQuery TEXT DEFAULT NULL")
            }
        }
    }
}
