package com.example.nutritracker.util

import com.example.nutritracker.data.entity.CaloriesProfile
import com.example.nutritracker.data.entity.Gender
import com.example.nutritracker.data.entity.User
import java.time.LocalDate

enum class MicronutrientKey(
    val label: String,
    val labelEn: String,
    val unit: String,
    val excessMatters: Boolean,
    val basis: String
) {
    FIBER("膳食纤维", "Fiber", "g", excessMatters = false, basis = "AI"),
    SUGARS("糖", "Sugars", "g", excessMatters = true, basis = "DV"),
    SATURATED_FAT("饱和脂肪", "Sat. Fat", "g", excessMatters = true, basis = "DV"),
    SODIUM("钠", "Sodium", "mg", excessMatters = true, basis = "CDRR"),
    CHOLESTEROL("胆固醇", "Cholesterol", "mg", excessMatters = true, basis = "DV");

    fun displayLabel(isZh: Boolean): String = if (isZh) label else labelEn
}

data class MicronutrientTotals(
    val fiberG: Double = 0.0,
    val sugarsG: Double = 0.0,
    val saturatedFatG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val cholesterolMg: Double = 0.0
)

/**
 * IOM Dietary Reference Intakes & FDA Daily Values for micronutrient tracking.
 */
object DriReference {
    const val DEFAULT_SUGARS_LIMIT_G = 50.0
    const val DEFAULT_SAT_FAT_LIMIT_G = 20.0
    const val DEFAULT_SODIUM_LIMIT_MG = 2300.0
    const val DEFAULT_CHOLESTEROL_LIMIT_MG = 300.0

    fun getReferenceAmount(
        key: MicronutrientKey,
        user: User?,
        referenceDate: LocalDate = LocalDate.now()
    ): Double = when (key) {
        MicronutrientKey.FIBER -> getFiberTargetG(user, referenceDate)
        MicronutrientKey.SUGARS -> DEFAULT_SUGARS_LIMIT_G
        MicronutrientKey.SATURATED_FAT -> DEFAULT_SAT_FAT_LIMIT_G
        MicronutrientKey.SODIUM -> DEFAULT_SODIUM_LIMIT_MG
        MicronutrientKey.CHOLESTEROL -> DEFAULT_CHOLESTEROL_LIMIT_MG
    }

    private fun getFiberTargetG(user: User?, referenceDate: LocalDate): Double {
        if (user == null) return 28.0
        val age = AgeCalc.getAge(user.birthday, referenceDate)
        val maleTarget = if (age <= 50) 38.0 else 30.0
        val femaleTarget = if (age <= 50) 25.0 else 21.0
        return when (user.gender) {
            Gender.MALE -> maleTarget
            Gender.FEMALE -> femaleTarget
            Gender.NON_BINARY -> when (user.caloriesProfile) {
                CaloriesProfile.TESTOSTERONE_TYPICAL -> maleTarget
                CaloriesProfile.ESTROGEN_TYPICAL -> femaleTarget
                CaloriesProfile.AVERAGED -> (maleTarget + femaleTarget) / 2.0
            }
        }
    }
}
