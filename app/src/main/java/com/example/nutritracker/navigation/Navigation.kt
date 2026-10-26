package com.example.nutritracker.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
    data object Home : Screen("home")
    data object Diary : Screen("diary")
    data object Analytics : Screen("analytics")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")

    data object AddMeal : Screen("add_meal/{intakeTypeId}?date={date}") {
        fun createRoute(intakeTypeId: Int, dateEpochDay: Long = LocalDate.now().toEpochDay()) =
            "add_meal/$intakeTypeId?date=$dateEpochDay"
    }

    data object CameraCapture : Screen("camera_capture?intakeTypeId={intakeTypeId}&date={date}") {
        fun createRoute(intakeTypeId: Int = 0, dateEpochDay: Long = LocalDate.now().toEpochDay()) =
            "camera_capture?intakeTypeId=$intakeTypeId&date=$dateEpochDay"
    }

    data object MealEdit : Screen("meal_edit?intakeId={intakeId}&mealId={mealId}&intakeTypeId={intakeTypeId}&date={date}") {
        fun createRoute(
            intakeId: Long = -1L,
            mealId: Long = -1L,
            intakeTypeId: Int = 0,
            dateEpochDay: Long = LocalDate.now().toEpochDay()
        ) = "meal_edit?intakeId=$intakeId&mealId=$mealId&intakeTypeId=$intakeTypeId&date=$dateEpochDay"
    }

    data object AddActivity : Screen("add_activity?date={date}") {
        fun createRoute(dateEpochDay: Long = LocalDate.now().toEpochDay()) =
            "add_activity?date=$dateEpochDay"
    }

    data object FoodLibrary : Screen("food_library?intakeTypeId={intakeTypeId}&date={date}") {
        fun createRoute(intakeTypeId: Int = 0, dateEpochDay: Long = LocalDate.now().toEpochDay()) =
            "food_library?intakeTypeId=$intakeTypeId&date=$dateEpochDay"
    }

    data object WeightHistory : Screen("weight_history")
    data object Sources : Screen("sources")
}

data class BottomNavItem(
    val labelZh: String,
    val labelEn: String,
    val icon: ImageVector,
    val route: String
) {
    fun label(isZh: Boolean): String = if (isZh) labelZh else labelEn
}

val bottomNavItems = listOf(
    BottomNavItem("首页", "Home", Icons.Filled.Home, Screen.Home.route),
    BottomNavItem("日记", "Diary", Icons.Filled.CalendarMonth, Screen.Diary.route),
    BottomNavItem("统计", "Stats", Icons.Filled.BarChart, Screen.Analytics.route),
    BottomNavItem("我的", "Profile", Icons.Filled.Person, Screen.Profile.route),
    BottomNavItem("设置", "Settings", Icons.Filled.Settings, Screen.Settings.route)
)
