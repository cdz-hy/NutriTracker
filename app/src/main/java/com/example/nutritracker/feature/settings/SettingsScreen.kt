package com.example.nutritracker.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.R
import com.example.nutritracker.data.entity.ThemeMode
import com.example.nutritracker.ui.theme.Dimens
import com.example.nutritracker.ui.theme.LocalUseDarkTheme
import com.example.nutritracker.ui.theme.SuccessColor
import com.example.nutritracker.ui.theme.SuccessColorDark
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToSources: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val exportStatus by vm.exportStatus.collectAsStateWithLifecycle()
    val importStatus by vm.importStatus.collectAsStateWithLifecycle()
    val isDark = LocalUseDarkTheme.current
    val successColor = if (isDark) SuccessColorDark else SuccessColor
    val context = LocalContext.current

    var showMacroDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showWaterQuickDialog by remember { mutableStateOf(false) }
    var editingReminderSlot by remember { mutableStateOf<Int?>(null) }

    // Draft buffers decoupled from DataStore flows so users can clear & retype cleanly
    var dayBoundaryDraft by remember(state.dayBoundaryMinutes) { mutableStateOf(state.dayBoundaryMinutes.toString()) }
    var kcalAdjDraft by remember(state.kcalAdjustment) { mutableStateOf(state.kcalAdjustment.toInt().toString()) }
    var waterGoalDraft by remember(state.waterGoalMl) { mutableStateOf(state.waterGoalMl.toString()) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "未授予通知权限，提醒将无法弹出", Toast.LENGTH_SHORT).show()
        }
    }

    fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? -> uri?.let { vm.exportData(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { vm.importData(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Dimens.ContentPadding)
        )

        // ── 外观与单位 ────────────────────────────────────────────────────
        SectionTitle(icon = Icons.Filled.Palette, title = "外观与单位")

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("主题模式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = listOf(ThemeMode.SYSTEM to "跟随系统", ThemeMode.LIGHT to "浅色", ThemeMode.DARK to "深色")
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = state.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material You 动态取色", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("根据系统壁纸自动配色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.dynamicColorEnabled, onCheckedChange = { vm.setDynamicColorEnabled(it) })
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("能量单位", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(if (state.useKj) "当前：千焦 (kJ)" else "当前：千卡 (kcal)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !state.useKj,
                            onClick = { vm.setUseKj(false) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("kcal") }
                        SegmentedButton(
                            selected = state.useKj,
                            onClick = { vm.setUseKj(true) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("kJ") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("身体测量单位", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(if (state.usesImperialUnits) "当前：英制 (lbs / in)" else "当前：公制 (kg / cm)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !state.usesImperialUnits,
                            onClick = { vm.setUsesImperialUnits(false) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("公制") }
                        SegmentedButton(
                            selected = state.usesImperialUnits,
                            onClick = { vm.setUsesImperialUnits(true) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("英制") }
                    }
                }
            }
        }

        // ── 计算参数 ──────────────────────────────────────────────────────
        SectionTitle(icon = Icons.Filled.Calculate, title = "计算与目标")

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("日边界偏移 (0 ~ 1439 分钟)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text("例如设为 270 (04:30)，凌晨 4:30 前的记录归入前一天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val bndVal = dayBoundaryDraft.toIntOrNull()
                val bndValid = bndVal != null && bndVal in 0..1439
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dayBoundaryDraft,
                        onValueChange = { dayBoundaryDraft = it },
                        label = { Text("分钟") },
                        isError = dayBoundaryDraft.isNotBlank() && !bndValid,
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    val safeMin = (bndVal ?: 0).coerceIn(0, 1439)
                    Text(
                        text = "(${safeMin / 60}:${String.format(Locale.US, "%02d", safeMin % 60)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = { bndVal?.let { vm.updateDayBoundary(it) } },
                        enabled = bndValid && bndVal != state.dayBoundaryMinutes
                    ) { Text("应用") }
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("每日热量微调 (kcal)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text("在 TDEE 基础目标上手动加减 (-3000 ~ +3000 kcal)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val adjVal = kcalAdjDraft.toDoubleOrNull()
                val adjValid = adjVal != null && adjVal in -3000.0..3000.0
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kcalAdjDraft,
                        onValueChange = { kcalAdjDraft = it },
                        label = { Text("偏移 kcal") },
                        isError = kcalAdjDraft.isNotBlank() && !adjValid,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    FilledTonalIconButton(onClick = {
                        val cur = kcalAdjDraft.toIntOrNull() ?: 0
                        kcalAdjDraft = (-cur).toString()
                    }) {
                        Icon(Icons.Filled.Exposure, contentDescription = "正负切换")
                    }
                    FilledTonalButton(
                        onClick = { adjVal?.let { vm.updateKcalAdj(it) } },
                        enabled = adjValid && adjVal != state.kcalAdjustment
                    ) { Text("应用") }
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            ListItem(
                headlineContent = { Text("宏量营养素分配", fontWeight = FontWeight.Medium) },
                supportingContent = {
                    Text("碳水 ${(state.carbPct * 100).roundToInt()}% / 脂肪 ${(state.fatPct * 100).roundToInt()}% / 蛋白质 ${(state.proteinPct * 100).roundToInt()}%")
                },
                trailingContent = { TextButton(onClick = { showMacroDialog = true }) { Text("修改") } },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }

        // 饮水设置
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("饮水目标与快捷杯量", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                val wgVal = waterGoalDraft.toIntOrNull()
                val wgValid = wgVal != null && wgVal in 100..20000
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = waterGoalDraft,
                        onValueChange = { waterGoalDraft = it },
                        label = { Text("每日目标 (ml)") },
                        isError = waterGoalDraft.isNotBlank() && !wgValid,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    FilledTonalButton(
                        onClick = { wgVal?.let { vm.updateWaterGoal(it) } },
                        enabled = wgValid && wgVal != state.waterGoalMl
                    ) { Text("应用") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "快捷杯量：${state.waterQuickAmounts.joinToString(" / ") { "${it}ml" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showWaterQuickDialog = true }) { Text("自定义") }
                }
            }
        }

        // ── 提醒通知 ──────────────────────────────────────────────────────
        SectionTitle(icon = Icons.Filled.Notifications, title = "智能提醒 (WorkManager)")

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            Column(modifier = Modifier.padding(Dimens.CardInnerPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("三餐漏记提醒", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("到点仍未记录该餐时才提醒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.mealReminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) ensureNotificationPermissionIfNeeded()
                            vm.setMealReminderEnabled(enabled)
                        }
                    )
                }

                if (state.mealReminderEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val slots = listOf(
                            Triple(0, "早餐", state.breakfastReminderMin),
                            Triple(1, "午餐", state.lunchReminderMin),
                            Triple(2, "晚餐", state.dinnerReminderMin)
                        )
                        slots.forEach { (slot, label, minOfDay) ->
                            OutlinedButton(
                                onClick = { editingReminderSlot = slot },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text("$label ${formatMinuteOfDay(minOfDay)}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("定时饮水提醒", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("08:00-22:00 未达标时定期提醒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.waterReminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) ensureNotificationPermissionIfNeeded()
                            vm.setWaterReminderEnabled(enabled)
                        }
                    )
                }

                if (state.waterReminderEnabled) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(60, 90, 120, 180).forEach { interval ->
                            FilterChip(
                                selected = state.waterReminderIntervalMin == interval,
                                onClick = { vm.setWaterReminderIntervalMin(interval) },
                                label = { Text("每 $interval 分钟") }
                            )
                        }
                    }
                }
            }
        }

        // ── AI 食物识别 ───────────────────────────────────────────────────
        SectionTitle(icon = Icons.Filled.SmartToy, title = "AI 食物识别")

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("AI API 配置", fontWeight = FontWeight.Medium)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (state.aiApiKey.isBlank()) MaterialTheme.colorScheme.outline else successColor,
                                        shape = CircleShape
                                    )
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            if (state.aiApiKey.isBlank()) "未配置 - 点击设置 API Key (Keystore 加密存储)"
                            else "模型: ${state.aiModel} · Key 已加密存储"
                        )
                    },
                    trailingContent = { TextButton(onClick = { showAiDialog = true }) { Text("配置") } },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("识别后直接自动入库", fontWeight = FontWeight.Medium) },
                    supportingContent = {
                        Text(if (state.aiAutoConfirm) "已开启：识别完成后直接保存" else "已关闭：识别后弹出确认微调窗口")
                    },
                    trailingContent = {
                        Switch(checked = state.aiAutoConfirm, onCheckedChange = { vm.setAiAutoConfirm(it) })
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                )
            }
        }

        // ── 数据管理 ──────────────────────────────────────────────────────
        SectionTitle(icon = Icons.Filled.Sync, title = "数据备份与恢复")

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            ListItem(
                headlineContent = { Text("导出数据", fontWeight = FontWeight.Medium) },
                supportingContent = {
                    Column {
                        Text("导出饮食、食谱、体重、饮水、设置与缩略图至 ZIP")
                        val (status, msg) = exportStatus
                        if (status != ExportImportStatus.IDLE) {
                            Text(
                                text = msg,
                                color = when (status) {
                                    ExportImportStatus.SUCCESS -> successColor
                                    ExportImportStatus.FAILURE -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                trailingContent = {
                    if (exportStatus.first == ExportImportStatus.RUNNING) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = {
                            val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            exportLauncher.launch("NutriTracker_$dateStr.zip")
                        }) { Text("导出") }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            ListItem(
                headlineContent = { Text("导入数据", fontWeight = FontWeight.Medium) },
                supportingContent = {
                    Column {
                        Text("从 ZIP 备份恢复数据（事务原子写入，重复项自动合并）")
                        val (status, msg) = importStatus
                        if (status != ExportImportStatus.IDLE) {
                            Text(
                                text = msg,
                                color = when (status) {
                                    ExportImportStatus.SUCCESS -> successColor
                                    ExportImportStatus.FAILURE -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                trailingContent = {
                    if (importStatus.first == ExportImportStatus.RUNNING) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = {
                            importLauncher.launch(
                                arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")
                            )
                        }) { Text("导入") }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }

        // ── 关于与科学依据 ────────────────────────────────────────────────
        SectionTitle(icon = Icons.Filled.Info, title = "关于与科学依据")

        val versionName = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知" }
                .getOrDefault("未知")
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cdz-hy/NutriTracker")))
                    }.onFailure {
                        Toast.makeText(context, "未找到可用的浏览器应用", Toast.LENGTH_SHORT).show()
                    }
                },
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            ListItem(
                headlineContent = { Text("NutriTracker", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("版本 $versionName · GPLv3 开源") },
                leadingContent = { Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSources() },
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
        ) {
            ListItem(
                headlineContent = { Text("科学文献来源与依据", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("查看 TDEE、BMI、DRI 与 MET 公式数据出处") },
                leadingContent = { Icon(Icons.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }

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

        Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
    }

    if (showMacroDialog) {
        MacroDialog(
            carbPct = state.carbPct,
            fatPct = state.fatPct,
            proteinPct = state.proteinPct,
            onDismiss = { showMacroDialog = false },
            onConfirm = { c, f, p ->
                vm.updateMacros(c, f, p)
                showMacroDialog = false
            }
        )
    }

    if (showAiDialog) {
        AiConfigDialog(
            apiKey = state.aiApiKey,
            baseUrl = state.aiBaseUrl,
            model = state.aiModel,
            onDismiss = { showAiDialog = false },
            onConfirm = { key, url, model ->
                vm.updateAiConfig(key, url, model)
                showAiDialog = false
            },
            vm = vm
        )
    }

    if (showWaterQuickDialog) {
        var amountsText by remember { mutableStateOf(state.waterQuickAmounts.joinToString(",")) }
        val parsed = amountsText.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 10..5000 }.distinct()
        AlertDialog(
            onDismissRequest = { showWaterQuickDialog = false },
            title = { Text("自定义饮水快捷按钮 (ml)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("用英文逗号分隔 1~5 个数值（范围 10~5000 ml）", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = amountsText,
                        onValueChange = { amountsText = it },
                        singleLine = true,
                        label = { Text("如 150,250,350,500") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.updateWaterQuickAmounts(parsed); showWaterQuickDialog = false },
                    enabled = parsed.isNotEmpty()
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showWaterQuickDialog = false }) { Text("取消") } }
        )
    }

    editingReminderSlot?.let { slot ->
        val initialMin = when (slot) {
            0 -> state.breakfastReminderMin
            1 -> state.lunchReminderMin
            else -> state.dinnerReminderMin
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialMin / 60,
            initialMinute = initialMin % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { editingReminderSlot = null },
            title = { Text("设置提醒时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val mins = timePickerState.hour * 60 + timePickerState.minute
                    when (slot) {
                        0 -> vm.setBreakfastReminderMin(mins)
                        1 -> vm.setLunchReminderMin(mins)
                        else -> vm.setDinnerReminderMin(mins)
                    }
                    editingReminderSlot = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { editingReminderSlot = null }) { Text("取消") } }
        )
    }
}

private fun formatMinuteOfDay(min: Int): String {
    val safe = min.coerceIn(0, 1439)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}

@Composable
private fun MacroDialog(
    carbPct: Double,
    fatPct: Double,
    proteinPct: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double) -> Unit
) {
    var c by remember { mutableStateOf((carbPct * 100).roundToInt().toString()) }
    var f by remember { mutableStateOf((fatPct * 100).roundToInt().toString()) }
    var p by remember { mutableStateOf((proteinPct * 100).roundToInt().toString()) }

    val cInt = c.toIntOrNull()
    val fInt = f.toIntOrNull()
    val pInt = p.toIntOrNull()
    val valid = cInt != null && fInt != null && pInt != null && cInt >= 0 && fInt >= 0 && pInt >= 0 && (cInt + fInt + pInt) == 100
    val total = (cInt ?: 0) + (fInt ?: 0) + (pInt ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("宏量分配 (%)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = c, onValueChange = { c = it }, label = { Text("碳水 %") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = f, onValueChange = { f = it }, label = { Text("脂肪 %") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = p, onValueChange = { p = it }, label = { Text("蛋白质 %") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (!valid) {
                    Text("总计: $total% (每项需非负且总和为 100%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm((cInt ?: 60) / 100.0, (fInt ?: 25) / 100.0, (pInt ?: 15) / 100.0) },
                enabled = valid
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun AiConfigDialog(
    apiKey: String,
    baseUrl: String,
    model: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    vm: SettingsViewModel
) {
    var key by remember { mutableStateOf(apiKey) }
    var url by remember { mutableStateOf(baseUrl) }
    var mdl by remember { mutableStateOf(model) }
    var showKey by remember { mutableStateOf(false) }
    val testState by vm.testState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = { vm.resetTestState(); onDismiss() },
        title = { Text("AI API 配置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("配置兼容 OpenAI /chat/completions 接口的多模态模型", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                        }
                    }
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.openai.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mdl,
                    onValueChange = { mdl = it },
                    label = { Text("模型名称") },
                    placeholder = { Text("gpt-4o-mini / qwen-vl-plus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val canTest = key.isNotBlank() && url.isNotBlank() && mdl.isNotBlank()
                val (status, message) = testState

                OutlinedButton(
                    onClick = { vm.testAiConnection(key, url, mdl) },
                    enabled = canTest && status != TestStatus.TESTING,
                    modifier = Modifier.fillMaxWidth(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = canTest).copy(
                        brush = SolidColor(
                            when (status) {
                                TestStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                                TestStatus.FAILURE -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    )
                ) {
                    if (status == TestStatus.TESTING) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试中...")
                    } else {
                        Icon(
                            when (status) {
                                TestStatus.SUCCESS -> Icons.Filled.CheckCircle
                                TestStatus.FAILURE -> Icons.Filled.Error
                                else -> Icons.Filled.NetworkCheck
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (status) {
                                TestStatus.SUCCESS -> "测试通过"
                                TestStatus.FAILURE -> "重新测试"
                                else -> "测试连接"
                            }
                        )
                    }
                }

                if (status != TestStatus.IDLE && message.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (status) {
                                TestStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                                TestStatus.FAILURE -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (status) {
                                TestStatus.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                                TestStatus.FAILURE -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { vm.resetTestState(); onConfirm(key, url, mdl) },
                enabled = key.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = { vm.resetTestState(); onDismiss() }) { Text("取消") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
        )
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
