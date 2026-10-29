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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val isZh = LocalIsChinese.current
    val appLanguage by vm.appLanguage.collectAsStateWithLifecycle()
    var showLangMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tr("欢迎使用 NutriTracker", "Welcome to NutriTracker"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (page > 0) {
                        IconButton(onClick = { page-- }, enabled = !vm.isSaving) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("返回", "Back"))
                        }
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { showLangMenu = true }) {
                            Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(LocaleHelper.appLanguageLabel(appLanguage, isZh))
                        }
                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false }
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(LocaleHelper.appLanguageLabel(lang, isZh)) },
                                    trailingIcon = {
                                        if (appLanguage == lang) Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        vm.setAppLanguage(lang)
                                        showLangMenu = false
                                    }
                                )
                            }
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
                    text = tr("步骤 ${page + 1} / $totalPages", "Step ${page + 1} / $totalPages"),
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
                        text = if (page == totalPages - 1) tr("开始使用", "Get Started") else tr("下一步", "Next"),
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
    val isZh = LocalIsChinese.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(tr("性别", "Biological Sex"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = tr(
                "选择您的性别以获得更准确的卡路里计算\n参考: IOM 2005 膳食营养素参考摄入量",
                "Select your sex for accurate calorie estimation\nRef: IOM 2005 Dietary Reference Intakes"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Gender.entries.forEach { gender ->
            SelectableCard(
                selected = vm.gender == gender,
                onClick = { vm.gender = gender },
                label = when (gender) {
                    Gender.MALE -> tr("男", "Male")
                    Gender.FEMALE -> tr("女", "Female")
                    Gender.NON_BINARY -> tr("非二元", "Non-binary")
                },
                description = when (gender) {
                    Gender.MALE -> tr("使用男性 TDEE 公式", "Uses male TDEE equation")
                    Gender.FEMALE -> tr("使用女性 TDEE 公式", "Uses female TDEE equation")
                    Gender.NON_BINARY -> tr("可选择计算方式", "Choose calculation profile")
                }
            )
        }

        if (vm.gender == Gender.NON_BINARY) {
            Text(tr("卡路里计算配置", "Calorie Profile"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            CaloriesProfile.entries.forEach { profile ->
                SelectableCard(
                    selected = vm.caloriesProfile == profile,
                    onClick = { vm.caloriesProfile = profile },
                    label = when (profile) {
                        CaloriesProfile.AVERAGED -> tr("平均值", "Averaged")
                        CaloriesProfile.ESTROGEN_TYPICAL -> tr("雌激素典型", "Estrogen-typical")
                        CaloriesProfile.TESTOSTERONE_TYPICAL -> tr("睾酮典型", "Testosterone-typical")
                    },
                    description = when (profile) {
                        CaloriesProfile.AVERAGED -> tr("取男女公式的平均值", "Average of male and female equations")
                        CaloriesProfile.ESTROGEN_TYPICAL -> tr("使用女性公式计算", "Uses female equation")
                        CaloriesProfile.TESTOSTERONE_TYPICAL -> tr("使用男性公式计算", "Uses male equation")
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
                    Text(tr("生日", "Birthday"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val isZh = LocalIsChinese.current
    var showHeightDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showTargetWeightDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(tr("身体数据", "Body Metrics"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = tr(
                "输入您的身高和体重以计算卡路里需求\nBMI = 体重(kg) / 身高(m)²",
                "Enter height and weight to estimate calorie needs\nBMI = weight (kg) / height² (m²)"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PickerRow(
            label = tr("身高", "Height"),
            value = if (vm.heightStr.isNotEmpty()) "${vm.heightStr} cm" else tr("点击设置", "Tap to set"),
            onClick = { showHeightDialog = true }
        )

        PickerRow(
            label = tr("体重", "Weight"),
            value = if (vm.weightStr.isNotEmpty()) "${vm.weightStr} kg" else tr("点击设置", "Tap to set"),
            onClick = { showWeightDialog = true }
        )

        PickerRow(
            label = tr("目标体重 (可选)", "Target Weight (optional)"),
            value = if (vm.targetWeightStr.isNotEmpty()) "${vm.targetWeightStr} kg" else tr("点击设置", "Tap to set"),
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
                        Text(tr("BMI 预览", "BMI Preview"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        text = if (std == BmiStandard.CHINA) tr("中国标准", "WGOC") else tr("世界标准", "WHO"),
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
                        Text(tr("健康状态", "Status"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${UnitCalc.formatDecimal(bmi, 1)} (${status.getLabel(vm.bmiStandard, isZh)})",
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
            title = tr("身高", "Height"),
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
            title = tr("体重", "Weight"),
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
            title = tr("目标体重", "Target Weight"),
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
        Text(tr("活动水平", "Activity Level"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = tr(
                "选择您的日常活动水平\n参考: IOM 2004 体力活动建议 (PMID: 15113740)",
                "Select your typical daily activity level\nRef: IOM 2004 Physical Activity (PMID: 15113740)"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ActivityLevel.entries.forEach { level ->
            SelectableCard(
                selected = vm.activityLevel == level,
                onClick = { vm.activityLevel = level },
                label = when (level) {
                    ActivityLevel.SEDENTARY -> tr("久坐不动", "Sedentary")
                    ActivityLevel.LOW_ACTIVE -> tr("低度活跃", "Low Active")
                    ActivityLevel.ACTIVE -> tr("活跃", "Active")
                    ActivityLevel.VERY_ACTIVE -> tr("非常活跃", "Very Active")
                },
                description = when (level) {
                    ActivityLevel.SEDENTARY -> tr("PAL 1.25 - 办公室工作,很少运动", "PAL 1.25 — Desk job, minimal exercise")
                    ActivityLevel.LOW_ACTIVE -> tr("PAL 1.5 - 轻度运动,每周1-3次", "PAL 1.5 — Light exercise 1–3x/week")
                    ActivityLevel.ACTIVE -> tr("PAL 1.75 - 中度运动,每周3-5次", "PAL 1.75 — Moderate exercise 3–5x/week")
                    ActivityLevel.VERY_ACTIVE -> tr("PAL 2.2 - 高强度运动,每周6-7次", "PAL 2.2 — Vigorous exercise 6–7x/week")
                }
            )
        }
    }
}

@Composable
private fun GoalPage(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(tr("体重目标", "Weight Goal"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = tr(
                "选择您的体重管理目标\n1kg 体脂 ≈ 7700 kcal",
                "Choose your weight target\n1 kg body fat ≈ 7,700 kcal"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        WeightGoal.entries.forEach { goal ->
            SelectableCard(
                selected = vm.weightGoal == goal,
                onClick = { vm.weightGoal = goal },
                label = when (goal) {
                    WeightGoal.LOSE -> tr("减重", "Lose Weight")
                    WeightGoal.MAINTAIN -> tr("维持体重", "Maintain Weight")
                    WeightGoal.GAIN -> tr("增重", "Gain Weight")
                },
                description = when (goal) {
                    WeightGoal.LOSE -> tr("每日目标 = TDEE - 500 kcal (每周约减0.5kg)", "Daily goal = TDEE - 500 kcal (~0.5 kg/wk loss)")
                    WeightGoal.MAINTAIN -> tr("每日目标 = TDEE", "Daily goal = TDEE")
                    WeightGoal.GAIN -> tr("每日目标 = TDEE + 500 kcal (每周约增0.5kg)", "Daily goal = TDEE + 500 kcal (~0.5 kg/wk gain)")
                }
            )
        }

        if (vm.weightGoal != WeightGoal.MAINTAIN) {
            var showWeeklyGoalDialog by remember { mutableStateOf(false) }
            PickerRow(
                label = tr("每周目标 (可选)", "Weekly Rate (optional)"),
                value = if (vm.weeklyGoalStr.isNotEmpty()) {
                    tr("${vm.weeklyGoalStr} kg/周", "${vm.weeklyGoalStr} kg/wk")
                } else {
                    tr("默认 0.5 kg/周", "Default 0.5 kg/wk")
                },
                onClick = { showWeeklyGoalDialog = true }
            )

            if (showWeeklyGoalDialog) {
                NumberPickerDialog(
                    title = tr("每周目标", "Weekly Rate"),
                    unit = tr("kg/周", "kg/wk"),
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
    val isZh = LocalIsChinese.current
    val context = LocalContext.current
    val user = vm.buildUser() ?: run {
        Text(tr("请完善前面的信息", "Please complete previous steps"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(tr("概览", "Summary"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            tr("根据您输入的信息，以下是您的卡路里和营养目标", "Here are your personalized calorie and nutrition targets"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("健康指标", "Health Metrics"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OverviewRow("BMI", "${UnitCalc.formatDecimal(bmi, 1)} (${status.getLabel(vm.bmiStandard, isZh)})")
                OverviewRow("TDEE", "${tdee.roundToInt()} kcal")
                OverviewRow(tr("每日目标", "Daily Goal"), "${goal.roundToInt()} kcal")

                val floor = CalorieGoalCalc.recommendedFloor(user)
                if (CalorieGoalCalc.isBelowRecommendedFloor(goal, user)) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = tr("⚠ 目标低于推荐最低值 ${floor.roundToInt()} kcal", "⚠ Goal below recommended ${floor.roundToInt()} kcal floor"),
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
                Text(tr("营养目标", "Macro Targets"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OverviewRow(tr("碳水目标", "Carbs Goal"), "${carbs.roundToInt()}g (60%)")
                OverviewRow(tr("脂肪目标", "Fat Goal"), "${fat.roundToInt()}g (25%)")
                OverviewRow(tr("蛋白质目标", "Protein Goal"), "${protein.roundToInt()}g (15%)")
                Text(
                    tr("参考: WHO TRS 894, p.104", "Ref: WHO TRS 894, p.104"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
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
                    Text(tr("科学依据", "Scientific Basis"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ChevronRight, tr("查看详情", "View details"), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                Text(
                    tr("查看 IOM 2005 TDEE、WHO BMI/宏量与 2024 MET 原始文献 →", "View IOM 2005 TDEE, WHO BMI/Macro & 2024 MET references →"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Medical disclaimer
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = context.getString(R.string.medical_disclaimer),
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
