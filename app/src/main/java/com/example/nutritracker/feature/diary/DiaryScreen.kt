package com.example.nutritracker.feature.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.feature.home.mealTypeIcon
import com.example.nutritracker.feature.home.mealTypeLabel
import com.example.nutritracker.ui.components.CalorieOverviewCard
import com.example.nutritracker.ui.components.MacroProgressRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(vm: DiaryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(selectedDate) { vm.loadDay(selectedDate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Text(
            text = "日记",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Date selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "前一天",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "后一天",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Calorie overview
                item {
                    CalorieOverviewCard(
                        goal = state.calorieGoal,
                        supplied = state.caloriesTracked,
                        burned = state.caloriesBurned,
                        left = state.calorieGoal - state.caloriesTracked,
                        isBelowFloor = false,
                        floor = 0.0
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

                // Grouped by intake type
                IntakeType.entries.forEach { type ->
                    val intakesForType = state.intakes.filter { it.intakeType == type }
                    if (intakesForType.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    mealTypeIcon(type),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = mealTypeLabel(type),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${intakesForType.sumOf { intake ->
                                        val meal = state.meals[intake.mealId]
                                        (meal?.energyKcal100 ?: 0.0) * intake.amount / 100.0
                                    }.roundToInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(intakesForType) { intake ->
                            val meal = state.meals[intake.mealId]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = meal?.name ?: "未知",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "${intake.amount.roundToInt()}g · ${(meal?.energyKcal100?.times(intake.amount)?.div(100))?.roundToInt()} kcal",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                )
                            }
                        }
                    }
                }

                // Activities
                if (state.activities.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.DirectionsRun,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "活动",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${state.activities.sumOf { it.burnedKcal }.roundToInt()} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(state.activities) { activity ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = activity.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = "${activity.durationMinutes.roundToInt()}分钟 · ${activity.burnedKcal.roundToInt()} kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
