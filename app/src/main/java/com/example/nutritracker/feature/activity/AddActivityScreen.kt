package com.example.nutritracker.feature.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.ui.theme.*
import com.example.nutritracker.util.LocalIsChinese
import com.example.nutritracker.util.MetCalc
import com.example.nutritracker.util.UnitCalc
import com.example.nutritracker.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    onBack: () -> Unit,
    vm: ActivityViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsStateWithLifecycle()
    val useKj by vm.useKj.collectAsStateWithLifecycle()
    val isZh = LocalIsChinese.current
    var selectedActivity by remember { mutableStateOf<PhysicalActivity?>(null) }
    var lastSelectedActivity by remember { mutableStateOf<PhysicalActivity?>(null) }
    var durationStr by remember { mutableStateOf("30") }
    var searchQuery by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedActivity) {
        if (selectedActivity != null) lastSelectedActivity = selectedActivity
    }

    val activities = remember(searchQuery) {
        PhysicalActivityDatabase.searchActivities(searchQuery)
    }
    val grouped = remember(activities) {
        activities.groupBy { it.category }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tr("添加活动", "Log Activity"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("返回", "Back"),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = tr("自定义活动", "Custom Activity"),
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
                .padding(horizontal = Dimens.ContentPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(tr("搜索运动...", "Search activities...")) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = tr("清除", "Clear"))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(Dimens.CardSpacing))

            AnimatedExpand(visible = selectedActivity != null) {
                lastSelectedActivity?.let { activity ->
                    Column {
                        ActivityDetailCard(
                            activity = activity,
                            durationStr = durationStr,
                            onDurationChange = { durationStr = it },
                            user = user,
                            useKj = useKj,
                            onCancel = { selectedActivity = null },
                            onRecord = { kcal ->
                                val dur = durationStr.toDoubleOrNull() ?: 0.0
                                if (dur > 0.0 && kcal > 0.0) {
                                    vm.addActivity(activity.displayName(isZh), activity.mets, dur, kcal, activity.category.label(isZh))
                                    onBack()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(Dimens.CardSpacing))
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                var globalIdx = 0
                grouped.forEach { (category, categoryActivities) ->
                    val headerIdx = globalIdx++
                    item(key = "category_${category.name}") {
                        StaggeredFadeIn(index = headerIdx) {
                            Text(
                                text = category.label(isZh),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    val itemsStartIdx = globalIdx
                    itemsIndexed(categoryActivities, key = { _, act -> act.code }) { index, activity ->
                        StaggeredFadeIn(
                            modifier = Modifier.animateItem(),
                            index = itemsStartIdx + index
                        ) {
                            ActivityListItem(
                                activity = activity,
                                isSelected = selectedActivity?.code == activity.code,
                                onClick = { selectedActivity = activity }
                            )
                        }
                    }
                    globalIdx += categoryActivities.size
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomActivityDialog(
            useKj = useKj,
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
private fun ActivityDetailCard(
    activity: PhysicalActivity,
    durationStr: String,
    onDurationChange: (String) -> Unit,
    user: User?,
    useKj: Boolean,
    onCancel: () -> Unit,
    onRecord: (Double) -> Unit
) {
    val isZh = LocalIsChinese.current
    val weight = user?.weightKg ?: 70.0
    val duration = durationStr.toDoubleOrNull() ?: 0.0
    val validDuration = duration > 0.0
    val kcal = if (validDuration) MetCalc.getBurnedKcal(weight, activity.mets, duration) else 0.0

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.CardElevationHigh)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = activity.displayName(isZh),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            val desc = activity.displayDescription(isZh)
            if (desc.isNotBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Text(
                text = "MET: ${activity.mets}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            OutlinedTextField(
                value = durationStr,
                onValueChange = onDurationChange,
                label = { Text(tr("时长 (分钟)", "Duration (min)")) },
                isError = durationStr.isNotBlank() && !validDuration,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            Text(
                text = tr(
                    "预计消耗: ${UnitCalc.formatEnergy(kcal, useKj)}",
                    "Estimated burn: ${UnitCalc.formatEnergy(kcal, useKj)}"
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("取消", "Cancel"))
                }
                Button(
                    onClick = { onRecord(kcal) },
                    enabled = validDuration && kcal > 0.0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("记录", "Log"))
                }
            }
        }
    }
}

@Composable
private fun ActivityListItem(
    activity: PhysicalActivity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isZh = LocalIsChinese.current
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                when (activity.category) {
                    ActivityCategory.BICYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
                    ActivityCategory.CONDITIONING -> Icons.Filled.FitnessCenter
                    ActivityCategory.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
                    ActivityCategory.SPORT -> Icons.Filled.Sports
                    ActivityCategory.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
                    ActivityCategory.WATER -> Icons.Filled.Water
                    ActivityCategory.OTHER -> Icons.Filled.SelfImprovement
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.displayName(isZh),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                val desc = activity.displayDescription(isZh)
                if (desc.isNotBlank()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "MET ${activity.mets}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomActivityDialog(
    useKj: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, kcal: Double, duration: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var energyStr by remember { mutableStateOf("") }
    var durationStr by remember { mutableStateOf("30") }

    val energyVal = energyStr.toDoubleOrNull()
    val durationVal = durationStr.toDoubleOrNull()
    val isValid = name.isNotBlank() && energyVal != null && energyVal > 0.0 && durationVal != null && durationVal > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr("自定义活动", "Custom Activity"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tr("手动输入活动消耗的能量与时长", "Manually enter calories burned and duration"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("活动名称", "Activity name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = energyStr,
                    onValueChange = { energyStr = it },
                    label = { Text(tr("消耗 (${UnitCalc.energyUnitLabel(useKj)})", "Burned (${UnitCalc.energyUnitLabel(useKj)})")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it },
                    label = { Text(tr("时长 (分钟)", "Duration (min)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val kcal = UnitCalc.displayEnergyToKcal(energyVal ?: 0.0, useKj)
                    onConfirm(name, kcal, durationVal ?: 0.0)
                },
                enabled = isValid
            ) {
                Text(tr("添加", "Add"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("取消", "Cancel")) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
