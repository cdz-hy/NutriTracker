package com.example.nutritracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.nutritracker.data.entity.AppLanguage
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.feature.activity.AddActivityScreen
import com.example.nutritracker.feature.analytics.AnalyticsScreen
import com.example.nutritracker.feature.camera.CameraCaptureScreen
import com.example.nutritracker.feature.diary.DiaryScreen
import com.example.nutritracker.feature.home.HomeScreen
import com.example.nutritracker.feature.meal.AddMealScreen
import com.example.nutritracker.feature.meal.FoodLibraryScreen
import com.example.nutritracker.feature.meal.MealEditScreen
import com.example.nutritracker.feature.onboarding.OnboardingScreen
import com.example.nutritracker.feature.profile.ProfileScreen
import com.example.nutritracker.feature.profile.WeightHistoryScreen
import com.example.nutritracker.feature.settings.SettingsScreen
import com.example.nutritracker.feature.sources.SourcesScreen
import com.example.nutritracker.navigation.Screen
import com.example.nutritracker.navigation.bottomNavItems
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.LocalAppLanguage
import com.example.nutritracker.util.LocalIsChinese
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingCameraLaunch = mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntentExtras(intent)
        setContent {
            val rootVm: RootViewModel = hiltViewModel()
            val themeMode by rootVm.themeMode.collectAsStateWithLifecycle()
            val appLanguage by rootVm.appLanguage.collectAsStateWithLifecycle()
            val dynamicColor by rootVm.dynamicColorEnabled.collectAsStateWithLifecycle()

            var previousLanguage by remember { mutableStateOf<AppLanguage?>(null) }
            val isZh = LocaleHelper.isChinese(appLanguage)
            val localizedCtx = remember(appLanguage, resources.configuration) {
                LocaleHelper.wrapActivityContext(this@MainActivity, appLanguage)
            }
            LaunchedEffect(appLanguage) {
                if (previousLanguage != null && previousLanguage != appLanguage) {
                    LocaleHelper.persistSync(applicationContext, appLanguage)
                    NutriTrackerWidgetUpdater.enqueueUpdate(applicationContext)
                }
                previousLanguage = appLanguage
            }

            CompositionLocalProvider(
                LocalContext provides localizedCtx,
                LocalConfiguration provides localizedCtx.resources.configuration,
                LocalIsChinese provides isZh,
                LocalAppLanguage provides appLanguage
            ) {
                NutriTrackerTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor
                ) {
                    NutriTrackerNav(
                        rootVm = rootVm,
                        openCameraSignal = pendingCameraLaunch.value,
                        onConsumeCameraSignal = { pendingCameraLaunch.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentExtras(intent)
    }

    private fun handleIntentExtras(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_CAMERA, false) == true) {
            pendingCameraLaunch.value = true
            intent.removeExtra(EXTRA_OPEN_CAMERA)
        }
    }

    companion object {
        const val EXTRA_OPEN_CAMERA = "extra_open_camera"
    }
}

