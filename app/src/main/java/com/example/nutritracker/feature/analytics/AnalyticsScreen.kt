package com.example.nutritracker.feature.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.ui.components.*
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.DriReference
import com.example.nutritracker.util.MicronutrientKey
import com.example.nutritracker.util.UnitCalc
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    onNavigateToWeightHistory: () -> Unit,
    onNavigateToSources: () -> Unit,
    vm: AnalyticsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "趋势分析",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SingleChoiceSegmentedButtonRow {
                    AnalyticsRange.entries.forEachIndexed { idx, r ->
                        SegmentedButton(
                            selected = state.range == r,
                            onClick = { vm.selectRange(r) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = AnalyticsRange.entries.size)
                        ) {
                            Text(r.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 连续打卡与周期概览
        item(key = "summary_metrics") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    title = "连续打卡",
                    value = "${state.streakDays} 天",
                    subtitle = "周期已记 ${state.loggedDaysCount}/${state.range.days} 天",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    icon = Icons.Filled.Restaurant,
                    title = "日均摄入",
                    value = UnitCalc.formatEnergy(state.avgIntakeKcal, state.useKj),
                    subtitle = "日均目标 ${UnitCalc.formatEnergy(state.avgGoalKcal, state.useKj)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 热量柱状图
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
                        text = "${state.range.label}热量收支",
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

        // 宏量供能比环形图
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
                        text = "平均日均宏量供能比",
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

        // 体重趋势折线图
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
                            text = "体重变化趋势 (近90天)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToWeightHistory) {
                            Text("管理记录")
                        }
                    }

                    if (state.weightPoints.isEmpty()) {
                        Text(
                            text = "暂无体重日志，点击“管理记录”补充体重历史",
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

        // 周期平均 DRI 微量指标
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
                            text = "日均关键营养素 (DRI)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onNavigateToSources, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = "科学依据", modifier = Modifier.size(18.dp))
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
                            Text(key.label, style = MaterialTheme.typography.bodyMedium)
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
