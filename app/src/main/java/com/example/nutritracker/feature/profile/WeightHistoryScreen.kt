package com.example.nutritracker.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nutritracker.ui.components.AppDatePickerDialog
import com.example.nutritracker.ui.components.WeightChartPoint
import com.example.nutritracker.ui.components.WeightLineChart
import com.example.nutritracker.ui.theme.Dimens
import com.example.nutritracker.ui.theme.StaggeredFadeIn
import com.example.nutritracker.util.UnitCalc
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightHistoryScreen(
    onBack: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val weightLogs by vm.weightLogs.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val imperial = user?.usesImperialUnits ?: false

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteConfirmDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("体重历史", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Filled.Add, "添加") },
                text = { Text("记录体重") }
            )
        }
    ) { padding ->
        if (weightLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Filled.TrendingFlat, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("暂无体重记录", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("点击右下角按钮添加或补录历史体重", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Dimens.ContentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
            ) {
                item(key = "weight_chart") {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevation)
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.CardInnerPadding),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("体重变化曲线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            WeightLineChart(
                                points = weightLogs.take(90).map { WeightChartPoint(it.date, it.weightKg) },
                                targetWeightKg = user?.targetWeightKg,
                                useImperial = imperial
                            )
                        }
                    }
                }

                item(key = "trend_card") {
                    val recent = weightLogs.take(30).reversed()
                    if (recent.size >= 2) {
                        val first = recent.first().weightKg
                        val last = recent.last().weightKg
                        val diff = last - first

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("近期对比", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(
                                        when {
                                            diff > 0 -> Icons.Filled.TrendingUp
                                            diff < 0 -> Icons.Filled.TrendingDown
                                            else -> Icons.Filled.TrendingFlat
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Column {
                                        Text(
                                            text = "${UnitCalc.formatWeight(first, imperial)} → ${UnitCalc.formatWeight(last, imperial)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = when {
                                                diff > 0 -> "↑ 增加 ${UnitCalc.formatWeight(diff, imperial)}"
                                                diff < 0 -> "↓ 减少 ${UnitCalc.formatWeight(-diff, imperial)}"
                                                else -> "→ 持平"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "list_title") {
                    Text(
                        text = "历史记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                itemsIndexed(weightLogs, key = { _, log -> log.date.toString() }) { index, log ->
                    StaggeredFadeIn(modifier = Modifier.animateItem(), index = index) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp)
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(UnitCalc.formatWeight(log.weightKg, imperial), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                },
                                supportingContent = {
                                    Text(log.date.format(DateTimeFormatter.ISO_LOCAL_DATE), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                trailingContent = {
                                    IconButton(onClick = { deleteConfirmDate = log.date }, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var weightStr by remember { mutableStateOf("") }
        var logDate by remember { mutableStateOf(LocalDate.now()) }
        var showDatePicker by remember { mutableStateOf(false) }

        val inputVal = weightStr.toDoubleOrNull()
        val kgVal = inputVal?.let { if (imperial) UnitCalc.lbsToKg(it) else it }
        val isValid = kgVal != null && kgVal in 20.0..400.0

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("记录体重", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("体重 (${UnitCalc.weightUnitLabel(imperial)})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("日期：${logDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        kgVal?.let { vm.logWeight(it, logDate) }
                        showAddDialog = false
                    },
                    enabled = isValid
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )

        if (showDatePicker) {
            AppDatePickerDialog(
                initialDate = logDate,
                onDismiss = { showDatePicker = false },
                onConfirm = { logDate = it; showDatePicker = false }
            )
        }
    }

    deleteConfirmDate?.let { date ->
        AlertDialog(
            onDismissRequest = { deleteConfirmDate = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除 ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)} 的体重记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteWeightLog(date); deleteConfirmDate = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmDate = null }) { Text("取消") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
