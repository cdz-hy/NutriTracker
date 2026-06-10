package com.example.nutritracker.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddMeal: (Int) -> Unit,
    onNavigateToAddActivity: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Filled.Add, "添加") },
                text = { Text("记录") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "首页",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Calorie overview
            item {
                CalorieOverviewCard(
                    goal = state.calorieGoal,
                    supplied = state.caloriesSupplied,
                    burned = state.caloriesBurned,
                    left = state.caloriesLeft,
                    isBelowFloor = state.isBelowKcalFloor,
                    floor = state.recommendedFloor
                )
            }

            // Macro progress
            item {
                MacroProgressRow(
                    carbsCurrent = state.carbsTracked,
                    carbsGoal = state.carbsGoal,
                    fatCurrent = state.fatTracked,
                    fatGoal = state.fatGoal,
                    proteinCurrent = state.proteinTracked,
                    proteinGoal = state.proteinGoal
                )
            }

            // Water intake
            item {
                WaterCard(
                    currentMl = state.waterMl,
                    goalMl = state.waterGoalMl,
                    onAdd = { vm.addWater(250) }
                )
            }

            // Meal sections
            items(
                state.mealSections.filter {
                    it.type != IntakeType.SNACK || it.intakes.isNotEmpty()
                }
            ) { section ->
                MealSectionCard(
                    section = section,
                    onAddClick = { onNavigateToAddMeal(section.type.ordinal) },
                    onDeleteIntake = { vm.deleteIntake(it) }
                )
            }

            // Activities section
            if (state.activities.isNotEmpty()) {
                item {
                    Text(
                        text = "活动",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.activities) { activity ->
                    ActivityItem(
                        activity = activity,
                        onDelete = { vm.deleteActivity(activity) }
                    )
                }
            }

            // Bottom spacing for FAB
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Add bottom sheet
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "添加记录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Meal types
                IntakeType.entries.forEach { type ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = mealTypeLabel(type),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingContent = {
                            Icon(
                                mealTypeIcon(type),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddSheet = false
                                onNavigateToAddMeal(type.ordinal)
                            }
                            .padding(vertical = 4.dp),
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Activity
                ListItem(
                    headlineContent = {
                        Text(
                            text = "体育活动",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddSheet = false
                            onNavigateToAddActivity()
                        }
                        .padding(vertical = 4.dp),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            }
        }
    }
}

fun mealTypeLabel(type: IntakeType): String = when (type) {
    IntakeType.BREAKFAST -> "早餐"
    IntakeType.LUNCH -> "午餐"
    IntakeType.DINNER -> "晚餐"
    IntakeType.SNACK -> "零食"
}

fun mealTypeIcon(type: IntakeType) = when (type) {
    IntakeType.BREAKFAST -> Icons.Filled.FreeBreakfast
    IntakeType.LUNCH -> Icons.Filled.LunchDining
    IntakeType.DINNER -> Icons.Filled.DinnerDining
    IntakeType.SNACK -> Icons.Filled.Cookie
}
