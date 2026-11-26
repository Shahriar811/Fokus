package com.example.smartread.fokus.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.975f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        if (pressed) tween(85, easing = LinearOutSlowInEasing)
        else spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium),
        label = "Press feedback"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
internal fun FokusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Button(onClick, modifier.pressScale(source), enabled = enabled, shape = shape, colors = colors,
        contentPadding = contentPadding, interactionSource = source, content = content)
}

@Composable
internal fun FokusTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    TextButton(onClick, modifier.pressScale(source, 0.95f), interactionSource = source, content = content)
}

@Composable
internal fun FokusOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ButtonDefaults.outlinedShape,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    OutlinedButton(onClick, modifier.pressScale(source), shape = shape, interactionSource = source, content = content)
}

@Composable
internal fun FokusIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    IconButton(onClick, modifier.pressScale(source, 0.90f), colors = colors, interactionSource = source, content = content)
}

@Composable
internal fun FokusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = panelShape,
    color: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Surface(onClick, modifier.pressScale(source, 0.986f), enabled = enabled, shape = shape,
        color = color, border = border, interactionSource = source, content = content)
}
