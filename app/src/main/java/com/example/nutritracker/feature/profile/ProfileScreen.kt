package com.example.nutritracker.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.util.AgeCalc
import com.example.nutritracker.util.BmiCalc
import com.example.nutritracker.util.TdeeCalc
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    onNavigateToWeightHistory: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsStateWithLifecycle()
    val weightLogs by vm.weightLogs.collectAsStateWithLifecycle()

    // Dialog states
    var showBirthdayDialog by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showTargetWeightDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showActivityDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showWeeklyGoalDialog by remember { mutableStateOf(false) }
    var showCaloriesProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Text(
            text = "个人资料",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        val u = user
        if (u == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "请先完成引导设置",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        // BMI Card
        val bmi = BmiCalc.getBmi(u.weightKg, u.heightCm)
        val status = BmiCalc.getNutritionalStatus(bmi)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "BMI 指数",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "%.1f".format(bmi),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                LinearProgressIndicator(
                    progress = { ((bmi.toFloat() - 10f) / 40f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Body Data Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "身体数据",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                EditableProfileRow(
                    label = "生日",
                    value = "${u.birthday.format(DateTimeFormatter.ISO_LOCAL_DATE)} (${AgeCalc.getAge(u.birthday)}岁)",
                    onClick = { showBirthdayDialog = true }
                )
                EditableProfileRow(
                    label = "性别",
                    value = when (u.gender) {
                        Gender.MALE -> "男"
                        Gender.FEMALE -> "女"
                        Gender.NON_BINARY -> "非二元"
                    },
                    onClick = { showGenderDialog = true }
                )
                if (u.gender == Gender.NON_BINARY) {
                    EditableProfileRow(
                        label = "卡路里计算",
                        value = when (u.caloriesProfile) {
                            CaloriesProfile.AVERAGED -> "平均值"
                            CaloriesProfile.ESTROGEN_TYPICAL -> "雌激素典型"
                            CaloriesProfile.TESTOSTERONE_TYPICAL -> "睾酮典型"
                        },
                        onClick = { showCaloriesProfileDialog = true }
                    )
                }
                EditableProfileRow(
                    label = "身高",
                    value = "%.1f cm".format(u.heightCm),
                    onClick = { showHeightDialog = true }
                )
                EditableProfileRow(
                    label = "体重",
                    value = "%.1f kg".format(u.weightKg),
                    onClick = { showWeightDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Goal Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "目标设置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                EditableProfileRow(
                    label = "活动水平",
                    value = when (u.activityLevel) {
                        ActivityLevel.SEDENTARY -> "久坐不动"
                        ActivityLevel.LOW_ACTIVE -> "低度活跃"
                        ActivityLevel.ACTIVE -> "活跃"
                        ActivityLevel.VERY_ACTIVE -> "非常活跃"
                    },
                    onClick = { showActivityDialog = true }
                )
                EditableProfileRow(
                    label = "体重目标",
                    value = when (u.weightGoal) {
                        WeightGoal.LOSE -> "减重"
                        WeightGoal.MAINTAIN -> "维持体重"
                        WeightGoal.GAIN -> "增重"
                    },
                    onClick = { showGoalDialog = true }
                )
                if (u.weightGoal != WeightGoal.MAINTAIN) {
                    EditableProfileRow(
                        label = "目标体重",
                        value = u.targetWeightKg?.let { "%.1f kg".format(it) } ?: "未设置",
                        onClick = { showTargetWeightDialog = true }
                    )
                    EditableProfileRow(
                        label = "每周目标",
                        value = u.weeklyWeightGoalKg?.let { "%.1f kg/周".format(it) } ?: "默认",
                        onClick = { showWeeklyGoalDialog = true }
                    )
                }
                ProfileRow("TDEE", "${TdeeCalc.getTdeeKcal(u).roundToInt()} kcal")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calorie taper toggle
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            var taperEnabled by remember { mutableStateOf(u.caloriesTaperEnabled) }
            ListItem(
                headlineContent = {
                    Text(
                        text = "卡路里渐近减速",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Text(
                        text = "接近目标体重时逐步减少调整幅度",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Switch(
                        checked = taperEnabled,
                        onCheckedChange = {
                            taperEnabled = it
                            vm.toggleTaper(it)
                        }
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weight History
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToWeightHistory),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            ListItem(
                headlineContent = { Text("体重历史") },
                supportingContent = { Text("查看趋势和记录体重") },
                leadingContent = {
                    Icon(Icons.Filled.MonitorWeight, null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Dialogs ──────────────────────────────────────────────────────────

        if (showBirthdayDialog) {
            DatePickerDialog(
                initialDate = u.birthday,
                onDismiss = { showBirthdayDialog = false },
                onConfirm = { vm.updateBirthday(it); showBirthdayDialog = false }
            )
        }

        if (showHeightDialog) {
            NumberPickerDialog(
                title = "身高",
                unit = "cm",
                initialValue = u.heightCm,
                range = 100.0..250.0,
                step = 0.5,
                onDismiss = { showHeightDialog = false },
                onConfirm = { vm.updateHeight(it); showHeightDialog = false }
            )
        }

        if (showWeightDialog) {
            NumberPickerDialog(
                title = "体重",
                unit = "kg",
                initialValue = u.weightKg,
                range = 30.0..300.0,
                step = 0.1,
                onDismiss = { showWeightDialog = false },
                onConfirm = { vm.updateWeight(it); showWeightDialog = false }
            )
        }

        if (showTargetWeightDialog) {
            NumberPickerDialog(
                title = "目标体重",
                unit = "kg",
                initialValue = u.targetWeightKg ?: u.weightKg,
                range = 30.0..300.0,
                step = 0.1,
                onDismiss = { showTargetWeightDialog = false },
                onConfirm = { vm.updateTargetWeight(it); showTargetWeightDialog = false }
            )
        }

        if (showWeeklyGoalDialog) {
            NumberPickerDialog(
                title = "每周目标",
                unit = "kg/周",
                initialValue = u.weeklyWeightGoalKg ?: 0.5,
                range = 0.1..2.0,
                step = 0.1,
                onDismiss = { showWeeklyGoalDialog = false },
                onConfirm = { vm.updateWeeklyGoal(it); showWeeklyGoalDialog = false }
            )
        }

        if (showGenderDialog) {
            SelectionDialog(
                title = "性别",
                options = Gender.entries.map {
                    when (it) {
                        Gender.MALE -> "男"
                        Gender.FEMALE -> "女"
                        Gender.NON_BINARY -> "非二元"
                    }
                },
                selectedIndex = Gender.entries.indexOf(u.gender),
                onDismiss = { showGenderDialog = false },
                onConfirm = { vm.updateGender(Gender.entries[it]); showGenderDialog = false }
            )
        }

        if (showActivityDialog) {
            SelectionDialog(
                title = "活动水平",
                options = listOf("久坐不动", "低度活跃", "活跃", "非常活跃"),
                selectedIndex = ActivityLevel.entries.indexOf(u.activityLevel),
                onDismiss = { showActivityDialog = false },
                onConfirm = { vm.updateActivityLevel(ActivityLevel.entries[it]); showActivityDialog = false }
            )
        }

        if (showGoalDialog) {
            SelectionDialog(
                title = "体重目标",
                options = listOf("减重", "维持体重", "增重"),
                selectedIndex = WeightGoal.entries.indexOf(u.weightGoal),
                onDismiss = { showGoalDialog = false },
                onConfirm = { vm.updateWeightGoal(WeightGoal.entries[it]); showGoalDialog = false }
            )
        }

        if (showCaloriesProfileDialog) {
            SelectionDialog(
                title = "卡路里计算",
                options = listOf("平均值", "雌激素典型", "睾酮典型"),
                selectedIndex = CaloriesProfile.entries.indexOf(u.caloriesProfile),
                onDismiss = { showCaloriesProfileDialog = false },
                onConfirm = { vm.updateCaloriesProfile(CaloriesProfile.entries[it]); showCaloriesProfileDialog = false }
            )
        }
    }
}

// ── Helper Composables ──────────────────────────────────────────────────────

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EditableProfileRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Number Picker Dialog ────────────────────────────────────────────────────

@Composable
private fun NumberPickerDialog(
    title: String,
    unit: String,
    initialValue: Double,
    range: ClosedFloatingPointRange<Double>,
    step: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var currentValue by remember { mutableStateOf(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
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

                // Current value display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "%.1f %s".format(currentValue, unit),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Slider
                Slider(
                    value = currentValue.toFloat(),
                    onValueChange = { currentValue = it.toDouble() },
                    valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
                    steps = ((range.endInclusive - range.start) / step).toInt() - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Quick adjust buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(-1.0, -0.5, -0.1, 0.1, 0.5, 1.0).forEach { delta ->
                        FilledTonalButton(
                            onClick = {
                                currentValue = (currentValue + delta).coerceIn(range)
                            },
                            modifier = Modifier.width(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (delta > 0) "+%.1f".format(delta) else "%.1f".format(delta),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(currentValue) }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

// ── Selection Dialog ────────────────────────────────────────────────────────

@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(selectedIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (index == selected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = index == selected,
                                onClick = { selected = index },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        },
                        modifier = Modifier.clickable { selected = index },
                        colors = ListItemDefaults.colors(
                            containerColor = if (index == selected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

// ── Date Picker Dialog ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
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
                        val date = LocalDate.ofEpochDay(millis / 86400000L)
                        onConfirm(date)
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
