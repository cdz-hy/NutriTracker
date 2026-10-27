package com.example.nutritracker.feature.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.nutritracker.ui.components.*
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.DriReference
import com.example.nutritracker.util.LocalIsChinese
import com.example.nutritracker.util.MicronutrientKey
import com.example.nutritracker.util.UnitCalc
import com.example.nutritracker.util.tr
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    onNavigateToWeightHistory: () -> Unit,
    onNavigateToSources: () -> Unit,
    vm: AnalyticsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val isZh = LocalIsChinese.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.onResumed()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("趋势分析", "Analytics"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                SingleChoiceSegmentedButtonRow {
                    AnalyticsRange.entries.forEachIndexed { idx, r ->
                        SegmentedButton(
                            selected = state.range == r,
                            onClick = { vm.selectRange(r) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = AnalyticsRange.entries.size)
                        ) {
                            Text(
                                text = r.displayLabel(isZh),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Streak & Period Summary
        item(key = "summary_metrics") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    title = tr("连续打卡", "Log Streak"),
                    value = if (isZh) "${state.streakDays} 天" else "${state.streakDays}d",
                    subtitle = if (isZh) {
                        "周期已记 ${state.loggedDaysCount}/${state.range.days} 天"
                    } else {
                        "Logged ${state.loggedDaysCount}/${state.range.days} days"
                    },
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    icon = Icons.Filled.Restaurant,
                    title = tr("日均摄入", "Daily Avg"),
                    value = UnitCalc.formatEnergy(state.avgIntakeKcal, state.useKj),
                    subtitle = tr(
                        "日均目标 ${UnitCalc.formatEnergy(state.avgGoalKcal, state.useKj)}",
                        "Avg goal ${UnitCalc.formatEnergy(state.avgGoalKcal, state.useKj)}"
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calorie Bar Chart
        item(key = "calorie_bar_chart") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.CardInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = state.range.chartTitle(isZh),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    CalorieBarChart(
                        points = state.caloriePoints,
                        useKj = state.useKj
                    )
                }
            }
        }

        // Macro Donut Chart
        item(key = "macro_donut") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.CardInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = tr("日均宏量供能比", "Avg Macro Breakdown"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    MacroDonutChart(
                        carbsG = state.avgCarbsG,
                        fatG = state.avgFatG,
                        proteinG = state.avgProteinG
                    )
                }
            }
        }

        // Weight Trend Chart
        item(key = "weight_chart") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.CardInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("体重变化趋势 (近90天)", "Weight Trend (90 Days)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToWeightHistory) {
                            Text(tr("管理记录", "Manage"))
                        }
                    }

                    if (state.weightPoints.isEmpty()) {
                        Text(
                            text = tr(
                                "暂无体重日志，点击“管理记录”补充体重历史",
                                "No weight logs yet — tap Manage to add weight history"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        WeightLineChart(
                            points = state.weightPoints,
                            targetWeightKg = state.targetWeightKg,
                            useImperial = state.usesImperial
                        )
                    }
                }
            }
        }

        // Period Average DRI Micronutrients
        item(key = "micro_summary") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.CardInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("日均关键营养素 (DRI)", "Daily Avg Nutrients (DRI)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onNavigateToSources, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = tr("科学依据", "Scientific sources"), modifier = Modifier.size(18.dp))
                        }
                    }
                    val micros = state.periodAvgMicros
                    val items = listOf(
                        Triple(MicronutrientKey.FIBER, micros.fiberG, DriReference.getReferenceAmount(MicronutrientKey.FIBER, state.user)),
                        Triple(MicronutrientKey.SUGARS, micros.sugarsG, DriReference.getReferenceAmount(MicronutrientKey.SUGARS, state.user)),
                        Triple(MicronutrientKey.SATURATED_FAT, micros.saturatedFatG, DriReference.getReferenceAmount(MicronutrientKey.SATURATED_FAT, state.user)),
                        Triple(MicronutrientKey.SODIUM, micros.sodiumMg, DriReference.getReferenceAmount(MicronutrientKey.SODIUM, state.user)),
                        Triple(MicronutrientKey.CHOLESTEROL, micros.cholesterolMg, DriReference.getReferenceAmount(MicronutrientKey.CHOLESTEROL, state.user))
                    )
                    items.forEach { (key, avgVal, ref) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(key.displayLabel(isZh), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${UnitCalc.formatDecimal(avgVal, if (avgVal >= 10) 0 else 1)} / ${ref.roundToInt()} ${key.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (key.excessMatters && avgVal > ref) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
