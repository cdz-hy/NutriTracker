package com.example.nutritracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nutritracker.data.converter.Converters
import com.example.nutritracker.data.dao.*
import com.example.nutritracker.data.entity.*

@Database(
    entities = [
        User::class, Meal::class, Intake::class,
        TrackedDay::class, UserActivityEntity::class,
        WeightLog::class, WaterIntake::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun mealDao(): MealDao
    abstract fun intakeDao(): IntakeDao
    abstract fun trackedDayDao(): TrackedDayDao
    abstract fun activityDao(): ActivityDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun waterIntakeDao(): WaterIntakeDao
}
