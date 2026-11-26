package com.example.smartread.fokus.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.smartread.fokus.Category
import kotlin.math.roundToInt

internal val LocalOpenClock = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = panelShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
internal fun PageTitle(title: String, info: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
        LocalOpenClock.current?.let { open ->
            FokusIconButton(onClick = open) { Icon(Icons.Outlined.Schedule, "Open full-screen clock") }
        }
        if (info != null) InfoButton(title, info)
    }
}

@Composable
internal fun InfoButton(topic: String, click: () -> Unit) {
    FokusIconButton(onClick = click) {
        Icon(Icons.Outlined.Info, "About ${topic.lowercase()}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun <T> Selector(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ((T) -> ImageVector)? = null,
    detail: ((T) -> String)? = null
) {
    val colors = MaterialTheme.colorScheme
    SlidingChoices(
        items, selected, onSelect,
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceVariant).padding(4.dp),
        indicatorColor = colors.primaryContainer, enabled = enabled,
        minHeight = if (detail == null) 44.dp else 58.dp,
        indicatorPadding = 0.dp
    ) { item, active ->
        val target = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant
        val tint by animateColorAsState(if (enabled) target else target.copy(alpha = 0.74f), tween(180), label = "Segment text")
        Row(
            Modifier.padding(horizontal = 3.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let { Icon(it(item), null, Modifier.size(18.dp), tint) }
            Column(horizontalAlignment = if (icon == null) Alignment.CenterHorizontally else Alignment.Start) {
                Text(label(item), style = MaterialTheme.typography.labelLarge, color = tint, textAlign = TextAlign.Center)
                detail?.let { Text(it(item), style = MaterialTheme.typography.labelSmall, color = tint) }
            }
        }
    }
}

@Composable
internal fun <T> SlidingChoices(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    indicatorColor: Color = MaterialTheme.colorScheme.primaryContainer,
    minHeight: Dp = 58.dp,
    indicatorPadding: Dp = 2.dp,
    enabled: Boolean = true,
    content: @Composable (T, Boolean) -> Unit
) {
    val position by animateFloatAsState(
        items.indexOf(selected).coerceAtLeast(0).toFloat(),
        tween(290, easing = FastOutSlowInEasing),
        label = "Sliding selection"
    )
    BoxWithConstraints(modifier.selectableGroup()) {
        val slot = maxWidth / items.size
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier.offset { IntOffset((slot.toPx() * position).roundToInt(), 0) }
                    .width(slot).fillMaxHeight().padding(indicatorPadding)
                    .background(indicatorColor, RoundedCornerShape(14.dp))
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val active = item == selected
                val source = remember { MutableInteractionSource() }
                Box(
                    Modifier.weight(1f).heightIn(min = minHeight)
                        .selectable(active, source, indication = null, enabled = enabled, role = Role.Tab) {
                            if (!active) onSelect(item)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.pressScale(source, 0.94f), contentAlignment = Alignment.Center) { content(item, active) }
                }
            }
        }
    }
}

internal val Category.icon: ImageVector
    get() = when (this) {
        Category.TASK -> Icons.Outlined.CheckCircle
        Category.COURSE -> Icons.Outlined.School
        Category.BOOK -> Icons.Outlined.AutoStories
        Category.STUDY -> Icons.Outlined.EditNote
        Category.WORK -> Icons.Outlined.WorkOutline
        Category.GYM -> Icons.Outlined.FitnessCenter
        Category.WALKING -> Icons.AutoMirrored.Outlined.DirectionsWalk
        Category.RUNNING -> Icons.AutoMirrored.Outlined.DirectionsRun
        Category.OTHER -> Icons.Outlined.Interests
    }

@Composable
internal fun CategoryBadge(category: Category) {
    val colors = MaterialTheme.colorScheme
    val (container, ink) = when (category) {
        Category.GYM, Category.WALKING, Category.RUNNING -> colors.secondaryContainer to colors.onSecondaryContainer
        Category.BOOK, Category.WORK -> colors.tertiaryContainer to colors.onTertiaryContainer
        else -> colors.primaryContainer to colors.onPrimaryContainer
    }
    Box(
        Modifier.size(44.dp).background(container, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(category.icon, null, Modifier.size(21.dp), ink)
    }
}

@Composable
internal fun RowLabel(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Muted(subtitle)
    }
}

@Composable
internal fun EmptyPanel(icon: ImageVector, title: String, subtitle: String, action: String? = null, onClick: () -> Unit = {}) {
    Panel {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier.size(46.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(22.dp), MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) FokusTextButton(onClick = onClick) { Text(action) }
        }
    }
}
