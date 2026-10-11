package com.example.nutritracker.feature.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.nutritracker.data.dao.IntakeWithMeal
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.feature.camera.AnalysisTask
import com.example.nutritracker.feature.camera.TaskStatus
import com.example.nutritracker.ui.components.*
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.UnitCalc
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onNavigateToAddMeal: (Int, Long) -> Unit,
    onNavigateToAddActivity: (Long) -> Unit,
    onNavigateToSources: () -> Unit,
    onNavigateToCamera: (Int, Long) -> Unit,
    onNavigateToEdit: (Long, Long, Int, Long) -> Unit,
    onNavigateToLibrary: (Int, Long) -> Unit,
    rootNavController: NavController,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val aiTasks by vm.aiTasks.collectAsStateWithLifecycle()
    val aiActiveCount by vm.aiActiveCount.collectAsStateWithLifecycle()
    val pendingConfirmTasks by vm.aiPendingConfirmTasks.collectAsStateWithLifecycle()
    val failedTasks by vm.aiFailedTasks.collectAsStateWithLifecycle()
    val aiAnalysisError by vm.aiAnalysisError.collectAsStateWithLifecycle()
    val aiSuccessEvent by vm.aiSuccessEvent.collectAsStateWithLifecycle(initialValue = null)

    var showTextAiDialog by remember { mutableStateOf(false) }
    var showWaterDetail by remember { mutableStateOf(false) }
    var reviewingTask by remember { mutableStateOf<AnalysisTask?>(null) }

    val context = LocalContext.current

    // Auto-present review sheet when a new pending AI task arrives
    val firstPendingId = pendingConfirmTasks.firstOrNull()?.id
    LaunchedEffect(firstPendingId) {
        if (firstPendingId != null && reviewingTask == null) {
            reviewingTask = pendingConfirmTasks.firstOrNull()
        }
    }

    val selectedImageUrisStr = rootNavController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("selected_image_uris", null)?.collectAsStateWithLifecycle()
    val selectedImageNotes = rootNavController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("selected_image_notes", null)?.collectAsStateWithLifecycle()
    val returnedIntakeTypeId = rootNavController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<Int>("intake_type_id", 0)?.collectAsStateWithLifecycle()
    val returnedDateEpochDay = rootNavController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<Long?>("selected_date", null)?.collectAsStateWithLifecycle()

    LaunchedEffect(selectedImageUrisStr?.value) {
        val urisJson = selectedImageUrisStr?.value
        if (!urisJson.isNullOrBlank()) {
            val tId = returnedIntakeTypeId?.value ?: 0
            val intakeType = IntakeType.entries.getOrElse(tId) { IntakeType.BREAKFAST }
            val notes = selectedImageNotes?.value ?: ""
            val dateEpochDay = returnedDateEpochDay?.value ?: state.logicalToday.toEpochDay()
            val date = LocalDate.ofEpochDay(dateEpochDay)
            rootNavController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_image_uris")
            rootNavController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_image_notes")
            rootNavController.currentBackStackEntry?.savedStateHandle?.remove<Int>("intake_type_id")
            rootNavController.currentBackStackEntry?.savedStateHandle?.remove<Long>("selected_date")
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                val uriStrings: List<String> = Gson().fromJson(urisJson, listType)
                val uris = uriStrings.map { android.net.Uri.parse(it) }
                vm.analyzeAndCreateMeals(context, uris, intakeType, notes, date)
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(aiSuccessEvent) {
        aiSuccessEvent?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(aiAnalysisError) {
        aiAnalysisError?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            vm.clearAiError()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.onResumed()
        }
    }

    val logicalEpoch = state.logicalToday.toEpochDay()

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.ContentPadding,
                    end = Dimens.ContentPadding,
                    top = 8.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
            ) {
                // 顶部快捷操作条 (AI 一句话记餐 / 拍照识图 / 食物库与食谱)
                item(key = "quick_actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { showTextAiDialog = true },
                            label = { Text("AI 一句话记餐") },
                            leadingIcon = {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            }
                        )
                        AssistChip(
                            onClick = { onNavigateToCamera(IntakeType.LUNCH.ordinal, logicalEpoch) },
                            label = { Text("拍照识图") },
                            leadingIcon = {
                                Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                            }
                        )
                        AssistChip(
                            onClick = { onNavigateToLibrary(IntakeType.BREAKFAST.ordinal, logicalEpoch) },
                            label = { Text("食物库与食谱") },
                            leadingIcon = {
                                Icon(Icons.Filled.MenuBook, null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }

                // 卡路里总览
                item(key = "calorie_overview") {
                    StaggeredFadeIn(index = 0) {
                        CalorieOverviewCard(
                            title = "今日热量",
                            goal = state.calorieGoal,
                            supplied = state.caloriesSupplied,
                            burned = state.caloriesBurned,
                            left = state.caloriesLeft,
                            isBelowFloor = state.isBelowKcalFloor,
                            floor = state.recommendedFloor,
                            useKj = state.useKj,
                            onNavigateToSources = onNavigateToSources
                        )
                    }
                }

                // 宏量营养素
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

                // 饮水
                item(key = "water") {
                    StaggeredFadeIn(index = 2) {
                        WaterCard(
                            currentMl = state.waterMl,
                            goalMl = state.waterGoalMl,
                            quickAmounts = state.waterQuickAmounts,
                            onAddAmount = { vm.addWater(it) },
                            onUndo = { vm.undoLastWaterIntake() },
                            onOpenDetail = { showWaterDetail = true }
                        )
                    }
                }

                // 四餐分区
                var animIdx = 3
                IntakeType.entries.forEach { type ->
                    val section = state.mealSections.find { it.type == type }
                    val headerIdx = animIdx++

                    item(key = "meal_header_$type") {
                        StaggeredFadeIn(index = headerIdx) {
                            MealSectionHeader(
                                type = type,
                                totalKcal = section?.totalKcal ?: 0.0,
                                useKj = state.useKj,
                                onAddClick = { onNavigateToAddMeal(type.ordinal, logicalEpoch) },
                                onCameraClick = { onNavigateToCamera(type.ordinal, logicalEpoch) }
                            )
                        }
                    }

                    if (section != null && section.items.isNotEmpty()) {
                        val itemsStartIdx = animIdx
                        itemsIndexed(section.items, key = { _, item -> "intake_${item.intake.id}" }) { index, item ->
                            StaggeredFadeIn(
                                modifier = Modifier.animateItem(),
                                index = itemsStartIdx + index
                            ) {
                                MealIntakeItem(
                                    item = item,
                                    useKj = state.useKj,
                                    onEdit = {
                                        onNavigateToEdit(
                                            item.intake.id,
                                            item.meal.id,
                                            type.ordinal,
                                            logicalEpoch
                                        )
                                    },
                                    onDelete = { vm.deleteIntake(item.intake) }
                                )
                            }
                        }
                        animIdx += section.items.size
                    }
                }

                // 体育活动
                val activityHeaderIdx = animIdx++
                item(key = "activity_header") {
                    StaggeredFadeIn(index = activityHeaderIdx) {
                        ActivitySectionHeader(
                            totalKcal = state.activities.sumOf { it.burnedKcal },
                            useKj = state.useKj,
                            onAddClick = { onNavigateToAddActivity(logicalEpoch) }
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
                                useKj = state.useKj,
                                onDelete = { vm.deleteActivity(activity) }
                            )
                        }
                    }
                }
            }
        }

        // 非阻塞 AI 后台分析队列条 & 待确认/重试引导
        AiFloatingQueueBanner(
            activeCount = aiActiveCount,
            firstActiveTask = aiTasks.firstOrNull {
                it.status == TaskStatus.PENDING || it.status == TaskStatus.ANALYZING
            },
            pendingConfirmTasks = pendingConfirmTasks,
            failedTasks = failedTasks,
            onCancelActive = { vm.cancelAiTask(it) },
            onReviewPending = { reviewingTask = it },
            onRetryFailed = { vm.retryAiTask(it) },
            onDismissFailed = { vm.dismissAiTask(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showTextAiDialog) {
        QuickAiTextLogDialog(
            initialType = IntakeType.LUNCH,
            onDismiss = { showTextAiDialog = false },
            onSubmit = { desc, type ->
                vm.analyzeTextMeal(desc, type)
                showTextAiDialog = false
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

    reviewingTask?.let { task ->
        AiResultReviewDialog(
            task = task,
            useKj = state.useKj,
            onDismiss = { reviewingTask = null },
            onDiscard = {
                vm.dismissAiTask(task.id)
                reviewingTask = null
            },
            onConfirm = { mealName, type, date, mult, items, fallback, thumb ->
                vm.confirmAiTask(task.id, mealName, type, date, mult, items, fallback, thumb)
                reviewingTask = null
            }
        )
    }
}

@Composable
private fun MealSectionHeader(
    type: IntakeType,
    totalKcal: Double,
    useKj: Boolean,
    onAddClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onAddClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevationLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.CardInnerPaddingCompact, vertical = 10.dp),
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
                    text = UnitCalc.formatEnergy(totalKcal, useKj),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onCameraClick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "拍照识别",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            FilledIconButton(
                onClick = onAddClick,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加食物", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ActivitySectionHeader(
    totalKcal: Double,
    useKj: Boolean,
    onAddClick: () -> Unit
) {
    val burnColor = if (LocalUseDarkTheme.current) BurnColorDark else BurnColor
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onAddClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
                tint = burnColor
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
                    text = UnitCalc.formatEnergy(totalKcal, useKj),
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
                Icon(Icons.Filled.Add, contentDescription = "添加活动", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MealIntakeItem(
    item: IntakeWithMeal,
    useKj: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val intake = item.intake
    val meal = item.meal
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf<String?>(null) }
    val kcal = meal.energyKcal100 * intake.amount / 100.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.CardInnerPaddingCompact, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val thumbnailPath = meal.localImagePath
            if (thumbnailPath != null && File(thumbnailPath).exists()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(File(thumbnailPath))
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullScreenImage = thumbnailPath },
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
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
                text = UnitCalc.formatEnergy(kcal, useKj),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
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
            text = { Text("确定要删除 \"${meal.name}\" 吗？") },
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

    showFullScreenImage?.let { path ->
        FullScreenImageDialog(imagePath = path, onDismiss = { showFullScreenImage = null })
    }
}
