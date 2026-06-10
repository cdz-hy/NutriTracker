package com.example.nutritracker.feature.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.util.*
import com.example.nutritracker.ui.theme.*
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
    var page by remember { mutableStateOf(0) }
    val totalPages = 5

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "欢迎使用 NutriTracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    if (page > 0) {
                        IconButton(onClick = { page-- }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Progress
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(totalPages) { index ->
                        val isCompletedOrActive = index <= page
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .background(
                                    color = if (isCompletedOrActive) MaterialTheme.colorScheme.primary
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

            // Page content
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val isForward = targetState > initialState
                    val enter = fadeIn(tween(M3Duration.Medium2, easing = M3Easing.Decelerate)) + slideInHorizontally(
                        initialOffsetX = { if (isForward) it / 3 else -it / 3 },
                        animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate)
                    )
                    val exit = fadeOut(tween(M3Duration.Short3, easing = M3Easing.Accelerate)) + slideOutHorizontally(
                        targetOffsetX = { if (isForward) -it / 3 else it / 3 },
                        animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate)
                    )
                    enter togetherWith exit
                },
                label = "page"
            ) { p ->
                when (p) {
                    0 -> GenderPage(vm)
                    1 -> BodyPage(vm)
                    2 -> ActivityPage(vm)
                    3 -> GoalPage(vm)
                    4 -> OverviewPage(vm, onNavigateToSources)
                }
            }

            // Validation error message
            val validationError = vm.getValidationError(page)
            if (validationError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = validationError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation button
            Button(
                onClick = {
                    if (page < totalPages - 1) page++
                    else { vm.save(); onDone() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = vm.canProceed(page),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.medium
            ) {
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 性别页面
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GenderPage(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "性别",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            Text(
                text = "卡路里计算配置",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
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

        // Birthday
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
                    Text(
                        text = "生日",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = vm.birthdayStr,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showBirthdayDialog) {
            val initialDate = try { LocalDate.parse(vm.birthdayStr) } catch (_: Exception) { LocalDate.now().minusYears(25) }
            DatePickerDialog(
                initialDate = initialDate,
                onDismiss = { showBirthdayDialog = false },
                onConfirm = { vm.birthdayStr = it.format(DateTimeFormatter.ISO_LOCAL_DATE); showBirthdayDialog = false }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 身体数据页面
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BodyPage(vm: OnboardingViewModel) {
    var showHeightDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showTargetWeightDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "身体数据",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "输入您的身高和体重以计算卡路里需求\nBMI = 体重(kg) / 身高(m)²",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Height
        PickerRow(
            label = "身高",
            value = if (vm.heightStr.isNotEmpty()) "${vm.heightStr} cm" else "点击设置",
            onClick = { showHeightDialog = true }
        )

        // Weight
        PickerRow(
            label = "体重",
            value = if (vm.weightStr.isNotEmpty()) "${vm.weightStr} kg" else "点击设置",
            onClick = { showWeightDialog = true }
        )

        // Target weight
        PickerRow(
            label = "目标体重 (可选)",
            value = if (vm.targetWeightStr.isNotEmpty()) "${vm.targetWeightStr} kg" else "点击设置",
            onClick = { showTargetWeightDialog = true }
        )

        // BMI preview
        val height = vm.heightStr.toDoubleOrNull()
        val weight = vm.weightStr.toDoubleOrNull()
        if (height != null && weight != null) {
            val bmi = BmiCalc.getBmi(weight, height)
            val status = BmiCalc.getNutritionalStatus(bmi, vm.bmiStandard)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
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
                        Text(
                            text = "BMI 预览",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (std in BmiStandard.entries) {
                                val active = vm.bmiStandard == std
                                Box(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
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
                        Text(
                            text = "健康状态",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.1f (%s)".format(bmi, status.getLabel(vm.bmiStandard)),
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
            onConfirm = { vm.heightStr = "%.1f".format(it); showHeightDialog = false }
        )
    }
    if (showWeightDialog) {
        NumberPickerDialog(
            title = "体重",
            unit = "kg",
            initialValue = vm.weightStr.toDoubleOrNull() ?: 70.0,
            range = 30.0..180.0,
            step = 0.1,
            onDismiss = { showWeightDialog = false },
            onConfirm = { vm.weightStr = "%.1f".format(it); showWeightDialog = false }
        )
    }
    if (showTargetWeightDialog) {
        NumberPickerDialog(
            title = "目标体重",
            unit = "kg",
            initialValue = vm.targetWeightStr.toDoubleOrNull() ?: vm.weightStr.toDoubleOrNull() ?: 65.0,
            range = 30.0..180.0,
            step = 0.1,
            onDismiss = { showTargetWeightDialog = false },
            onConfirm = { vm.targetWeightStr = "%.1f".format(it); showTargetWeightDialog = false }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 活动水平页面
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ActivityPage(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "活动水平",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
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

// ══════════════════════════════════════════════════════════════════════════════
// 体重目标页面
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GoalPage(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "体重目标",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
                    onConfirm = { vm.weeklyGoalStr = "%.1f".format(it); showWeeklyGoalDialog = false }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 概览页面
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OverviewPage(
    vm: OnboardingViewModel,
    onNavigateToSources: () -> Unit
) {
    val user = vm.buildUser() ?: run {
        Text(
            text = "请完善前面的信息",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val tdee = TdeeCalc.getTdeeKcal(user)
    val goal = CalorieGoalCalc.getTotalKcalGoal(user)
    val carbs = MacroCalc.getCarbsGoal(goal)
    val fat = MacroCalc.getFatGoal(goal)
    val protein = MacroCalc.getProteinGoal(goal)
    val bmi = BmiCalc.getBmi(user.weightKg, user.heightCm)
    val status = BmiCalc.getNutritionalStatus(bmi)

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "概览",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "根据您输入的信息，以下是您的卡路里和营养目标",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Health metrics
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "健康指标",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val overviewStatus = BmiCalc.getNutritionalStatus(bmi, vm.bmiStandard)
                OverviewRow("BMI", "%.1f (%s)".format(bmi, overviewStatus.getLabel(vm.bmiStandard)))
                OverviewRow("TDEE", "${tdee.roundToInt()} kcal")
                Text(
                    text = "TDEE = 每日总能量消耗，即维持当前体重所需的卡路里",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                OverviewRow("每日目标", "${goal.roundToInt()} kcal")

                val floor = CalorieGoalCalc.recommendedFloor(user)
                if (CalorieGoalCalc.isBelowRecommendedFloor(goal, user)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
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

        // Nutrition goals
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "营养目标",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OverviewRow("碳水目标", "${carbs.roundToInt()}g (60%)")
                OverviewRow("脂肪目标", "${fat.roundToInt()}g (25%)")
                OverviewRow("蛋白质目标", "${protein.roundToInt()}g (15%)")
                Text(
                    text = "参考: WHO TRS 894, p.104",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Scientific references
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToSources() },
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "科学依据",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "查看详情",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                ReferenceLink("IOM 2005 TDEE 公式", "DOI: 10.17226/10490")
                ReferenceLink("WHO 宏量营养素比例", "WHO TRS 894, ISBN 92 4 120894 5")
                ReferenceLink("BMI 分类标准", "WHO Europe")
                ReferenceLink("MET 运动消耗", "2024 Compendium, PMID: 38242596")
                ReferenceLink("最低卡路里建议", "Harvard Health Publishing")

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击可查看所有指标背后的完整科学依据 →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 通用组件
// ══════════════════════════════════════════════════════════════════════════════

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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (selected) 2.dp else 0.5.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
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
                    style = MaterialTheme.typography.bodyLarge,
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
}

@Composable
private fun OverviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
private fun ReferenceLink(title: String, source: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = source,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 数字选择器对话框
// ══════════════════════════════════════════════════════════════════════════════

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
                        textAlign = TextAlign.Center
                    )
                }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(-1.0, -0.5, -0.1, 0.1, 0.5, 1.0).forEach { delta ->
                        FilledTonalButton(
                            onClick = { currentValue = (currentValue + delta).coerceIn(range) },
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

// ══════════════════════════════════════════════════════════════════════════════
// 日期选择器对话框
// ══════════════════════════════════════════════════════════════════════════════

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
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
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
