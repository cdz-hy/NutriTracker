package com.example.nutritracker.feature.meal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.nutritracker.data.entity.Intake
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.data.entity.Meal
import com.example.nutritracker.feature.camera.AnalysisResult
import com.example.nutritracker.feature.home.mealTypeIcon
import com.example.nutritracker.feature.home.mealTypeLabel
import com.example.nutritracker.feature.camera.NutritionResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    intakeTypeId: Int,
    onNavigateToCamera: () -> Unit,
    onMealSaved: () -> Unit,
    onNavigateToEdit: (Long, Int) -> Unit,
    navController: NavController,
    vm: AddMealViewModel = hiltViewModel()
) {
    val intakeType = IntakeType.entries.getOrElse(intakeTypeId) { IntakeType.BREAKFAST }
    val todayIntakes by vm.todayIntakes.collectAsStateWithLifecycle()
    val mealsMap by vm.mealsMap.collectAsStateWithLifecycle()
    var showManualAdd by remember { mutableStateOf(false) }

    val isAnalyzing by vm.isAnalyzing.collectAsStateWithLifecycle()
    val analysisError by vm.analysisError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 监听相机返回的选中图片 URI
    val selectedImageUriStr = navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("selected_image_uri", null)?.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(selectedImageUriStr?.value) {
        selectedImageUriStr?.value?.let { uriStr ->
            if (uriStr.isNotBlank()) {
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_image_uri")
                navController.currentBackStackEntry?.savedStateHandle?.remove<Int>("intake_type_id")
                vm.analyzeAndCreateMeals(context, android.net.Uri.parse(uriStr), intakeType)
            }
        }
    }

    // 监听记录被修改的通知以重新加载数据
    val mealEdited = navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow("meal_edited", false)?.collectAsStateWithLifecycle()
    LaunchedEffect(mealEdited?.value) {
        if (mealEdited?.value == true) {
            vm.loadTodayIntakes(intakeType)
            navController.currentBackStackEntry?.savedStateHandle?.set("meal_edited", false)
        }
    }

    // 监听分析错误信息
    LaunchedEffect(analysisError) {
        analysisError?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            vm.clearAnalysisError()
        }
    }

    // 加载今日该餐类型的数据 (在首次进入或 AI 分析结束时)
    LaunchedEffect(intakeTypeId, isAnalyzing) {
        if (!isAnalyzing) {
            vm.loadTodayIntakes(intakeType)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = mealTypeLabel(intakeType),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMealSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 操作按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 拍照识别
                    FilledTonalButton(
                        onClick = onNavigateToCamera,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("拍照识别")
                    }
                    // 手动添加
                    FilledTonalButton(
                        onClick = { showManualAdd = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("手动添加")
                    }
                }

                // 今日该餐的摄入列表
                if (todayIntakes.isEmpty()) {
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
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "还没有记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点击上方按钮添加食物",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // 总计卡片
                    val totalKcal = todayIntakes.sumOf { intake ->
                        val meal = mealsMap[intake.mealId]
                        (meal?.energyKcal100 ?: 0.0) * intake.amount / 100.0
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "本餐总计",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${totalKcal.roundToInt()} kcal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // 摄入列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(todayIntakes, key = { it.id }) { intake ->
                            val meal = mealsMap[intake.mealId]
                            Box(modifier = Modifier.animateItem()) {
                                MealIntakeCard(
                                    intake = intake,
                                    meal = meal,
                                    onEdit = { onNavigateToEdit(meal?.id ?: 0, intakeTypeId) },
                                    onDelete = { vm.deleteIntake(intake) }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }

            // Loading overlay
            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .clickable(enabled = false) {}, // Intercept clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "智能分析中，请稍候...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // 手动添加对话框
    if (showManualAdd) {
        ManualAddDialog(
            onDismiss = { showManualAdd = false },
            onConfirm = { name, kcal, carbs, fat, protein, weight ->
                vm.createManualMeal(name, kcal, carbs, fat, protein, weight, intakeType)
                showManualAdd = false
            }
        )
    }
}

@Composable
private fun MealIntakeCard(
    intake: Intake,
    meal: Meal?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    // 解析食物项列表
    val foodItems = remember(meal?.foodItemsJson) {
        meal?.foodItemsJson?.let { json ->
            try {
                val type = object : TypeToken<List<NutritionResult.FoodItem>>() {}.type
                Gson().fromJson<List<NutritionResult.FoodItem>>(json, type)
            } catch (_: Exception) { null }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.clickable { expanded = !expanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 缩略图
                val thumbnailPath = meal?.localImagePath
                if (thumbnailPath != null && File(thumbnailPath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(thumbnailPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 食物信息
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meal?.name ?: "未知食物",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (foodItems != null) {
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "展开",
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
                    if (meal != null && !expanded) {
                        val factor = intake.amount / 100.0
                        Text(
                            text = "碳水 ${(meal.carbohydrates100 * factor).roundToInt()}g · 脂肪 ${(meal.fat100 * factor).roundToInt()}g · 蛋白质 ${(meal.proteins100 * factor).roundToInt()}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // 卡路里
                val kcal = (meal?.energyKcal100 ?: 0.0) * intake.amount / 100.0
                Text(
                    text = "${kcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                // 编辑和删除按钮
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 展开的子食物项列表
            if (expanded && foodItems != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Column(modifier = Modifier.padding(12.dp)) {
                    foodItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Fastfood,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${item.weightG.roundToInt()}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${item.calories.roundToInt()} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        // 子项宏量
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "碳水 ${item.carbs.roundToInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "蛋白 ${item.protein.roundToInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "脂肪 ${item.fat.roundToInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
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

@Composable
private fun ManualAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, kcal: Double, carbs: Double, fat: Double, protein: Double, weight: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "手动添加食物",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "输入每 100g 的营养成分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("食物名称") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kcal, onValueChange = { kcal = it },
                    label = { Text("卡路里 (kcal)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = carbs, onValueChange = { carbs = it },
                    label = { Text("碳水化合物 (g)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = fat, onValueChange = { fat = it },
                    label = { Text("脂肪 (g)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = protein, onValueChange = { protein = it },
                    label = { Text("蛋白质 (g)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it },
                    label = { Text("份量 (g)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        kcal.toDoubleOrNull() ?: 0.0,
                        carbs.toDoubleOrNull() ?: 0.0,
                        fat.toDoubleOrNull() ?: 0.0,
                        protein.toDoubleOrNull() ?: 0.0,
                        weight.toDoubleOrNull() ?: 100.0
                    )
                },
                enabled = name.isNotBlank() && kcal.toDoubleOrNull() != null
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
