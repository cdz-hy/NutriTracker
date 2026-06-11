package com.example.nutritracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nutritracker.data.entity.UserActivityEntity
import com.example.nutritracker.data.entity.Intake
import com.example.nutritracker.feature.home.MealSection
import com.example.nutritracker.feature.home.mealTypeIcon
import com.example.nutritracker.feature.home.mealTypeLabel
import com.example.nutritracker.ui.theme.*
import kotlin.math.roundToInt

// ══════════════════════════════════════════════════════════════════════════════
// 卡路里总览卡片 - 圆环进度 + 数字动画
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun CalorieOverviewCard(
    goal: Double,
    supplied: Double,
    burned: Double,
    left: Double,
    isBelowFloor: Boolean,
    floor: Double,
    onNavigateToSources: () -> Unit
) {
    var warningExpanded by remember { mutableStateOf(false) }
    val animGoal by animatedIntAsState(goal.roundToInt())
    val animSupplied by animatedIntAsState(supplied.roundToInt())
    val animBurned by animatedIntAsState(burned.roundToInt())
    val animLeft by animatedIntAsState(left.roundToInt())

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = Dimens.CardElevation
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.CardInnerPadding)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日卡路里",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onNavigateToSources,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        contentDescription = "查看计算科学依据",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.IconSizeSmall)
                    )
                }
            }

            // 圆环进度 + 数值区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 圆环进度指示器
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    CalorieRing(
                        supplied = supplied,
                        burned = burned,
                        goal = goal,
                        modifier = Modifier.size(100.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$animLeft",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (left < 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "剩余",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 数值列
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KcalRow(
                        icon = Icons.Filled.Flag,
                        label = "目标",
                        value = animGoal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    KcalRow(
                        icon = Icons.Filled.Restaurant,
                        label = "已摄入",
                        value = animSupplied,
                        color = MaterialTheme.colorScheme.primary
                    )
                    KcalRow(
                        icon = Icons.Filled.LocalFireDepartment,
                        label = "已燃烧",
                        value = animBurned,
                        color = BurnColor
                    )
                }
            }

            // 低卡警告
            if (isBelowFloor) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { warningExpanded = !warningExpanded },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(Dimens.IconSizeSmall)
                            )
                            Text(
                                text = "目标低于推荐值 ${floor.roundToInt()} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (warningExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = warningExpanded,
                            enter = fadeSlideIn(offsetY = 20),
                            exit = fadeSlideOut(offsetY = 20)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "在没有医疗指导的情况下，成年人不宜长期每天摄入低于 ${floor.roundToInt()} kcal 的热量。这可能导致肌肉流失与代谢放缓。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                TextButton(
                                    onClick = {
                                        warningExpanded = false
                                        onNavigateToSources()
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = "查看医学依据与说明 →",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 圆环进度 - 双层弧形（摄入 + 燃烧）
 */
@Composable
private fun CalorieRing(
    supplied: Double,
    burned: Double,
    goal: Double,
    modifier: Modifier = Modifier
) {
    val suppliedRatio = if (goal > 0) (supplied / goal).coerceIn(0.0, 1.5) else 0.0
    val burnedRatio = if (goal > 0) (burned / goal).coerceIn(0.0, 0.5) else 0.0

    val animSupplied by animatedFloatAsState(suppliedRatio.toFloat(), label = "suppliedArc")
    val animBurned by animatedFloatAsState(burnedRatio.toFloat(), label = "burnedArc")

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val suppliedColor = MaterialTheme.colorScheme.primary
    val burnedColor = BurnColor

    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        val innerStroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        val padding = 8.dp.toPx()
        val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
        val arcOffset = Offset(padding, padding)

        // 轨道
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arcOffset,
            size = arcSize,
            style = stroke
        )
        // 摄入弧
        drawArc(
            color = suppliedColor,
            startAngle = -90f,
            sweepAngle = animSupplied * 360f,
            useCenter = false,
            topLeft = arcOffset,
            size = arcSize,
            style = stroke
        )
        // 燃烧弧（内圈）
        if (animBurned > 0f) {
            val innerPadding = 18.dp.toPx()
            val innerSize = Size(size.width - innerPadding * 2, size.height - innerPadding * 2)
            val innerOffset = Offset(innerPadding, innerPadding)
            drawArc(
                color = trackColor.copy(alpha = 0.5f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = innerOffset,
                size = innerSize,
                style = innerStroke
            )
            drawArc(
                color = burnedColor,
                startAngle = -90f,
                sweepAngle = animBurned * 360f,
                useCenter = false,
                topLeft = innerOffset,
                size = innerSize,
                style = innerStroke
            )
        }
    }
}

@Composable
private fun KcalRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = "$value",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 宏量营养素进度 - 语义化颜色 + 进度动画
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun MacroProgressRow(
    carbsCurrent: Double,
    carbsGoal: Double,
    fatCurrent: Double,
    fatGoal: Double,
    proteinCurrent: Double,
    proteinGoal: Double
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = Dimens.CardElevation
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardInnerPadding),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroItem("碳水", carbsCurrent, carbsGoal, if (isDark) CarbsColorDark else CarbsColor)
            MacroItem("脂肪", fatCurrent, fatGoal, if (isDark) FatColorDark else FatColor)
            MacroItem("蛋白质", proteinCurrent, proteinGoal, if (isDark) ProteinColorDark else ProteinColor)
        }
    }
}

