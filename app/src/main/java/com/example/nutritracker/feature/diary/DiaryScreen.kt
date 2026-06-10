package com.example.nutritracker.feature.diary

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.feature.home.mealTypeIcon
import com.example.nutritracker.feature.home.mealTypeLabel
import com.example.nutritracker.ui.components.*
import com.example.nutritracker.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onNavigateToSources: () -> Unit,
    vm: DiaryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) { vm.loadDay(selectedDate) }

    // 整体使用单一 LazyColumn，消除嵌套滚动问题
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        // ── 标题 ─────────────────────────────────────────────────────
        item(key = "header") {
            Text(
                text = "日记",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // ── 日期选择器 ───────────────────────────────────────────────
        item(key = "date_selector") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
                shape = MaterialTheme.shapes.large
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

                    // 日期文本带动画
                    AnimatedContent(
                        targetState = selectedDate,
                        transitionSpec = {
                            val isForward = targetState > initialState
                            val enter = fadeIn(tween(M3Duration.Medium2, easing = M3Easing.Decelerate)) + slideInHorizontally(
                                initialOffsetX = { if (isForward) it / 4 else -it / 4 },
                                animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate)
                            )
                            val exit = fadeOut(tween(M3Duration.Short3, easing = M3Easing.Accelerate)) + slideOutHorizontally(
                                targetOffsetX = { if (isForward) -it / 4 else it / 4 },
                                animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate)
                            )
                            enter togetherWith exit
                        },
                        label = "dateAnimation"
                    ) { date ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .bounceClick { showDatePicker = true }
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
        }

        // ── 加载状态 ─────────────────────────────────────────────────
        if (state.isLoading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // ── 卡路里总览 ───────────────────────────────────────────
            item(key = "calorie_overview") {
                StaggeredFadeIn(index = 0) {
                    CalorieOverviewCard(
                        goal = state.calorieGoal,
                        supplied = state.caloriesTracked,
                        burned = state.caloriesBurned,
                        left = state.calorieGoal - state.caloriesTracked,
                        isBelowFloor = false,
                        floor = 0.0,
                        onNavigateToSources = onNavigateToSources
                    )
                }
            }

            // ── 宏量营养素 ───────────────────────────────────────────
            item(key = "macro_progress") {
                StaggeredFadeIn(index = 1) {
                    MacroProgressRow(
                        carbsCurrent = state.carbsTracked,
                        carbsGoal = state.carbsGoal,
                        fatCurrent = state.fatTracked,
                        fatGoal = state.fatGoal,
                        proteinCurrent = state.proteinTracked,
                        proteinGoal = state.proteinGoal
                    )
                }
            }

            // ── 饮水记录 ─────────────────────────────────────────────
            item(key = "water_progress") {
                StaggeredFadeIn(index = 2) {
                    WaterCard(
                        currentMl = state.waterMl,
                        goalMl = state.waterGoalMl
                    )
                }
            }

            // ── 按餐类型分组 ─────────────────────────────────────────
            var sectionIdx = 3
            IntakeType.entries.forEach { type ->
                val intakesForType = state.intakes.filter { it.intakeType == type }
                if (intakesForType.isNotEmpty()) {
                    item(key = "section_header_$type") {
                        StaggeredFadeIn(index = sectionIdx++) {
                            DiaryMealHeader(
                                type = type,
                                totalKcal = intakesForType.sumOf { intake ->
                                    val meal = state.meals[intake.mealId]
                                    (meal?.energyKcal100 ?: 0.0) * intake.amount / 100.0
                                }
                            )
                        }
                    }
                    items(intakesForType, key = { "diary_intake_${it.id}" }) { intake ->
                        val meal = state.meals[intake.mealId]
                        Box(modifier = Modifier.animateItem()) {
                            DiaryIntakeCard(intake = intake, meal = meal)
                        }
                    }
                }
            }

            // ── 活动 ─────────────────────────────────────────────────
            if (state.activities.isNotEmpty()) {
                item(key = "activity_header") {
                    StaggeredFadeIn(index = sectionIdx++) {
                        DiaryActivityHeader(
                            totalKcal = state.activities.sumOf { it.burnedKcal }
                        )
                    }
                }
                items(state.activities, key = { "diary_activity_${it.id}" }) { activity ->
                    Box(modifier = Modifier.animateItem()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevationLow),
                            shape = MaterialTheme.shapes.medium
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
                                leadingContent = {
                                    Icon(
                                        Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = BurnColor,
                                        modifier = Modifier.size(Dimens.IconSizeMedium)
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 86400000L
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = LocalDate.ofEpochDay(millis / 86400000L)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// ── 日记餐食分组头 ──────────────────────────────────────────────────────────

@Composable
private fun DiaryMealHeader(type: IntakeType, totalKcal: Double) {
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
            modifier = Modifier.size(Dimens.IconSizeMedium),
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
            text = "${totalKcal.roundToInt()} kcal",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── 日记活动头 ──────────────────────────────────────────────────────────────

@Composable
private fun DiaryActivityHeader(totalKcal: Double) {
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
            modifier = Modifier.size(Dimens.IconSizeMedium),
            tint = BurnColor
        )
        Text(
            text = "活动",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${totalKcal.roundToInt()} kcal",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── 日记摄入卡片 ────────────────────────────────────────────────────────────

@Composable
private fun DiaryIntakeCard(
    intake: com.example.nutritracker.data.entity.Intake,
    meal: com.example.nutritracker.data.entity.Meal?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevationLow),
        shape = MaterialTheme.shapes.medium
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
