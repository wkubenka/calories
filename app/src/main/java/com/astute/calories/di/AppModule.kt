package com.astute.calories.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.astute.calories.data.local.AppDatabase
import com.astute.calories.data.local.dao.DailyLogDao
import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.dao.SavedMealDao
import com.astute.calories.data.local.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "calories_db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    fun provideDailyLogDao(db: AppDatabase): DailyLogDao = db.dailyLogDao()

    @Provides
    fun provideFoodCacheDao(db: AppDatabase): FoodCacheDao = db.foodCacheDao()

    @Provides
    fun provideSavedMealDao(db: AppDatabase): SavedMealDao = db.savedMealDao()
}
