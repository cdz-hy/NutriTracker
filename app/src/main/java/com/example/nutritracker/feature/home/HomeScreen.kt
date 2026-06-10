package com.example.nutritracker.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.ui.components.*
import com.example.nutritracker.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddMeal: (Int) -> Unit,
    onNavigateToAddActivity: () -> Unit,
    onNavigateToSources: () -> Unit,
    onNavigateToCamera: (Int) -> Unit,
    rootNavController: NavController,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val aiIsAnalyzing by vm.aiIsAnalyzing.collectAsStateWithLifecycle()
    val aiAnalysisError by vm.aiAnalysisError.collectAsStateWithLifecycle()
    val aiSuccessEvent by vm.aiSuccessEvent.collectAsStateWithLifecycle(initialValue = null)

    val context = LocalContext.current

    val selectedImageUriStr = rootNavController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("selected_image_uri", null)?.collectAsStateWithLifecycle()
    val returnedIntakeTypeId = rootNavController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<Int>("intake_type_id", 0)?.collectAsStateWithLifecycle()

    LaunchedEffect(selectedImageUriStr?.value) {
        val uriStr = selectedImageUriStr?.value
        if (!uriStr.isNullOrBlank()) {
            val tId = returnedIntakeTypeId?.value ?: 0
            val intakeType = IntakeType.entries.getOrElse(tId) { IntakeType.BREAKFAST }
            rootNavController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_image_uri")
            rootNavController.currentBackStackEntry?.savedStateHandle?.remove<Int>("intake_type_id")
            vm.analyzeAndCreateMeals(context, android.net.Uri.parse(uriStr), intakeType)
        }
    }

    LaunchedEffect(aiSuccessEvent) {
        aiSuccessEvent?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(aiAnalysisError) {
        aiAnalysisError?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
            vm.clearAiError()
        }
    }

    // 每次页面可见时刷新数据
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.refresh()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.ContentPadding,
                    end = Dimens.ContentPadding,
                    top = 8.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
            ) {
        // ════════════════════════════════════════════════════════════════════
        // 卡路里总览卡片
        // ════════════════════════════════════════════════════════════════════
        item(key = "calorie_overview") {
            StaggeredAnimatedItem(index = 0) {
                CalorieOverviewCard(
                    goal = state.calorieGoal,
                    supplied = state.caloriesSupplied,
                    burned = state.caloriesBurned,
                    left = state.caloriesLeft,
                    isBelowFloor = state.isBelowKcalFloor,
                    floor = state.recommendedFloor,
                    onNavigateToSources = onNavigateToSources
                )
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 宏量营养素进度
        // ════════════════════════════════════════════════════════════════════
        item(key = "macro_progress") {
            StaggeredAnimatedItem(index = 1) {
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

        // ════════════════════════════════════════════════════════════════════
        // 饮水记录
        // ════════════════════════════════════════════════════════════════════
        item(key = "water") {
            StaggeredAnimatedItem(index = 2) {
                WaterCard(
                    currentMl = state.waterMl,
                    goalMl = state.waterGoalMl,
                    onAdd = { vm.addWater(250) },
                    onUndo = { vm.undoLastWaterIntake() }
                )
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 四餐分区 (早餐/午餐/晚餐/零食)
        // ════════════════════════════════════════════════════════════════════
        val mealTypes = listOf(IntakeType.BREAKFAST, IntakeType.LUNCH, IntakeType.DINNER, IntakeType.SNACK)

        mealTypes.forEachIndexed { mealIdx, type ->
            val section = state.mealSections.find { it.type == type }

            item(key = "meal_header_$type") {
                StaggeredAnimatedItem(index = 3 + mealIdx) {
                    MealSectionHeader(
                        type = type,
                        totalKcal = section?.totalKcal ?: 0.0,
                        onAddClick = { onNavigateToAddMeal(type.ordinal) },
                        onCameraClick = { onNavigateToCamera(type.ordinal) }
                    )
                }
            }

            if (section != null && section.intakes.isNotEmpty()) {
                items(section.intakes, key = { "intake_${it.id}" }) { intake ->
                    MealIntakeItem(
                        intake = intake,
                        meal = section.meals[intake.mealId],
                        onDelete = { vm.deleteIntake(intake) }
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 体育活动分区
        // ════════════════════════════════════════════════════════════════════
        item(key = "activity_header") {
            StaggeredAnimatedItem(index = 7) {
                ActivitySectionHeader(
                    totalKcal = state.activities.sumOf { it.burnedKcal },
                    onAddClick = onNavigateToAddActivity
                )
            }
        }

        if (state.activities.isNotEmpty()) {
            items(state.activities, key = { "activity_${it.id}" }) { activity ->
                ActivityItem(
                    activity = activity,
                    onDelete = { vm.deleteActivity(activity) }
                )
            }
        }
            }
        }

        // Glassmorphic background AI analyzing float card
        if (aiIsAnalyzing) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI 智能识别后台进行中...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "您可以继续浏览或记录其他项目",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 餐食分区头部
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MealSectionHeader(
    type: IntakeType,
    totalKcal: Double,
    onAddClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevationLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.CardInnerPaddingCompact, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                mealTypeIcon(type),
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconSizeMedium),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = mealTypeLabel(type),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (totalKcal > 0) {
                Text(
                    text = "${totalKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
            FilledIconButton(
                onClick = onCameraClick,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "添加",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 活动分区头部
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ActivitySectionHeader(
    totalKcal: Double,
    onAddClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevationLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.CardInnerPaddingCompact, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconSizeMedium),
                tint = BurnColor
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "体育活动",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (totalKcal > 0) {
                Text(
                    text = "${totalKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
            FilledIconButton(
                onClick = onAddClick,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "添加",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 餐食摄入条目
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MealIntakeItem(
    intake: com.example.nutritracker.data.entity.Intake,
    meal: com.example.nutritracker.data.entity.Meal?,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val kcal = (meal?.energyKcal100 ?: 0.0) * intake.amount / 100.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.CardInnerPaddingCompact, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图
            val thumbnailPath = meal?.localImagePath
            if (thumbnailPath != null && java.io.File(thumbnailPath).exists()) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(
                        coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(java.io.File(thumbnailPath))
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal?.name ?: "未知食物",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${intake.amount.roundToInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${kcal.roundToInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 空状态占位符
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyMealPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 辅助函数
// ══════════════════════════════════════════════════════════════════════════════

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
