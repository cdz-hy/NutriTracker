package com.example.nutritracker.util

import com.example.nutritracker.data.entity.*
import java.time.LocalDate
import java.time.Period
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

// ══════════════════════════════════════════════════════════════════════════════
// BMI 计算
// 来源: WHO Europe - https://www.who.int/europe/news-room/fact-sheets/item/a-healthy-lifestyle---who-recommendations
// ══════════════════════════════════════════════════════════════════════════════

enum class BmiStandard(val label: String, val labelEn: String) {
    WHO("世界标准 (WHO)", "WHO Standard"),
    CHINA("中国标准 (WGOC)", "China Standard (WGOC)");

    fun displayLabel(isZh: Boolean): String = if (isZh) label else labelEn
}

object BmiCalc {
    fun getBmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0.0 || weightKg <= 0.0) return 0.0
        return weightKg / (heightCm / 100.0).pow(2)
    }

    fun getNutritionalStatus(
        bmi: Double,
        standard: BmiStandard = if (Locale.getDefault().language == "zh") BmiStandard.CHINA else BmiStandard.WHO
    ): NutritionalStatus = when (standard) {
        BmiStandard.WHO -> when {
            bmi < 18.5 -> NutritionalStatus.UNDERWEIGHT
            bmi < 25.0 -> NutritionalStatus.NORMAL
            bmi < 30.0 -> NutritionalStatus.PRE_OBESITY
            bmi < 35.0 -> NutritionalStatus.OBESITY_I
            bmi < 40.0 -> NutritionalStatus.OBESITY_II
            else -> NutritionalStatus.OBESITY_III
        }
        BmiStandard.CHINA -> when {
            bmi < 18.5 -> NutritionalStatus.UNDERWEIGHT
            bmi < 24.0 -> NutritionalStatus.NORMAL
            bmi < 28.0 -> NutritionalStatus.PRE_OBESITY
            bmi < 32.0 -> NutritionalStatus.OBESITY_I
            bmi < 38.0 -> NutritionalStatus.OBESITY_II
            else -> NutritionalStatus.OBESITY_III
        }
    }

    enum class NutritionalStatus {
        UNDERWEIGHT, NORMAL, PRE_OBESITY, OBESITY_I, OBESITY_II, OBESITY_III;

        fun getLabel(
            standard: BmiStandard = if (Locale.getDefault().language == "zh") BmiStandard.CHINA else BmiStandard.WHO,
            isZh: Boolean = Locale.getDefault().language == "zh"
        ): String = when (standard) {
            BmiStandard.CHINA -> when (this) {
                UNDERWEIGHT -> if (isZh) "体重过低" else "Underweight"
                NORMAL -> if (isZh) "正常" else "Normal"
                PRE_OBESITY -> if (isZh) "超重" else "Overweight"
                OBESITY_I -> if (isZh) "肥胖 I 级" else "Obesity I"
                OBESITY_II -> if (isZh) "肥胖 II 级" else "Obesity II"
                OBESITY_III -> if (isZh) "肥胖 III 级" else "Obesity III"
            }
            BmiStandard.WHO -> when (this) {
                UNDERWEIGHT -> if (isZh) "体重过低" else "Underweight"
                NORMAL -> if (isZh) "正常体重" else "Normal Weight"
                PRE_OBESITY -> if (isZh) "超重前期" else "Pre-obesity"
                OBESITY_I -> if (isZh) "肥胖 I 级" else "Obesity Class I"
                OBESITY_II -> if (isZh) "肥胖 II 级" else "Obesity Class II"
                OBESITY_III -> if (isZh) "肥胖 III 级" else "Obesity Class III"
            }
        }

        val label: String
            get() = getLabel()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 年龄计算
// ══════════════════════════════════════════════════════════════════════════════

object AgeCalc {
    fun getAge(birthday: LocalDate, referenceDate: LocalDate = LocalDate.now()): Int =
        Period.between(birthday, referenceDate).years.coerceAtLeast(0)
}

// ══════════════════════════════════════════════════════════════════════════════
// PAL (体力活动水平) 计算
// 来源: IOM 2004 - Brooks et al. PMID: 15113740
// 来源: IOM 2005, p.204 - DOI: 10.17226/10490
// ══════════════════════════════════════════════════════════════════════════════

object PalCalc {
    fun getPALValue(level: ActivityLevel): Double = when (level) {
        ActivityLevel.SEDENTARY -> 1.25
        ActivityLevel.LOW_ACTIVE -> 1.5
        ActivityLevel.ACTIVE -> 1.75
        ActivityLevel.VERY_ACTIVE -> 2.2
    }

    fun getPaCoefficient(palValue: Double, isMaleFormula: Boolean): Double = when {
        palValue < 1.4 -> 1.0
        palValue < 1.6 -> if (isMaleFormula) 1.12 else 1.14
        palValue < 1.9 -> 1.27
        else -> if (isMaleFormula) 1.54 else 1.45
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TDEE (每日总能量消耗) 计算
// 来源: IOM 2005, p.204 - DOI: 10.17226/10490
// ══════════════════════════════════════════════════════════════════════════════

object TdeeCalc {
    fun getTdeeKcal(user: User, referenceDate: LocalDate = LocalDate.now()): Double {
        val age = AgeCalc.getAge(user.birthday, referenceDate).toDouble()
        val palValue = PalCalc.getPALValue(user.activityLevel)

        fun maleFormula(): Double {
            val pa = PalCalc.getPaCoefficient(palValue, isMaleFormula = true)
            return 864 - 9.72 * age + pa * 14.2 * user.weightKg + 503 * (user.heightCm / 100.0)
        }

        fun femaleFormula(): Double {
            val pa = PalCalc.getPaCoefficient(palValue, isMaleFormula = false)
            return 387 - 7.31 * age + pa * 10.9 * user.weightKg + 660.7 * (user.heightCm / 100.0)
        }

        return when (user.gender) {
            Gender.MALE -> maleFormula()
            Gender.FEMALE -> femaleFormula()
            Gender.NON_BINARY -> when (user.caloriesProfile) {
                CaloriesProfile.TESTOSTERONE_TYPICAL -> maleFormula()
                CaloriesProfile.ESTROGEN_TYPICAL -> femaleFormula()
                CaloriesProfile.AVERAGED -> (maleFormula() + femaleFormula()) / 2.0
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 卡路里目标计算
// ══════════════════════════════════════════════════════════════════════════════

object CalorieGoalCalc {
    const val LOSE_KCAL = -500.0
    const val GAIN_KCAL = 500.0
    private const val KCAL_PER_KG_PER_WEEK_DAILY = 1100.0
    private const val TAPER_START_KG = 5.0
    private const val TAPER_END_KG = 1.0
    const val MIN_KCAL_MALE = 1500.0
    const val MIN_KCAL_FEMALE = 1200.0

    fun getKcalGoalAdjustment(goal: WeightGoal, weeklyWeightGoalKg: Double? = null): Double {
        if (weeklyWeightGoalKg != null) {
            val magnitude = weeklyWeightGoalKg * KCAL_PER_KG_PER_WEEK_DAILY
            return when (goal) {
                WeightGoal.LOSE -> -magnitude
                WeightGoal.GAIN -> magnitude
                WeightGoal.MAINTAIN -> 0.0
            }
        }
        return when (goal) {
            WeightGoal.LOSE -> LOSE_KCAL
            WeightGoal.GAIN -> GAIN_KCAL
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
        if (!taperEnabled || targetWeightKg == null || goal == WeightGoal.MAINTAIN || baseAdjustment == 0.0) {
            return baseAdjustment
        }
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
        userKcalAdjustment: Double = 0.0,
        totalKcalActivities: Double = 0.0,
        referenceDate: LocalDate = LocalDate.now()
    ): Double {
        val tdee = TdeeCalc.getTdeeKcal(user, referenceDate)
        val baseAdj = getKcalGoalAdjustment(user.weightGoal, user.weeklyWeightGoalKg)
        val adj = applyTargetWeightTaper(baseAdj, user.weightKg, user.targetWeightKg, user.weightGoal, user.caloriesTaperEnabled)
        return (tdee + adj + userKcalAdjustment + totalKcalActivities).coerceAtLeast(0.0)
    }

    fun isBelowRecommendedFloor(goalKcal: Double, user: User): Boolean =
        goalKcal < recommendedFloor(user)

    fun recommendedFloor(user: User): Double {
        val usesMaleFloor = user.gender == Gender.MALE ||
            (user.gender == Gender.NON_BINARY && user.caloriesProfile == CaloriesProfile.TESTOSTERONE_TYPICAL)
        return if (usesMaleFloor) MIN_KCAL_MALE else MIN_KCAL_FEMALE
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 宏量营养素目标计算
// ══════════════════════════════════════════════════════════════════════════════

object MacroCalc {
    const val DEFAULT_CARB_PCT = 0.60
    const val DEFAULT_FAT_PCT = 0.25
    const val DEFAULT_PROTEIN_PCT = 0.15

    fun getCarbsGoal(totalKcal: Double, pct: Double = DEFAULT_CARB_PCT): Double = (totalKcal * pct) / 4.0
    fun getFatGoal(totalKcal: Double, pct: Double = DEFAULT_FAT_PCT): Double = (totalKcal * pct) / 9.0
    fun getProteinGoal(totalKcal: Double, pct: Double = DEFAULT_PROTEIN_PCT): Double = (totalKcal * pct) / 4.0
}

// ══════════════════════════════════════════════════════════════════════════════
// MET 活动消耗计算
// ══════════════════════════════════════════════════════════════════════════════

object MetCalc {
    fun getBurnedKcal(weightKg: Double, mets: Double, durationMinutes: Double): Double =
        (mets * weightKg * durationMinutes / 60.0).coerceAtLeast(0.0)
}

// ══════════════════════════════════════════════════════════════════════════════
// 单位换算与显示格式化
// ══════════════════════════════════════════════════════════════════════════════

object UnitCalc {
    private const val KCAL_TO_KJ = 4.184
    private const val CM_PER_INCH = 2.54
    private const val LBS_PER_KG = 2.20462

    fun kcalToKj(kcal: Double): Double = kcal * KCAL_TO_KJ
    fun kjToKcal(kj: Double): Double = kj / KCAL_TO_KJ
    fun cmToInches(cm: Double): Double = cm / CM_PER_INCH
    fun inchesToCm(inches: Double): Double = inches * CM_PER_INCH
    fun kgToLbs(kg: Double): Double = kg * LBS_PER_KG
    fun lbsToKg(lbs: Double): Double = lbs / LBS_PER_KG

    fun energyUnitLabel(useKj: Boolean): String = if (useKj) "kJ" else "kcal"
    fun weightUnitLabel(useImperial: Boolean): String = if (useImperial) "lbs" else "kg"
    fun heightUnitLabel(useImperial: Boolean): String = if (useImperial) "in" else "cm"

    fun kcalToDisplayInt(kcal: Double, useKj: Boolean): Int =
        (if (useKj) kcalToKj(kcal) else kcal).roundToInt()

    fun displayEnergyToKcal(value: Double, useKj: Boolean): Double =
        if (useKj) kjToKcal(value) else value

    fun formatEnergy(kcal: Double, useKj: Boolean): String =
        "${kcalToDisplayInt(kcal, useKj)} ${energyUnitLabel(useKj)}"

    fun formatWeight(kg: Double, useImperial: Boolean): String =
        if (useImperial) String.format(Locale.US, "%.1f lbs", kgToLbs(kg))
        else String.format(Locale.US, "%.1f kg", kg)

    fun formatWeightNumber(kg: Double, useImperial: Boolean): String =
        if (useImperial) String.format(Locale.US, "%.1f", kgToLbs(kg))
        else String.format(Locale.US, "%.1f", kg)

    fun formatHeight(cm: Double, useImperial: Boolean): String =
        if (useImperial) String.format(Locale.US, "%.1f in", cmToInches(cm))
        else String.format(Locale.US, "%.1f cm", cm)

    fun formatHeightNumber(cm: Double, useImperial: Boolean): String =
        if (useImperial) String.format(Locale.US, "%.1f", cmToInches(cm))
        else String.format(Locale.US, "%.1f", cm)

    fun formatDecimal(value: Double, decimals: Int = 1): String =
        String.format(Locale.US, "%.${decimals}f", value)
}
