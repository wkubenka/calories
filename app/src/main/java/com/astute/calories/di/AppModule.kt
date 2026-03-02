package com.astute.calories.di

import android.content.Context
import androidx.room.Room
import com.astute.calories.data.local.AppDatabase
import com.astute.calories.data.local.dao.DailyLogDao
import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.dao.SavedMealDao
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
        ).build()

    @Provides
    fun provideDailyLogDao(db: AppDatabase): DailyLogDao = db.dailyLogDao()

    @Provides
    fun provideFoodCacheDao(db: AppDatabase): FoodCacheDao = db.foodCacheDao()

    @Provides
    fun provideSavedMealDao(db: AppDatabase): SavedMealDao = db.savedMealDao()
}
