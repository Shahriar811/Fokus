package com.example.smartread.fokus.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartread.fokus.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun TreePage(
    state: FokusState,
    controller: FokusController,
    seenWatering: String?,
    onWateringSeen: (String) -> Unit,
    onFocus: () -> Unit,
    onHelp: () -> Unit
) {
    val time by liveTime(controller)
    val zone = ZoneId.systemDefault()
    val today = time.wall.localDate(zone)
    val tree = remember(state.sessions, today, zone) { treeGrowth(state.sessions, today, zone) }
    val session = state.lastSavedSession?.takeUnless { it.id == seenWatering }
    val initial = remember {
        if (session == null) tree.growth
        else treeGrowth(state.sessions.filterNot { it.id == session.id }, today, zone).growth
    }
    val plant = remember { Animatable(initial) }
    val water = remember { Animatable(0f) }
    var wateredTime by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(tree.growth, session?.id) {
        if (session == null) {
            wateredTime = null
            water.snapTo(0f)
            plant.animateTo(tree.growth, tween(540, easing = FastOutSlowInEasing))
        } else {
            wateredTime = session.durationMillis
            water.snapTo(0f)
            coroutineScope {
                launch { water.animateTo(1f, tween(1150, easing = LinearEasing)) }
                launch { plant.animateTo(tree.growth, tween(900, delayMillis = 260, easing = FastOutSlowInEasing)) }
            }
            delay(1300)
            onWateringSeen(session.id)
        }
    }

    TreeContent(tree, plant.asState(), water.asState(), wateredTime, state.run != null, onFocus, onHelp)
}

