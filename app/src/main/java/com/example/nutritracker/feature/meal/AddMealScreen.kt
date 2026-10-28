package com.example.nutritracker.feature.meal

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nutritracker.data.dao.IntakeWithMeal
import com.example.nutritracker.data.dao.RecipeWithIngredients
import com.example.nutritracker.data.entity.Intake
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.data.entity.Meal
import com.example.nutritracker.feature.camera.AnalysisTask
import com.example.nutritracker.feature.camera.NutritionResult
import com.example.nutritracker.feature.camera.TaskStatus
import com.example.nutritracker.ui.components.*
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.LocalIsChinese
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.util.UnitCalc
import com.example.nutritracker.util.tr
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    intakeTypeId: Int,
    dateEpochDay: Long = LocalDate.now().toEpochDay(),
    onNavigateToCamera: () -> Unit,
    onMealSaved: () -> Unit,
    onNavigateToEdit: (Long, Long, Int) -> Unit,
    onNavigateToLibrary: (Int, Long) -> Unit,
    navController: NavController,
    vm: AddMealViewModel = hiltViewModel()
) {
    val intakeType = IntakeType.entries.getOrElse(intakeTypeId) { IntakeType.BREAKFAST }
    val targetDate = remember(dateEpochDay) { LocalDate.ofEpochDay(dateEpochDay) }

    LaunchedEffect(intakeType, targetDate) {
        vm.setTargetContext(intakeType, targetDate)
    }

    val todayIntakes by vm.todayIntakes.collectAsStateWithLifecycle()
    val recentIntakes by vm.recentIntakes.collectAsStateWithLifecycle()
    val librarySearchResults by vm.filteredLibraryMeals.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val useKj by vm.useKj.collectAsStateWithLifecycle()

    val aiTasks by vm.aiTasks.collectAsStateWithLifecycle()
    val aiActiveCount by vm.aiActiveCount.collectAsStateWithLifecycle()
    val pendingConfirmTasks by vm.pendingConfirmTasks.collectAsStateWithLifecycle()
    val failedTasks by vm.failedTasks.collectAsStateWithLifecycle()
    val analysisError by vm.analysisError.collectAsStateWithLifecycle()
    val analysisSuccess by vm.analysisSuccess.collectAsStateWithLifecycle(initialValue = null)

    var showManualAdd by remember { mutableStateOf(false) }
    var showTextAi by remember { mutableStateOf(false) }
    var reviewingTask by remember { mutableStateOf<AnalysisTask?>(null) }
    var quickLogMealTarget by remember { mutableStateOf<Meal?>(null) }
    var quickLogRecipeTarget by remember { mutableStateOf<RecipeWithIngredients?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val firstPendingId = pendingConfirmTasks.firstOrNull()?.id
    LaunchedEffect(firstPendingId) {
        if (firstPendingId != null && reviewingTask == null) {
            reviewingTask = pendingConfirmTasks.firstOrNull()
        }
    }

    val selectedImageUrisStr = navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("selected_image_uris", null)?.collectAsStateWithLifecycle()
    val selectedImageNotes = navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("selected_image_notes", null)?.collectAsStateWithLifecycle()
    val returnedDateEpochDay = navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<Long?>("selected_date", null)?.collectAsStateWithLifecycle()

    LaunchedEffect(selectedImageUrisStr?.value) {
        val urisJson = selectedImageUrisStr?.value
        if (!urisJson.isNullOrBlank()) {
            val notes = selectedImageNotes?.value ?: ""
            val camDateEpochDay = returnedDateEpochDay?.value ?: dateEpochDay
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_image_uris")
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_image_notes")
            navController.currentBackStackEntry?.savedStateHandle?.remove<Int>("intake_type_id")
            navController.currentBackStackEntry?.savedStateHandle?.remove<Long>("selected_date")
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                val uriStrings: List<String> = Gson().fromJson(urisJson, listType)
                val uris = uriStrings.map { Uri.parse(it) }
                vm.analyzeAndCreateMeals(context, uris, intakeType, notes, LocalDate.ofEpochDay(camDateEpochDay))
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(analysisSuccess) {
        analysisSuccess?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    LaunchedEffect(analysisError) {
        analysisError?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            vm.clearAnalysisError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = mealTypeLabel(intakeType),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = LocaleHelper.formatFullDate(targetDate, LocalIsChinese.current),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMealSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("返回", "Back"))
                    }
                },
                actions = {
                    TextButton(onClick = { onNavigateToLibrary(intakeTypeId, dateEpochDay) }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("食物库", "Library"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Three quick log modes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.ContentPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onNavigateToCamera,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("拍照识图", "Photo"), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { showTextAi = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("AI文字", "AI Text"), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { showManualAdd = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("手动/快记", "Manual"), maxLines = 1)
                    }
                }

                // Local food library & recipe search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { vm.setSearchQuery(it) },
                    placeholder = { Text(tr("搜索历史食物或自制食谱...", "Search saved foods or recipes...")) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.setSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, tr("清除", "Clear"))
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.ContentPadding, vertical = 4.dp)
                )

                // Search results or recent/recipes
                if (searchQuery.isNotBlank()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Dimens.ContentPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (recipes.isNotEmpty()) {
                            item {
                                Text(tr("匹配食谱", "Matching Recipes"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            items(recipes, key = { "rec_${it.recipe.id}" }) { r ->
                                RecipeQuickCard(r, useKj) { quickLogRecipeTarget = r }
                            }
                        }
                        if (librarySearchResults.isNotEmpty()) {
                            item {
                                Text(tr("食物库结果", "Food Library"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            items(librarySearchResults, key = { "lib_${it.id}" }) { m ->
                                LibraryQuickCard(m, useKj) { quickLogMealTarget = m }
                            }
                        }
                        if (recipes.isEmpty() && librarySearchResults.isEmpty()) {
                            item {
                                Text(
                                    text = tr(
                                        "未找到匹配的食物或食谱，可点击上方“AI文字”或“手动/快记”添加",
                                        "No matches found — try AI Text or Manual entry above"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        }
                    }
                } else {
                    if (recentIntakes.isNotEmpty()) {
                        RecentIntakesRow(
                            intakes = recentIntakes,
                            targetDate = targetDate,
                            useKj = useKj,
                            onSelect = { item ->
                                vm.quickAddIntake(item.meal, item.amount, intakeType, targetDate)
                            }
                        )
                    }

                    if (recipes.isNotEmpty()) {
                        RecipeChipsRow(
                            recipes = recipes,
                            onSelect = { quickLogRecipeTarget = it }
                        )
                    }

                    if (todayIntakes.isEmpty()) {
                        EmptyMealState(intakeType = intakeType)
                    } else {
                        val totalKcal = todayIntakes.sumOf { it.meal.energyKcal100 * it.intake.amount / 100.0 }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.ContentPadding, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("本餐总计", "Meal Total"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = UnitCalc.formatEnergy(totalKcal, useKj),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = Dimens.ContentPadding, end = Dimens.ContentPadding, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(todayIntakes, key = { it.intake.id }) { item ->
                                Box(modifier = Modifier.animateItem()) {
                                    MealIntakeCard(
                                        item = item,
                                        useKj = useKj,
                                        onEdit = { onNavigateToEdit(item.intake.id, item.meal.id, intakeTypeId) },
                                        onDelete = { vm.deleteIntake(item.intake) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
    }

    if (showManualAdd) {
        ManualAddDialog(
            useKj = useKj,
            onDismiss = { showManualAdd = false },
            onConfirm = { mode, name, kcal, carbs, fat, protein, weight, portionLabel, fiber, sugars, satFat, sodium, cholesterol ->
                vm.createManualMeal(
                    mode = mode,
                    name = name,
                    kcalInput = kcal,
                    carbsInput = carbs,
                    fatInput = fat,
                    proteinInput = protein,
                    weightGrams = weight,
                    portionLabel = portionLabel,
                    fiberInput = fiber,
                    sugarsInput = sugars,
                    satFatInput = satFat,
                    sodiumInput = sodium,
                    cholesterolInput = cholesterol,
                    intakeType = intakeType,
                    date = targetDate
                )
                showManualAdd = false
            }
        )
    }

    if (showTextAi) {
        QuickAiTextLogDialog(
            initialType = intakeType,
            onDismiss = { showTextAi = false },
            onSubmit = { desc, chosenType ->
                vm.analyzeTextMeal(desc, chosenType, targetDate)
                showTextAi = false
            }
        )
    }

    reviewingTask?.let { task ->
        AiResultReviewDialog(
            task = task,
            useKj = useKj,
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

    quickLogMealTarget?.let { meal ->
        PortionLogDialog(
            meal = meal,
            useKj = useKj,
            onDismiss = { quickLogMealTarget = null },
            onConfirm = { grams ->
                vm.quickAddIntake(meal, grams, intakeType, targetDate)
                quickLogMealTarget = null
                vm.setSearchQuery("")
            }
        )
    }

    quickLogRecipeTarget?.let { r ->
        RecipeLogDialog(
            recipe = r,
            useKj = useKj,
            onDismiss = { quickLogRecipeTarget = null },
            onConfirm = { servings ->
                vm.logRecipe(r, servings, intakeType, targetDate)
                quickLogRecipeTarget = null
                vm.setSearchQuery("")
            }
        )
    }
}

@Composable
private fun EmptyMealState(intakeType: IntakeType) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                mealTypeIcon(intakeType),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(tr("还没有记录", "No entries yet"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                tr(
                    "拍照识图、AI 一句话记餐或搜索食物库添加",
                    "Tap Photo, AI Text, or search your library above"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MealIntakeCard(
    item: IntakeWithMeal,
    useKj: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val intake = item.intake
    val meal = item.meal
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf<String?>(null) }

    val foodItems = remember(meal.foodItemsJson) {
        meal.foodItemsJson?.let { json ->
            try {
                val type = object : TypeToken<List<NutritionResult.FoodItem>>() {}.type
                Gson().fromJson<List<NutritionResult.FoodItem>>(json, type)
            } catch (_: Exception) {
                null
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .animateContentSize(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val thumbnailPath = meal.localImagePath
                if (thumbnailPath != null && File(thumbnailPath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(thumbnailPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { showFullScreenImage = thumbnailPath },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Restaurant, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meal.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (foodItems != null) {
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = tr("展开", "Expand"),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${intake.amount.roundToInt()}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!expanded) {
                        val factor = intake.amount / 100.0
                        Text(
                            text = tr(
                                "碳水 ${(meal.carbohydrates100 * factor).roundToInt()}g · 脂肪 ${(meal.fat100 * factor).roundToInt()}g · 蛋白质 ${(meal.proteins100 * factor).roundToInt()}g",
                                "C ${(meal.carbohydrates100 * factor).roundToInt()}g · F ${(meal.fat100 * factor).roundToInt()}g · P ${(meal.proteins100 * factor).roundToInt()}g"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                val kcal = meal.energyKcal100 * intake.amount / 100.0
                Text(
                    text = UnitCalc.formatEnergy(kcal, useKj),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = tr("编辑", "Edit"), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = tr("删除", "Delete"), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }

            if (expanded && foodItems != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.padding(12.dp)) {
                    foodItems.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Fastfood, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            Text(sub.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("${sub.weightG.roundToInt()}g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(UnitCalc.formatEnergy(sub.calories, useKj), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(tr("确认删除", "Delete Entry")) },
            text = { Text(tr("确定要删除这条记录吗？", "Delete this entry?")) },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(tr("删除", "Delete")) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(tr("取消", "Cancel")) } }
        )
    }

    showFullScreenImage?.let { path ->
        FullScreenImageDialog(imagePath = path, onDismiss = { showFullScreenImage = null })
    }
}

@Composable
private fun ManualAddDialog(
    useKj: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        mode: ManualEntryMode,
        name: String,
        kcal: Double,
        carbs: Double,
        fat: Double,
        protein: Double,
        weight: Double,
        portionLabel: String?,
        fiber: Double?,
        sugars: Double?,
        satFat: Double?,
        sodium: Double?,
        cholesterol: Double?
    ) -> Unit
) {
    var mode by remember { mutableStateOf(ManualEntryMode.WHOLE_PORTION) }
    var name by remember { mutableStateOf("") }
    var energy by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("100") }
    val defaultPortionLabel = tr("份", "serving")
    var portionLabel by remember { mutableStateOf(defaultPortionLabel) }
    var showMicros by remember { mutableStateOf(false) }
    var fiber by remember { mutableStateOf("") }
    var sugars by remember { mutableStateOf("") }
    var satFat by remember { mutableStateOf("") }
    var sodium by remember { mutableStateOf("") }
    var cholesterol by remember { mutableStateOf("") }

    val isZh = LocalIsChinese.current
    val energyVal = energy.toDoubleOrNull()
    val weightVal = weight.toDoubleOrNull()
    val isValid = name.isNotBlank() &&
        energyVal != null && energyVal >= 0.0 &&
        (mode == ManualEntryMode.QUICK_ADD || (weightVal != null && weightVal > 0.0))

    val unitLabel = UnitCalc.energyUnitLabel(useKj)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("手动添加 / 快记", "Manual / Quick Add"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ManualEntryMode.entries.forEachIndexed { idx, m ->
                        SegmentedButton(
                            selected = mode == m,
                            onClick = { mode = m },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = ManualEntryMode.entries.size)
                        ) {
                            Text(m.displayLabel(isZh), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }

                Text(
                    text = when (mode) {
                        ManualEntryMode.PER_100G -> tr("输入每 100g 包装营养表及食用克重", "Enter label nutrition per 100g and serving weight")
                        ManualEntryMode.WHOLE_PORTION -> tr("直接输入整份食物总重及总热量，系统自动换算", "Enter total serving weight & calories — auto-scaled")
                        ManualEntryMode.QUICK_ADD -> tr("只填名称与总热量即可极速记录（宏量可选）", "Name and calories only for instant logging")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("食物名称", "Food name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = energy,
                    onValueChange = { energy = it },
                    label = {
                        Text(
                            if (mode == ManualEntryMode.PER_100G) tr("每100g热量 ($unitLabel)", "Calories / 100g ($unitLabel)")
                            else tr("总热量 ($unitLabel)", "Total calories ($unitLabel)")
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                if (mode != ManualEntryMode.QUICK_ADD) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text(tr("份量 (g)", "Weight (g)")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        if (mode == ManualEntryMode.WHOLE_PORTION) {
                            OutlinedTextField(
                                value = portionLabel,
                                onValueChange = { portionLabel = it },
                                label = { Text(tr("单位(碗/个/份)", "Unit (bowl/pc)")) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text(tr("碳水(g)", "Carbs (g)")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text(tr("脂肪(g)", "Fat (g)")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text(tr("蛋白(g)", "Protein (g)")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                if (mode != ManualEntryMode.QUICK_ADD) {
                    TextButton(
                        onClick = { showMicros = !showMicros },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (showMicros) tr("收起微量营养素", "Hide micronutrients")
                            else tr("展开微量营养素（纤维/糖/钠等，可选）", "Show micronutrients (fiber, sugars, sodium, optional)")
                        )
                    }
                    if (showMicros) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = fiber, onValueChange = { fiber = it },
                                label = { Text(tr("纤维(g)", "Fiber (g)")) }, singleLine = true, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = sugars, onValueChange = { sugars = it },
                                label = { Text(tr("糖(g)", "Sugars (g)")) }, singleLine = true, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = satFat, onValueChange = { satFat = it },
                                label = { Text(tr("饱和脂肪(g)", "Sat. Fat (g)")) }, singleLine = true, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = sodium, onValueChange = { sodium = it },
                                label = { Text(tr("钠(mg)", "Sodium (mg)")) }, singleLine = true, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        OutlinedTextField(
                            value = cholesterol, onValueChange = { cholesterol = it },
                            label = { Text(tr("胆固醇(mg)", "Cholesterol (mg)")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val kcal = UnitCalc.displayEnergyToKcal(energyVal ?: 0.0, useKj)
                    onConfirm(
                        mode,
                        name,
                        kcal,
                        (carbs.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        (fat.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        (protein.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        weightVal ?: 100.0,
                        portionLabel,
                        fiber.toDoubleOrNull()?.coerceAtLeast(0.0),
                        sugars.toDoubleOrNull()?.coerceAtLeast(0.0),
                        satFat.toDoubleOrNull()?.coerceAtLeast(0.0),
                        sodium.toDoubleOrNull()?.coerceAtLeast(0.0),
                        cholesterol.toDoubleOrNull()?.coerceAtLeast(0.0)
                    )
                },
                enabled = isValid
            ) { Text(tr("添加", "Add")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("取消", "Cancel")) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun RecentIntakesRow(
    intakes: List<AddMealViewModel.RecentIntake>,
    targetDate: LocalDate,
    useKj: Boolean,
    onSelect: (AddMealViewModel.RecentIntake) -> Unit
) {
    var confirmItem by remember { mutableStateOf<AddMealViewModel.RecentIntake?>(null) }

    val isZh = LocalIsChinese.current
    Column {
        Text(
            text = tr("近期常吃", "Recently Eaten"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.ContentPadding, vertical = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = Dimens.ContentPadding)
        ) {
            items(intakes.size, key = { "recent_${intakes[it].meal.id}" }) { index ->
                val item = intakes[index]
                SuggestionChip(
                    onClick = { confirmItem = item },
                    label = {
                        Text(
                            "${item.meal.name} · ${item.amount.roundToInt()}g · ${UnitCalc.formatEnergy(item.kcal, useKj)}",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    icon = {
                        Icon(Icons.Filled.Restore, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
    }

    confirmItem?.let { item ->
        val dateStr = LocaleHelper.formatMonthDay(targetDate, isZh)
        AlertDialog(
            onDismissRequest = { confirmItem = null },
            title = { Text(tr("复用记录", "Re-log Meal")) },
            text = {
                Text(
                    tr(
                        "将 \"${item.meal.name}\"\n${item.amount.roundToInt()}g · ${UnitCalc.formatEnergy(item.kcal, useKj)}\n添加到 $dateStr 的饮食？",
                        "Add \"${item.meal.name}\"\n${item.amount.roundToInt()}g · ${UnitCalc.formatEnergy(item.kcal, useKj)}\nto $dateStr?"
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onSelect(item); confirmItem = null }) { Text(tr("确认添加", "Add")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmItem = null }) { Text(tr("取消", "Cancel")) }
            }
        )
    }
}

@Composable
private fun RecipeChipsRow(
    recipes: List<RecipeWithIngredients>,
    onSelect: (RecipeWithIngredients) -> Unit
) {
    Column {
        Text(
            text = tr("我的食谱 / 套餐", "My Recipes / Presets"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.ContentPadding, vertical = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = Dimens.ContentPadding)
        ) {
            items(recipes.size, key = { "rec_chip_${recipes[it].recipe.id}" }) { idx ->
                val r = recipes[idx]
                AssistChip(
                    onClick = { onSelect(r) },
                    label = { Text(r.recipe.name) },
                    leadingIcon = { Icon(Icons.Filled.RestaurantMenu, null, Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
private fun LibraryQuickCard(meal: Meal, useKj: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        ListItem(
            headlineContent = { Text(meal.name, fontWeight = FontWeight.Medium) },
            supportingContent = {
                Text(
                    tr(
                        "每100g: ${UnitCalc.formatEnergy(meal.energyKcal100, useKj)} · 碳水 ${meal.carbohydrates100.roundToInt()}g · 脂肪 ${meal.fat100.roundToInt()}g · 蛋白 ${meal.proteins100.roundToInt()}g",
                        "Per 100g: ${UnitCalc.formatEnergy(meal.energyKcal100, useKj)} · C ${meal.carbohydrates100.roundToInt()}g · F ${meal.fat100.roundToInt()}g · P ${meal.proteins100.roundToInt()}g"
                    )
                )
            },
            trailingContent = {
                Icon(Icons.Filled.AddCircleOutline, contentDescription = tr("添加", "Add"), tint = MaterialTheme.colorScheme.primary)
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@Composable
private fun RecipeQuickCard(recipe: RecipeWithIngredients, useKj: Boolean, onClick: () -> Unit) {
    val totalKcal = recipe.ingredients.sumOf { it.meal.energyKcal100 * it.ingredient.amountGrams / 100.0 }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        ListItem(
            leadingContent = { Icon(Icons.Filled.RestaurantMenu, null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(recipe.recipe.name, fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Text(
                    tr(
                        "${recipe.ingredients.size} 种食材 · 约 ${UnitCalc.formatEnergy(totalKcal, useKj)}",
                        "${recipe.ingredients.size} items · ~${UnitCalc.formatEnergy(totalKcal, useKj)}"
                    )
                )
            },
            trailingContent = { Icon(Icons.Filled.AddCircle, null, tint = MaterialTheme.colorScheme.primary) },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}

@Composable
fun PortionLogDialog(
    meal: Meal,
    useKj: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var gramsStr by remember { mutableStateOf((meal.portionGrams ?: 100.0).roundToInt().toString()) }
    val grams = gramsStr.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("记录 ${meal.name}", "Log ${meal.name}")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (meal.portionGrams != null && !meal.portionLabel.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5, 1.0, 2.0).forEach { mult ->
                            val g = (meal.portionGrams * mult).roundToInt()
                            SuggestionChip(
                                onClick = { gramsStr = g.toString() },
                                label = { Text("${mult} ${meal.portionLabel} (${g}g)") }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = gramsStr,
                    onValueChange = { gramsStr = it },
                    label = { Text(tr("食用克重 (g)", "Serving weight (g)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = tr(
                        "预计摄入：${UnitCalc.formatEnergy(meal.energyKcal100 * grams / 100.0, useKj)}",
                        "Estimated: ${UnitCalc.formatEnergy(meal.energyKcal100 * grams / 100.0, useKj)}"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(grams) }, enabled = grams > 0.0) { Text(tr("添加", "Add")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("取消", "Cancel")) } }
    )
}

@Composable
fun RecipeLogDialog(
    recipe: RecipeWithIngredients,
    useKj: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var servingsStr by remember { mutableStateOf(recipe.recipe.defaultServings.toString()) }
    val servings = servingsStr.toDoubleOrNull() ?: 0.0
    val baseKcal = recipe.ingredients.sumOf { it.meal.energyKcal100 * it.ingredient.amountGrams / 100.0 }
    val factor = if (recipe.recipe.defaultServings > 0) servings / recipe.recipe.defaultServings else servings

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("记入食谱：${recipe.recipe.name}", "Log Recipe: ${recipe.recipe.name}")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5, 1.0, 1.5, 2.0).forEach { s ->
                        FilterChip(
                            selected = servings == s,
                            onClick = { servingsStr = s.toString() },
                            label = { Text(tr("${s}份", "${s}x")) }
                        )
                    }
                }
                OutlinedTextField(
                    value = servingsStr,
                    onValueChange = { servingsStr = it },
                    label = { Text(tr("食用份数", "Servings")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = tr(
                        "预计摄入：${UnitCalc.formatEnergy(baseKcal * factor, useKj)}",
                        "Estimated: ${UnitCalc.formatEnergy(baseKcal * factor, useKj)}"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(servings) }, enabled = servings > 0.0) { Text(tr("确认记入", "Log Recipe")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("取消", "Cancel")) } }
    )
}
