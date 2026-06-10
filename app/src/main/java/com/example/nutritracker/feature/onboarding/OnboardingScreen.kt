package com.example.nutritracker.feature.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.util.BmiCalc
import com.example.nutritracker.util.CalorieGoalCalc
import com.example.nutritracker.util.MacroCalc
import com.example.nutritracker.util.TdeeCalc
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
            // Progress indicator
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (page + 1f) / totalPages },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                        initialOffsetX = { if (targetState > initialState) it else -it }
                    ) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "page"
            ) { p ->
                when (p) {
                    0 -> GenderPage(vm)
                    1 -> BodyPage(vm)
                    2 -> ActivityPage(vm)
                    3 -> GoalPage(vm)
                    4 -> OverviewPage(vm)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation button
            Button(
                onClick = {
                    if (page < totalPages - 1) page++
                    else {
                        vm.save()
                        onDone()
                    }
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
                    if (page == totalPages - 1) Icons.Filled.Check
                    else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

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
            text = "选择您的性别以获得更准确的卡路里计算",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Gender.entries.forEach { gender ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (vm.gender == gender)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = if (vm.gender == gender) 2.dp else 0.5.dp
                ),
                onClick = { vm.gender = gender }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = vm.gender == gender,
                        onClick = { vm.gender = gender },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = when (gender) {
                            Gender.MALE -> "男"
                            Gender.FEMALE -> "女"
                            Gender.NON_BINARY -> "非二元"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (vm.gender == gender)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (vm.gender == Gender.NON_BINARY) {
            Text(
                text = "卡路里计算配置",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
            CaloriesProfile.entries.forEach { profile ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (vm.caloriesProfile == profile)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = if (vm.caloriesProfile == profile) 2.dp else 0.5.dp
                    ),
                    onClick = { vm.caloriesProfile = profile }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RadioButton(
                            selected = vm.caloriesProfile == profile,
                            onClick = { vm.caloriesProfile = profile },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = when (profile) {
                                CaloriesProfile.AVERAGED -> "平均值"
                                CaloriesProfile.ESTROGEN_TYPICAL -> "雌激素典型"
                                CaloriesProfile.TESTOSTERONE_TYPICAL -> "睾酮典型"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (vm.caloriesProfile == profile)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = vm.birthdayStr,
            onValueChange = { vm.birthdayStr = it },
            label = {
                Text(
                    text = "生日 (YYYY-MM-DD)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun BodyPage(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "身体数据",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "输入您的身高和体重以计算卡路里需求",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = vm.heightStr,
            onValueChange = { vm.heightStr = it },
            label = {
                Text(
                    text = "身高 (cm)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        OutlinedTextField(
            value = vm.weightStr,
            onValueChange = { vm.weightStr = it },
            label = {
                Text(
                    text = "体重 (kg)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        OutlinedTextField(
            value = vm.targetWeightStr,
            onValueChange = { vm.targetWeightStr = it },
            label = {
                Text(
                    text = "目标体重 (kg, 可选)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

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
            text = "选择您的日常活动水平",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ActivityLevel.entries.forEach { level ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (vm.activityLevel == level)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = if (vm.activityLevel == level) 2.dp else 0.5.dp
                ),
                onClick = { vm.activityLevel = level }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = vm.activityLevel == level,
                        onClick = { vm.activityLevel = level },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = when (level) {
                            ActivityLevel.SEDENTARY -> "久坐不动"
                            ActivityLevel.LOW_ACTIVE -> "低度活跃"
                            ActivityLevel.ACTIVE -> "活跃"
                            ActivityLevel.VERY_ACTIVE -> "非常活跃"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (vm.activityLevel == level)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

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
            text = "选择您的体重管理目标",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        WeightGoal.entries.forEach { goal ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (vm.weightGoal == goal)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = if (vm.weightGoal == goal) 2.dp else 0.5.dp
                ),
                onClick = { vm.weightGoal = goal }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = vm.weightGoal == goal,
                        onClick = { vm.weightGoal = goal },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = when (goal) {
                            WeightGoal.LOSE -> "减重"
                            WeightGoal.MAINTAIN -> "维持体重"
                            WeightGoal.GAIN -> "增重"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (vm.weightGoal == goal)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (vm.weightGoal != WeightGoal.MAINTAIN) {
            OutlinedTextField(
                value = vm.weeklyGoalStr,
                onValueChange = { vm.weeklyGoalStr = it },
                label = {
                    Text(
                        text = "每周目标 (kg/周, 可选)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun OverviewPage(vm: OnboardingViewModel) {
    val user = vm.buildUser() ?: run {
        Text(
            text = "请完善前面的信息",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val tdee = TdeeCalc.getTdeeKcal(user)
    val goal = CalorieGoalCalc.getTotalKcalGoal(user, 0.0)
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

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "健康指标",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OverviewRow("BMI", "%.1f (%s)".format(bmi, status.label))
                OverviewRow("TDEE", "${tdee.roundToInt()} kcal")
                OverviewRow("每日目标", "${goal.roundToInt()} kcal")
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