@Composable
private fun MacroItem(
    label: String,
    current: Double,
    goal: Double,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val targetProgress = if (goal > 0) (current / goal).coerceIn(0.0, 1.0).toFloat() else 0f
        val animProgress by animatedFloatAsState(targetProgress, durationMs = 1000, label = "${label}Progress")

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animProgress },
                modifier = Modifier.size(64.dp),
                color = color,
                trackColor = color.copy(alpha = 0.12f),
                strokeWidth = 8.dp
            )
            Text(
                text = "${(animProgress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${current.roundToInt()}/${goal.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 饮水卡片 - 动画进度 + 按钮弹跳
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun WaterCard(
    currentMl: Int,
    goalMl: Int,
    onAdd: (() -> Unit)? = null,
    onUndo: (() -> Unit)? = null
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val waterColor = if (isDark) WaterColorDark else WaterColor
    val targetProgress = (currentMl.toFloat() / goalMl).coerceIn(0f, 1f)
    val animProgress by animatedFloatAsState(targetProgress, durationMs = 600, label = "waterProgress")
    val animMl by animatedIntAsState(currentMl, durationMs = 600, label = "waterMl")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = Dimens.CardElevation
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = waterColor,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "饮水",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    progress = { animProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = waterColor,
                    trackColor = waterColor.copy(alpha = 0.15f)
                )
                Text(
                    text = "${animMl}ml / ${goalMl}ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onAdd != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FilledTonalButton(
                        onClick = onAdd,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = waterColor.copy(alpha = 0.15f),
                            contentColor = waterColor
                        )
                    ) {
                        Text("+250ml")
                    }
                    if (currentMl > 0 && onUndo != null) {
                        TextButton(
                            onClick = onUndo,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("撤回", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 餐食分区卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun MealSectionCard(
    section: MealSection,
    onAddClick: () -> Unit,
    onDeleteIntake: (Intake) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = Dimens.CardElevation
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.CardInnerPaddingCompact)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    mealTypeIcon(section.type),
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSizeMedium),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = mealTypeLabel(section.type),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${section.totalKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "添加",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (section.intakes.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            section.intakes.forEach { intake ->
                val meal = section.meals[intake.mealId]
                ListItem(
                    headlineContent = {
                        Text(
                            text = meal?.name ?: "未知食物",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "${intake.amount.roundToInt()}g · ${(meal?.energyKcal100?.times(intake.amount)?.div(100))?.roundToInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        IconButton(
                            onClick = { onDeleteIntake(intake) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(Dimens.IconSizeSmall)
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 活动条目
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ActivityItem(
    activity: UserActivityEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    text = "${activity.durationMinutes.roundToInt()} 分钟 · ${activity.burnedKcal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = BurnColor,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            },
            trailingContent = {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimens.IconSizeSmall)
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
