package com.example.nutritracker.di

import com.example.nutritracker.data.dao.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.DayBoundaryCalc
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides @Singleton
    fun provideUserRepository(dao: UserDao) = UserRepository(dao)

    @Provides @Singleton
    fun provideMealRepository(dao: MealDao) = MealRepository(dao)

    @Provides @Singleton
    fun provideIntakeRepository(dao: IntakeDao, dbc: DayBoundaryCalc) = IntakeRepository(dao, dbc)

    @Provides @Singleton
    fun provideTrackedDayRepository(dao: TrackedDayDao) = TrackedDayRepository(dao)

    @Provides @Singleton
    fun provideActivityRepository(dao: ActivityDao, dbc: DayBoundaryCalc) = ActivityRepository(dao, dbc)

    @Provides @Singleton
    fun provideWeightLogRepository(dao: WeightLogDao) = WeightLogRepository(dao)

    @Provides @Singleton
    fun provideWaterIntakeRepository(dao: WaterIntakeDao, dbc: DayBoundaryCalc) = WaterIntakeRepository(dao, dbc)
}
