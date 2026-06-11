package com.example.nutritracker.di

import android.content.Context
import com.example.nutritracker.data.DataExportManager
import com.example.nutritracker.data.dao.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.DayBoundaryCalc
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides @Singleton
    fun provideDataExportManager(
        @ApplicationContext context: Context,
        mealRepo: MealRepository,
        intakeRepo: IntakeRepository,
        trackedDayRepo: TrackedDayRepository,
        activityRepo: ActivityRepository,
        weightLogRepo: WeightLogRepository,
        waterRepo: WaterIntakeRepository,
        userRepo: UserRepository,
        settingsRepo: SettingsRepository
    ) = DataExportManager(context, mealRepo, intakeRepo, trackedDayRepo, activityRepo, weightLogRepo, waterRepo, userRepo, settingsRepo)
}
