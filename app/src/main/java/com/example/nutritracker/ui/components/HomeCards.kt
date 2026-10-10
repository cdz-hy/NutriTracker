package com.example.nutritracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.data.entity.User
import com.example.nutritracker.data.entity.UserActivityEntity
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.*
import kotlin.math.roundToInt

// ══════════════════════════════════════════════════════════════════════════════
// 卡路里总览卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun CalorieOverviewCard(
    title: String = "今日热量",
    goal: Double,
    supplied: Double,
    burned: Double,
    left: Double,
    isBelowFloor: Boolean,
    floor: Double,
    useKj: Boolean = false,
    onNavigateToSources: () -> Unit
) {
    var warningExpanded by remember { mutableStateOf(false) }
    val unitLabel = UnitCalc.energyUnitLabel(useKj)

    val animGoal by animatedIntAsState(UnitCalc.kcalToDisplayInt(goal, useKj))
    val animSupplied by animatedIntAsState(UnitCalc.kcalToDisplayInt(supplied, useKj))
    val animBurned by animatedIntAsState(UnitCalc.kcalToDisplayInt(burned, useKj))
    val animLeft by animatedIntAsState(UnitCalc.kcalToDisplayInt(left, useKj))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title ($unitLabel)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onNavigateToSources, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        contentDescription = "查看计算科学依据",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.IconSizeSmall)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(104.dp)
                ) {
                    CalorieRing(
                        supplied = supplied,
                        burned = burned,
                        goal = goal,
                        modifier = Modifier.size(104.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$animLeft",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (left < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "剩余 $unitLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KcalRow(
                        icon = Icons.Filled.Flag,
                        label = "目标",
                        value = "$animGoal $unitLabel",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    KcalRow(
                        icon = Icons.Filled.Restaurant,
                        label = "已摄入",
                        value = "$animSupplied $unitLabel",
                        color = if (supplied > goal && goal > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    KcalRow(
                        icon = Icons.Filled.LocalFireDepartment,
                        label = "已燃烧",
                        value = "$animBurned $unitLabel",
                        color = if (LocalUseDarkTheme.current) BurnColorDark else BurnColor
                    )
                }
            }

            if (isBelowFloor) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { warningExpanded = !warningExpanded },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
                                text = "目标低于推荐值 ${UnitCalc.formatEnergy(floor, useKj)}",
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
                                    text = "在没有医疗指导的情况下，成年人不宜长期每天摄入低于 ${UnitCalc.formatEnergy(floor, useKj)} 的热量。这可能导致肌肉流失与代谢放缓。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                                TextButton(
                                    onClick = {
                                        warningExpanded = false
                                        onNavigateToSources()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
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
    val burnedColor = if (LocalUseDarkTheme.current) BurnColorDark else BurnColor

    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        val innerStroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        val padding = 8.dp.toPx()
        val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
        val arcOffset = Offset(padding, padding)

        drawArc(trackColor, -90f, 360f, false, arcOffset, arcSize, style = stroke)
        drawArc(suppliedColor, -90f, animSupplied * 360f, false, arcOffset, arcSize, style = stroke)

        if (animBurned > 0f) {
            val innerPadding = 18.dp.toPx()
            val innerSize = Size(size.width - innerPadding * 2, size.height - innerPadding * 2)
            val innerOffset = Offset(innerPadding, innerPadding)
            drawArc(trackColor.copy(alpha = 0.5f), -90f, 360f, false, innerOffset, innerSize, style = innerStroke)
            drawArc(burnedColor, -90f, animBurned * 360f, false, innerOffset, innerSize, style = innerStroke)
        }
    }
}

@Composable
private fun KcalRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(46.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 宏量营养素进度
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
    val isDark = LocalUseDarkTheme.current
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
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
        val ratio = if (goal > 0) current / goal else 0.0
        val isOver = ratio > 1.0
        val ringColor = if (isOver) MaterialTheme.colorScheme.error else color
        val targetProgress = if (goal > 0) ratio.coerceIn(0.0, 1.0).toFloat() else 0f
        val animProgress by animatedFloatAsState(targetProgress, durationMs = 900, label = "${label}Progress")
        val actualPct = (ratio * 100).roundToInt()

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animProgress },
                modifier = Modifier.size(64.dp),
                color = ringColor,
                trackColor = ringColor.copy(alpha = 0.14f),
                strokeWidth = 8.dp
            )
            Text(
                text = "${actualPct}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ringColor
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
                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
// 微量营养素与膳食参考面板 (IOM DRI Panel)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun DriNutrientPanel(
    dayTotals: MicronutrientTotals,
    weeklyAvgTotals: MicronutrientTotals,
    user: User?,
    onNavigateToSources: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var weeklyView by remember { mutableStateOf(false) }
    val activeTotals = if (weeklyView) weeklyAvgTotals else dayTotals

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(Dimens.CardInnerPaddingCompact)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "膳食营养参考面板 (DRI)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (expanded) "基于 IOM 膳食参考摄入量" else "点击展开查看纤维、糖、钠、饱和脂肪与胆固醇",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onNavigateToSources, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "查看科学文献来源",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !weeklyView,
                            onClick = { weeklyView = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("当日", style = MaterialTheme.typography.labelMedium)
                        }
                        SegmentedButton(
                            selected = weeklyView,
                            onClick = { weeklyView = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("7日均", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val rows = listOf(
                    Triple(MicronutrientKey.FIBER, activeTotals.fiberG, DriReference.getReferenceAmount(MicronutrientKey.FIBER, user)),
                    Triple(MicronutrientKey.SUGARS, activeTotals.sugarsG, DriReference.getReferenceAmount(MicronutrientKey.SUGARS, user)),
                    Triple(MicronutrientKey.SATURATED_FAT, activeTotals.saturatedFatG, DriReference.getReferenceAmount(MicronutrientKey.SATURATED_FAT, user)),
                    Triple(MicronutrientKey.SODIUM, activeTotals.sodiumMg, DriReference.getReferenceAmount(MicronutrientKey.SODIUM, user)),
                    Triple(MicronutrientKey.CHOLESTEROL, activeTotals.cholesterolMg, DriReference.getReferenceAmount(MicronutrientKey.CHOLESTEROL, user))
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { (key, value, ref) ->
                        DriNutrientRow(key = key, value = value, reference = ref)
                    }
                }
            }
        }
    }
}

@Composable
private fun DriNutrientRow(
    key: MicronutrientKey,
    value: Double,
    reference: Double
) {
    val isDark = LocalUseDarkTheme.current
    val ratio = if (reference > 0) (value / reference).toFloat() else 0f
    val clamped = ratio.coerceIn(0f, 1f)
    val barColor = when {
        key.excessMatters && ratio > 1f -> MaterialTheme.colorScheme.error
        key.excessMatters && ratio > 0.8f -> if (isDark) WarningColorDark else WarningColor
        !key.excessMatters && ratio >= 1f -> if (isDark) SuccessColorDark else SuccessColor
        else -> MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = key.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${if (key.excessMatters) "上限 " else "建议 "}${UnitCalc.formatDecimal(value, if (value >= 10) 0 else 1)} / ${reference.roundToInt()} ${key.unit}",
                style = MaterialTheme.typography.labelSmall,
                color = if (key.excessMatters && ratio > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { clamped },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.14f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 饮水卡片 - 支持快捷容量与详情时间线
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun WaterCard(
    currentMl: Int,
    goalMl: Int,
    quickAmounts: List<Int> = listOf(150, 250, 350, 500),
    onAddAmount: (Int) -> Unit,
    onUndo: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val isDark = LocalUseDarkTheme.current
    val waterColor = if (isDark) WaterColorDark else WaterColor
    val targetProgress = if (goalMl > 0) (currentMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
    val animProgress by animatedFloatAsState(targetProgress, durationMs = 600, label = "waterProgress")
    val animMl by animatedIntAsState(currentMl, durationMs = 600, label = "waterMl")

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = waterColor,
                    modifier = Modifier.size(26.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "饮水",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${animMl}ml / ${goalMl}ml",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = waterColor,
                        trackColor = waterColor.copy(alpha = 0.15f)
                    )
                }
                if (currentMl > 0) {
                    TextButton(
                        onClick = onUndo,
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("撤回", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickAmounts.forEach { amount ->
                    FilledTonalButton(
                        onClick = { onAddAmount(amount) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = waterColor.copy(alpha = 0.14f),
                            contentColor = waterColor
                        )
                    ) {
                        Text("+${amount}ml", style = MaterialTheme.typography.labelMedium)
                    }
                }
                TextButton(
                    onClick = onOpenDetail,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("明细", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 活动条目 (统一删除确认)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ActivityItem(
    activity: UserActivityEntity,
    useKj: Boolean = false,
    onDelete: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    val burnColor = if (LocalUseDarkTheme.current) BurnColorDark else BurnColor

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevationLow),
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
                    text = "${activity.durationMinutes.roundToInt()} 分钟 · ${UnitCalc.formatEnergy(activity.burnedKcal, useKj)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = burnColor,
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            },
            trailingContent = {
                IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimens.IconSizeSmall)
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("确认删除活动") },
            text = { Text("确定要删除 \"${activity.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = { showConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 餐别标签与图标共享 Helper
// ══════════════════════════════════════════════════════════════════════════════

fun mealTypeLabel(type: IntakeType): String = when (type) {
    IntakeType.BREAKFAST -> "早餐"
    IntakeType.LUNCH -> "午餐"
    IntakeType.DINNER -> "晚餐"
    IntakeType.SNACK -> "零食"
}

fun mealTypeIcon(type: IntakeType): ImageVector = when (type) {
    IntakeType.BREAKFAST -> Icons.Filled.FreeBreakfast
    IntakeType.LUNCH -> Icons.Filled.LunchDining
    IntakeType.DINNER -> Icons.Filled.DinnerDining
    IntakeType.SNACK -> Icons.Filled.Cookie
}
