package com.example.nutritracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.data.entity.WaterIntake
import com.example.nutritracker.feature.camera.AnalysisTask
import com.example.nutritracker.feature.camera.NutritionResult
import com.example.nutritracker.ui.theme.Dimens
import com.example.nutritracker.ui.theme.LocalUseDarkTheme
import com.example.nutritracker.ui.theme.WaterColor
import com.example.nutritracker.ui.theme.WaterColorDark
import com.example.nutritracker.util.UnitCalc
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Shared step-snapping numeric slider picker dialog (replaces duplicated Onboarding/Profile dialogs).
 */
@Composable
fun NumberPickerDialog(
    title: String,
    unit: String,
    initialValue: Double,
    range: ClosedFloatingPointRange<Double>,
    step: Double = 0.1,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    fun snap(v: Double): Double {
        val clamped = v.coerceIn(range)
        if (step <= 0.0) return clamped
        val stepped = ((clamped - range.start) / step).roundToInt() * step + range.start
        return ((stepped * 10.0).roundToInt() / 10.0).coerceIn(range)
    }

    var currentValue by remember { mutableStateOf(snap(initialValue)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.ContentPadding),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", currentValue)} $unit",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.CardInnerPaddingCompact),
                        textAlign = TextAlign.Center
                    )
                }

                // Continuous slider with snap-to-step (avoids rendering 1000+ M3 step ticks)
                Slider(
                    value = currentValue.toFloat(),
                    onValueChange = { currentValue = snap(it.toDouble()) },
                    valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(-1.0, -0.5, -0.1, 0.1, 0.5, 1.0).forEach { delta ->
                        FilledTonalButton(
                            onClick = { currentValue = snap(currentValue + delta) },
                            modifier = Modifier.width(46.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (delta > 0) "+${String.format(Locale.US, "%.1f", delta)}"
                                else String.format(Locale.US, "%.1f", delta),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(currentValue) }) { Text("确定") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochDay() * 86400000L
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onConfirm(LocalDate.ofEpochDay(millis / 86400000L))
                    }
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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

/**
 * Natural-language Quick AI Meal Logging dialog.
 */
@Composable
fun QuickAiTextLogDialog(
    initialType: IntakeType,
    onDismiss: () -> Unit,
    onSubmit: (description: String, type: IntakeType) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    val examples = listOf(
        "一碗牛肉面加一个卤蛋",
        "两片全麦面包 + 无糖拿铁",
        "轻食鸡胸肉沙拉配油醋汁",
        "黄焖鸡米饭微辣外卖"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Text("AI 一句话记餐", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "描述吃了什么及大致份量，AI 将自动估算热量、宏量与微量营养素",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("饮食描述") },
                    placeholder = { Text("例：一碗番茄鸡蛋面加半个煎蛋") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    examples.forEach { sample ->
                        SuggestionChip(
                            onClick = { text = sample },
                            label = { Text(sample, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IntakeType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(mealTypeLabel(type), style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text.trim(), selectedType) },
                enabled = text.isNotBlank()
            ) {
                Text("开始解析")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

/**
 * Interactive preview & portion-tuning dialog before committing AI recognition results to Room.
 */
@Composable
fun AiResultReviewDialog(
    task: AnalysisTask,
    useKj: Boolean,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onConfirm: (
        mealName: String,
        intakeType: IntakeType,
        date: LocalDate,
        multiplier: Double,
        items: List<NutritionResult.FoodItem>,
        fallbackResult: NutritionResult,
        thumbnailPath: String?
    ) -> Unit
) {
    val analysisResult = task.result ?: return
    val raw = analysisResult.nutritionResult

    var mealName by remember(task.id) {
        mutableStateOf(
            raw.mealName.ifBlank {
                raw.foodItems.take(3).joinToString(" + ") { it.name }.ifBlank { "AI 识别食物" }
            }
        )
    }
    var intakeType by remember(task.id) { mutableStateOf(task.intakeType) }
    var date by remember(task.id) { mutableStateOf(task.date) }
    var multiplier by remember(task.id) { mutableDoubleStateOf(1.0) }
    var items by remember(task.id) { mutableStateOf(raw.foodItems) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }

    val baseKcal = if (items.isNotEmpty()) items.sumOf { it.calories } else raw.totalCalories
    val baseWeight = if (items.isNotEmpty()) items.sumOf { it.weightG } else 100.0
    val baseCarbs = if (items.isNotEmpty()) items.sumOf { it.carbs } else raw.totalCarbs
    val baseFat = if (items.isNotEmpty()) items.sumOf { it.fat } else raw.totalFat
    val baseProtein = if (items.isNotEmpty()) items.sumOf { it.protein } else raw.totalProtein

    val scaledKcal = baseKcal * multiplier
    val scaledWeight = baseWeight * multiplier
    val scaledCarbs = baseCarbs * multiplier
    val scaledFat = baseFat * multiplier
    val scaledProtein = baseProtein * multiplier

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 680.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "确认识别结果",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "稍后确认")
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val thumb = analysisResult.thumbnailPath
                            if (thumb != null && File(thumb).exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(File(thumb))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "点击查看大图",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { showFullScreenImage = true }
                                )
                            }
                            OutlinedTextField(
                                value = mealName,
                                onValueChange = { mealName = it },
                                label = { Text("餐食名称") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Summary card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = UnitCalc.formatEnergy(scaledKcal, useKj),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${scaledWeight.roundToInt()}g",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "碳水 ${scaledCarbs.roundToInt()}g · 脂肪 ${scaledFat.roundToInt()}g · 蛋白质 ${scaledProtein.roundToInt()}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // Portion multiplier chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "份量倍率调整",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0).forEach { factor ->
                                    FilterChip(
                                        selected = kotlin.math.abs(multiplier - factor) < 0.01,
                                        onClick = { multiplier = factor },
                                        label = { Text("${factor}x") }
                                    )
                                }
                            }
                        }
                    }

                    // Meal type & date
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IntakeType.entries.forEach { type ->
                                    FilterChip(
                                        selected = intakeType == type,
                                        onClick = { intakeType = type },
                                        label = { Text(mealTypeLabel(type)) }
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("记录日期：${date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))}")
                            }
                        }
                    }

                    // Sub-items preview & editing
                    if (items.isNotEmpty()) {
                        item {
                            Text(
                                text = "识别明细 (${items.size} 项，可删减或调整克重)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        itemsIndexed(items) { idx, item ->
                            ReviewSubItemRow(
                                item = item,
                                multiplier = multiplier,
                                useKj = useKj,
                                onUpdateWeight = { newBaseWeight ->
                                    val safeNew = newBaseWeight.coerceAtLeast(1.0)
                                    val ratio = if (item.weightG > 0) safeNew / item.weightG else 1.0
                                    items = items.toMutableList().apply {
                                        set(
                                            idx,
                                            item.copy(
                                                weightG = safeNew,
                                                calories = item.calories * ratio,
                                                protein = item.protein * ratio,
                                                fat = item.fat * ratio,
                                                carbs = item.carbs * ratio,
                                                fiberG = item.fiberG?.times(ratio),
                                                sugarsG = item.sugarsG?.times(ratio),
                                                saturatedFatG = item.saturatedFatG?.times(ratio),
                                                sodiumMg = item.sodiumMg?.times(ratio),
                                                cholesterolMg = item.cholesterolMg?.times(ratio)
                                            )
                                        )
                                    }
                                },
                                onRemove = {
                                    if (items.size > 1) {
                                        items = items.filterIndexed { i, _ -> i != idx }
                                    }
                                },
                                canRemove = items.size > 1
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDiscard,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("丢弃")
                    }
                    Button(
                        onClick = {
                            onConfirm(
                                mealName,
                                intakeType,
                                date,
                                multiplier,
                                items,
                                raw,
                                analysisResult.thumbnailPath
                            )
                        },
                        enabled = mealName.isNotBlank(),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("确认记录")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date = it; showDatePicker = false }
        )
    }

    if (showFullScreenImage && analysisResult.thumbnailPath != null) {
        FullScreenImageDialog(
            imagePath = analysisResult.thumbnailPath,
            onDismiss = { showFullScreenImage = false }
        )
    }
}

@Composable
private fun ReviewSubItemRow(
    item: NutritionResult.FoodItem,
    multiplier: Double,
    useKj: Boolean,
    onUpdateWeight: (Double) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    var weightText by remember(item.name, item.weightG) {
        mutableStateOf(item.weightG.roundToInt().toString())
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = UnitCalc.formatEnergy(item.calories * multiplier, useKj),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedTextField(
                value = weightText,
                onValueChange = { raw ->
                    weightText = raw
                    raw.toDoubleOrNull()?.takeIf { it > 0 }?.let { onUpdateWeight(it) }
                },
                label = { Text("克") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(82.dp)
            )
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "移除子项",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Water timeline & custom amount dialog.
 */
@Composable
fun WaterDetailDialog(
    entries: List<WaterIntake>,
    goalMl: Int,
    quickAmounts: List<Int>,
    onAddWater: (Int) -> Unit,
    onDeleteEntry: (WaterIntake) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalUseDarkTheme.current
    val waterColor = if (isDark) WaterColorDark else WaterColor
    val totalMl = entries.sumOf { it.amountMl }
    var customMl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.WaterDrop, null, tint = waterColor)
                Text("饮水记录 ($totalMl / ${goalMl}ml)", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "快速记录",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickAmounts.forEach { amount ->
                        FilledTonalButton(
                            onClick = { onAddWater(amount) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+${amount}ml")
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customMl,
                        onValueChange = { customMl = it },
                        label = { Text("自定义 ml") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            customMl.toIntOrNull()?.takeIf { it in 10..5000 }?.let {
                                onAddWater(it)
                                customMl = ""
                            }
                        },
                        enabled = customMl.toIntOrNull()?.let { it in 10..5000 } == true
                    ) {
                        Text("添加")
                    }
                }

                HorizontalDivider()

                if (entries.isEmpty()) {
                    Text(
                        text = "暂无饮水记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "今日明细",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    entries.sortedByDescending { it.dateTime }.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${entry.dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}  ·  +${entry.amountMl} ml",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(
                                onClick = { onDeleteEntry(entry) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
