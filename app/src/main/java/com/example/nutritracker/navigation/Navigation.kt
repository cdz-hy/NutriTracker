package com.example.nutritracker.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
    data object Home : Screen("home")
    data object Diary : Screen("diary")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object AddMeal : Screen("add_meal/{intakeTypeId}") {
        fun createRoute(intakeTypeId: Int) = "add_meal/$intakeTypeId"
    }
    data object CameraCapture : Screen("camera_capture?intakeTypeId={intakeTypeId}") {
        fun createRoute(intakeTypeId: Int = 0) = "camera_capture?intakeTypeId=$intakeTypeId"
    }
    data object MealEdit : Screen("meal_edit?mealId={mealId}&intakeTypeId={intakeTypeId}") {
        fun createRoute(mealId: Long = -1, intakeTypeId: Int = 0) =
            "meal_edit?mealId=$mealId&intakeTypeId=$intakeTypeId"
    }
    data object AddActivity : Screen("add_activity")
    data object WeightHistory : Screen("weight_history")
    data object Sources : Screen("sources")
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("首页", Icons.Filled.Home, Screen.Home.route),
    BottomNavItem("日记", Icons.Filled.CalendarMonth, Screen.Diary.route),
    BottomNavItem("我的", Icons.Filled.Person, Screen.Profile.route),
    BottomNavItem("设置", Icons.Filled.Settings, Screen.Settings.route)
)
