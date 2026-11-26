package com.example.smartread.fokus.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartread.R
import com.example.smartread.fokus.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class DialFrame(val phase: Phase, val remaining: Long, val duration: Long)

private val aodText = Color(0xFFE4E8E5)
private val aodMuted = Color(0xFF7D8982)
private val aodTrack = Color(0xFF171E1A)
private val aodTickMuted = Color(0xFF2B3630)
private val aodControlSurface = Color(0xFF0E1310)
private val aodControlBorder = Color(0xFF232E28)
private val aodFocus = Color(0xFF7AD29F)
private val aodBreak = Color(0xFFE7AF6B)

@Composable
internal fun TimerPage(
    state: FokusState,
    controller: FokusController,
    onHelp: () -> Unit,
    onPick: () -> Unit,
    onDurations: () -> Unit,
    onToggleAod: () -> Unit,
    onFinish: () -> Unit,
    onToday: () -> Unit,
    onDisplay: () -> Unit,
    onClock: () -> Unit,
    onTree: () -> Unit
) {
    val time by liveTime(controller, state.run?.deadline != null)
    TimerHomeContent(
        state = state,
        time = time,
        onHelp = onHelp,
        onPick = onPick,
        onDurations = onDurations,
        onToggleAod = onToggleAod,
        onPhase = controller::changePhase,
        onToggle = { controller.toggleTimer(state.phase, state.run) },
        onFinish = onFinish,
        onToday = onToday,
        onDisplay = onDisplay,
        onClock = onClock,
        onTree = onTree
    )
}

@Composable
private fun TimerHomeContent(
    state: FokusState,
    time: TimeMark,
    onHelp: () -> Unit,
    onPick: () -> Unit,
    onDurations: () -> Unit,
    onToggleAod: () -> Unit,
    onPhase: (Phase) -> Unit,
    onToggle: () -> Unit,
    onFinish: () -> Unit,
    onToday: () -> Unit,
    onDisplay: () -> Unit,
    onClock: () -> Unit,
    onTree: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val today = time.wall.localDate(zone)
    val todaySessions = remember(state.sessions, today, zone) {
        state.sessions.filter { it.endMillis.localDate(zone) == today }
    }
    val tree = remember(state.sessions, today, zone) { treeGrowth(state.sessions, today, zone) }
    val activity = state.run?.activity ?: state.selectedActivity

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(2.dp))
        HomeHeader(today = today, wallTime = time.wall, onClock = onClock, onHelp = onHelp)
        PhaseSwitcher(
            selected = state.phase,
            focusMinutes = state.preferences.focusMinutes,
            breakMinutes = state.preferences.breakMinutes,
            enabled = state.run == null,
            onSelect = onPhase
        )
        TimerHeroCard(
            state = state,
            time = time,
            activity = activity,
            onPick = onPick,
            onDurations = onDurations,
            onToggleAod = onToggleAod,
            onExpand = onDisplay,
            onToggle = onToggle,
            onFinish = onFinish
        )
        DailyMomentumRow(
            todaySessions = todaySessions,
            tree = tree,
            onToday = onToday,
            onTree = onTree
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun HomeHeader(
    today: LocalDate,
    wallTime: Long,
    onClock: () -> Unit,
    onHelp: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dateLabel = remember(today) { today.format(DateTimeFormatter.ofPattern("EEE, MMM d")) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_fokus_status), null, Modifier.size(20.dp), colors.onPrimary)
        }
        Column(Modifier.padding(start = 11.dp).weight(1f)) {
            Text("Fokus", style = MaterialTheme.typography.titleLarge)
            Text(dateLabel, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
        ClockButton(wallTime, onClock)
        InfoButton("Pomodoro timer", onHelp)
    }
}

@Composable
private fun ClockButton(wallTime: Long, onClick: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val minute = wallTime / 60_000
    val clock = remember(minute, context) { DateFormat.getTimeFormat(context).format(Date(wallTime)) }
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier.heightIn(min = 48.dp)
            .clickable(source, indication = null, onClickLabel = "Open full-screen clock", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.pressScale(source, 0.95f)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.outlineVariant, CircleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp), colors.primary)
            Text(clock, style = tabularLabelStyle, color = colors.onSurface, maxLines = 1)
        }
    }
}

