package com.example.nutritracker.feature.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nutritracker.R
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.ui.components.AppDatePickerDialog
import com.example.nutritracker.ui.components.NumberPickerDialog
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    onNavigateToSources: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel()
) {
    var page by remember { mutableIntStateOf(0) }
    val totalPages = 5

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "欢迎使用 NutriTracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (page > 0) {
                        IconButton(onClick = { page-- }, enabled = !vm.isSaving) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(totalPages) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .background(
                                    color = if (index <= page) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }
                Text(
                    text = "步骤 ${page + 1} / $totalPages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val isForward = targetState > initialState
                    val enter = fadeIn(tween(M3Duration.Medium2, easing = M3Easing.Decelerate)) +
                        slideInHorizontally(
                            initialOffsetX = { if (isForward) it / 3 else -it / 3 },
                            animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate)
                        )
                    val exit = fadeOut(tween(M3Duration.Short3, easing = M3Easing.Accelerate)) +
                        slideOutHorizontally(
                            targetOffsetX = { if (isForward) -it / 3 else it / 3 },
                            animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate)
                        )
                    enter togetherWith exit
                },
                label = "onboardingPage"
            ) { p ->
                when (p) {
                    0 -> GenderPage(vm)
                    1 -> BodyPage(vm)
                    2 -> ActivityPage(vm)
                    3 -> GoalPage(vm)
                    4 -> OverviewPage(vm, onNavigateToSources)
                }
            }

            val validationError = vm.getValidationError(page)
            if (validationError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text(validationError, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (page < totalPages - 1) page++
                    else vm.save(onDone)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeight),
                enabled = vm.canProceed(page),
                shape = MaterialTheme.shapes.medium
            ) {
                if (vm.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = if (page == totalPages - 1) "开始使用" else "下一步",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (page == totalPages - 1) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GenderPage(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("性别", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "选择您的性别以获得更准确的卡路里计算\n参考: IOM 2005 膳食营养素参考摄入量",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Gender.entries.forEach { gender ->
            SelectableCard(
                selected = vm.gender == gender,
                onClick = { vm.gender = gender },
                label = when (gender) {
                    Gender.MALE -> "男"
                    Gender.FEMALE -> "女"
                    Gender.NON_BINARY -> "非二元"
                },
                description = when (gender) {
                    Gender.MALE -> "使用男性 TDEE 公式"
                    Gender.FEMALE -> "使用女性 TDEE 公式"
                    Gender.NON_BINARY -> "可选择计算方式"
                }
            )
        }

        if (vm.gender == Gender.NON_BINARY) {
            Text("卡路里计算配置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            CaloriesProfile.entries.forEach { profile ->
                SelectableCard(
                    selected = vm.caloriesProfile == profile,
                    onClick = { vm.caloriesProfile = profile },
                    label = when (profile) {
                        CaloriesProfile.AVERAGED -> "平均值"
                        CaloriesProfile.ESTROGEN_TYPICAL -> "雌激素典型"
                        CaloriesProfile.TESTOSTERONE_TYPICAL -> "睾酮典型"
                    },
                    description = when (profile) {
                        CaloriesProfile.AVERAGED -> "取男女公式的平均值"
                        CaloriesProfile.ESTROGEN_TYPICAL -> "使用女性公式计算"
                        CaloriesProfile.TESTOSTERONE_TYPICAL -> "使用男性公式计算"
                    }
                )
            }
        }

        var showBirthdayDialog by remember { mutableStateOf(false) }
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBirthdayDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("生日", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(vm.birthdayStr, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (showBirthdayDialog) {
            val initialDate = runCatching { LocalDate.parse(vm.birthdayStr) }.getOrElse { LocalDate.now().minusYears(25) }
            AppDatePickerDialog(
                initialDate = initialDate,
                onDismiss = { showBirthdayDialog = false },
                onConfirm = {
                    vm.birthdayStr = it.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    showBirthdayDialog = false
                }
            )
        }
    }
}

@Composable
private fun BodyPage(vm: OnboardingViewModel) {
    var showHeightDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showTargetWeightDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("身体数据", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "输入您的身高和体重以计算卡路里需求\nBMI = 体重(kg) / 身高(m)²",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PickerRow(
            label = "身高",
            value = if (vm.heightStr.isNotEmpty()) "${vm.heightStr} cm" else "点击设置",
            onClick = { showHeightDialog = true }
        )

        PickerRow(
            label = "体重",
            value = if (vm.weightStr.isNotEmpty()) "${vm.weightStr} kg" else "点击设置",
            onClick = { showWeightDialog = true }
        )

        PickerRow(
            label = "目标体重 (可选)",
            value = if (vm.targetWeightStr.isNotEmpty()) "${vm.targetWeightStr} kg" else "点击设置",
            onClick = { showTargetWeightDialog = true }
        )

        val height = vm.heightStr.toDoubleOrNull()
        val weight = vm.weightStr.toDoubleOrNull()
        if (height != null && weight != null && height > 0 && weight > 0) {
            val bmi = BmiCalc.getBmi(weight, height)
            val status = BmiCalc.getNutritionalStatus(bmi, vm.bmiStandard)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BMI 预览", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.extraSmall)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (std in BmiStandard.entries) {
                                val active = vm.bmiStandard == std
                                Box(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { vm.bmiStandard = std }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (std == BmiStandard.CHINA) "中国标准" else "世界标准",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("健康状态", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${UnitCalc.formatDecimal(bmi, 1)} (${status.getLabel(vm.bmiStandard)})",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showHeightDialog) {
        NumberPickerDialog(
            title = "身高",
            unit = "cm",
            initialValue = vm.heightStr.toDoubleOrNull() ?: 170.0,
            range = 100.0..250.0,
            step = 0.5,
            onDismiss = { showHeightDialog = false },
            onConfirm = { vm.heightStr = UnitCalc.formatDecimal(it, 1); showHeightDialog = false }
        )
    }
    if (showWeightDialog) {
        NumberPickerDialog(
            title = "体重",
            unit = "kg",
            initialValue = vm.weightStr.toDoubleOrNull() ?: 70.0,
            range = 30.0..300.0,
            step = 0.1,
            onDismiss = { showWeightDialog = false },
            onConfirm = { vm.weightStr = UnitCalc.formatDecimal(it, 1); showWeightDialog = false }
        )
    }
    if (showTargetWeightDialog) {
        NumberPickerDialog(
            title = "目标体重",
            unit = "kg",
            initialValue = vm.targetWeightStr.toDoubleOrNull() ?: vm.weightStr.toDoubleOrNull() ?: 65.0,
            range = 30.0..300.0,
            step = 0.1,
            onDismiss = { showTargetWeightDialog = false },
            onConfirm = { vm.targetWeightStr = UnitCalc.formatDecimal(it, 1); showTargetWeightDialog = false }
        )
    }
}

@Composable
private fun ActivityPage(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("活动水平", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "选择您的日常活动水平\n参考: IOM 2004 体力活动建议 (PMID: 15113740)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ActivityLevel.entries.forEach { level ->
            SelectableCard(
                selected = vm.activityLevel == level,
                onClick = { vm.activityLevel = level },
                label = when (level) {
                    ActivityLevel.SEDENTARY -> "久坐不动"
                    ActivityLevel.LOW_ACTIVE -> "低度活跃"
                    ActivityLevel.ACTIVE -> "活跃"
                    ActivityLevel.VERY_ACTIVE -> "非常活跃"
                },
                description = when (level) {
                    ActivityLevel.SEDENTARY -> "PAL 1.25 - 办公室工作,很少运动"
                    ActivityLevel.LOW_ACTIVE -> "PAL 1.5 - 轻度运动,每周1-3次"
                    ActivityLevel.ACTIVE -> "PAL 1.75 - 中度运动,每周3-5次"
                    ActivityLevel.VERY_ACTIVE -> "PAL 2.2 - 高强度运动,每周6-7次"
                }
            )
        }
    }
}

@Composable
private fun GoalPage(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("体重目标", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "选择您的体重管理目标\n1kg 体脂 ≈ 7700 kcal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        WeightGoal.entries.forEach { goal ->
            SelectableCard(
                selected = vm.weightGoal == goal,
                onClick = { vm.weightGoal = goal },
                label = when (goal) {
                    WeightGoal.LOSE -> "减重"
                    WeightGoal.MAINTAIN -> "维持体重"
                    WeightGoal.GAIN -> "增重"
                },
                description = when (goal) {
                    WeightGoal.LOSE -> "每日目标 = TDEE - 500 kcal (每周约减0.5kg)"
                    WeightGoal.MAINTAIN -> "每日目标 = TDEE"
                    WeightGoal.GAIN -> "每日目标 = TDEE + 500 kcal (每周约增0.5kg)"
                }
            )
        }

        if (vm.weightGoal != WeightGoal.MAINTAIN) {
            var showWeeklyGoalDialog by remember { mutableStateOf(false) }
            PickerRow(
                label = "每周目标 (可选)",
                value = if (vm.weeklyGoalStr.isNotEmpty()) "${vm.weeklyGoalStr} kg/周" else "默认 0.5 kg/周",
                onClick = { showWeeklyGoalDialog = true }
            )

            if (showWeeklyGoalDialog) {
                NumberPickerDialog(
                    title = "每周目标",
                    unit = "kg/周",
                    initialValue = vm.weeklyGoalStr.toDoubleOrNull() ?: 0.5,
                    range = 0.1..2.0,
                    step = 0.1,
                    onDismiss = { showWeeklyGoalDialog = false },
                    onConfirm = { vm.weeklyGoalStr = UnitCalc.formatDecimal(it, 1); showWeeklyGoalDialog = false }
                )
            }
        }
    }
}

@Composable
private fun OverviewPage(
    vm: OnboardingViewModel,
    onNavigateToSources: () -> Unit
) {
    val user = vm.buildUser() ?: run {
        Text("请完善前面的信息", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val tdee = TdeeCalc.getTdeeKcal(user)
    val goal = CalorieGoalCalc.getTotalKcalGoal(user)
    val carbs = MacroCalc.getCarbsGoal(goal)
    val fat = MacroCalc.getFatGoal(goal)
    val protein = MacroCalc.getProteinGoal(goal)
    val bmi = BmiCalc.getBmi(user.weightKg, user.heightCm)
    val status = BmiCalc.getNutritionalStatus(bmi, vm.bmiStandard)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("概览", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("根据您输入的信息，以下是您的卡路里和营养目标", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("健康指标", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OverviewRow("BMI", "${UnitCalc.formatDecimal(bmi, 1)} (${status.getLabel(vm.bmiStandard)})")
                OverviewRow("TDEE", "${tdee.roundToInt()} kcal")
                OverviewRow("每日目标", "${goal.roundToInt()} kcal")

                val floor = CalorieGoalCalc.recommendedFloor(user)
                if (CalorieGoalCalc.isBelowRecommendedFloor(goal, user)) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = "⚠ 目标低于推荐最低值 ${floor.roundToInt()} kcal",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("营养目标", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OverviewRow("碳水目标", "${carbs.roundToInt()}g (60%)")
                OverviewRow("脂肪目标", "${fat.roundToInt()}g (25%)")
                OverviewRow("蛋白质目标", "${protein.roundToInt()}g (15%)")
                Text("参考: WHO TRS 894, p.104", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSources() },
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("科学依据", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ChevronRight, "查看详情", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                Text("查看 IOM 2005 TDEE、WHO BMI/宏量与 2024 MET 原始文献 →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        // 医疗免责声明
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.medical_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    description: String
) {
    val scale by animatedSpringScale(selected = selected)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (selected) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OverviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
