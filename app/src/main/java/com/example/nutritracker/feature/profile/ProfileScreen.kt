package com.example.nutritracker.feature.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.ui.components.AppDatePickerDialog
import com.example.nutritracker.ui.components.NumberPickerDialog
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    onNavigateToWeightHistory: () -> Unit,
    onNavigateToFoodLibrary: () -> Unit,
    onNavigateToSources: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsStateWithLifecycle()
    val useKj by vm.useKj.collectAsStateWithLifecycle()
    val isZh = LocalIsChinese.current

    var showBirthdayDialog by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showTargetWeightDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showActivityDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showWeeklyGoalDialog by remember { mutableStateOf(false) }
    var showCaloriesProfileDialog by remember { mutableStateOf(false) }
    var bmiStandard by remember(isZh) {
        mutableStateOf(if (isZh) BmiStandard.CHINA else BmiStandard.WHO)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
    ) {
        Text(
            text = tr("个人资料", "Profile"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Dimens.ContentPadding)
        )

        val u = user
        if (u == null) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(tr("请先完成引导设置", "Please finish setup first"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        val imperial = u.usesImperialUnits
        val bmi = BmiCalc.getBmi(u.weightKg, u.heightCm)
        val status = BmiCalc.getNutritionalStatus(bmi, bmiStandard)
        val animBmi by animatedFloatAsState(bmi.toFloat(), durationMs = 900, label = "bmiAnim")

        // BMI Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevationHigh),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    )
                    .padding(Dimens.CardInnerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("BMI 指数", "BMI Index"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (std in BmiStandard.entries) {
                                val active = bmiStandard == std
                                Box(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .clickable { bmiStandard = std }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (std == BmiStandard.CHINA) tr("中国标准", "WGOC") else tr("世界标准", "WHO"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onNavigateToSources, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = tr("查看 BMI 科学依据", "View BMI references"),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = UnitCalc.formatDecimal(animBmi.toDouble(), 1),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = status.getLabel(bmiStandard, isZh),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                BmiSegmentedBar(bmi = bmi, standard = bmiStandard)
            }
        }

        // Body Data
        SectionCard(title = tr("身体数据", "Body Metrics")) {
            EditableProfileRow(
                icon = Icons.Filled.Cake,
                label = tr("生日", "Birthday"),
                value = tr(
                    "${u.birthday.format(DateTimeFormatter.ISO_LOCAL_DATE)} (${AgeCalc.getAge(u.birthday)}岁)",
                    "${u.birthday.format(DateTimeFormatter.ISO_LOCAL_DATE)} (${AgeCalc.getAge(u.birthday)} yrs)"
                ),
                onClick = { showBirthdayDialog = true }
            )
            EditableProfileRow(
                icon = Icons.Filled.Wc,
                label = tr("性别", "Sex"),
                value = when (u.gender) {
                    Gender.MALE -> tr("男", "Male")
                    Gender.FEMALE -> tr("女", "Female")
                    Gender.NON_BINARY -> tr("非二元", "Non-binary")
                },
                onClick = { showGenderDialog = true }
            )
            if (u.gender == Gender.NON_BINARY) {
                EditableProfileRow(
                    icon = Icons.Filled.Tune,
                    label = tr("卡路里计算", "Calorie Profile"),
                    value = when (u.caloriesProfile) {
                        CaloriesProfile.AVERAGED -> tr("平均值", "Averaged")
                        CaloriesProfile.ESTROGEN_TYPICAL -> tr("雌激素典型", "Estrogen-typical")
                        CaloriesProfile.TESTOSTERONE_TYPICAL -> tr("睾酮典型", "Testosterone-typical")
                    },
                    onClick = { showCaloriesProfileDialog = true }
                )
            }
            EditableProfileRow(
                icon = Icons.Filled.Height,
                label = tr("身高", "Height"),
                value = UnitCalc.formatHeight(u.heightCm, imperial),
                onClick = { showHeightDialog = true }
            )
            EditableProfileRow(
                icon = Icons.Filled.MonitorWeight,
                label = tr("体重", "Weight"),
                value = UnitCalc.formatWeight(u.weightKg, imperial),
                onClick = { showWeightDialog = true }
            )
        }

        // Goals
        SectionCard(title = tr("目标设置", "Goals & Activity")) {
            EditableProfileRow(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                label = tr("活动水平", "Activity Level"),
                value = when (u.activityLevel) {
                    ActivityLevel.SEDENTARY -> tr("久坐不动", "Sedentary")
                    ActivityLevel.LOW_ACTIVE -> tr("低度活跃", "Low Active")
                    ActivityLevel.ACTIVE -> tr("活跃", "Active")
                    ActivityLevel.VERY_ACTIVE -> tr("非常活跃", "Very Active")
                },
                onClick = { showActivityDialog = true }
            )
            EditableProfileRow(
                icon = Icons.Filled.FitnessCenter,
                label = tr("体重目标", "Weight Goal"),
                value = when (u.weightGoal) {
                    WeightGoal.LOSE -> tr("减重", "Lose Weight")
                    WeightGoal.MAINTAIN -> tr("维持体重", "Maintain")
                    WeightGoal.GAIN -> tr("增重", "Gain Weight")
                },
                onClick = { showGoalDialog = true }
            )
            if (u.weightGoal != WeightGoal.MAINTAIN) {
                EditableProfileRow(
                    icon = Icons.Filled.Flag,
                    label = tr("目标体重", "Target Weight"),
                    value = u.targetWeightKg?.let { UnitCalc.formatWeight(it, imperial) } ?: tr("未设置", "Not set"),
                    onClick = { showTargetWeightDialog = true }
                )
                EditableProfileRow(
                    icon = Icons.Filled.Speed,
                    label = tr("每周目标", "Weekly Rate"),
                    value = u.weeklyWeightGoalKg?.let {
                        tr("${UnitCalc.formatWeight(it, imperial)}/周", "${UnitCalc.formatWeight(it, imperial)}/wk")
                    } ?: tr("默认", "Default"),
                    onClick = { showWeeklyGoalDialog = true }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSizeMedium),
                        tint = if (LocalUseDarkTheme.current) BurnColorDark else BurnColor
                    )
                    Text("TDEE", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = UnitCalc.formatEnergy(TdeeCalc.getTdeeKcal(u), useKj),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = tr(
                    "TDEE = 每日总能量消耗，即维持当前体重所需的能量",
                    "TDEE = Total Daily Energy Expenditure needed to maintain weight"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // Calories Taper
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
            shape = MaterialTheme.shapes.large
        ) {
            ListItem(
                headlineContent = { Text(tr("卡路里渐近减速", "Calorie Goal Taper"), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(tr("接近目标体重时逐步减少调整幅度", "Gradually ease calorie deficit/surplus near goal weight")) },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.IconSizeMedium))
                },
                trailingContent = {
                    Switch(
                        checked = u.caloriesTaperEnabled,
                        onCheckedChange = { vm.toggleTaper(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }

        // Weight History
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToWeightHistory),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
            shape = MaterialTheme.shapes.large
        ) {
            ListItem(
                headlineContent = { Text(tr("体重历史与趋势图", "Weight History & Chart"), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(tr("查看折线走势及补录历史体重", "View trend line and log past weights")) },
                leadingContent = { Icon(Icons.Filled.MonitorWeight, null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }

        // Food Library & Recipes
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToFoodLibrary),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
            shape = MaterialTheme.shapes.large
        ) {
            ListItem(
                headlineContent = { Text(tr("食物库与自制食谱", "Food Library & Recipes"), fontWeight = FontWeight.Medium) },
                supportingContent = { Text(tr("收藏常用食物、组合自定义营养套餐", "Favorite foods and build custom meal recipes")) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

        if (showBirthdayDialog) {
            AppDatePickerDialog(
                initialDate = u.birthday,
                onDismiss = { showBirthdayDialog = false },
                onConfirm = { vm.updateBirthday(it); showBirthdayDialog = false }
            )
        }

        if (showHeightDialog) {
            NumberPickerDialog(
                title = tr("身高", "Height"),
                unit = UnitCalc.heightUnitLabel(imperial),
                initialValue = if (imperial) UnitCalc.cmToInches(u.heightCm) else u.heightCm,
                range = if (imperial) 39.0..99.0 else 100.0..250.0,
                step = 0.5,
                onDismiss = { showHeightDialog = false },
                onConfirm = {
                    val cm = if (imperial) UnitCalc.inchesToCm(it) else it
                    vm.updateHeight(cm)
                    showHeightDialog = false
                }
            )
        }

        if (showWeightDialog) {
            NumberPickerDialog(
                title = tr("体重", "Weight"),
                unit = UnitCalc.weightUnitLabel(imperial),
                initialValue = if (imperial) UnitCalc.kgToLbs(u.weightKg) else u.weightKg,
                range = if (imperial) 66.0..660.0 else 30.0..300.0,
                step = 0.1,
                onDismiss = { showWeightDialog = false },
                onConfirm = {
                    val kg = if (imperial) UnitCalc.lbsToKg(it) else it
                    vm.logWeight(kg)
                    showWeightDialog = false
                }
            )
        }

        if (showTargetWeightDialog) {
            val baseTarget = u.targetWeightKg ?: u.weightKg
            NumberPickerDialog(
                title = tr("目标体重", "Target Weight"),
                unit = UnitCalc.weightUnitLabel(imperial),
                initialValue = if (imperial) UnitCalc.kgToLbs(baseTarget) else baseTarget,
                range = if (imperial) 66.0..660.0 else 30.0..300.0,
                step = 0.1,
                onDismiss = { showTargetWeightDialog = false },
                onConfirm = {
                    val kg = if (imperial) UnitCalc.lbsToKg(it) else it
                    vm.updateTargetWeight(kg)
                    showTargetWeightDialog = false
                }
            )
        }

        if (showWeeklyGoalDialog) {
            val baseWeekly = u.weeklyWeightGoalKg ?: 0.5
            NumberPickerDialog(
                title = tr("每周目标", "Weekly Rate"),
                unit = tr("${UnitCalc.weightUnitLabel(imperial)}/周", "${UnitCalc.weightUnitLabel(imperial)}/wk"),
                initialValue = if (imperial) UnitCalc.kgToLbs(baseWeekly) else baseWeekly,
                range = if (imperial) 0.2..4.4 else 0.1..2.0,
                step = 0.1,
                onDismiss = { showWeeklyGoalDialog = false },
                onConfirm = {
                    val kg = if (imperial) UnitCalc.lbsToKg(it) else it
                    vm.updateWeeklyGoal(kg)
                    showWeeklyGoalDialog = false
                }
            )
        }

        if (showGenderDialog) {
            SelectionDialog(
                title = tr("性别", "Sex"),
                options = Gender.entries.map {
                    when (it) {
                        Gender.MALE -> tr("男", "Male")
                        Gender.FEMALE -> tr("女", "Female")
                        Gender.NON_BINARY -> tr("非二元", "Non-binary")
                    }
                },
                selectedIndex = Gender.entries.indexOf(u.gender),
                onDismiss = { showGenderDialog = false },
                onConfirm = { vm.updateGender(Gender.entries[it]); showGenderDialog = false }
            )
        }

        if (showActivityDialog) {
            SelectionDialog(
                title = tr("活动水平", "Activity Level"),
                options = listOf(
                    tr("久坐不动", "Sedentary"),
                    tr("低度活跃", "Low Active"),
                    tr("活跃", "Active"),
                    tr("非常活跃", "Very Active")
                ),
                selectedIndex = ActivityLevel.entries.indexOf(u.activityLevel),
                onDismiss = { showActivityDialog = false },
                onConfirm = { vm.updateActivityLevel(ActivityLevel.entries[it]); showActivityDialog = false }
            )
        }

        if (showGoalDialog) {
            SelectionDialog(
                title = tr("体重目标", "Weight Goal"),
                options = listOf(
                    tr("减重", "Lose Weight"),
                    tr("维持体重", "Maintain Weight"),
                    tr("增重", "Gain Weight")
                ),
                selectedIndex = WeightGoal.entries.indexOf(u.weightGoal),
                onDismiss = { showGoalDialog = false },
                onConfirm = { vm.updateWeightGoal(WeightGoal.entries[it]); showGoalDialog = false }
            )
        }

        if (showCaloriesProfileDialog) {
            SelectionDialog(
                title = tr("卡路里计算", "Calorie Profile"),
                options = listOf(
                    tr("平均值", "Averaged"),
                    tr("雌激素典型", "Estrogen-typical"),
                    tr("睾酮典型", "Testosterone-typical")
                ),
                selectedIndex = CaloriesProfile.entries.indexOf(u.caloriesProfile),
                onDismiss = { showCaloriesProfileDialog = false },
                onConfirm = { vm.updateCaloriesProfile(CaloriesProfile.entries[it]); showCaloriesProfileDialog = false }
            )
        }
    }
}

@Composable
private fun BmiSegmentedBar(bmi: Double, standard: BmiStandard) {
    val segments = if (standard == BmiStandard.CHINA) {
        listOf(
            Triple(Color(0xFF2196F3), 18.5f, tr("偏瘦", "Low")),
            Triple(Color(0xFF4CAF50), 24f, tr("正常", "Norm")),
            Triple(Color(0xFFFFC107), 28f, tr("超重", "Over")),
            Triple(Color(0xFFFF9800), 32f, tr("肥胖I", "Ob I")),
            Triple(Color(0xFFFF5722), 38f, tr("肥胖II", "Ob II")),
            Triple(Color(0xFFF44336), 50f, tr("肥胖III", "Ob III"))
        )
    } else {
        listOf(
            Triple(Color(0xFF2196F3), 18.5f, tr("偏瘦", "Low")),
            Triple(Color(0xFF4CAF50), 25f, tr("正常", "Norm")),
            Triple(Color(0xFFFFC107), 30f, tr("超重", "Over")),
            Triple(Color(0xFFFF9800), 35f, tr("肥胖I", "Ob I")),
            Triple(Color(0xFFFF5722), 40f, tr("肥胖II", "Ob II")),
            Triple(Color(0xFFF44336), 50f, tr("肥胖III", "Ob III"))
        )
    }

    val boundaries = if (standard == BmiStandard.CHINA) {
        listOf(10.0, 18.5, 24.0, 28.0, 32.0, 38.0, 50.0)
    } else {
        listOf(10.0, 18.5, 25.0, 30.0, 35.0, 40.0, 50.0)
    }

    val markerPosition = remember(bmi, standard) {
        val bmiVal = bmi.coerceIn(10.0, 50.0)
        var pos = 0f
        for (i in 0 until boundaries.size - 1) {
            val low = boundaries[i]
            val high = boundaries[i + 1]
            if (bmiVal in low..high) {
                val fraction = (bmiVal - low) / (high - low)
                pos = (i + fraction).toFloat() / (boundaries.size - 1)
                break
            }
        }
        pos
    }
    val animMarker by animatedFloatAsState(markerPosition, durationMs = 900, label = "bmiMarker")
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val ticks = if (standard == BmiStandard.CHINA) {
        listOf("10", "18.5", "24", "28", "32", "38", "50")
    } else {
        listOf("10", "18.5", "25", "30", "35", "40", "50")
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            val segW = size.width / segments.size
            segments.forEachIndexed { i, (color, _, _) ->
                drawRect(color = color, topLeft = Offset(segW * i, 0f), size = Size(segW, size.height))
            }
            val markerX = animMarker * size.width
            drawCircle(
                color = onContainer,
                radius = 8.dp.toPx(),
                center = Offset(markerX.coerceIn(8.dp.toPx(), size.width - 8.dp.toPx()), size.height / 2)
            )
        }
        Layout(
            content = {
                ticks.forEach { tick ->
                    Text(tick, style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.6f))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            val height = placeables.maxOfOrNull { it.height } ?: 0
            layout(constraints.maxWidth, height) {
                val stepCount = boundaries.size - 1
                placeables.forEachIndexed { index, placeable ->
                    val fraction = index.toFloat() / stepCount
                    val x = (constraints.maxWidth * fraction) - (placeable.width / 2f)
                    placeable.placeRelative(x.coerceIn(0f, (constraints.maxWidth - placeable.width).toFloat()).roundToInt(), 0)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(Dimens.CardInnerPadding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun EditableProfileRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(Dimens.IconSizeSmall), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(selectedIndex.coerceAtLeast(0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = option,
                                fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (index == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingContent = {
                            RadioButton(selected = index == selected, onClick = { selected = index })
                        },
                        modifier = Modifier.clickable { selected = index },
                        colors = ListItemDefaults.colors(
                            containerColor = if (index == selected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selected) }) { Text(tr("确定", "Confirm")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("取消", "Cancel")) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
