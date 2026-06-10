package com.example.nutritracker.util

import com.example.nutritracker.data.entity.*
import java.time.LocalDate
import java.time.Period
import kotlin.math.pow

// ══════════════════════════════════════════════════════════════════════════════
// BMI 计算
// 来源: WHO Europe - https://www.who.int/europe/news-room/fact-sheets/item/a-healthy-lifestyle---who-recommendations
// ══════════════════════════════════════════════════════════════════════════════

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

// ══════════════════════════════════════════════════════════════════════════════
// 年龄计算
// ══════════════════════════════════════════════════════════════════════════════

object AgeCalc {
    fun getAge(birthday: LocalDate): Int = Period.between(birthday, LocalDate.now()).years
}

// ══════════════════════════════════════════════════════════════════════════════
// PAL (体力活动水平) 计算
// 来源: IOM 2004 - Brooks et al. PMID: 15113740
// 来源: IOM 2005, p.204 - DOI: 10.17226/10490
// ══════════════════════════════════════════════════════════════════════════════

object PalCalc {
    /**
     * PAL 分类到数值的映射
     * 来源: IOM Physical Activity Recommendations 2004
     */
    fun getPALValue(level: ActivityLevel): Double = when (level) {
        ActivityLevel.SEDENTARY -> 1.25
        ActivityLevel.LOW_ACTIVE -> 1.5
        ActivityLevel.ACTIVE -> 1.75
        ActivityLevel.VERY_ACTIVE -> 2.2
    }

    /**
     * PAL 到 PA 系数的映射 (IOM 2005 TDEE 公式专用)
     * 来源: IOM 2005, p.204 - Dietary Reference Intakes for Energy
     *
     * | PAL 范围  | 男性 PA | 女性 PA |
     * |-----------|---------|---------|
     * | < 1.4     | 1.0     | 1.0     |
     * | 1.4-1.6   | 1.12    | 1.14    |
     * | 1.6-1.9   | 1.27    | 1.27    |
     * | >= 1.9    | 1.54    | 1.45    |
     */
    fun getPaCoefficient(palValue: Double, isMaleFormula: Boolean): Double = when {
        palValue < 1.4 -> 1.0
        palValue < 1.6 -> if (isMaleFormula) 1.12 else 1.14
        palValue < 1.9 -> 1.27
        else           -> if (isMaleFormula) 1.54 else 1.45
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TDEE (每日总能量消耗) 计算
// 来源: IOM 2005, p.204 - DOI: 10.17226/10490
// 公式: https://nap.nationalacademies.org/catalog/10490/dietary-reference-intakes-for-energy-carbohydrate-fiber-fat-fatty-acids-cholesterol-protein-and-amino-acids
// ══════════════════════════════════════════════════════════════════════════════

object TdeeCalc {
    /**
     * IOM 2005 TDEE 公式
     *
     * 男性: TDEE = 864 - 9.72×年龄 + PA×14.2×体重(kg) + 503×(身高(m))
     * 女性: TDEE = 387 - 7.31×年龄 + PA×10.9×体重(kg) + 660.7×(身高(m))
     *
     * 非二元性别处理:
     * - averaged (默认): (男性公式 + 女性公式) / 2
     * - estrogenTypical: 使用女性公式
     * - testosteroneTypical: 使用男性公式
     */
    fun getTdeeKcal(user: User): Double {
        val age = AgeCalc.getAge(user.birthday).toDouble()
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
// 来源: 1kg 体脂 ≈ 7700 kcal, 每周 1kg = 每天 1100 kcal
// 最低卡路里: Harvard Health - https://www.health.harvard.edu/healthy-aging-and-longevity/calorie-counting-made-easy
// ══════════════════════════════════════════════════════════════════════════════

object CalorieGoalCalc {
    const val LOSE_KCAL = -500.0
    const val GAIN_KCAL = 500.0
    private const val KCAL_PER_KG_PER_WEEK_DAILY = 1100.0
    private const val TAPER_START_KG = 5.0
    private const val TAPER_END_KG = 1.0
    const val MIN_KCAL_MALE = 1500.0
    const val MIN_KCAL_FEMALE = 1200.0

    /**
     * 体重目标调整
     * 如果设置了每周目标(kg/周), 则: 
     * - 减重: 调整 = -每周目标 × 1100
     * - 增重: 调整 = +每周目标 × 1100
     * - 维持: 0
     * 否则使用固定预设值: 减重 -500, 增重 +500, 维持 0
     */
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
            WeightGoal.LOSE     -> LOSE_KCAL
            WeightGoal.GAIN     -> GAIN_KCAL
            WeightGoal.MAINTAIN -> 0.0
        }
    }

    /**
     * 目标体重渐近调整
     * 当启用渐近且设置了目标体重时:
     * - 距目标 ≥5kg: 全额调整
     * - 距目标 1-5kg: 线性插值 (从全额到零)
     * - 距目标 ≤1kg 或已达标: 调整为零 (进入维持)
     */
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

    /**
     * 每日卡路里目标
     * 公式: TDEE + 体重调整 + 用户手动偏移 + 今日额外运动消耗
     * 注意: TDEE 已包含活动水平系数 (PAL)
     */
    fun getTotalKcalGoal(
        user: User,
        userKcalAdjustment: Double = 0.0,
        totalKcalActivities: Double = 0.0
    ): Double {
        val tdee = TdeeCalc.getTdeeKcal(user)
        val baseAdj = getKcalGoalAdjustment(user.weightGoal, user.weeklyWeightGoalKg)
        val adj = applyTargetWeightTaper(baseAdj, user.weightKg, user.targetWeightKg, user.weightGoal, user.caloriesTaperEnabled)
        return tdee + adj + userKcalAdjustment + totalKcalActivities
    }

    /**
     * 是否低于推荐最低卡路里
     * 男性/睾酮典型: <1500 kcal
     * 女性/雌激素典型/平均: <1200 kcal
     */
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

// ══════════════════════════════════════════════════════════════════════════════
// 宏量营养素目标计算
// 来源: WHO Technical Report Series 894, p.104
// ISBN 92 4 120894 5, ISSN 0512-3054
//
// 能量密度: 碳水 4 kcal/g, 脂肪 9 kcal/g, 蛋白质 4 kcal/g
// 默认比例: 碳水 60%, 脂肪 25%, 蛋白质 15%
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
// 来源: 2024 Adult Compendium of Physical Activities - Herrmann et al.
// PubMed: https://pubmed.ncbi.nlm.nih.gov/38242596/
// 公式: 消耗(kcal) = MET × 体重(kg) × 时间(小时)
// ══════════════════════════════════════════════════════════════════════════════

object MetCalc {
    fun getBurnedKcal(weightKg: Double, mets: Double, durationMinutes: Double): Double =
        mets * weightKg * durationMinutes / 60.0
}

// ══════════════════════════════════════════════════════════════════════════════
// 单位换算
// ══════════════════════════════════════════════════════════════════════════════

object UnitCalc {
    fun kcalToKj(kcal: Double): Double = kcal * 4.184
    fun kjToKcal(kj: Double): Double = kj / 4.184
    fun cmToInches(cm: Double): Double = cm / 2.54
    fun inchesToCm(inches: Double): Double = inches * 2.54
    fun kgToLbs(kg: Double): Double = kg * 2.20462
    fun lbsToKg(lbs: Double): Double = lbs / 2.20462
}
