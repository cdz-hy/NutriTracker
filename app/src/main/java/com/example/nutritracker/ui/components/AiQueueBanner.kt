package com.example.nutritracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nutritracker.feature.camera.AnalysisTask
import com.example.nutritracker.ui.theme.M3Duration
import com.example.nutritracker.ui.theme.M3Easing

@Composable
fun AiFloatingQueueBanner(
    activeCount: Int,
    firstActiveTask: AnalysisTask?,
    pendingConfirmTasks: List<AnalysisTask>,
    failedTasks: List<AnalysisTask>,
    onCancelActive: (Int) -> Unit,
    onReviewPending: (AnalysisTask) -> Unit,
    onRetryFailed: (Int) -> Unit,
    onDismissFailed: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = activeCount > 0 || pendingConfirmTasks.isNotEmpty() || failedTasks.isNotEmpty()

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate)
        ) + fadeIn(animationSpec = tween(M3Duration.Medium2)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate)
        ) + fadeOut(animationSpec = tween(M3Duration.Short3)),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pendingConfirmTasks.isNotEmpty()) {
                val first = pendingConfirmTasks.first()
                val title = first.result?.nutritionResult?.mealName?.ifBlank { "AI 识别结果" } ?: "AI 识别结果"
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReviewPending(first) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "待确认：$title",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (pendingConfirmTasks.size > 1)
                                    "共 ${pendingConfirmTasks.size} 项识别已完成，点击微调份量并入库"
                                else "识别完成，点击微调份量并确认入库",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        FilledTonalButton(
                            onClick = { onReviewPending(first) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("确认")
                        }
                    }
                }
            }

            if (activeCount > 0 && firstActiveTask != null) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI 智能分析中 ($activeCount 个任务)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "后台处理中，可继续自由操作",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onCancelActive(firstActiveTask.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "取消任务", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (failedTasks.isNotEmpty() && activeCount == 0 && pendingConfirmTasks.isEmpty()) {
                val failed = failedTasks.first()
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI 识别失败",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = failed.errorMessage ?: "请检查网络或配置",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 2
                            )
                        }
                        IconButton(onClick = { onRetryFailed(failed.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "重试",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(onClick = { onDismissFailed(failed.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
