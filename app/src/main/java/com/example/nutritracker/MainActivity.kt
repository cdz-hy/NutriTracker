package com.example.nutritracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.example.nutritracker.feature.sources.SourcesScreen
import com.example.nutritracker.navigation.Screen
import com.example.nutritracker.ui.theme.*
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
    val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle(initialValue = null)

    // 等待 DataStore 加载完成，避免闪屏
    if (onboardingDone == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val startDest = if (onboardingDone == true) "main" else Screen.Onboarding.route

    NavHost(
        navController = rootNav,
        startDestination = startDest,
        enterTransition = M3NavEnterTransition,
        exitTransition = M3NavExitTransition,
        popEnterTransition = M3NavPopEnterTransition,
        popExitTransition = M3NavPopExitTransition
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onDone = {
                    rootNav.navigate("main") {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToSources = {
                    rootNav.navigate(Screen.Sources.route)
                }
            )
        }
        composable("main") { MainScaffold(rootNav) }
        composable(
            Screen.AddMeal.route,
            arguments = listOf(navArgument("intakeTypeId") { type = NavType.IntType })
        ) {
            val typeId = it.arguments?.getInt("intakeTypeId") ?: 0
            AddMealScreen(
                intakeTypeId = typeId,
                onNavigateToCamera = { rootNav.navigate(Screen.CameraCapture.createRoute(typeId)) },
                onMealSaved = { rootNav.popBackStack() },
                onNavigateToEdit = { mealId, tId ->
                    rootNav.navigate(Screen.MealEdit.createRoute(mealId, tId))
                },
                navController = rootNav
            )
        }
        composable(
            route = Screen.CameraCapture.route,
            arguments = listOf(navArgument("intakeTypeId") { type = NavType.IntType; defaultValue = 0 })
        ) { backStackEntry ->
            val intakeTypeId = backStackEntry.arguments?.getInt("intakeTypeId") ?: 0
            CameraCaptureScreen(
                onImageSelected = { uris ->
                    val urisJson = com.google.gson.Gson().toJson(uris.map { it.toString() })
                    rootNav.previousBackStackEntry?.savedStateHandle?.set("selected_image_uris", urisJson)
                    rootNav.previousBackStackEntry?.savedStateHandle?.set("intake_type_id", intakeTypeId)
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
            MealEditScreen(
                onBack = { rootNav.popBackStack() },
                onSaveSuccess = {
                    rootNav.previousBackStackEntry?.savedStateHandle?.set("meal_edited", true)
                    rootNav.popBackStack()
                }
            )
        }
        composable(Screen.AddActivity.route) {
            AddActivityScreen(onBack = { rootNav.popBackStack() })
        }
        composable(Screen.WeightHistory.route) {
            WeightHistoryScreen(onBack = { rootNav.popBackStack() })
        }
        composable(Screen.Sources.route) {
            SourcesScreen(onBack = { rootNav.popBackStack() })
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
            navController = tabNav,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(animationSpec = tween(M3Duration.Medium1, easing = M3Easing.Standard)) },
            exitTransition = { fadeOut(animationSpec = tween(M3Duration.Short3, easing = M3Easing.Standard)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAddMeal = {
                        rootNav.navigate(Screen.AddMeal.createRoute(it))
                    },
                    onNavigateToAddActivity = {
                        rootNav.navigate(Screen.AddActivity.route)
                    },
                    onNavigateToSources = {
                        rootNav.navigate(Screen.Sources.route)
                    },
                    onNavigateToCamera = { intakeTypeId ->
                        rootNav.navigate(Screen.CameraCapture.createRoute(intakeTypeId))
                    },
                    onNavigateToEdit = { mealId, typeId ->
                        rootNav.navigate(Screen.MealEdit.createRoute(mealId, typeId))
                    },
                    rootNavController = rootNav
                )
            }
            composable(Screen.Diary.route) {
                DiaryScreen(
                    onNavigateToSources = {
                        rootNav.navigate(Screen.Sources.route)
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToWeightHistory = {
                        rootNav.navigate(Screen.WeightHistory.route)
                    },
                    onNavigateToSources = {
                        rootNav.navigate(Screen.Sources.route)
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToSources = {
                        rootNav.navigate(Screen.Sources.route)
                    }
                )
            }
        }
    }
}
