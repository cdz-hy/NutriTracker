package com.example.nutritracker.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.composed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

// ══════════════════════════════════════════════════════════════════════════════
// MD3 动画规范
// 参考: Material Design 3 - Motion Guidelines
// https://m3.material.io/styles/motion/easing-and-duration
// ══════════════════════════════════════════════════════════════════════════════

/** MD3 标准过渡时长 (ms) */
object M3Duration {
    const val Short1 = 50    // 微交互: 涟漪、开关
    const val Short2 = 100   // 小型: icon toggle
    const val Short3 = 150   // 短过渡: chip 选择
    const val Medium1 = 250  // 中等: 卡片展开
    const val Medium2 = 300  // 页面转场
    const val Long1 = 400    // 大型: 全屏过渡
    const val Long2 = 500    // 长效果: 启动动画
}

/** MD3 标准缓动曲线 */
object M3Easing {
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)           // 强调（全能）
    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)             // 标准缓动
    val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)          // 强调减速（入场）
    val Accelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)          // 强调加速（出场）
}

// ══════════════════════════════════════════════════════════════════════════════
// 导航全局转场动效规格 (M3 共享轴)
// ══════════════════════════════════════════════════════════════════════════════

val M3NavEnterTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate)) +
    slideInHorizontally(
        animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate),
        initialOffsetX = { it / 4 }
    )
}

val M3NavExitTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Accelerate)) +
    slideOutHorizontally(
        animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Accelerate),
        targetOffsetX = { -it / 4 }
    ) +
    scaleOut(
        animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Accelerate),
        targetScale = 0.9f
    )
}

val M3NavPopEnterTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate)) +
    slideInHorizontally(
        animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate),
        initialOffsetX = { -it / 4 }
    ) +
    scaleIn(
        animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Decelerate),
        initialScale = 0.9f
    )
}

val M3NavPopExitTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Accelerate)) +
    slideOutHorizontally(
        animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Accelerate),
        targetOffsetX = { it / 4 }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// 通用动画规格
// ══════════════════════════════════════════════════════════════════════════════

/**
 * 列表交错入场动画 (MD3 Staggered Entrance)
 *
 * 首屏元素（index < maxIndexForDelay）会以交错延迟 + 淡入上滑的方式入场，
 * 营造 MD3 标准的瀑布流视觉效果。
 *
 * 滚动时新出现的元素（index >= maxIndexForDelay）直接以极短的淡入呈现（50ms），
 * 避免滚动时出现明显的"延迟浮现"感。
 */
@Composable
fun StaggeredFadeIn(
    modifier: Modifier = Modifier,
    index: Int,
    baseDelayMs: Int = 35,
    maxIndexForDelay: Int = 10,
    offsetY: Int = 30,
    content: @Composable () -> Unit
) {
    // 滚动时出现的元素：直接可见，无动画
    if (index >= maxIndexForDelay) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    // 首屏元素：交错入场动画
    val alpha = remember { Animatable(0f) }
    val transY = remember { Animatable(offsetY.toFloat()) }

    LaunchedEffect(Unit) {
        delay(index.toLong() * baseDelayMs)
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = M3Duration.Short3,  // 150ms — 轻快入场
                    easing = M3Easing.Decelerate
                )
            )
        }
        launch {
            transY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = M3Duration.Medium1,  // 250ms — 位移略慢于透明度
                    easing = M3Easing.Emphasized
                )
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = transY.value
        }
    ) {
        content()
    }
}

/**
 * 弹性进入动画 - 用于 FAB、重要按钮
 */
@Composable
fun BounceIn(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.6f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ).plus(fadeIn(animationSpec = tween(M3Duration.Medium1))),
        exit = scaleOut(
            targetScale = 0.6f,
            animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate)
        ).plus(fadeOut(animationSpec = tween(M3Duration.Short3)))
    ) {
        content()
    }
}

/**
 * 页面转场 - 前进时从右滑入，后退时从左滑入
 */
fun M3PageTransition(forward: Boolean): ContentTransform {
    val enterX: (Int) -> Int = { if (forward) it / 5 else -it / 5 }
    val exitX: (Int) -> Int = { if (forward) -it / 5 else it / 5 }
    val enter = fadeIn(animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Emphasized))
        .plus(slideInHorizontally(
            animationSpec = tween(M3Duration.Medium2, easing = M3Easing.Emphasized),
            initialOffsetX = enterX
        ))
    val exit = fadeOut(animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate))
        .plus(slideOutHorizontally(
            animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate),
            targetOffsetX = exitX
        ))
    return enter togetherWith exit
}

