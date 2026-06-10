package com.example.nutritracker.util

import com.example.nutritracker.data.entity.*
import java.time.LocalDate
import java.time.Period
import kotlin.math.pow

// ── BMI ──────────────────────────────────────────────────────────────────────

object BmiCalc {
    fun getBmi(weightKg: Double, heightCm: Double): Double =
        weightKg / (heightCm / 100.0).pow(2)

    fun getNutritionalStatus(bmi: Double): NutritionalStatus = when {
        bmi < 18.5 -> NutritionalStatus.UNDERWEIGHT
        bmi < 25.0 -> NutritionalStatus.NORMAL
        bmi < 30.0 -> NutritionalStatus.PRE_OBESITY
        bmi < 35.0 -> NutritionalStatus.OBESITY_I
        bmi < 40.0 -> NutritionalStatus.OBESITY_II
        else        -> NutritionalStatus.OBESITY_III
    }

    enum class NutritionalStatus(val label: String) {
        UNDERWEIGHT("体重过低"), NORMAL("正常体重"),
        PRE_OBESITY("超重前期"), OBESITY_I("肥胖 I 级"),
        OBESITY_II("肥胖 II 级"), OBESITY_III("肥胖 III 级")
    }
}

// ── Age ──────────────────────────────────────────────────────────────────────

object AgeCalc {
    fun getAge(birthday: LocalDate): Int = Period.between(birthday, LocalDate.now()).years
}

// ── BMR (IOM 2005 uses PA, not direct BMR — kept for reference) ─────────────

object BmrCalc {
    fun getMifflinStJeor(user: User): Double {
        val age = AgeCalc.getAge(user.birthday).toDouble()
        return when (user.gender) {
            Gender.MALE   -> 10 * user.weightKg + 6.25 * user.heightCm - 5 * age + 5
            Gender.FEMALE -> 10 * user.weightKg + 6.25 * user.heightCm - 5 * age - 161
            Gender.NON_BINARY -> when (user.caloriesProfile) {
                CaloriesProfile.TESTOSTERONE_TYPICAL ->
                    10 * user.weightKg + 6.25 * user.heightCm - 5 * age + 5
                CaloriesProfile.ESTROGEN_TYPICAL ->
                    10 * user.weightKg + 6.25 * user.heightCm - 5 * age - 161
                CaloriesProfile.AVERAGED ->
                    (10 * user.weightKg + 6.25 * user.heightCm - 5 * age + 5 +
                     10 * user.weightKg + 6.25 * user.heightCm - 5 * age - 161) / 2.0
            }
        }
    }
}

// ── PAL → PA coefficient (IOM 2005) ─────────────────────────────────────────

object PalCalc {
    fun getPaCoefficient(palValue: Double, isMaleFormula: Boolean): Double = when {
        palValue < 1.4 -> 1.0
        palValue < 1.6 -> if (isMaleFormula) 1.12 else 1.14
        palValue < 1.9 -> 1.27
        else           -> if (isMaleFormula) 1.54 else 1.45
    }
}

// ── TDEE (IOM 2005) ─────────────────────────────────────────────────────────

object TdeeCalc {
    fun getTdeeKcal(user: User): Double {
        val age = AgeCalc.getAge(user.birthday).toDouble()
        val pa = PalCalc.getPaCoefficient(user.activityLevel.palValue, true) // placeholder

        fun maleFormula()   = 864 - 9.72 * age + pa * 14.2 * user.weightKg + 503 * (user.heightCm / 100.0)
        fun femaleFormula() = 387 - 7.31 * age + pa * 10.9 * user.weightKg + 660.7 * (user.heightCm / 100.0)

        return when (user.gender) {
            Gender.MALE   -> {
                val paM = PalCalc.getPaCoefficient(user.activityLevel.palValue, true)
                864 - 9.72 * age + paM * 14.2 * user.weightKg + 503 * (user.heightCm / 100.0)
            }
            Gender.FEMALE -> {
                val paF = PalCalc.getPaCoefficient(user.activityLevel.palValue, false)
                387 - 7.31 * age + paF * 10.9 * user.weightKg + 660.7 * (user.heightCm / 100.0)
            }
            Gender.NON_BINARY -> when (user.caloriesProfile) {
                CaloriesProfile.TESTOSTERONE_TYPICAL -> {
                    val paM = PalCalc.getPaCoefficient(user.activityLevel.palValue, true)
                    864 - 9.72 * age + paM * 14.2 * user.weightKg + 503 * (user.heightCm / 100.0)
                }
                CaloriesProfile.ESTROGEN_TYPICAL -> {
                    val paF = PalCalc.getPaCoefficient(user.activityLevel.palValue, false)
                    387 - 7.31 * age + paF * 10.9 * user.weightKg + 660.7 * (user.heightCm / 100.0)
                }
                CaloriesProfile.AVERAGED -> (maleFormula() + femaleFormula()) / 2.0
            }
        }
    }
}

