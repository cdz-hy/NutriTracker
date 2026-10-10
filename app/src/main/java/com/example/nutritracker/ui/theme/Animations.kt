package com.example.nutritracker.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** MD3 标准过渡时长 (ms) */
object M3Duration {
    const val Short3 = 150
    const val Medium1 = 250
    const val Medium2 = 300
}

/** MD3 标准缓动曲线 */
object M3Easing {
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val Accelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}

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

@Composable
fun StaggeredFadeIn(
    modifier: Modifier = Modifier,
    index: Int,
    baseDelayMs: Int = 35,
    maxIndexForDelay: Int = 10,
    offsetY: Int = 30,
    content: @Composable () -> Unit
) {
    if (index >= maxIndexForDelay) {
        Box(modifier = modifier) { content() }
        return
    }

    val alpha = remember { Animatable(0f) }
    val transY = remember { Animatable(offsetY.toFloat()) }

    LaunchedEffect(Unit) {
        delay(index.toLong() * baseDelayMs)
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = M3Duration.Short3, easing = M3Easing.Decelerate)
            )
        }
        launch {
            transY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = M3Duration.Medium1, easing = M3Easing.Emphasized)
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

@Composable
fun PressScale(
    pressed: Boolean,
    pressScale: Float = 0.97f
): State<Float> = animateFloatAsState(
    targetValue = if (pressed) pressScale else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    ),
    label = "pressScale"
)

@Composable
fun animatedIntAsState(
    targetValue: Int,
    durationMs: Int = 700,
    label: String = "animatedInt"
): State<Int> = animateIntAsState(
    targetValue = targetValue,
    animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
    label = label
)

fun fadeSlideIn(
    durationMs: Int = 300,
    offsetY: Int = 40
): EnterTransition = fadeIn(animationSpec = tween(durationMs, easing = FastOutSlowInEasing)) +
    slideInVertically(
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        initialOffsetY = { offsetY }
    )

fun fadeSlideOut(
    durationMs: Int = 200,
    offsetY: Int = 40
): ExitTransition = fadeOut(animationSpec = tween(durationMs, easing = FastOutSlowInEasing)) +
    slideOutVertically(
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        targetOffsetY = { -offsetY }
    )

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

@Composable
fun AnimatedExpand(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(M3Duration.Medium1, easing = M3Easing.Emphasized)
        ) + fadeIn(animationSpec = tween(M3Duration.Medium1, easing = M3Easing.Emphasized)),
        exit = shrinkVertically(
            animationSpec = tween(M3Duration.Short3, easing = M3Easing.Accelerate)
        ) + fadeOut(animationSpec = tween(M3Duration.Short3))
    ) {
        content()
    }
}

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