@Composable
fun NutriTrackerNav(
    rootVm: RootViewModel,
    openCameraSignal: Boolean,
    onConsumeCameraSignal: () -> Unit
) {
    val rootNav = rememberNavController()
    val onboardingDone by rootVm.onboardingDone.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (onboardingDone == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val startDest = if (onboardingDone == true) Screen.Main.route else Screen.Onboarding.route

    LaunchedEffect(openCameraSignal, onboardingDone) {
        if (openCameraSignal && onboardingDone == true) {
            val logicalDay = rootVm.currentLogicalDay()
            onConsumeCameraSignal()
            rootNav.navigate(Screen.CameraCapture.createRoute(IntakeType.suggestForTime().ordinal, logicalDay.toEpochDay()))
        }
    }

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
                    rootNav.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToSources = { rootNav.navigate(Screen.Sources.route) }
            )
        }

        composable(Screen.Main.route) {
            MainScaffold(rootNav)
        }

        composable(
            route = Screen.AddMeal.route,
            arguments = listOf(
                navArgument("intakeTypeId") { type = NavType.IntType },
                navArgument("date") { type = NavType.LongType; defaultValue = LocalDate.now().toEpochDay() }
            )
        ) { backStackEntry ->
            val typeId = backStackEntry.arguments?.getInt("intakeTypeId") ?: 0
            val dateEpochDay = backStackEntry.arguments?.getLong("date") ?: LocalDate.now().toEpochDay()
            AddMealScreen(
                intakeTypeId = typeId,
                dateEpochDay = dateEpochDay,
                onNavigateToCamera = { rootNav.navigate(Screen.CameraCapture.createRoute(typeId, dateEpochDay)) },
                onMealSaved = { rootNav.popBackStack() },
                onNavigateToEdit = { intakeId, mealId, tId ->
                    rootNav.navigate(Screen.MealEdit.createRoute(intakeId, mealId, tId, dateEpochDay))
                },
                onNavigateToLibrary = { tId, dEpoch ->
                    rootNav.navigate(Screen.FoodLibrary.createRoute(tId, dEpoch))
                },
                navController = rootNav
            )
        }

        composable(
            route = Screen.CameraCapture.route,
            arguments = listOf(
                navArgument("intakeTypeId") { type = NavType.IntType; defaultValue = 0 },
                navArgument("date") { type = NavType.LongType; defaultValue = LocalDate.now().toEpochDay() }
            )
        ) { backStackEntry ->
            val intakeTypeId = backStackEntry.arguments?.getInt("intakeTypeId") ?: 0
            val dateEpochDay = backStackEntry.arguments?.getLong("date") ?: LocalDate.now().toEpochDay()
            CameraCaptureScreen(
                onImageSelected = { uris, notes ->
                    val urisJson = Gson().toJson(uris.map { it.toString() })
                    rootNav.previousBackStackEntry?.savedStateHandle?.set("selected_image_uris", urisJson)
                    rootNav.previousBackStackEntry?.savedStateHandle?.set("selected_image_notes", notes)
                    rootNav.previousBackStackEntry?.savedStateHandle?.set("intake_type_id", intakeTypeId)
                    rootNav.previousBackStackEntry?.savedStateHandle?.set("selected_date", dateEpochDay)
                    rootNav.popBackStack()
                },
                onBack = { rootNav.popBackStack() }
            )
        }

        composable(
            route = Screen.MealEdit.route,
            arguments = listOf(
                navArgument("intakeId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("mealId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("intakeTypeId") { type = NavType.IntType; defaultValue = 0 },
                navArgument("date") { type = NavType.LongType; defaultValue = LocalDate.now().toEpochDay() }
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

        composable(
            route = Screen.AddActivity.route,
            arguments = listOf(
                navArgument("date") { type = NavType.LongType; defaultValue = LocalDate.now().toEpochDay() }
            )
        ) {
            AddActivityScreen(onBack = { rootNav.popBackStack() })
        }

        composable(
            route = Screen.FoodLibrary.route,
            arguments = listOf(
                navArgument("intakeTypeId") { type = NavType.IntType; defaultValue = 0 },
                navArgument("date") { type = NavType.LongType; defaultValue = LocalDate.now().toEpochDay() }
            )
        ) { backStackEntry ->
            val dateEpochDay = backStackEntry.arguments?.getLong("date") ?: LocalDate.now().toEpochDay()
            FoodLibraryScreen(
                onBack = { rootNav.popBackStack() },
                onEditMealTemplate = { mealId ->
                    rootNav.navigate(Screen.MealEdit.createRoute(intakeId = -1L, mealId = mealId, dateEpochDay = dateEpochDay))
                }
            )
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
fun MainScaffold(rootNav: NavHostController) {
    val tabNav = rememberNavController()
    val currentBackStack by tabNav.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { tab ->
                    val label = tab.label(LocalIsChinese.current)
                    NavigationBarItem(
                        icon = {
                            Icon(tab.icon, contentDescription = label, modifier = Modifier.size(24.dp))
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
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
                    onNavigateToAddMeal = { typeId, dateEpoch ->
                        rootNav.navigate(Screen.AddMeal.createRoute(typeId, dateEpoch))
                    },
                    onNavigateToAddActivity = { dateEpoch ->
                        rootNav.navigate(Screen.AddActivity.createRoute(dateEpoch))
                    },
                    onNavigateToSources = { rootNav.navigate(Screen.Sources.route) },
                    onNavigateToCamera = { typeId, dateEpoch ->
                        rootNav.navigate(Screen.CameraCapture.createRoute(typeId, dateEpoch))
                    },
                    onNavigateToEdit = { intakeId, mealId, typeId, dateEpoch ->
                        rootNav.navigate(Screen.MealEdit.createRoute(intakeId, mealId, typeId, dateEpoch))
                    },
                    onNavigateToLibrary = { typeId, dateEpoch ->
                        rootNav.navigate(Screen.FoodLibrary.createRoute(typeId, dateEpoch))
                    },
                    rootNavController = rootNav
                )
            }
            composable(Screen.Diary.route) {
                DiaryScreen(
                    onNavigateToSources = { rootNav.navigate(Screen.Sources.route) },
                    onNavigateToEdit = { intakeId, mealId, typeId, dateEpochDay ->
                        rootNav.navigate(Screen.MealEdit.createRoute(intakeId, mealId, typeId, dateEpochDay))
                    },
                    onNavigateToAddMeal = { typeId, dateEpochDay ->
                        rootNav.navigate(Screen.AddMeal.createRoute(typeId, dateEpochDay))
                    },
                    onNavigateToAddActivity = { dateEpochDay ->
                        rootNav.navigate(Screen.AddActivity.createRoute(dateEpochDay))
                    }
                )
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    onNavigateToWeightHistory = { rootNav.navigate(Screen.WeightHistory.route) },
                    onNavigateToSources = { rootNav.navigate(Screen.Sources.route) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToWeightHistory = { rootNav.navigate(Screen.WeightHistory.route) },
                    onNavigateToFoodLibrary = { rootNav.navigate(Screen.FoodLibrary.createRoute()) },
                    onNavigateToSources = { rootNav.navigate(Screen.Sources.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToSources = { rootNav.navigate(Screen.Sources.route) }
                )
            }
        }
    }
}