@Composable
private fun PhaseSwitcher(
    selected: Phase,
    focusMinutes: Int,
    breakMinutes: Int,
    enabled: Boolean,
    onSelect: (Phase) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val tokens = LocalFokusTokens.current
    val isBreak = selected == Phase.BREAK
    val pillColor by animateColorAsState(
        if (isBreak) tokens.breakAccentSoft else tokens.focusAccentSoft,
        tween(240),
        label = "Phase pill color"
    )
    SlidingChoices(
        items = Phase.entries,
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainer)
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(4.dp),
        indicatorColor = pillColor,
        minHeight = 46.dp,
        indicatorPadding = 0.dp,
        enabled = enabled
    ) { phase, active ->
        val activeColor = if (phase == Phase.BREAK) tokens.breakAccent else tokens.focusAccent
        val tint by animateColorAsState(
            if (active) activeColor else colors.onSurfaceVariant,
            tween(180),
            label = "Phase text"
        )
        val itemAlpha = if (enabled || active) 1f else 0.45f
        val mins = if (phase == Phase.FOCUS) focusMinutes else breakMinutes
        Row(
            Modifier.alpha(itemAlpha).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                if (phase == Phase.FOCUS) Icons.Outlined.CenterFocusStrong else Icons.Outlined.Coffee,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = tint
            )
            Text(
                "${phase.label} · ${mins}m",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = tint,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TimerHeroCard(
    state: FokusState,
    time: TimeMark,
    activity: Activity,
    onPick: () -> Unit,
    onDurations: () -> Unit,
    onToggleAod: () -> Unit,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onFinish: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val tokens = LocalFokusTokens.current
    val dark = LocalDarkTheme.current
    val isBreak = state.phase == Phase.BREAK

    val accent by animateColorAsState(
        if (isBreak) tokens.breakAccent else tokens.focusAccent,
        tween(300),
        label = "Hero accent"
    )
    val accentSoft by animateColorAsState(
        if (isBreak) tokens.breakAccentSoft else tokens.focusAccentSoft,
        tween(300),
        label = "Hero accent soft"
    )
    val onAccent by animateColorAsState(
        if (isBreak) tokens.onBreakAccent else tokens.onFocusAccent,
        tween(300),
        label = "Hero onAccent"
    )
    val glow by animateColorAsState(
        if (isBreak) tokens.breakGlow else tokens.focusGlow,
        tween(300),
        label = "Hero glow"
    )

    val duration = state.run?.durationMillis ?: state.preferences.minutes(state.phase) * 60_000L
    val remaining = state.run?.remaining(time) ?: duration
    val running = state.run?.deadline != null
    val paused = state.run != null && !running

    val borderStroke by animateColorAsState(
        if (running) accent.copy(alpha = if (dark) 0.48f else 0.36f) else tokens.heroBorder,
        tween(280),
        label = "Hero border"
    )

    val statusText = when {
        running -> if (isBreak) "On break" else "Focusing"
        paused -> "Paused"
        state.ready -> if (isBreak) "Focus saved" else "Break complete"
        else -> if (isBreak) "Ready to recharge" else "Ready to focus"
    }

    val endWallMillis = state.run?.takeIf { it.deadline != null }?.completedAt(time) ?: (time.wall + remaining)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = featureShape,
        color = tokens.heroSurface,
        border = BorderStroke(1.dp, borderStroke)
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(
                    listOf(
                        accentSoft.copy(alpha = if (dark) 0.24f else 0.42f),
                        Color.Transparent,
                        Color.Transparent
                    )
                )
            )
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HeroTopBar(
                    status = statusText,
                    running = running,
                    completed = state.ready && state.run == null,
                    quietBadge = state.preferences.quietMode && !isBreak,
                    aodMode = state.preferences.aodMode,
                    canEditDurations = state.run == null,
                    durationMinutes = state.preferences.minutes(state.phase),
                    accent = accent,
                    accentSoft = accentSoft,
                    onDurations = onDurations,
                    onToggleAod = onToggleAod,
                    onExpand = onExpand
                )

                IntentionAnchor(
                    isBreak = isBreak,
                    running = state.run != null,
                    activity = activity,
                    accent = accent,
                    accentSoft = accentSoft,
                    onPick = onPick
                )

                AnimatedContent(
                    targetState = DialFrame(state.phase, remaining, duration),
                    modifier = Modifier.widthIn(max = 224.dp).fillMaxWidth().aspectRatio(1f),
                    contentKey = { it.phase },
                    transitionSpec = { fadeIn(tween(245, 55)) togetherWith fadeOut(tween(95)) },
                    label = "Timer dial phase"
                ) { frame ->
                    ChronographDial(
                        remaining = frame.remaining,
                        duration = frame.duration,
                        endWallMillis = endWallMillis,
                        running = running,
                        paused = paused,
                        accent = if (frame.phase == Phase.BREAK) tokens.breakAccent else tokens.focusAccent,
                        onAccent = if (frame.phase == Phase.BREAK) tokens.onBreakAccent else tokens.onFocusAccent,
                        glow = if (frame.phase == Phase.BREAK) tokens.breakGlow else tokens.focusGlow,
                        dialTrack = tokens.dialTrack,
                        tickMuted = tokens.dialTickMuted,
                        textColor = colors.onSurface,
                        mutedTextColor = colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                HeroActionControls(
                    running = running,
                    activeRun = state.run != null,
                    isBreak = isBreak,
                    accent = accent,
                    accentSoft = accentSoft,
                    onAccent = onAccent,
                    onToggle = onToggle,
                    onFinish = onFinish
                )
            }
        }
    }
}

