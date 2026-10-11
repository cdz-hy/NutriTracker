package com.example.nutritracker.feature.diary

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nutritracker.data.dao.IntakeWithMeal
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.ui.components.*
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.UnitCalc
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DiaryScreen(
    onNavigateToSources: () -> Unit,
    onNavigateToEdit: (Long, Long, Int, Long) -> Unit = { _, _, _, _ -> },
    onNavigateToAddMeal: (Int, Long) -> Unit = { _, _ -> },
    onNavigateToAddActivity: (Long) -> Unit = {},
    vm: DiaryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showWaterDetail by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "日记",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!state.isLogicalToday) {
                    TextButton(onClick = { vm.selectDate(LocalDate.now()) }) {
                        Text("回到今天")
                    }
                }
            }
        }

        // 日期选择器
        item(key = "date_selector") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
                    IconButton(onClick = { vm.selectDate(selectedDate.minusDays(1)) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "前一天",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    AnimatedContent(
                        targetState = selectedDate,
                        transitionSpec = {
                            val isForward = targetState > initialState
                            val enter = fadeIn(tween(M3Duration.Medium2, easing = M3Easing.Decelerate)) +
                                slideInHorizontally(
                                    initialOffsetX = { if (isForward) it / 4 else -it / 4 },
                                    animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate)
                                )
                            val exit = fadeOut(tween(M3Duration.Short3, easing = M3Easing.Accelerate)) +
                                slideOutHorizontally(
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

                    IconButton(onClick = { vm.selectDate(selectedDate.plusDays(1)) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "后一天",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            val overviewTitle = if (state.isLogicalToday) {
                "今日热量"
            } else {
                selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日热量"))
            }

            item(key = "calorie_overview") {
                StaggeredFadeIn(index = 0) {
                    CalorieOverviewCard(
                        title = overviewTitle,
                        goal = state.calorieGoal,
                        supplied = state.caloriesTracked,
                        burned = state.caloriesBurned,
                        left = state.calorieGoal - state.caloriesTracked,
                        isBelowFloor = state.isBelowKcalFloor,
                        floor = state.recommendedFloor,
                        useKj = state.useKj,
                        onNavigateToSources = onNavigateToSources
                    )
                }
            }

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

            // IOM 膳食营养参考面板 (DRI Panel)
            item(key = "dri_panel") {
                StaggeredFadeIn(index = 2) {
                    DriNutrientPanel(
                        dayTotals = state.dayMicros,
                        weeklyAvgTotals = state.weeklyAvgMicros,
                        user = state.user,
                        onNavigateToSources = onNavigateToSources
                    )
                }
            }

            item(key = "water_progress") {
                StaggeredFadeIn(index = 3) {
                    WaterCard(
                        currentMl = state.waterMl,
                        goalMl = state.waterGoalMl,
                        quickAmounts = state.waterQuickAmounts,
                        onAddAmount = { vm.addWater(it) },
                        onUndo = { vm.undoLastWater() },
                        onOpenDetail = { showWaterDetail = true }
                    )
                }
            }

            var sectionIdx = 4
            IntakeType.entries.forEach { type ->
                val intakesForType = state.intakes.filter { it.intake.intakeType == type }
                val headerIdx = sectionIdx++
                item(key = "section_header_$type") {
                    StaggeredFadeIn(index = headerIdx) {
                        DiaryMealHeader(
                            type = type,
                            totalKcal = intakesForType.sumOf { it.meal.energyKcal100 * it.intake.amount / 100.0 },
                            useKj = state.useKj,
                            onAddClick = { onNavigateToAddMeal(type.ordinal, selectedDate.toEpochDay()) }
                        )
                    }
                }
                if (intakesForType.isNotEmpty()) {
                    val itemsStartIdx = sectionIdx
                    itemsIndexed(intakesForType, key = { _, it -> "diary_intake_${it.intake.id}" }) { index, item ->
                        StaggeredFadeIn(
                            modifier = Modifier.animateItem(),
                            index = itemsStartIdx + index
                        ) {
                            DiaryIntakeCard(
                                item = item,
                                useKj = state.useKj,
                                onEdit = {
                                    onNavigateToEdit(
                                        item.intake.id,
                                        item.meal.id,
                                        type.ordinal,
                                        selectedDate.toEpochDay()
                                    )
                                },
                                onDelete = { vm.deleteIntake(item.intake) }
                            )
                        }
                    }
                    sectionIdx += intakesForType.size
                }
            }

            val activityHeaderIdx = sectionIdx++
            item(key = "activity_header") {
                StaggeredFadeIn(index = activityHeaderIdx) {
                    DiaryActivityHeader(
                        totalKcal = state.activities.sumOf { it.burnedKcal },
                        useKj = state.useKj,
                        onAddClick = { onNavigateToAddActivity(selectedDate.toEpochDay()) }
                    )
                }
            }
            if (state.activities.isNotEmpty()) {
                val activityStartIdx = sectionIdx
                itemsIndexed(state.activities, key = { _, it -> "diary_activity_${it.id}" }) { index, activity ->
                    StaggeredFadeIn(
                        modifier = Modifier.animateItem(),
                        index = activityStartIdx + index
                    ) {
                        ActivityItem(
                            activity = activity,
                            useKj = state.useKj,
                            onDelete = { vm.deleteActivity(activity) }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                vm.selectDate(it)
                showDatePicker = false
            }
        )
    }

    if (showWaterDetail) {
        WaterDetailDialog(
            entries = state.waterEntries,
            goalMl = state.waterGoalMl,
            quickAmounts = state.waterQuickAmounts,
            onAddWater = { vm.addWater(it) },
            onDeleteEntry = { vm.deleteWaterEntry(it) },
            onDismiss = { showWaterDetail = false }
        )
    }
}

@Composable
private fun DiaryMealHeader(
    type: IntakeType,
    totalKcal: Double,
    useKj: Boolean,
    onAddClick: () -> Unit
) {
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
            text = UnitCalc.formatEnergy(totalKcal, useKj),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onAddClick, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "添加",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DiaryActivityHeader(
    totalKcal: Double,
    useKj: Boolean,
    onAddClick: () -> Unit
) {
    val burnColor = if (LocalUseDarkTheme.current) BurnColorDark else BurnColor
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
            tint = burnColor
        )
        Text(
            text = "活动",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = UnitCalc.formatEnergy(totalKcal, useKj),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onAddClick, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "添加活动",
                modifier = Modifier.size(18.dp),
                tint = burnColor
            )
        }
    }
}

@Composable
private fun DiaryIntakeCard(
    item: IntakeWithMeal,
    useKj: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val intake = item.intake
    val meal = item.meal
    var showFullScreenImage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val kcal = meal.energyKcal100 * intake.amount / 100.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevationLow),
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            leadingContent = {
                val thumbnailPath = meal.localImagePath
                if (thumbnailPath != null && File(thumbnailPath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(thumbnailPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { showFullScreenImage = thumbnailPath },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            headlineContent = {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    text = "${intake.amount.roundToInt()}g · ${UnitCalc.formatEnergy(kcal, useKj)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这条记录？") },
            text = { Text("将删除 \"${meal.name}\" 的摄入记录") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    showFullScreenImage?.let { path ->
        FullScreenImageDialog(imagePath = path, onDismiss = { showFullScreenImage = null })
    }
}