@Composable
private fun TreeContent(
    tree: TreeGrowth,
    plant: State<Float>,
    water: State<Float>,
    wateredTime: Long?,
    timerActive: Boolean,
    onFocus: () -> Unit,
    onHelp: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        PageTitle("Focus tree", onHelp)
        TreeHero(tree, plant, water, wateredTime)
        if (tree.next != null) {
            Panel {
                Text("Next stage", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(tree.next.label, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(13.dp))
                GoalTrack("Focus time", "${durationLabel(tree.focusedMillis)} / ${durationLabel(tree.next.durationMillis)}",
                    tree.focusedMillis.toFloat() / tree.next.durationMillis)
                Spacer(Modifier.height(12.dp))
                GoalTrack("Days with focus", "${tree.focusedDays} / ${tree.next.focusedDays}",
                    tree.focusedDays.toFloat() / tree.next.focusedDays)
            }
        } else {
            Panel {
                Text("${tree.focusedDays} days with focus", style = MaterialTheme.typography.titleLarge)
                Muted("Every saved session still helps your canopy grow.")
            }
        }
        FokusButton(
            onClick = onFocus,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.secondary, contentColor = colors.onSecondary)
        ) {
            Icon(if (timerActive) Icons.Outlined.ChevronRight else Icons.Outlined.PlayArrow, null)
            Spacer(Modifier.width(6.dp))
            Text(if (timerActive) "Back to timer" else "Go to timer")
        }
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Last 7 days", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Text(
                    if (tree.streak == 0) "No current streak" else "${tree.streak}-day streak",
                    style = MaterialTheme.typography.labelMedium, color = colors.secondary
                )
            }
            Spacer(Modifier.height(12.dp))
            WeekRow(tree.week)
            HorizontalDivider(Modifier.padding(top = 13.dp, bottom = 10.dp))
            Text("${sessionCountLabel(tree.sessionCount)} · ${durationLabel(tree.focusedMillis)} of focus",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun TreeHero(tree: TreeGrowth, plant: State<Float>, water: State<Float>, wateredTime: Long?) {
    val dark = LocalDarkTheme.current
    val colors = MaterialTheme.colorScheme
    val sky = if (dark) listOf(Color(0xFF1C3028), Color(0xFF334C39))
    else listOf(Color(0xFFF0F4E3), Color(0xFFE0EBD6))
    val caption = when {
        wateredTime != null -> "Watered with ${durationLabel(wateredTime)} of focus"
        tree.week.last().duration > 0 -> "${durationLabel(tree.week.last().duration)} of focus today"
        tree.sessionCount == 0 -> "Your first focus session will water it."
        else -> "A focus session today will water it."
    }
    Column(
        Modifier.fillMaxWidth().clip(featureShape).background(Brush.verticalGradient(sky))
    ) {
        Column(Modifier.padding(start = 20.dp, top = 17.dp, end = 20.dp)) {
            Text("Growth stage", style = MaterialTheme.typography.labelMedium, color = colors.onSecondaryContainer)
            AnimatedContent(tree.stage.label, transitionSpec = { fadeIn(tween(210)) togetherWith fadeOut(tween(120)) },
                label = "Tree stage") { stage ->
                Text(stage, style = MaterialTheme.typography.headlineMedium, color = colors.onSecondaryContainer)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val height = (maxWidth * 0.67f).coerceIn(174.dp, 220.dp)
            TreeIllustration(plant, water, Modifier.fillMaxWidth().height(height))
        }
        Row(
            Modifier.fillMaxWidth().background(colors.secondaryContainer).padding(horizontal = 18.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.WaterDrop, null, Modifier.size(17.dp), tint = colors.secondary)
            AnimatedContent(caption, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(100)) },
                label = "Watering update") {
                Text(it, style = MaterialTheme.typography.labelLarge, color = colors.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun GoalTrack(label: String, amount: String, progress: Float) {
    val colors = MaterialTheme.colorScheme
    val value by animateFloatAsState(progress.coerceIn(0f, 1f), tween(450), label = "$label progress")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(amount, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            if (progress >= 1f) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.Check, "Goal reached", Modifier.size(16.dp), tint = colors.secondary)
            }
        }
        Box(
            Modifier.fillMaxWidth().height(5.dp).progressSemantics(value)
                .clip(CircleShape).background(colors.secondaryContainer)
        ) {
            if (value > 0f) {
                Box(Modifier.fillMaxWidth(value).fillMaxHeight().background(colors.secondary, CircleShape))
            }
        }
    }
}

@Composable
private fun WeekRow(days: List<DailyTotal>) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        days.forEach { day ->
            val active = day.duration > 0L
            val today = day == days.last()
            val fullDate = day.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
            Column(
                Modifier.weight(1f).clearAndSetSemantics {
                    contentDescription = "${if (today) "Today, " else ""}$fullDate, ${durationLabel(day.duration)} of focus"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier.size(29.dp).background(if (active) colors.secondaryContainer else colors.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (active) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), colors.onSecondaryContainer)
                    else Box(Modifier.size(if (today) 6.dp else 4.dp)
                        .background(if (today) colors.primary else colors.outline, CircleShape))
                }
                Text(day.date.format(DateTimeFormatter.ofPattern("EEEEE")),
                    style = MaterialTheme.typography.labelSmall, color = if (today) colors.primary else colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun TreeShortcut(tree: TreeGrowth, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FokusSurface(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = panelShape, color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(colors.secondaryContainer, panelShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Park, null, Modifier.size(22.dp), colors.onSecondaryContainer)
            }
            RowLabel("Your tree", if (tree.streak == 0) tree.stage.label else "${tree.stage.label} · ${tree.streak}-day streak",
                Modifier.weight(1f))
            Icon(Icons.Outlined.ChevronRight, null, tint = colors.secondary)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun SeedlingPreview() {
    val tree = treeGrowth(emptyList(), LocalDate.of(2026, 1, 1), ZoneId.of("UTC"))
    FokusTheme(false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            TreeContent(tree, rememberUpdatedState(tree.growth), rememberUpdatedState(0f), null, false, {}, {})
        }
    }
}
