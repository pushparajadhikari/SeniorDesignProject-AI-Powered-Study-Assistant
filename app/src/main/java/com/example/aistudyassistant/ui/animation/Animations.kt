package com.example.aistudyassistant.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ── Shared motion helpers ───────────────────────────────────────────────────────
// Small, brand-neutral animation building blocks reused across screens so loading,
// entrance, and press feedback feel consistent instead of ad hoc per screen.

/**
 * Subtle press-down scale for a clickable card/button. Pass the same
 * [interactionSource] used by the node's `clickable` modifier so the two stay in sync.
 */
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "pressScale"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Animated placeholder block for loading states — replaces static gray boxes. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape:    Shape     = RoundedCornerShape(12.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translate by infiniteTransition.animateFloat(
        initialValue  = -600f,
        targetValue   = 600f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label         = "shimmerTranslate"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.55f),
            Color.LightGray.copy(alpha = 0.3f)
        ),
        start = Offset(translate - 300f, 0f),
        end   = Offset(translate + 300f, 300f)
    )
    Box(modifier = modifier.clip(shape).background(brush))
}

/** A stat number that animates up from its previous value whenever [targetValue] changes. */
@Composable
fun CountUpText(
    targetValue:    Int,
    modifier:       Modifier   = Modifier,
    fontSize:       TextUnit   = 22.sp,
    fontWeight:     FontWeight = FontWeight.Bold,
    color:          Color      = TextPrimary,
    durationMillis: Int        = 700
) {
    var previousValue by remember { mutableStateOf(0) }
    val animated = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animated.snapTo(previousValue.toFloat())
        animated.animateTo(
            targetValue   = targetValue.toFloat(),
            animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
        )
        previousValue = targetValue
    }

    Text(
        text       = animated.value.roundToInt().toString(),
        fontSize   = fontSize,
        fontWeight = fontWeight,
        color      = color,
        modifier   = modifier
    )
}

/**
 * Wraps a list item with a staggered fade + slide-in entrance, delayed by [index].
 * Use inside `LazyColumn`/`Column` items to make lists feel less abrupt on first load.
 */
@Composable
fun StaggeredEntranceItem(
    index:    Int,
    modifier: Modifier = Modifier,
    content:  @Composable () -> Unit
) {
    var visible by remember(index) { mutableStateOf(false) }

    LaunchedEffect(index) {
        delay((index * 40L).coerceAtMost(400L))
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label         = "entranceAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue   = if (visible) 0f else 24f,
        animationSpec = tween(300),
        label         = "entranceOffsetY"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha  = alpha
            translationY = offsetY
        }
    ) {
        content()
    }
}
