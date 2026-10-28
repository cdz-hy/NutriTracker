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
import com.example.nutritracker.util.LocalIsChinese
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.util.UnitCalc
import com.example.nutritracker.util.tr

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
                            vm.isTemplateOnlyEdit -> tr("编辑食物模板", "Edit Food Template")
                            vm.isEditing -> tr("编辑记录", "Edit Entry")
                            else -> tr("新建食物", "New Food")
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !vm.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("返回", "Back"))
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite() }, enabled = !vm.isSaving) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = tr("收藏食物", "Favorite food"),
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
                            Icon(Icons.Filled.Save, contentDescription = tr("保存", "Save"), tint = MaterialTheme.colorScheme.primary)
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
            // Shared template edit strategy
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
                                text = tr(
                                    "此食物被引用了 ${vm.sharedIntakeCount} 次",
                                    "Referenced in ${vm.sharedIntakeCount} entries"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (vm.cloneAsNewMeal) {
                                    tr(
                                        "开启：若修改营养将另存为新食物，不影响历史记录",
                                        "On: Save edits as a new food without changing history"
                                    )
                                } else {
                                    tr(
                                        "关闭：修改营养将同步更新所有历史记录与食物库",
                                        "Off: Edits will update all past entries & library"
                                    )
                                },
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
                label = { Text(tr("食物名称", "Food name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date picker (hidden in template-only mode)
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
                            Text(tr("记录日期", "Entry Date"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = LocaleHelper.formatFullDate(vm.selectedDate, LocalIsChinese.current),
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

            // Sub-items
            if (vm.foodItems.isNotEmpty()) {
                Text(
                    text = tr("包含子项 (修改子项将自动重算总计)", "Ingredients (totals recalculate automatically)"),
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
                    Text(tr("添加新子项", "Add Ingredient"))
                }

                HorizontalDivider()
            } else {
                OutlinedButton(
                    onClick = { vm.addFoodItem() },
                    enabled = !vm.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("拆分为子食材项", "Split into Ingredients"))
                }
            }

            val fieldsEnabled = !vm.isSaving && vm.foodItems.isEmpty()

            if (!vm.isTemplateOnlyEdit) {
                OutlinedTextField(
                    value = state.amountStr,
                    onValueChange = { vm.updateAmount(it) },
                    enabled = fieldsEnabled,
                    label = {
                        Text(
                            if (!fieldsEnabled) {
                                tr("食用份量 (g) - 由子项自动汇总", "Serving (g) — auto-summed")
                            } else {
                                tr("食用份量 (g)", "Serving weight (g)")
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Text(
                text = tr("每 100g 营养成分", "Nutrition per 100g"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            NutrientField(tr("卡路里 (kcal / 100g)", "Calories (kcal / 100g)"), state.energyStr, enabled = fieldsEnabled) { vm.updateEnergy(it) }
            NutrientField(tr("碳水化合物 (g / 100g)", "Carbs (g / 100g)"), state.carbsStr, enabled = fieldsEnabled) { vm.updateCarbs(it) }
            NutrientField(tr("脂肪 (g / 100g)", "Fat (g / 100g)"), state.fatStr, enabled = fieldsEnabled) { vm.updateFat(it) }
            NutrientField(tr("蛋白质 (g / 100g)", "Protein (g / 100g)"), state.proteinStr, enabled = fieldsEnabled) { vm.updateProtein(it) }

            // Portion unit
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.portionLabel,
                    onValueChange = { vm.updatePortionLabel(it) },
                    enabled = !vm.isSaving,
                    label = { Text(tr("默认单位(如 碗/个)", "Unit (e.g. bowl/pc)")) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.portionGramsStr,
                    onValueChange = { vm.updatePortionGrams(it) },
                    enabled = !vm.isSaving,
                    label = { Text(tr("每份克重 (g)", "Grams / unit (g)")) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Expandable micronutrients
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
                Text(tr("微量营养素与健康指标 (每100g，可选)", "Micronutrients (per 100g, optional)"))
            }

            if (showMicros) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NutrientField(tr("膳食纤维 (g / 100g)", "Dietary Fiber (g / 100g)"), state.fiberStr, enabled = !vm.isSaving) { vm.updateFiber(it) }
                    NutrientField(tr("糖 (g / 100g)", "Sugars (g / 100g)"), state.sugarsStr, enabled = !vm.isSaving) { vm.updateSugars(it) }
                    NutrientField(tr("饱和脂肪 (g / 100g)", "Saturated Fat (g / 100g)"), state.satFatStr, enabled = !vm.isSaving) { vm.updateSatFat(it) }
                    NutrientField(tr("钠 (mg / 100g)", "Sodium (mg / 100g)"), state.sodiumStr, enabled = !vm.isSaving) { vm.updateSodium(it) }
                    NutrientField(tr("胆固醇 (mg / 100g)", "Cholesterol (mg / 100g)"), state.cholesterolStr, enabled = !vm.isSaving) { vm.updateCholesterol(it) }
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
                        text = if (vm.isTemplateOnlyEdit) tr("每100g 能量概览", "Energy per 100g") else tr("本次摄入合计", "Entry Total"),
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
                        text = tr(
                            "碳水 ${UnitCalc.formatDecimal((state.carbsStr.toDoubleOrNull() ?: 0.0) * factor, 1)}g · " +
                                "脂肪 ${UnitCalc.formatDecimal((state.fatStr.toDoubleOrNull() ?: 0.0) * factor, 1)}g · " +
                                "蛋白质 ${UnitCalc.formatDecimal((state.proteinStr.toDoubleOrNull() ?: 0.0) * factor, 1)}g",
                            "Carbs ${UnitCalc.formatDecimal((state.carbsStr.toDoubleOrNull() ?: 0.0) * factor, 1)}g · " +
                                "Fat ${UnitCalc.formatDecimal((state.fatStr.toDoubleOrNull() ?: 0.0) * factor, 1)}g · " +
                                "Protein ${UnitCalc.formatDecimal((state.proteinStr.toDoubleOrNull() ?: 0.0) * factor, 1)}g"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            if (!vm.isTemplateOnlyEdit) {
                Text(tr("餐食类型", "Meal Type"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                            vm.isTemplateOnlyEdit -> tr("保存模板", "Save Template")
                            vm.isEditing -> tr("保存修改", "Save Changes")
                            else -> tr("记录此餐", "Log Meal")
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
                    label = { Text(tr("名称", "Name")) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(Icons.Filled.Delete, contentDescription = tr("删除", "Remove"), tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.weightG,
                    onValueChange = { onUpdate(item.copy(weightG = it)) },
                    label = { Text(tr("重量(g)", "Weight(g)")) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = item.calories,
                    onValueChange = { onUpdate(item.copy(calories = it)) },
                    label = { Text("kcal") },
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
                    label = { Text(tr("碳水", "Carbs")) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = item.protein,
                    onValueChange = { onUpdate(item.copy(protein = it)) },
                    label = { Text(tr("蛋白", "Protein")) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = item.fat,
                    onValueChange = { onUpdate(item.copy(fat = it)) },
                    label = { Text(tr("脂肪", "Fat")) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }
}