@Composable
private fun HeroTopBar(
    status: String,
    running: Boolean,
    completed: Boolean,
    quietBadge: Boolean,
    aodMode: Boolean,
    canEditDurations: Boolean,
    durationMinutes: Int,
    accent: Color,
    accentSoft: Color,
    onDurations: () -> Unit,
    onToggleAod: () -> Unit,
    onExpand: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val pulseTransition = rememberInfiniteTransition(label = "Status pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Status dot alpha"
    )
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.weight(1f, fill = false)
                    .clip(CircleShape)
                    .background(accentSoft.copy(alpha = 0.78f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (completed) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(14.dp), tint = accent)
                } else {
                    Box(
                        Modifier.size(7.dp)
                            .alpha(if (running) pulseAlpha else 1f)
                            .background(accent, CircleShape)
                    )
                }
                AnimatedContent(
                    status,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(110)) },
                    label = "Status label"
                ) { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (quietBadge) {
                Row(
                    Modifier.clip(CircleShape)
                        .background(colors.surfaceContainerHigh)
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Outlined.DoNotDisturbOn, null, Modifier.size(12.dp), colors.onSurfaceVariant)
                    Text("DND", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
                }
            }
        }

        val aodSource = remember { MutableInteractionSource() }
        Box(
            Modifier.heightIn(min = 42.dp)
                .clickable(
                    aodSource,
                    indication = null,
                    onClickLabel = if (aodMode) "Disable in-app AOD" else "Enable in-app AOD",
                    onClick = onToggleAod
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.pressScale(aodSource, 0.94f)
                    .clip(CircleShape)
                    .background(if (aodMode) accentSoft else colors.surfaceContainerLow)
                    .border(
                        1.dp,
                        if (aodMode) accent.copy(alpha = 0.55f) else colors.outlineVariant,
                        CircleShape
                    )
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (aodMode) Icons.Outlined.Brightness4 else Icons.Outlined.Brightness2,
                    null,
                    Modifier.size(13.dp),
                    if (aodMode) accent else colors.onSurfaceVariant
                )
                Text(
                    "AOD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (aodMode) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (aodMode) accent else colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        if (canEditDurations) {
            val source = remember { MutableInteractionSource() }
            Box(
                Modifier.heightIn(min = 42.dp)
                    .clickable(source, indication = null, onClickLabel = "Edit timer durations", onClick = onDurations),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier.pressScale(source, 0.94f)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerLow)
                        .border(1.dp, colors.outlineVariant, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.Tune, null, Modifier.size(13.dp), colors.onSurfaceVariant)
                    Text("${durationMinutes}m", style = tabularLabelStyle, color = colors.onSurface, maxLines = 1)
                }
            }
        }

        FokusIconButton(
            onClick = onExpand,
            modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = colors.surfaceContainerLow,
                contentColor = colors.onSurfaceVariant
            )
        ) {
            Icon(Icons.Outlined.OpenInFull, "Open in-app AOD timer", Modifier.size(16.dp))
        }
    }
}

