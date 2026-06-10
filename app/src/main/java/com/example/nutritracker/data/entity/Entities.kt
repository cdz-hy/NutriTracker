package com.example.nutritracker.data.entity

import androidx.room.*
import java.time.LocalDate
import java.time.LocalDateTime

// ── Enums ────────────────────────────────────────────────────────────────────

enum class Gender { MALE, FEMALE, NON_BINARY }

enum class CaloriesProfile { AVERAGED, ESTROGEN_TYPICAL, TESTOSTERONE_TYPICAL }

enum class ActivityLevel(val palValue: Double) {
    SEDENTARY(1.25), LOW_ACTIVE(1.5), ACTIVE(1.75), VERY_ACTIVE(2.2)
}

enum class WeightGoal { LOSE, MAINTAIN, GAIN }

enum class IntakeType { BREAKFAST, LUNCH, DINNER, SNACK }

enum class MealSource { CUSTOM, AI_ANALYSIS, MANUAL }

// ── User ─────────────────────────────────────────────────────────────────────

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int = 1,
    val birthday: LocalDate,
    val heightCm: Double,
    val weightKg: Double,
    val gender: Gender,
    val activityLevel: ActivityLevel,
    val weightGoal: WeightGoal,
    val caloriesProfile: CaloriesProfile = CaloriesProfile.AVERAGED,
    val weeklyWeightGoalKg: Double? = null,
    val targetWeightKg: Double? = null,
    val caloriesTaperEnabled: Boolean = false,
    val usesImperialUnits: Boolean = false
)

// ── Meal ─────────────────────────────────────────────────────────────────────

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brands: String? = null,
    val barcode: String? = null,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val source: MealSource = MealSource.MANUAL,
    // Nutriments per 100g
    val energyKcal100: Double = 0.0,
    val carbohydrates100: Double = 0.0,
    val fat100: Double = 0.0,
    val proteins100: Double = 0.0,
    val sugars100: Double? = null,
    val saturatedFat100: Double? = null,
    val fiber100: Double? = null,
    val sodium100: Double? = null,
    val cholesterol100: Double? = null,
    val servingSize: String? = null,
    val servingQuantityG: Double? = null,
    val foodItemsJson: String? = null  // AI分析的子食物项JSON，如 [{"name":"番茄炒蛋","weight_g":200,...}]
)

// ── Intake ───────────────────────────────────────────────────────────────────

@Entity(
    tableName = "intakes",
    foreignKeys = [ForeignKey(
        entity = Meal::class,
        parentColumns = ["id"],
        childColumns = ["mealId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("mealId"), Index("dateTime")]
)
data class Intake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val intakeType: IntakeType,
    val amount: Double,       // in grams
    val unit: String = "g",
    val dateTime: LocalDateTime
)

// ── TrackedDay ───────────────────────────────────────────────────────────────

@Entity(tableName = "tracked_days")
data class TrackedDay(
    @PrimaryKey val date: LocalDate,
    val calorieGoal: Double = 0.0,
    val caloriesTracked: Double = 0.0,
    val carbsGoal: Double? = null,
    val carbsTracked: Double? = null,
    val fatGoal: Double? = null,
    val fatTracked: Double? = null,
    val proteinGoal: Double? = null,
    val proteinTracked: Double? = null
)

// ── UserActivity ─────────────────────────────────────────────────────────────

@Entity(tableName = "activities")
data class UserActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mets: Double,
    val durationMinutes: Double,
    val burnedKcal: Double,
    val dateTime: LocalDateTime,
    val isCustom: Boolean = false,
    val category: String? = null
)

// ── WeightLog ────────────────────────────────────────────────────────────────

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey val date: LocalDate,
    val weightKg: Double
)

// ── WaterIntake ──────────────────────────────────────────────────────────────

@Entity(tableName = "water_intakes")
data class WaterIntake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val dateTime: LocalDateTime
)
