package com.example.nutritracker.feature.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.nutritracker.data.entity.UserActivityEntity
import com.example.nutritracker.util.MetCalc
import kotlin.math.roundToInt

data class ActivityTemplate(val name: String, val mets: Double, val category: String)

val activityTemplates = listOf(
    ActivityTemplate("步行 (慢速)", 2.5, "步行"),
    ActivityTemplate("步行 (中速)", 3.5, "步行"),
    ActivityTemplate("步行 (快速)", 4.5, "步行"),
    ActivityTemplate("跑步 (8km/h)", 8.0, "跑步"),
    ActivityTemplate("跑步 (10km/h)", 10.0, "跑步"),
    ActivityTemplate("跑步 (12km/h)", 12.0, "跑步"),
    ActivityTemplate("骑自行车 (休闲)", 4.0, "骑车"),
    ActivityTemplate("骑自行车 (中速)", 6.8, "骑车"),
    ActivityTemplate("骑自行车 (快速)", 10.0, "骑车"),
    ActivityTemplate("游泳 (慢速)", 5.0, "游泳"),
    ActivityTemplate("游泳 (中速)", 7.0, "游泳"),
    ActivityTemplate("力量训练", 5.0, "健身"),
    ActivityTemplate("瑜伽", 3.0, "健身"),
    ActivityTemplate("跳绳", 11.0, "健身"),
    ActivityTemplate("篮球", 6.5, "球类"),
    ActivityTemplate("足球", 7.0, "球类"),
    ActivityTemplate("羽毛球", 5.5, "球类"),
    ActivityTemplate("乒乓球", 4.0, "球类"),
    ActivityTemplate("跳舞", 5.0, "其他"),
    ActivityTemplate("家务", 3.0, "其他"),
    ActivityTemplate("园艺", 3.5, "其他")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    onBack: () -> Unit,
    vm: ActivityViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsStateWithLifecycle()
    var selectedTemplate by remember { mutableStateOf<ActivityTemplate?>(null) }
    var durationStr by remember { mutableStateOf("30") }
    var searchQuery by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }

    val filtered = if (searchQuery.isBlank()) activityTemplates
    else activityTemplates.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "添加活动",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "自定义活动",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                .padding(horizontal = 16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "搜索活动...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selected template card
            if (selectedTemplate != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = selectedTemplate!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "MET: ${selectedTemplate!!.mets}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        OutlinedTextField(
                            value = durationStr,
                            onValueChange = { durationStr = it },
                            label = {
                                Text(
                                    text = "时长 (分钟)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        val weight = user?.weightKg ?: 70.0
                        val duration = durationStr.toDoubleOrNull() ?: 0.0
                        val kcal = MetCalc.getBurnedKcal(weight, selectedTemplate!!.mets, duration)
                        Text(
                            text = "预计消耗: ${kcal.roundToInt()} kcal",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedTemplate = null },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onPrimaryContainer)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("取消")
                            }
                            Button(
                                onClick = {
                                    vm.addActivity(selectedTemplate!!.name, selectedTemplate!!.mets, duration, kcal)
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("记录")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Activity list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val grouped = filtered.groupBy { it.category }
                grouped.forEach { (category, templates) ->
                    item {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(templates) { template ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplate = template },
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (selectedTemplate == template)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.elevatedCardElevation(
                                defaultElevation = if (selectedTemplate == template) 2.dp else 0.5.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (selectedTemplate == template)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedTemplate == template)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "MET: ${template.mets}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selectedTemplate == template)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomActivityDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, kcal, duration ->
                vm.addCustomActivity(name, kcal, duration)
                showCustomDialog = false
                onBack()
            }
        )
    }
}

@Composable
private fun CustomActivityDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, kcal: Double, duration: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "自定义活动",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            text = "活动名称",
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
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = {
                        Text(
                            text = "消耗卡路里",
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
                    value = duration,
                    onValueChange = { duration = it },
                    label = {
                        Text(
                            text = "时长 (分钟)",
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name, kcal.toDoubleOrNull() ?: 0.0, duration.toDoubleOrNull() ?: 0.0)
                },
                enabled = name.isNotBlank() && kcal.toDoubleOrNull() != null,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("添加")
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
