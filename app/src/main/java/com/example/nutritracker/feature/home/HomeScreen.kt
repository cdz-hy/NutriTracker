package com.example.nutritracker.feature.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
    onNavigateToEdit: (Long, Int) -> Unit,
    rootNavController: NavController,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val aiIsAnalyzing by vm.aiIsAnalyzing.collectAsStateWithLifecycle()
    val aiAnalysisError by vm.aiAnalysisError.collectAsStateWithLifecycle()
    val aiSuccessEvent by vm.aiSuccessEvent.collectAsStateWithLifecycle(initialValue = null)

    val context = LocalContext.current

    val selectedImageUrisStr = rootNavController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("selected_image_uris", null)?.collectAsStateWithLifecycle()
    val returnedIntakeTypeId = rootNavController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<Int>("intake_type_id", 0)?.collectAsStateWithLifecycle()

    LaunchedEffect(selectedImageUrisStr?.value) {
        val urisJson = selectedImageUrisStr?.value
        if (!urisJson.isNullOrBlank()) {
            val tId = returnedIntakeTypeId?.value ?: 0
            val intakeType = IntakeType.entries.getOrElse(tId) { IntakeType.BREAKFAST }
            rootNavController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_image_uris")
            rootNavController.currentBackStackEntry?.savedStateHandle?.remove<Int>("intake_type_id")
            try {
                val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                val uriStrings: List<String> = com.google.gson.Gson().fromJson(urisJson, listType)
                val uris = uriStrings.map { android.net.Uri.parse(it) }
                vm.analyzeAndCreateMeals(context, uris, intakeType)
            } catch (e: Exception) {
                // Ignore parsing errors
            }
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
            StaggeredFadeIn(index = 0) {
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

        // ════════════════════════════════════════════════════════════════════
        // 饮水记录
        // ════════════════════════════════════════════════════════════════════
        item(key = "water") {
            StaggeredFadeIn(index = 2) {
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
        var animIdx = 3

        mealTypes.forEach { type ->
            val section = state.mealSections.find { it.type == type }
            val headerIdx = animIdx++

            item(key = "meal_header_$type") {
                StaggeredFadeIn(index = headerIdx) {
                    MealSectionHeader(
                        type = type,
                        totalKcal = section?.totalKcal ?: 0.0,
                        onAddClick = { onNavigateToAddMeal(type.ordinal) },
                        onCameraClick = { onNavigateToCamera(type.ordinal) }
                    )
                }
            }

            if (section != null && section.intakes.isNotEmpty()) {
                val itemsStartIdx = animIdx
                itemsIndexed(section.intakes, key = { _, intake -> "intake_${intake.id}" }) { index, intake ->
                    StaggeredFadeIn(
                        modifier = Modifier.animateItem(),
                        index = itemsStartIdx + index
                    ) {
                        MealIntakeItem(
                            intake = intake,
                            meal = section.meals[intake.mealId],
                            onEdit = { onNavigateToEdit(section.meals[intake.mealId]?.id ?: 0, section.type.ordinal) },
                            onDelete = { vm.deleteIntake(intake) }
                        )
                    }
                }
                animIdx += section.intakes.size
            }
        }
        // ════════════════════════════════════════════════════════════════════
        val activityHeaderIdx = animIdx++
        item(key = "activity_header") {
            StaggeredFadeIn(index = activityHeaderIdx) {
                ActivitySectionHeader(
                    totalKcal = state.activities.sumOf { it.burnedKcal },
                    onAddClick = onNavigateToAddActivity
                )
            }
        }

        if (state.activities.isNotEmpty()) {
            val activityStartIdx = animIdx
            itemsIndexed(state.activities, key = { _, activity -> "activity_${activity.id}" }) { index, activity ->
                StaggeredFadeIn(
                    modifier = Modifier.animateItem(),
                    index = activityStartIdx + index
                ) {
                    ActivityItem(
                        activity = activity,
                        onDelete = { vm.deleteActivity(activity) }
                    )
                }
            }
            animIdx += state.activities.size
        }
            }
        }

        // Glassmorphic background AI analyzing float card
        AnimatedVisibility(
            visible = aiIsAnalyzing,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate)
            ) + fadeIn(animationSpec = tween(M3Duration.Medium2)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate)
            ) + fadeOut(animationSpec = tween(M3Duration.Short3)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
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
            .bounceClick { onAddClick() },
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
            .bounceClick { onAddClick() },
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
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf<String?>(null) }
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
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .clickable { showFullScreenImage = thumbnailPath },
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

            onEdit?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

    if (showFullScreenImage != null) {
        FullScreenImageDialog(
            imagePath = showFullScreenImage!!,
            onDismiss = { showFullScreenImage = null }
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
