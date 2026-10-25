package com.example.nutritracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.LocalIsChinese
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.util.UnitCalc
import com.example.nutritracker.util.tr
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class DailyCaloriePoint(
    val date: LocalDate,
    val intakeKcal: Double,
    val goalKcal: Double,
    val burnedKcal: Double
)

@Composable
fun CalorieBarChart(
    points: List<DailyCaloriePoint>,
    useKj: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    var selectedIdx by remember(points) { mutableStateOf<Int?>(points.lastIndex) }

    val primary = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val goalLineColor = MaterialTheme.colorScheme.tertiary
    val selectedColor = MaterialTheme.colorScheme.secondary

    val maxVal = remember(points) {
        points.maxOfOrNull { maxOf(it.intakeKcal, it.goalKcal) }?.coerceAtLeast(100.0) ?: 2000.0
    }
    val avgGoal = remember(points) {
        points.map { it.goalKcal }.filter { it > 0.0 }.average().takeIf { !it.isNaN() } ?: 0.0
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        selectedIdx?.let { idx ->
            val p = points.getOrNull(idx)
            if (p != null) {
                val isZh = LocalIsChinese.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LocaleHelper.formatMonthDay(p.date, isZh),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr(
                            "摄入 ${UnitCalc.formatEnergy(p.intakeKcal, useKj)} / 目标 ${UnitCalc.formatEnergy(p.goalKcal, useKj)}" +
                                if (p.burnedKcal > 0) " · 运动 ${UnitCalc.formatEnergy(p.burnedKcal, useKj)}" else "",
                            "In ${UnitCalc.formatEnergy(p.intakeKcal, useKj)} / Goal ${UnitCalc.formatEnergy(p.goalKcal, useKj)}" +
                                if (p.burnedKcal > 0) " · Burned ${UnitCalc.formatEnergy(p.burnedKcal, useKj)}" else ""
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val stepW = size.width.toFloat() / points.size.coerceAtLeast(1)
                        val tapped = (offset.x / stepW).toInt().coerceIn(0, points.lastIndex)
                        selectedIdx = tapped
                    }
                }
        ) {
            val count = points.size
            val slotW = size.width / count
            val barW = (slotW * 0.56f).coerceIn(4.dp.toPx(), 24.dp.toPx())
            val chartH = size.height - 8.dp.toPx()

            // Horizontal grid lines
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = chartH * (1f - frac) + 4.dp.toPx()
                drawLine(
                    color = trackColor.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Bars
            points.forEachIndexed { i, point ->
                val x = i * slotW + (slotW - barW) / 2f
                val ratio = (point.intakeKcal / maxVal).toFloat().coerceIn(0f, 1f)
                val barH = (chartH * ratio).coerceAtLeast(if (point.intakeKcal > 0) 3.dp.toPx() else 0f)
                val y = 4.dp.toPx() + chartH - barH

                val isSelected = selectedIdx == i
                val isOver = point.goalKcal > 0 && point.intakeKcal > point.goalKcal * 1.1
                val barColor = when {
                    isOver -> errorColor
                    isSelected -> primary
                    else -> primary.copy(alpha = 0.72f)
                }

                if (isSelected) {
                    drawRoundRect(
                        color = selectedColor.copy(alpha = 0.14f),
                        topLeft = Offset(i * slotW + 1.dp.toPx(), 0f),
                        size = Size(slotW - 2.dp.toPx(), size.height),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }

                if (barH > 0f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // Average goal dashed reference line
            if (avgGoal > 0) {
                val goalY = 4.dp.toPx() + chartH * (1f - (avgGoal / maxVal).toFloat().coerceIn(0f, 1f))
                drawLine(
                    color = goalLineColor,
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.first().date.format(DateTimeFormatter.ofPattern("MM/dd")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (points.size > 2) {
                Text(
                    text = points[points.size / 2].date.format(DateTimeFormatter.ofPattern("MM/dd")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = points.last().date.format(DateTimeFormatter.ofPattern("MM/dd")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegendDot(color = primary, label = tr("当日摄入", "Intake"))
            ChartLegendDot(color = errorColor, label = tr("超标(>110%)", "Over (>110%)"))
            ChartLegendDot(color = goalLineColor, label = tr("平均目标线", "Avg Goal"))
        }
    }
}

@Composable
fun MacroDonutChart(
    carbsG: Double,
    fatG: Double,
    proteinG: Double,
    modifier: Modifier = Modifier
) {
    val isDark = LocalUseDarkTheme.current
    val carbsColor = if (isDark) CarbsColorDark else CarbsColor
    val fatColor = if (isDark) FatColorDark else FatColor
    val proteinColor = if (isDark) ProteinColorDark else ProteinColor

    val carbsKcal = carbsG * 4.0
    val fatKcal = fatG * 9.0
    val proteinKcal = proteinG * 4.0
    val totalKcal = carbsKcal + fatKcal + proteinKcal

    val carbsPct = if (totalKcal > 0) (carbsKcal / totalKcal * 100).roundToInt() else 0
    val fatPct = if (totalKcal > 0) (fatKcal / totalKcal * 100).roundToInt() else 0
    val proteinPct = if (totalKcal > 0) (100 - carbsPct - fatPct).coerceAtLeast(0) else 0

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier.size(112.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Butt)
                val pad = 8.dp.toPx()
                val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                val offset = Offset(pad, pad)

                if (totalKcal <= 0.0) {
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = offset,
                        size = arcSize,
                        style = stroke
                    )
                } else {
                    val carbsSweep = (carbsKcal / totalKcal * 360.0).toFloat()
                    val fatSweep = (fatKcal / totalKcal * 360.0).toFloat()
                    val proteinSweep = (360f - carbsSweep - fatSweep).coerceAtLeast(0f)

                    var start = -90f
                    drawArc(carbsColor, start, carbsSweep, false, offset, arcSize, style = stroke)
                    start += carbsSweep
                    drawArc(fatColor, start, fatSweep, false, offset, arcSize, style = stroke)
                    start += fatSweep
                    drawArc(proteinColor, start, proteinSweep, false, offset, arcSize, style = stroke)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (totalKcal > 0) tr("供能比", "Energy %") else tr("无数据", "No Data"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MacroLegendRow(tr("碳水化合物", "Carbs"), carbsG, carbsPct, carbsColor)
            MacroLegendRow(tr("脂肪", "Fat"), fatG, fatPct, fatColor)
            MacroLegendRow(tr("蛋白质", "Protein"), proteinG, proteinPct, proteinColor)
        }
    }
}

@Composable
private fun MacroLegendRow(label: String, grams: Double, pct: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "${grams.roundToInt()}g · $pct%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class WeightChartPoint(
    val date: LocalDate,
    val weightKg: Double
)

@Composable
fun WeightLineChart(
    points: List<WeightChartPoint>,
    targetWeightKg: Double?,
    useImperial: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val sorted = remember(points) { points.sortedBy { it.date } }
    var selectedIdx by remember(sorted) { mutableStateOf(sorted.lastIndex) }

    val primary = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val pointCenterColor = MaterialTheme.colorScheme.surface

    val minKg = remember(sorted, targetWeightKg) {
        val dataMin = sorted.minOf { it.weightKg }
        val minWithTarget = targetWeightKg?.let { minOf(dataMin, it) } ?: dataMin
        minWithTarget - 1.5
    }
    val maxKg = remember(sorted, targetWeightKg) {
        val dataMax = sorted.maxOf { it.weightKg }
        val maxWithTarget = targetWeightKg?.let { maxOf(dataMax, it) } ?: dataMax
        (maxWithTarget + 1.5).coerceAtLeast(minKg + 2.0)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val selected = sorted.getOrNull(selectedIdx)
        if (selected != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = UnitCalc.formatWeight(selected.weightKg, useImperial),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(sorted) {
                    detectTapGestures { offset ->
                        if (sorted.size == 1) {
                            selectedIdx = 0
                        } else {
                            val padX = 12.dp.toPx()
                            val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                            val frac = ((offset.x - padX) / usableW).coerceIn(0f, 1f)
                            selectedIdx = (frac * sorted.lastIndex).roundToInt().coerceIn(0, sorted.lastIndex)
                        }
                    }
                }
        ) {
            val padX = 12.dp.toPx()
            val padY = 12.dp.toPx()
            val usableW = (size.width - padX * 2).coerceAtLeast(1f)
            val usableH = (size.height - padY * 2).coerceAtLeast(1f)
            val range = (maxKg - minKg).coerceAtLeast(0.1)

            fun yFor(kg: Double): Float {
                val norm = ((kg - minKg) / range).toFloat().coerceIn(0f, 1f)
                return padY + usableH * (1f - norm)
            }

            // Grid lines
            listOf(0f, 0.5f, 1f).forEach { frac ->
                val y = padY + usableH * frac
                drawLine(gridColor.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }

            // Target weight dashed line
            if (targetWeightKg != null) {
                val targetY = yFor(targetWeightKg)
                drawLine(
                    color = targetColor,
                    start = Offset(0f, targetY),
                    end = Offset(size.width, targetY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            val coords = sorted.mapIndexed { idx, p ->
                val x = if (sorted.size == 1) size.width / 2f else padX + usableW * idx / sorted.lastIndex
                Offset(x, yFor(p.weightKg))
            }

            if (coords.size >= 2) {
                val path = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = primary,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            coords.forEachIndexed { idx, center ->
                val isSel = idx == selectedIdx
                drawCircle(
                    color = primary,
                    radius = if (isSel) 5.5.dp.toPx() else 3.5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = pointCenterColor,
                    radius = if (isSel) 2.5.dp.toPx() else 1.5.dp.toPx(),
                    center = center
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sorted.first().date.format(DateTimeFormatter.ofPattern("MM/dd")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (targetWeightKg != null) {
                ChartLegendDot(
                    color = targetColor,
                    label = tr(
                        "目标 ${UnitCalc.formatWeight(targetWeightKg, useImperial)}",
                        "Goal ${UnitCalc.formatWeight(targetWeightKg, useImperial)}"
                    )
                )
            }
            Text(
                text = sorted.last().date.format(DateTimeFormatter.ofPattern("MM/dd")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
