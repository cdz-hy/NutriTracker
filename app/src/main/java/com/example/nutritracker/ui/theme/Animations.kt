package com.example.nutritracker.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ══════════════════════════════════════════════════════════════════════════════
// 可复用动画工具
// ══════════════════════════════════════════════════════════════════════════════

/**
 * 数字递增动画 - 从 0 动画到 targetValue
 * 适用于卡路里、BMI、体重等数值显示
 */
@Composable
fun animatedIntAsState(
    targetValue: Int,
    durationMs: Int = 800,
    label: String = "animatedInt"
): State<Int> {
    return animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = durationMs,
            easing = FastOutSlowInEasing
        ),
        label = label
    )
}

/**
 * 浮点数递增动画
 */
@Composable
fun animatedFloatAsState(
    targetValue: Float,
    durationMs: Int = 800,
    label: String = "animatedFloat"
): State<Float> {
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = durationMs,
            easing = FastOutSlowInEasing
        ),
        label = label
    )
}

/**
 * 列表项交错入场动画容器
 * @param index 当前列表项索引
 * @param baseDelayMs 每项之间的延迟
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    baseDelayMs: Long = 50,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val delayTime = if (index < 6) baseDelayMs * index else 0L
        if (delayTime > 0) {
            delay(delayTime)
        }
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            initialOffsetY = { 30 },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
    ) {
        content()
    }
}

/**
 * 淡入 + 上滑进入规范 - 用于 AnimatedVisibility
 */
fun fadeSlideIn(
    durationMs: Int = 300,
    offsetY: Int = 40
): EnterTransition = fadeIn(
    animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
) + slideInVertically(
    initialOffsetY = { offsetY },
    animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
)

/**
 * 淡出 + 下滑退出规范
 */
fun fadeSlideOut(
    durationMs: Int = 200,
    offsetY: Int = 40
): ExitTransition = fadeOut(
    animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
) + slideOutVertically(
    targetOffsetY = { -offsetY },
    animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
)

/**
 * 缩放进入动画规范
 */
fun scaleEnter(
    durationMs: Int = 300,
    initialScale: Float = 0.92f
): EnterTransition = fadeIn(
    animationSpec = tween(durationMs)
) + scaleIn(
    initialScale = initialScale,
    animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
)

/**
 * 页面间共享轴变换 - 供 AnimatedContent 使用
 * 前进: 从右滑入 + 旧页左滑出
 * 后退: 从左滑入 + 旧页右滑出
 */
fun sharedAxisTransition(isForward: Boolean): ContentTransform {
    val enter = fadeIn(tween(300)) + slideInHorizontally(
        initialOffsetX = { if (isForward) it / 4 else -it / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
    val exit = fadeOut(tween(200)) + slideOutHorizontally(
        targetOffsetX = { if (isForward) -it / 4 else it / 4 },
        animationSpec = tween(200, easing = FastOutSlowInEasing)
    )
    return enter togetherWith exit
}

/**
 * 弹性缩放点击效果 - 用于 Selection Cards
 */
@Composable
fun animatedSpringScale(
    selected: Boolean,
    selectedScale: Float = 1.02f
): State<Float> {
    return animateFloatAsState(
        targetValue = if (selected) selectedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )
}
