package com.example.nutritracker.di

import android.content.Context
import androidx.room.Room
import com.example.nutritracker.data.AppDatabase
import com.example.nutritracker.data.dao.*
import com.example.nutritracker.util.DayBoundaryCalc
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
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "nutritracker.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()
    @Provides fun provideIntakeDao(db: AppDatabase): IntakeDao = db.intakeDao()
    @Provides fun provideTrackedDayDao(db: AppDatabase): TrackedDayDao = db.trackedDayDao()
    @Provides fun provideActivityDao(db: AppDatabase): ActivityDao = db.activityDao()
    @Provides fun provideWeightLogDao(db: AppDatabase): WeightLogDao = db.weightLogDao()
    @Provides fun provideWaterIntakeDao(db: AppDatabase): WaterIntakeDao = db.waterIntakeDao()

    @Provides
    @Singleton
    fun provideDayBoundaryCalc(): DayBoundaryCalc = DayBoundaryCalc()
}