@Composable
private fun IntentionAnchor(
    isBreak: Boolean,
    running: Boolean,
    activity: Activity,
    accent: Color,
    accentSoft: Color,
    onPick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    AnimatedContent(
        targetState = isBreak,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
        label = "Intention mode"
    ) { breakPhase ->
        if (breakPhase) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentSoft.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.SelfImprovement, null, Modifier.size(19.dp), accent)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Take a breather", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    Text(
                        "Step away, stretch, or hydrate",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            val subtitle = when {
                running -> "${activity.category.label} · In session"
                activity.id == generalActivity.id -> "Tap to tag a book, task, or study"
                else -> activity.category.label
            }
            FokusSurface(
                onClick = onPick,
                enabled = !running,
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainerLow,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = if (running) 0.45f else 0.85f))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(activity.category.icon, null, Modifier.size(18.dp), colors.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            activity.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!running) {
                        Icon(
                            Icons.Outlined.UnfoldMore,
                            contentDescription = "Choose activity",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChronographDial(
    remaining: Long,
    duration: Long,
    endWallMillis: Long,
    running: Boolean,
    paused: Boolean,
    accent: Color,
    onAccent: Color,
    glow: Color,
    dialTrack: Color,
    tickMuted: Color,
    textColor: Color,
    mutedTextColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val goal = (remaining.toFloat() / duration.coerceAtLeast(1)).coerceIn(0f, 1f)
    val progress by animateFloatAsState(goal, tween(250), label = "Countdown ring")
    val value = countdown(remaining)

    val endMinuteBucket = endWallMillis / 60_000L
    val endClock = remember(endMinuteBucket, context) {
        DateFormat.getTimeFormat(context).format(Date(endWallMillis))
    }

    val breathingTransition = rememberInfiniteTransition(label = "Dial aura")
    val auraAlpha by breathingTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Aura pulse"
    )

    val spokenMinutes = value.substringBefore(':').toIntOrNull() ?: 0
    val spokenSeconds = value.substringAfter(':').toIntOrNull() ?: 0

    Box(
        modifier.clearAndSetSemantics {
            contentDescription = "$spokenMinutes minutes, $spokenSeconds seconds remaining"
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val radius = size.minDimension / 2f
            val ring = radius * 0.90f

            if (running) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(glow.copy(alpha = auraAlpha), glow.copy(alpha = auraAlpha * 0.35f), Color.Transparent),
                        center,
                        radius * 0.92f
                    ),
                    radius = radius * 0.92f
                )
            }

            // Subtle outer compass hairline
            drawCircle(
                dialTrack.copy(alpha = 0.7f),
                radius = radius * 0.985f,
                style = Stroke(1.dp.toPx())
            )

            // 60 radial precision ticks showing remaining vs elapsed arc
            repeat(60) { tick ->
                val tickFraction = tick / 60f
                val activeTick = progress >= 0.999f || tickFraction <= progress
                val angle = (tick / 60.0 * 2 - 0.5) * PI
                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                val major = tick % 5 == 0
                val tickColor = if (activeTick) {
                    accent.copy(alpha = if (major) 0.62f else 0.28f)
                } else {
                    tickMuted.copy(alpha = if (major) 0.45f else 0.20f)
                }
                drawLine(
                    color = tickColor,
                    start = center + direction * radius * if (major) 0.76f else 0.795f,
                    end = center + direction * radius * 0.835f,
                    strokeWidth = (if (major) 1.6.dp else 1.dp).toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Progress track
            drawCircle(dialTrack, radius = ring, style = Stroke(4.5.dp.toPx()))

            // Active progress arc + glowing leading orb
            if (progress > 0f) {
                val edge = Offset(center.x - ring, center.y - ring)
                val bounds = Size(2 * ring, 2 * ring)
                val sweep = 360f * progress
                drawArc(
                    color = accent.copy(alpha = 0.16f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = edge,
                    size = bounds,
                    style = Stroke(9.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = edge,
                    size = bounds,
                    style = Stroke(4.5.dp.toPx(), cap = StrokeCap.Round)
                )
                if (progress < 0.999f) {
                    val tipAngle = (-90f + sweep) * (PI / 180.0)
                    val tip = center + Offset(cos(tipAngle).toFloat(), sin(tipAngle).toFloat()) * ring
                    drawCircle(accent.copy(alpha = 0.28f), 6.dp.toPx(), tip)
                    drawCircle(accent, 3.6.dp.toPx(), tip)
                    drawCircle(onAccent, 1.4.dp.toPx(), tip)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth(0.70f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val fit = with(LocalDensity.current) { (maxWidth / if (value.length > 5) 3.7f else 3.05f).toSp() }
                val font = fit.value.coerceAtMost(62f).sp
                Text(
                    value,
                    color = textColor,
                    style = numberStyle.copy(fontSize = font, lineHeight = font * 1.05f),
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }

            val subText = when {
                running -> "Ends $endClock"
                paused -> "Paused · ${durationLabel(duration)}"
                else -> "Ends $endClock"
            }
            Text(
                subText,
                style = tabularLabelStyle,
                color = if (running) accent else mutedTextColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun HeroActionControls(
    running: Boolean,
    activeRun: Boolean,
    isBreak: Boolean,
    accent: Color,
    accentSoft: Color,
    onAccent: Color,
    endContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    endContentColor: Color = MaterialTheme.colorScheme.onSurface,
    endBorderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    onToggle: () -> Unit,
    onFinish: () -> Unit
) {
    if (!activeRun) {
        FokusButton(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent)
        ) {
            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(22.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                if (isBreak) "Start break" else "Start focus",
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FokusButton(
                onClick = onToggle,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = CircleShape,
                colors = if (running) {
                    ButtonDefaults.buttonColors(containerColor = accentSoft, contentColor = accent)
                } else {
                    ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent)
                }
            ) {
                Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(21.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    if (running) "Pause" else "Resume",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            FokusSurface(
                onClick = onFinish,
                modifier = Modifier.height(52.dp).semantics { contentDescription = "End timer" },
                shape = CircleShape,
                color = endContainerColor,
                border = BorderStroke(1.dp, endBorderColor)
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Stop, null, Modifier.size(20.dp), tint = endContentColor)
                    Text("End", style = MaterialTheme.typography.labelLarge, color = endContentColor)
                }
            }
        }
    }
}

@Composable
private fun DailyMomentumRow(
    todaySessions: List<Session>,
    tree: TreeGrowth,
    onToday: () -> Unit,
    onTree: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TodayBentoCard(todaySessions, onToday, Modifier.weight(1f))
        TreeBentoCard(tree, onTree, Modifier.weight(1f))
    }
}

@Composable
private fun TodayBentoCard(
    todaySessions: List<Session>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tokens = LocalFokusTokens.current
    val totalMillis = remember(todaySessions) { todaySessions.sumOf { it.durationMillis } }

    FokusSurface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 120.dp),
        shape = panelShape,
        color = tokens.bentoSurface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TODAY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
            }

            Text(
                durationLabel(totalMillis),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                maxLines = 1
            )

            if (todaySessions.isEmpty()) {
                Text(
                    "No blocks yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val dots = todaySessions.size.coerceAtMost(4)
                    repeat(dots) {
                        Box(
                            Modifier.size(7.dp).background(colors.primary, CircleShape)
                        )
                    }
                    Text(
                        sessionCountLabel(todaySessions.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TreeBentoCard(
    tree: TreeGrowth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tokens = LocalFokusTokens.current
    val stageProgress = if (tree.next == null) 1f else (tree.growth - tree.stage.ordinal).coerceIn(0f, 1f)
    val animatedStageProgress by animateFloatAsState(stageProgress, tween(450), label = "Mini tree stage progress")
    val breezeTransition = rememberInfiniteTransition(label = "Mini tree breeze")
    val breezeCycle by breezeTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart),
        label = "Mini breeze cycle"
    )
    val swayAngle = (1.2f + 2.6f * sin(breezeCycle * 2.0 * PI)).toFloat()

    FokusSurface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 120.dp),
        shape = panelShape,
        color = tokens.bentoSurface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TREE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (tree.streak > 0) {
                    Row(
                        Modifier.clip(CircleShape)
                            .background(tokens.streakContainer)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(12.dp), tokens.streakTint)
                        Text(
                            "${tree.streak}d",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = tokens.streakTint
                        )
                    }
                } else {
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                val miniTrunkPath = remember { Path() }
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 2.4.dp.toPx()
                        drawCircle(colors.secondaryContainer, style = Stroke(stroke))
                        if (animatedStageProgress > 0f) {
                            drawArc(
                                color = colors.secondary,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedStageProgress,
                                useCenter = false,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }

                        // Tiny drifting air streak across the bonsai badge
                        val airAlpha = sin(breezeCycle * PI).toFloat().coerceIn(0f, 1f) * 0.45f
                        val airX = size.width * (0.20f + breezeCycle * 0.44f)
                        val airY = size.height * (0.31f + sin(breezeCycle * 4.0 * PI).toFloat() * 0.04f)
                        drawLine(
                            color = colors.secondary.copy(alpha = airAlpha),
                            start = Offset(airX, airY),
                            end = Offset(airX + size.width * 0.20f, airY - 0.8.dp.toPx()),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Mini ceramic pot ("tob") at the bottom of the inner ring
                        val potColor = colors.tertiary
                        val potWidth = size.width * 0.38f
                        val potHeight = size.height * 0.12f
                        val potLeft = (size.width - potWidth) / 2f
                        val potTop = size.height * 0.67f
                        drawRoundRect(
                            color = potColor,
                            topLeft = Offset(potLeft, potTop),
                            size = Size(potWidth, potHeight),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                        drawLine(
                            color = potColor,
                            start = Offset(potLeft - 1.dp.toPx(), potTop),
                            end = Offset(potLeft + potWidth + 1.dp.toPx(), potTop),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Mini swaying S-curve bonsai trunk + dual foliage cloud pads
                        val base = Offset(size.width * 0.50f, potTop)
                        rotate(degrees = swayAngle, pivot = base) {
                            miniTrunkPath.reset()
                            miniTrunkPath.moveTo(base.x, base.y)
                            miniTrunkPath.cubicTo(
                                base.x - size.width * 0.06f, base.y - size.height * 0.10f,
                                base.x + size.width * 0.08f, base.y - size.height * 0.20f,
                                base.x + size.width * 0.01f, base.y - size.height * 0.30f
                            )
                            drawPath(
                                miniTrunkPath,
                                color = colors.onSurfaceVariant,
                                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawOval(
                                color = colors.secondary,
                                topLeft = Offset(base.x - size.width * 0.22f, base.y - size.height * 0.28f),
                                size = Size(size.width * 0.20f, size.height * 0.11f)
                            )
                            drawOval(
                                color = colors.primary,
                                topLeft = Offset(base.x - size.width * 0.06f, base.y - size.height * 0.38f),
                                size = Size(size.width * 0.26f, size.height * 0.14f)
                            )
                        }
                    }
                }
                Text(
                    tree.stage.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val nextText = tree.next?.let { "Next: ${it.label}" } ?: "${tree.focusedDays} focused days"
            Text(
                nextText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun InAppAodDisplay(
    state: FokusState,
    controller: FokusController,
    dimmed: Boolean,
    onToggleDim: () -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val time by liveTime(controller, state.run?.deadline != null)
    val duration = state.run?.durationMillis ?: state.preferences.minutes(state.phase) * 60_000L
    val remaining = state.run?.remaining(time) ?: duration
    val running = state.run?.deadline != null
    val paused = state.run != null && !running
    val isBreak = state.phase == Phase.BREAK
    val breakStartingAfterVibe = state.run != null && isBreak && state.run.startedAt > time.wall
    val accent = if (isBreak) aodBreak else aodFocus
    val activity = state.run?.activity ?: state.selectedActivity
    val endWallMillis = state.run?.takeIf { it.deadline != null }?.completedAt(time) ?: (time.wall + remaining)

    val minuteBucket = time.wall / 60_000L
    val clock = remember(minuteBucket, context) { DateFormat.getTimeFormat(context).format(Date(time.wall)) }
    val shiftX = (((minuteBucket % 5).toInt() - 2) * 2).dp
    val shiftY = ((((minuteBucket / 5) % 5).toInt() - 2) * 2).dp

    val statusLine = when {
        breakStartingAfterVibe -> "FOCUS COMPLETE · STARTING BREAK"
        running && isBreak -> "BREAK · TAKE A BREATHER"
        running -> "FOCUSING · ${activity.name.uppercase()}"
        paused -> "PAUSED · ${activity.name.uppercase()}"
        state.ready -> if (isBreak) "FOCUS COMPLETE · READY FOR BREAK" else "BREAK COMPLETE · READY TO FOCUS"
        else -> if (isBreak) "BREAK READY" else "READY · ${activity.name.uppercase()}"
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            Modifier.fillMaxSize()
                .offset(x = shiftX, y = shiftY)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(clock, style = tabularLabelStyle, color = aodMuted)
                    if (state.preferences.quietMode && !isBreak) {
                        Icon(Icons.Outlined.DoNotDisturbOn, "Focus mode DND", Modifier.size(13.dp), aodMuted)
                    }
                }
                Text(
                    "IN-APP AOD",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = aodMuted.copy(alpha = 0.72f)
                )
            }

            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Row(
                    Modifier.clip(CircleShape)
                        .background(aodControlSurface)
                        .border(1.dp, aodControlBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(Modifier.size(6.dp).background(accent, CircleShape))
                    Text(
                        statusLine,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                ChronographDial(
                    remaining = remaining,
                    duration = duration,
                    endWallMillis = endWallMillis,
                    running = running,
                    paused = paused,
                    accent = accent,
                    onAccent = Color.Black,
                    glow = Color.Transparent,
                    dialTrack = aodTrack,
                    tickMuted = aodTickMuted,
                    textColor = aodText,
                    mutedTextColor = aodMuted,
                    modifier = Modifier.widthIn(max = 264.dp).fillMaxWidth().aspectRatio(1f)
                )
            }

            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroActionControls(
                    running = running,
                    activeRun = state.run != null,
                    isBreak = isBreak,
                    accent = accent,
                    accentSoft = aodControlSurface,
                    onAccent = Color(0xFF06130C),
                    endContainerColor = aodControlSurface,
                    endContentColor = aodText,
                    endBorderColor = aodControlBorder,
                    onToggle = { controller.toggleTimer(state.phase, state.run) },
                    onFinish = onFinish
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AodUtilityButton(
                        icon = if (dimmed) Icons.Outlined.BrightnessLow else Icons.Outlined.BrightnessMedium,
                        label = if (dimmed) "Dimmed" else "Dim",
                        active = dimmed,
                        onClick = onToggleDim
                    )
                    Spacer(Modifier.width(10.dp))
                    AodUtilityButton(
                        icon = Icons.Outlined.FullscreenExit,
                        label = "Exit AOD",
                        active = false,
                        onClick = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun AodUtilityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier.heightIn(min = 48.dp)
            .clickable(source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.pressScale(source, 0.94f)
                .clip(CircleShape)
                .background(if (active) Color(0xFF17211B) else aodControlSurface)
                .border(1.dp, aodControlBorder, CircleShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = if (active) aodFocus else aodMuted)
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) aodText else aodMuted)
        }
    }
}

@Composable
internal fun liveTime(controller: FokusController, running: Boolean = false): State<TimeMark> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(controller.now(), controller, running, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = controller.now()
                delay(if (running) 250L else 60_000L - value.wall % 60_000L)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 820)
@Composable
private fun HomeLightIdlePreview() {
    val now = 1_795_680_000_000L
    val book = Activity("b1", "Designing Data-Intensive Apps", Category.BOOK)
    val state = FokusState(
        preferences = Preferences(focusMinutes = 25, breakMinutes = 5, quietMode = true, selectedActivityId = "b1"),
        activities = listOf(book),
        sessions = listOf(
            Session("s1", book, now - 3_600_000L, now - 2_100_000L, 25 * 60_000L),
            Session("s2", book, now - 7_200_000L, now - 5_700_000L, 25 * 60_000L)
        )
    )
    FokusTheme(dark = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimerHomeContent(state, TimeMark(now, 1000L, 1), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 820)
@Composable
private fun HomeDarkRunningPreview() {
    val now = 1_795_680_000_000L
    val study = Activity("s1", "Organic Chemistry Ch. 7", Category.STUDY)
    val run = TimerRun("r1", Phase.FOCUS, study, now - 600_000L, 25 * 60_000L, 15 * 60_000L, TimeMark(now + 900_000L, 901_000L, 1))
    val state = FokusState(
        preferences = Preferences(focusMinutes = 25, breakMinutes = 5, aodMode = true, quietMode = true, selectedActivityId = "s1"),
        activities = listOf(study),
        sessions = listOf(Session("s1", study, now - 86_400_000L, now - 84_000_000L, 40 * 60_000L)),
        run = run
    )
    FokusTheme(dark = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimerHomeContent(state, TimeMark(now, 1000L, 1), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
    }
}
