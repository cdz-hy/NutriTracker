package com.example.nutritracker

import com.example.nutritracker.data.entity.*
import com.example.nutritracker.util.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CalculationsTest {

    private fun sampleUser(
        gender: Gender = Gender.MALE,
        weightKg: Double = 70.0,
        heightCm: Double = 175.0,
        activityLevel: ActivityLevel = ActivityLevel.LOW_ACTIVE,
        weightGoal: WeightGoal = WeightGoal.MAINTAIN,
        caloriesProfile: CaloriesProfile = CaloriesProfile.AVERAGED,
        weeklyWeightGoalKg: Double? = null,
        targetWeightKg: Double? = null,
        taperEnabled: Boolean = false
    ) = User(
        birthday = LocalDate.of(1995, 5, 15),
        heightCm = heightCm,
        weightKg = weightKg,
        gender = gender,
        activityLevel = activityLevel,
        weightGoal = weightGoal,
        caloriesProfile = caloriesProfile,
        weeklyWeightGoalKg = weeklyWeightGoalKg,
        targetWeightKg = targetWeightKg,
        caloriesTaperEnabled = taperEnabled
    )

    @Test
    fun `bmi calculation and WHO vs China thresholds`() {
        val bmi = BmiCalc.getBmi(75.0, 175.0) // ~24.49
        assertEquals(24.49, bmi, 0.05)

        // 24.49 is Normal in WHO (<25), Overweight in China (>=24)
        assertEquals(BmiCalc.NutritionalStatus.NORMAL, BmiCalc.getNutritionalStatus(bmi, BmiStandard.WHO))
        assertEquals(BmiCalc.NutritionalStatus.PRE_OBESITY, BmiCalc.getNutritionalStatus(bmi, BmiStandard.CHINA))
        assertEquals(0.0, BmiCalc.getBmi(70.0, 0.0), 0.001)
    }

    @Test
    fun `tdee and calorie goal with taper`() {
        val refDate = LocalDate.of(2026, 6, 1)
        val user = sampleUser(
            weightKg = 80.0,
            weightGoal = WeightGoal.LOSE,
            targetWeightKg = 77.0, // distance = 3kg -> 50% of taper range [1..5]
            taperEnabled = true
        )
        val tdee = TdeeCalc.getTdeeKcal(user, refDate)
        val goalWithTaper = CalorieGoalCalc.getTotalKcalGoal(user, referenceDate = refDate)
        // Expected adjustment = -500 * ((3 - 1) / (5 - 1)) = -250
        assertEquals(tdee - 250.0, goalWithTaper, 0.1)

        // Within 1kg of target -> taper adjustment reaches 0
        val nearTarget = user.copy(weightKg = 77.8)
        val nearTargetTdee = TdeeCalc.getTdeeKcal(nearTarget, refDate)
        assertEquals(nearTargetTdee, CalorieGoalCalc.getTotalKcalGoal(nearTarget, referenceDate = refDate), 0.1)
    }

    @Test
    fun `recommended floor handles non-binary profiles accurately`() {
        val nbAvg = sampleUser(gender = Gender.NON_BINARY, caloriesProfile = CaloriesProfile.AVERAGED)
        val nbTest = sampleUser(gender = Gender.NON_BINARY, caloriesProfile = CaloriesProfile.TESTOSTERONE_TYPICAL)
        val female = sampleUser(gender = Gender.FEMALE, caloriesProfile = CaloriesProfile.TESTOSTERONE_TYPICAL)

        assertEquals(1200.0, CalorieGoalCalc.recommendedFloor(nbAvg), 0.01)
        assertEquals(1500.0, CalorieGoalCalc.recommendedFloor(nbTest), 0.01)
        assertEquals(1200.0, CalorieGoalCalc.recommendedFloor(female), 0.01)
    }

    @Test
    fun `unit conversions round trip cleanly`() {
        assertEquals(100.0, UnitCalc.kjToKcal(UnitCalc.kcalToKj(100.0)), 0.001)
        assertEquals(70.0, UnitCalc.lbsToKg(UnitCalc.kgToLbs(70.0)), 0.001)
        assertEquals(175.0, UnitCalc.inchesToCm(UnitCalc.cmToInches(175.0)), 0.001)
        assertEquals("418 kJ", UnitCalc.formatEnergy(100.0, useKj = true))
    }

    @Test
    fun `dri fiber reference varies by age and gender`() {
        val youngMale = sampleUser(gender = Gender.MALE)
        val youngFemale = sampleUser(gender = Gender.FEMALE)
        val refDate = LocalDate.of(2026, 6, 1)

        assertEquals(38.0, DriReference.getReferenceAmount(MicronutrientKey.FIBER, youngMale, refDate), 0.01)
        assertEquals(25.0, DriReference.getReferenceAmount(MicronutrientKey.FIBER, youngFemale, refDate), 0.01)
        assertEquals(2300.0, DriReference.getReferenceAmount(MicronutrientKey.SODIUM, youngMale, refDate), 0.01)
    }

    @Test
    fun `met calculation`() {
        assertEquals(280.0, MetCalc.getBurnedKcal(70.0, 8.0, 30.0), 0.01)
    }
}
