package com.example.nutritracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nutritracker.data.converter.Converters
import com.example.nutritracker.data.dao.*
import com.example.nutritracker.data.entity.*

@Database(
    entities = [
        User::class,
        Meal::class,
        Intake::class,
        TrackedDay::class,
        UserActivityEntity::class,
        WeightLog::class,
        WaterIntake::class,
        Recipe::class,
        RecipeIngredient::class
    ],
    version = 2,
    exportSchema = true
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
    abstract fun recipeDao(): RecipeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meals_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `localImagePath` TEXT,
                        `source` TEXT NOT NULL,
                        `energyKcal100` REAL NOT NULL,
                        `carbohydrates100` REAL NOT NULL,
                        `fat100` REAL NOT NULL,
                        `proteins100` REAL NOT NULL,
                        `sugars100` REAL,
                        `saturatedFat100` REAL,
                        `fiber100` REAL,
                        `sodium100` REAL,
                        `cholesterol100` REAL,
                        `portionLabel` TEXT,
                        `portionGrams` REAL,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `isArchived` INTEGER NOT NULL DEFAULT 0,
                        `foodItemsJson` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `meals_new` (
                        `id`, `name`, `localImagePath`, `source`,
                        `energyKcal100`, `carbohydrates100`, `fat100`, `proteins100`,
                        `sugars100`, `saturatedFat100`, `fiber100`, `sodium100`, `cholesterol100`,
                        `portionLabel`, `portionGrams`, `isFavorite`, `isArchived`, `foodItemsJson`
                    )
                    SELECT
                        `id`, `name`, `localImagePath`, `source`,
                        `energyKcal100`, `carbohydrates100`, `fat100`, `proteins100`,
                        `sugars100`, `saturatedFat100`, `fiber100`, `sodium100`, `cholesterol100`,
                        NULL, NULL, 0, 0, `foodItemsJson`
                    FROM `meals`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `meals`")
                db.execSQL("ALTER TABLE `meals_new` RENAME TO `meals`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `defaultServings` REAL NOT NULL,
                        `createdAt` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipe_ingredients` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipeId` INTEGER NOT NULL,
                        `mealId` INTEGER NOT NULL,
                        `amountGrams` REAL NOT NULL,
                        FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`mealId`) REFERENCES `meals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipeId` ON `recipe_ingredients` (`recipeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_mealId` ON `recipe_ingredients` (`mealId`)")
            }
        }
    }
}
