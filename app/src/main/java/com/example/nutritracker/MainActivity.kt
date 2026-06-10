package com.example.nutritracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.nutritracker.feature.home.HomeScreen
import com.example.nutritracker.feature.home.HomeViewModel
import com.example.nutritracker.feature.diary.DiaryScreen
import com.example.nutritracker.feature.profile.ProfileScreen
import com.example.nutritracker.feature.profile.WeightHistoryScreen
import com.example.nutritracker.feature.settings.SettingsScreen
import com.example.nutritracker.feature.onboarding.OnboardingScreen
import com.example.nutritracker.feature.meal.MealEditScreen
import com.example.nutritracker.feature.meal.AddMealScreen
import com.example.nutritracker.feature.camera.CameraCaptureScreen
import com.example.nutritracker.feature.activity.AddActivityScreen
import com.example.nutritracker.navigation.Screen
import com.example.nutritracker.ui.theme.NutriTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NutriTrackerTheme { NutriTrackerNav() } }
    }
}

@Composable
fun NutriTrackerNav() {
    val rootNav = rememberNavController()
    val vm: HomeViewModel = hiltViewModel()
    val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle(initialValue = false)
    val startDest = if (onboardingDone) "main" else Screen.Onboarding.route

    NavHost(navController = rootNav, startDestination = startDest) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onDone = {
                rootNav.navigate("main") {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable("main") { MainScaffold(rootNav) }
        composable(
            Screen.AddMeal.route,
            arguments = listOf(navArgument("intakeTypeId") { type = NavType.IntType })
        ) {
            val typeId = it.arguments?.getInt("intakeTypeId") ?: 0
            AddMealScreen(
                intakeTypeId = typeId,
                onNavigateToCamera = { rootNav.navigate(Screen.CameraCapture.route) },
                onMealSaved = { rootNav.popBackStack() },
                onNavigateToEdit = { mealId, tId ->
                    rootNav.navigate(Screen.MealEdit.createRoute(mealId, tId))
                },
                navController = rootNav
            )
        }
        composable(Screen.CameraCapture.route) {
            CameraCaptureScreen(
                onResult = { result ->
                    rootNav.previousBackStackEntry?.savedStateHandle?.set("nutrition_result", result)
                    rootNav.popBackStack()
                },
                onBack = { rootNav.popBackStack() }
            )
        }
        composable(
            Screen.MealEdit.route,
            arguments = listOf(
                navArgument("mealId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("intakeTypeId") { type = NavType.IntType; defaultValue = 0 }
            )
        ) {
            MealEditScreen(onBack = { rootNav.popBackStack() })
        }
        composable(Screen.AddActivity.route) {
            AddActivityScreen(onBack = { rootNav.popBackStack() })
        }
        composable(Screen.WeightHistory.route) {
            WeightHistoryScreen(onBack = { rootNav.popBackStack() })
        }
    }
}

@Composable
fun MainScaffold(rootNav: androidx.navigation.NavHostController) {
    val tabNav = rememberNavController()
    val currentBackStack by tabNav.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    data class TabDef(val label: String, val icon: ImageVector, val route: String)
    val tabs = listOf(
        TabDef("首页", Icons.Filled.Home, Screen.Home.route),
        TabDef("日记", Icons.Filled.CalendarMonth, Screen.Diary.route),
        TabDef("我的", Icons.Filled.Person, Screen.Profile.route),
        TabDef("设置", Icons.Filled.Settings, Screen.Settings.route)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNav.navigate(tab.route) {
                                popUpTo(tabNav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            tabNav,
            startDestination = Screen.Home.route,
            Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAddMeal = {
                        rootNav.navigate(Screen.AddMeal.createRoute(it))
                    },
                    onNavigateToAddActivity = {
                        rootNav.navigate(Screen.AddActivity.route)
                    }
                )
            }
            composable(Screen.Diary.route) { DiaryScreen() }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToWeightHistory = {
                        rootNav.navigate(Screen.WeightHistory.route)
                    }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
