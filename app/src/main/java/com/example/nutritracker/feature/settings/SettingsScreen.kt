package com.example.nutritracker.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.ui.theme.Dimens
import com.example.nutritracker.ui.theme.SuccessColor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToSources: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showMacroDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    val exportStatus by vm.exportStatus.collectAsStateWithLifecycle()
    val importStatus by vm.importStatus.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { vm.exportData(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { vm.importData(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardSpacing)
    ) {
        // Header
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Dimens.ContentPadding)
        )

        // Calculations section
        SectionTitle(icon = Icons.Filled.Calculate, title = "计算")

        // Day boundary
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "日边界偏移",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "设定一天的起始时间。例如设为 270 分钟(4:30)，则凌晨 4:30 前的记录算作前一天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.dayBoundaryStr,
                        onValueChange = { vm.updateDayBoundary(it) },
                        label = {
                            Text(
                                text = "分钟",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    Text(
                        text = "(${state.dayBoundaryStr.toIntOrNull()?.div(60) ?: 0}:${"%02d".format(state.dayBoundaryStr.toIntOrNull()?.rem(60) ?: 0)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }


        // Kcal adjustment
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "卡路里手动调整",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "在系统计算的 TDEE 基础上，手动增加或减少每日卡路里目标",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.kcalAdjStr,
                    onValueChange = { vm.updateKcalAdj(it) },
                    label = {
                        Text(
                            text = "kcal (正数增加, 负数减少)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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


        // Macro split
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "宏量分配",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Column {
                        Text(
                            text = "碳水 ${(state.carbPct * 100).roundToInt()}% / 脂肪 ${(state.fatPct * 100).roundToInt()}% / 蛋白质 ${(state.proteinPct * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "自定义碳水、脂肪、蛋白质的每日目标比例，总和需为 100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                trailingContent = {
                    TextButton(
                        onClick = { showMacroDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("修改")
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }


        // Water goal
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "每日饮水目标",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "设置每天的饮水量目标，首页会显示饮水进度",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.waterGoalStr,
                    onValueChange = { vm.updateWaterGoal(it) },
                    label = {
                        Text(
                            text = "ml",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // AI Settings section
        SectionTitle(icon = Icons.Filled.SmartToy, title = "AI 食物识别")

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            ListItem(
                headlineContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AI API 配置",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (state.aiApiKey.isBlank()) MaterialTheme.colorScheme.outline
                                           else SuccessColor,
                                    shape = CircleShape
                                )
                        )
                    }
                },
                supportingContent = {
                    Column {
                        Text(
                            text = if (state.aiApiKey.isBlank()) "未配置 - 请设置 API Key"
                            else "模型: ${state.aiModel}  ·  已配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "拍照识别食物营养成分，支持 OpenAI 兼容接口等",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                trailingContent = {
                    TextButton(
                        onClick = { showAiDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("配置")
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Data management section
        SectionTitle(icon = Icons.Filled.Sync, title = "数据管理")

        // Export
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "导出数据",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Column {
                        Text(
                            text = "导出所有饮食记录、设置和图片到 ZIP 文件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val (status, msg) = exportStatus
                        if (status != ExportImportStatus.IDLE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = when (status) {
                                    ExportImportStatus.SUCCESS -> SuccessColor
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
                        TextButton(
                            onClick = {
                                val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                                exportLauncher.launch("NutriTracker_$dateStr.zip")
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("导出")
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Import
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "导入数据",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Column {
                        Text(
                            text = "从 ZIP 文件导入数据，重复数据将自动跳过",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val (status, msg) = importStatus
                        if (status != ExportImportStatus.IDLE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = when (status) {
                                    ExportImportStatus.SUCCESS -> SuccessColor
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
                        TextButton(
                            onClick = { importLauncher.launch(arrayOf("application/zip")) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("导入")
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // About section
        SectionTitle(icon = Icons.Filled.Info, title = "关于")

        val context = LocalContext.current
        val versionName = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
            } catch (_: Exception) { "未知" }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth().clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cdz-hy/NutriTracker")))
            },
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "NutriTracker",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Text(
                        text = "版本 $versionName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }


        ElevatedCard(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToSources() },
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "科学文献来源与依据",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Text(
                        text = "查看医学方程与营养摄入量推荐的数据出处",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
    }

    // Macro dialog
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

    // AI config dialog
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "宏量分配 (%)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = c,
                    onValueChange = { c = it },
                    label = {
                        Text(
                            text = "碳水 %",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    value = f,
                    onValueChange = { f = it },
                    label = {
                        Text(
                            text = "脂肪 %",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    value = p,
                    onValueChange = { p = it },
                    label = {
                        Text(
                            text = "蛋白质 %",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                val total = (c.toIntOrNull() ?: 0) + (f.toIntOrNull() ?: 0) + (p.toIntOrNull() ?: 0)
                if (total != 100) {
                    Text(
                        text = "总计: $total% (应为100%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            val total = (c.toIntOrNull() ?: 0) + (f.toIntOrNull() ?: 0) + (p.toIntOrNull() ?: 0)
            TextButton(
                onClick = {
                    onConfirm(
                        c.toDoubleOrNull()?.div(100) ?: 0.6,
                        f.toDoubleOrNull()?.div(100) ?: 0.25,
                        p.toDoubleOrNull()?.div(100) ?: 0.15
                    )
                },
                enabled = total == 100,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AiConfigDialog(
    apiKey: String,
    baseUrl: String,
    model: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    var key by remember { mutableStateOf(apiKey) }
    var url by remember { mutableStateOf(baseUrl) }
    var mdl by remember { mutableStateOf(model) }
    var showKey by remember { mutableStateOf(false) }
    val testState by vm.testState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = {
            vm.resetTestState()
            onDismiss()
        },
        title = {
            Text(
                text = "AI API 配置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "配置兼容 OpenAI 接口的多模态大模型",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = {
                        Text(
                            text = "API Key",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
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
                    value = url,
                    onValueChange = { url = it },
                    label = {
                        Text(
                            text = "Base URL",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                    value = mdl,
                    onValueChange = { mdl = it },
                    label = {
                        Text(
                            text = "模型名称",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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

                // 测试按钮
                val canTest = key.isNotBlank() && url.isNotBlank() && mdl.isNotBlank()
                val (status, message) = testState

                OutlinedButton(
                    onClick = { vm.testAiConnection(key, url, mdl) },
                    enabled = canTest && status != TestStatus.TESTING,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = when (status) {
                            TestStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                            TestStatus.FAILURE -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = canTest).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            when (status) {
                                TestStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                                TestStatus.FAILURE -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    )
                ) {
                    if (status == TestStatus.TESTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
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

                // 测试结果提示
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

                Text(
                    text = "支持 OpenAI、Claude、Gemini 等兼容接口",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vm.resetTestState()
                    onConfirm(key, url, mdl)
                },
                enabled = key.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    vm.resetTestState()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
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
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