/**
 * 缩放弹出/收起动画 - 用于对话框、底部弹窗
 */
fun M3ScaleIn() = scaleIn(
    initialScale = 0.92f,
    animationSpec = tween(M3Duration.Medium1, easing = M3Easing.Emphasized)
).plus(fadeIn(animationSpec = tween(M3Duration.Medium1)))

fun M3ScaleOut() = scaleOut(
    targetScale = 0.92f,
    animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate)
).plus(fadeOut(animationSpec = tween(M3Duration.Short3)))

/**
 * 简单淡入 - 无位移
 */
fun M3FadeIn() = fadeIn(animationSpec = tween(M3Duration.Medium1, easing = M3Easing.Emphasized))
fun M3FadeOut() = fadeOut(animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate))

// ══════════════════════════════════════════════════════════════════════════════
// 数值动画
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedCounter(
    target: Int,
    durationMs: Int = M3Duration.Medium1
): State<Int> = animateIntAsState(
    targetValue = target,
    animationSpec = tween(durationMs, easing = M3Easing.Emphasized)
)

/**
 * 卡片按压缩放效果 - 0.97f 按压态, 1f 正常
 */
@Composable
fun PressScale(
    pressed: Boolean,
    pressScale: Float = 0.97f
): State<Float> = animateFloatAsState(
    targetValue = if (pressed) pressScale else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)

/**
 * 数字递增动画 - 兼容旧调用
 */
@Composable
fun animatedIntAsState(
    targetValue: Int,
    durationMs: Int = 800,
    label: String = "animatedInt"
): State<Int> = animateIntAsState(
    targetValue = targetValue,
    animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
    label = label
)

/**
 * 淡入 + 上滑动画规范 - 兼容旧调用
 */
fun fadeSlideIn(
    durationMs: Int = 300,
    offsetY: Int = 40
): EnterTransition = fadeIn(animationSpec = tween(durationMs, easing = FastOutSlowInEasing))
    .plus(slideInVertically(
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        initialOffsetY = { offsetY }
    ))

fun fadeSlideOut(
    durationMs: Int = 200,
    offsetY: Int = 40
): ExitTransition = fadeOut(animationSpec = tween(durationMs, easing = FastOutSlowInEasing))
    .plus(slideOutVertically(
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        targetOffsetY = { -offsetY }
    ))

/**
 * 弹性缩放效果 - 用于选择卡片 (保留兼容旧调用)
 */
@Composable
fun animatedSpringScale(
    selected: Boolean,
    selectedScale: Float = 1.02f
): State<Float> = animateFloatAsState(
    targetValue = if (selected) selectedScale else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    label = "cardScale"
)

/**
 * 浮点数动画 (保留兼容旧调用)
 */
@Composable
fun animatedFloatAsState(
    targetValue: Float,
    durationMs: Int = 800,
    label: String = "animatedFloat"
): State<Float> = animateFloatAsState(
    targetValue = targetValue,
    animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
    label = label
)

// ══════════════════════════════════════════════════════════════════════════════
// 快捷 AnimatedVisibility 包装器 - 减少样板代码
// ══════════════════════════════════════════════════════════════════════════════

/**
 * 列表项入场: 淡入 + 上滑 40dp
 */
@Composable
fun AnimatedItemEnter(
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(M3Duration.Medium1, easing = M3Easing.Emphasized))
            .plus(slideInVertically(
                animationSpec = tween(M3Duration.Medium1, easing = M3Easing.Emphasized),
                initialOffsetY = { 40 }
            )),
        exit = fadeOut(animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate))
            .plus(slideOutVertically(
                animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate),
                targetOffsetY = { -40 }
            ))
    ) {
        content()
    }
}

/**
 * 展开收起: 垂直方向 + 淡入淡出
 */
@Composable
fun AnimatedExpand(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(M3Duration.Medium1, easing = M3Easing.Emphasized)
        ).plus(fadeIn(animationSpec = tween(M3Duration.Medium1, easing = M3Easing.Emphasized))),
        exit = shrinkVertically(
            animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate)
        ).plus(fadeOut(animationSpec = tween(M3Duration.Short3)))
    ) {
        content()
    }
}

/**
 * 弹性点击修饰符 - 提供类似 MD3/iOS 的微弹物理反馈
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by PressScale(pressed = enabled && isPressed)

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick
        )
}