// ── Calorie Goal ─────────────────────────────────────────────────────────────

object CalorieGoalCalc {
    const val LOSE_KCAL = -500.0
    const val GAIN_KCAL = 500.0
    private const val KCAL_PER_KG_PER_WEEK_DAILY = 1100.0
    private const val TAPER_START_KG = 5.0
    private const val TAPER_END_KG = 1.0
    const val MIN_KCAL_MALE = 1500.0
    const val MIN_KCAL_FEMALE = 1200.0

    fun getKcalGoalAdjustment(goal: WeightGoal, weeklyWeightGoalKg: Double? = null): Double {
        if (weeklyWeightGoalKg != null) return weeklyWeightGoalKg * KCAL_PER_KG_PER_WEEK_DAILY
        return when (goal) {
            WeightGoal.LOSE     -> LOSE_KCAL
            WeightGoal.GAIN     -> GAIN_KCAL
            WeightGoal.MAINTAIN -> 0.0
        }
    }

    fun applyTargetWeightTaper(
        baseAdjustment: Double,
        currentWeightKg: Double,
        targetWeightKg: Double?,
        goal: WeightGoal,
        taperEnabled: Boolean
    ): Double {
        if (!taperEnabled || targetWeightKg == null || goal == WeightGoal.MAINTAIN || baseAdjustment == 0.0)
            return baseAdjustment
        val signedDistance = targetWeightKg - currentWeightKg
        val reachedOrOvershot = if (goal == WeightGoal.LOSE) signedDistance >= 0 else signedDistance <= 0
        if (reachedOrOvershot) return 0.0
        val distance = kotlin.math.abs(signedDistance)
        if (distance <= TAPER_END_KG) return 0.0
        if (distance >= TAPER_START_KG) return baseAdjustment
        val progress = (distance - TAPER_END_KG) / (TAPER_START_KG - TAPER_END_KG)
        return baseAdjustment * progress
    }

    fun getTotalKcalGoal(
        user: User,
        totalActivityKcal: Double,
        userKcalAdjustment: Double = 0.0
    ): Double {
        val tdee = TdeeCalc.getTdeeKcal(user)
        val baseAdj = getKcalGoalAdjustment(user.weightGoal, user.weeklyWeightGoalKg)
        val adj = applyTargetWeightTaper(baseAdj, user.weightKg, user.targetWeightKg, user.weightGoal, user.caloriesTaperEnabled)
        return tdee + adj + userKcalAdjustment + totalActivityKcal
    }

    fun isBelowRecommendedFloor(goalKcal: Double, user: User): Boolean {
        val floor = recommendedFloor(user)
        return goalKcal < floor
    }

    fun recommendedFloor(user: User): Double {
        val usesMaleFloor = user.gender == Gender.MALE ||
            user.caloriesProfile == CaloriesProfile.TESTOSTERONE_TYPICAL
        return if (usesMaleFloor) MIN_KCAL_MALE else MIN_KCAL_FEMALE
    }
}

// ── Macro Goals ──────────────────────────────────────────────────────────────

object MacroCalc {
    const val DEFAULT_CARB_PCT = 0.60
    const val DEFAULT_FAT_PCT = 0.25
    const val DEFAULT_PROTEIN_PCT = 0.15

    fun getCarbsGoal(totalKcal: Double, pct: Double = DEFAULT_CARB_PCT): Double = (totalKcal * pct) / 4.0
    fun getFatGoal(totalKcal: Double, pct: Double = DEFAULT_FAT_PCT): Double = (totalKcal * pct) / 9.0
    fun getProteinGoal(totalKcal: Double, pct: Double = DEFAULT_PROTEIN_PCT): Double = (totalKcal * pct) / 4.0
}

// ── MET Activity Burn ────────────────────────────────────────────────────────

object MetCalc {
    fun getBurnedKcal(weightKg: Double, mets: Double, durationMinutes: Double): Double =
        mets * weightKg * durationMinutes / 60.0
}
