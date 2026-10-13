package com.example.nutritracker.feature.meal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.ui.components.AppDatePickerDialog
import com.example.nutritracker.ui.components.mealTypeLabel
import com.example.nutritracker.ui.theme.Dimens
import com.example.nutritracker.util.UnitCalc
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEditScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    vm: MealEditViewModel = hiltViewModel()
) {
    val state = vm.state
    val useKj by vm.useKj.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMicros by remember { mutableStateOf(false) }

    LaunchedEffect(vm.isSaveCompleted) {
        if (vm.isSaveCompleted) onSaveSuccess()
    }

    LaunchedEffect(vm.validationError) {
        vm.validationError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearValidationError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            vm.isTemplateOnlyEdit -> "编辑食物模板"
                            vm.isEditing -> "编辑记录"
                            else -> "新建食物"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !vm.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite() }, enabled = !vm.isSaving) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "收藏食物",
                            tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { vm.save() },
                        enabled = !vm.isSaving && !vm.isSaveCompleted
                    ) {
                        if (vm.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = "保存", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ContentPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 共享模板修改策略提示
            if (vm.sharedIntakeCount > 1 && !vm.isTemplateOnlyEdit) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "此食物被引用了 ${vm.sharedIntakeCount} 次",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (vm.cloneAsNewMeal) "开启：若修改营养将另存为新食物，不影响历史记录"
                                else "关闭：修改营养将同步更新所有历史记录与食物库",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Switch(
                            checked = vm.cloneAsNewMeal,
                            onCheckedChange = { vm.cloneAsNewMeal = it }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { vm.updateName(it) },
                enabled = !vm.isSaving,
                label = { Text("食物名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 日期选择器（模板模式不显示）
            if (!vm.isTemplateOnlyEdit) {
                var showDatePicker by remember { mutableStateOf(false) }
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!vm.isSaving) showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("记录日期", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = vm.selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (showDatePicker) {
                    AppDatePickerDialog(
                        initialDate = vm.selectedDate,
                        onDismiss = { showDatePicker = false },
                        onConfirm = { vm.updateDate(it); showDatePicker = false }
                    )
                }
            }

            // 子项区域
            if (vm.foodItems.isNotEmpty()) {
                Text(
                    text = "包含子项 (修改子项将自动重算总计)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                vm.foodItems.forEach { item ->
                    SubItemCard(
                        item = item,
                        onUpdate = { vm.updateFoodItem(item.id) { _ -> it } },
                        onRemove = { vm.removeFoodItem(item.id) },
                        enabled = !vm.isSaving
                    )
                }

                OutlinedButton(
                    onClick = { vm.addFoodItem() },
                    enabled = !vm.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加新子项")
                }

                HorizontalDivider()
            } else {
                OutlinedButton(
                    onClick = { vm.addFoodItem() },
                    enabled = !vm.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("拆分为子食材项")
                }
            }

            val fieldsEnabled = !vm.isSaving && vm.foodItems.isEmpty()

            if (!vm.isTemplateOnlyEdit) {
                OutlinedTextField(
                    value = state.amountStr,
                    onValueChange = { vm.updateAmount(it) },
                    enabled = fieldsEnabled,
                    label = { Text(if (!fieldsEnabled) "食用份量 (g) - 由子项自动汇总" else "食用份量 (g)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Text(
                text = "每 100g 营养成分",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            NutrientField("卡路里 (kcal / 100g)", state.energyStr, enabled = fieldsEnabled) { vm.updateEnergy(it) }
            NutrientField("碳水化合物 (g / 100g)", state.carbsStr, enabled = fieldsEnabled) { vm.updateCarbs(it) }
            NutrientField("脂肪 (g / 100g)", state.fatStr, enabled = fieldsEnabled) { vm.updateFat(it) }
            NutrientField("蛋白质 (g / 100g)", state.proteinStr, enabled = fieldsEnabled) { vm.updateProtein(it) }

            // 常用份量单位（用于食物库快速复用）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.portionLabel,
                    onValueChange = { vm.updatePortionLabel(it) },
                    enabled = !vm.isSaving,
                    label = { Text("默认单位(如 碗/个)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.portionGramsStr,
                    onValueChange = { vm.updatePortionGrams(it) },
                    enabled = !vm.isSaving,
                    label = { Text("每份克重 (g)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // 可展开微量营养素编辑
            TextButton(
                onClick = { showMicros = !showMicros },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    if (showMicros) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("微量营养素与健康指标 (每100g，可选)")
            }

            if (showMicros) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NutrientField("膳食纤维 (g / 100g)", state.fiberStr, enabled = !vm.isSaving) { vm.updateFiber(it) }
                    NutrientField("糖 (g / 100g)", state.sugarsStr, enabled = !vm.isSaving) { vm.updateSugars(it) }
                    NutrientField("饱和脂肪 (g / 100g)", state.satFatStr, enabled = !vm.isSaving) { vm.updateSatFat(it) }
                    NutrientField("钠 (mg / 100g)", state.sodiumStr, enabled = !vm.isSaving) { vm.updateSodium(it) }
                    NutrientField("胆固醇 (mg / 100g)", state.cholesterolStr, enabled = !vm.isSaving) { vm.updateCholesterol(it) }
                }
            }

            HorizontalDivider()

            val amount = if (vm.isTemplateOnlyEdit) 100.0 else (state.amountStr.toDoubleOrNull() ?: 100.0)
            val factor = amount / 100.0
            val energyKcal = (state.energyStr.toDoubleOrNull() ?: 0.0) * factor

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (vm.isTemplateOnlyEdit) "每100g 能量概览" else "本次摄入合计",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = UnitCalc.formatEnergy(energyKcal, useKj),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "碳水 ${UnitCalc.formatDecimal((state.carbsStr.toDoubleOrNull() ?: 0.0) * factor, 1)}g · " +
                            "脂肪 ${UnitCalc.formatDecimal((state.fatStr.toDoubleOrNull() ?: 0.0) * factor, 1)}g · " +
                            "蛋白质 ${UnitCalc.formatDecimal((state.proteinStr.toDoubleOrNull() ?: 0.0) * factor, 1)}g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            if (!vm.isTemplateOnlyEdit) {
                Text("餐食类型", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IntakeType.entries.forEach { type ->
                        FilterChip(
                            selected = vm.intakeType == type,
                            onClick = { vm.intakeType = type },
                            enabled = !vm.isSaving,
                            label = { Text(mealTypeLabel(type)) }
                        )
                    }
                }
            }

            Button(
                onClick = { vm.saveIntake() },
                enabled = !vm.isSaving && !vm.isSaveCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeight),
                shape = MaterialTheme.shapes.medium
            ) {
                if (vm.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = when {
                            vm.isTemplateOnlyEdit -> "保存模板"
                            vm.isEditing -> "保存修改"
                            else -> "记录此餐"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NutrientField(
    label: String,
    value: String,
    enabled: Boolean = true,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun SubItemCard(
    item: EditableFoodItem,
    onUpdate: (EditableFoodItem) -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    label = { Text("名称") },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.weightG,
                    onValueChange = { onUpdate(item.copy(weightG = it)) },
                    label = { Text("重量(g)") },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = item.calories,
                    onValueChange = { onUpdate(item.copy(calories = it)) },
                    label = { Text("Kcal") },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.carbs,
                    onValueChange = { onUpdate(item.copy(carbs = it)) },
                    label = { Text("碳水") },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = item.protein,
                    onValueChange = { onUpdate(item.copy(protein = it)) },
                    label = { Text("蛋白") },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = item.fat,
                    onValueChange = { onUpdate(item.copy(fat = it)) },
                    label = { Text("脂肪") },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }
}
