package com.example.smartread.fokus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smartread.fokus.*
import kotlinx.coroutines.launch

private enum class Page(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    TIMER("Timer", Icons.Outlined.Timer, Icons.Filled.Timer),
    ACTIVITIES("Activities", Icons.Outlined.Layers, Icons.Filled.Layers),
    TREE("Tree", Icons.Outlined.Park, Icons.Filled.Park),
    INSIGHTS("Insights", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    SETTINGS("Settings", Icons.Outlined.Tune, Icons.Filled.Tune)
}

private enum class Overlay { PICK_ACTIVITY, EDIT_ACTIVITY, DURATIONS, LOG, SESSION, TIMER }

@Composable
fun FokusApp(
    state: FokusState,
    controller: FokusController,
    access: DeviceAccess,
    settingsRequest: Int,
    keepScreenOn: (Boolean) -> Unit,
    onAodState: (active: Boolean, dim: Boolean) -> Unit,
    openClock: () -> Unit,
    openNotifications: () -> Unit,
    openQuietAccess: () -> Unit,
    openExactAlarms: () -> Unit
) {
    var page by rememberSaveable { mutableStateOf(Page.TIMER) }
    var overlay by rememberSaveable { mutableStateOf<Overlay?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var sessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var stopRunId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectNew by rememberSaveable { mutableStateOf(false) }
    var help by rememberSaveable { mutableStateOf<Help?>(null) }
    var range by rememberSaveable { mutableStateOf(Range.WEEK) }
    var seenWatering by rememberSaveable { mutableStateOf<String?>(null) }
    var aodDimmed by rememberSaveable { mutableStateOf(false) }
    var autoAodRunId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val coroutine = rememberCoroutineScope()
    val savedPages = rememberSaveableStateHolder()
    val display = overlay == Overlay.TIMER
    val close = { overlay = null }

    val activeRunId = state.run?.takeIf { it.deadline != null }?.id
    LaunchedEffect(state.preferences.aodMode, activeRunId) {
        if (state.preferences.aodMode && activeRunId != null && autoAodRunId != activeRunId) {
            autoAodRunId = activeRunId
            overlay = Overlay.TIMER
        }
    }

    val keepOn = (display && state.run != null) ||
        (state.preferences.keepScreenOn && page == Page.TIMER && state.run?.deadline != null)
    DisposableEffect(keepOn) {
        keepScreenOn(keepOn)
        onDispose { keepScreenOn(false) }
    }
    DisposableEffect(display, aodDimmed) {
        onAodState(display, aodDimmed)
        onDispose { onAodState(false, false) }
    }
    LaunchedEffect(settingsRequest) {
        if (settingsRequest > 0) {
            page = Page.SETTINGS
            overlay = null
        }
    }
    LaunchedEffect(controller) {
        controller.messages.collect { snackbar.showSnackbar(it) }
    }
    BackHandler(display || page != Page.TIMER) {
        if (display) close() else page = Page.TIMER
    }
    Surface(Modifier.fillMaxSize(), color = if (display) Color.Black else MaterialTheme.colorScheme.background) {
        if (display) {
            InAppAodDisplay(
                state = state,
                controller = controller,
                dimmed = aodDimmed,
                onToggleDim = { aodDimmed = !aodDimmed },
                onFinish = { stopRunId = state.run?.id },
                onClose = close
            )
        } else {
            CompositionLocalProvider(LocalOpenClock provides openClock) {
                Scaffold(
                    modifier = Modifier.safeDrawingPadding(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = { BottomTabs(page) { page = it } }
                ) { padding ->
                    AnimatedContent(
                        targetState = page,
                        modifier = Modifier.fillMaxSize().padding(padding),
                        transitionSpec = {
                            (fadeIn(tween(220, 40)) + slideInVertically(tween(260)) { it / 45 }) togetherWith fadeOut(tween(110))
                        },
                        label = "Fokus screen"
                    ) { target ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
                                savedPages.SaveableStateProvider(target.name) {
                                    when (target) {
                                        Page.TIMER -> TimerPage(
                                            state = state,
                                            controller = controller,
                                            onHelp = { help = Help.TIMER },
                                            onPick = { overlay = Overlay.PICK_ACTIVITY },
                                            onDurations = { overlay = Overlay.DURATIONS },
                                            onToggleAod = {
                                                val enable = !state.preferences.aodMode
                                                controller.updatePreferences { it.copy(aodMode = enable) }
                                                if (enable && state.run != null) {
                                                    autoAodRunId = state.run.id
                                                    overlay = Overlay.TIMER
                                                }
                                            },
                                            onFinish = { stopRunId = state.run?.id },
                                            onToday = {
                                                range = Range.TODAY
                                                page = Page.INSIGHTS
                                            },
                                            onDisplay = { overlay = Overlay.TIMER },
                                            onClock = openClock,
                                            onTree = { page = Page.TREE }
                                        )
                                        Page.ACTIVITIES -> ActivitiesPage(
                                            state,
                                            onAdd = {
                                                editingId = null
                                                selectNew = false
                                                overlay = Overlay.EDIT_ACTIVITY
                                            },
                                            onEdit = {
                                                editingId = it.id
                                                selectNew = false
                                                overlay = Overlay.EDIT_ACTIVITY
                                            },
                                            onFocus = {
                                                controller.selectActivity(it)
                                                controller.changePhase(Phase.FOCUS)
                                                page = Page.TIMER
                                                if (state.run != null) {
                                                    coroutine.launch { snackbar.showSnackbar("Selected for your next session") }
                                                }
                                            },
                                            onArchive = { controller.archiveActivity(it) },
                                            onHelp = { help = Help.ACTIVITIES }
                                        )
                                        Page.TREE -> TreePage(
                                            state, controller, seenWatering, { seenWatering = it },
                                            onFocus = { page = Page.TIMER }, onHelp = { help = Help.TREE }
                                        )
                                        Page.INSIGHTS -> InsightsPage(
                                            state, range, { range = it }, { help = Help.INSIGHTS },
                                            onLog = { overlay = Overlay.LOG },
                                            onSession = {
                                                sessionId = it.id
                                                overlay = Overlay.SESSION
                                            }, controller = controller
                                        )
                                        Page.SETTINGS -> SettingsPage(
                                            state.preferences, access, { controller.updatePreferences(it) },
                                            { overlay = Overlay.DURATIONS }, { help = it },
                                            openNotifications, openQuietAccess, openExactAlarms
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    when (overlay) {
        Overlay.PICK_ACTIVITY -> ActivityPicker(state.activities, state.selectedActivity, close,
            onSelect = {
                controller.selectActivity(it)
                close()
            }, onCreate = {
                editingId = null
                selectNew = true
                overlay = Overlay.EDIT_ACTIVITY
            })
        Overlay.EDIT_ACTIVITY -> ActivityEditor(state.activities.firstOrNull { it.id == editingId }, close) { name, category ->
            controller.saveActivity(name, category, state.activities.firstOrNull { it.id == editingId }, selectNew)
            close()
        }
        Overlay.DURATIONS -> DurationEditor(state.preferences, close) { focus, rest ->
            controller.updatePreferences { it.copy(focusMinutes = focus, breakMinutes = rest) }
            close()
        }
        Overlay.LOG -> SessionEditor(state.activities, state.selectedActivity, close) { activity, start, duration, note ->
            controller.logSession(activity, start, duration, note)
            close()
        }
        Overlay.SESSION -> state.sessions.firstOrNull { it.id == sessionId }?.let { selected ->
            SessionDetail(selected, close,
                onDelete = {
                    controller.deleteSession(selected)
                    close()
                },
                onSaveNote = { controller.updateSessionNote(selected, it) })
        }
        else -> Unit
    }
    stopRunId?.let { targetRunId ->
        StopDialog(
            run = state.run?.takeIf { it.id == targetRunId },
            controller = controller,
            onConfirmed = {
                stopRunId = null
                if (overlay == Overlay.TIMER) overlay = null
            },
            onDismiss = { stopRunId = null }
        )
    }
    help?.let { topic ->
        AlertDialog(
            onDismissRequest = { help = null }, title = { Text(topic.title) }, text = { Text(topic.text) },
            confirmButton = { FokusTextButton(onClick = { help = null }) { Text("Got it") } }
        )
    }
}

@Composable
private fun StopDialog(
    run: TimerRun?,
    controller: FokusController,
    onConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    if (run == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val time by liveTime(controller, run.deadline != null)
    val save = run.phase == Phase.FOCUS && run.durationMillis - run.remaining(time) >= 1000
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (run.phase == Phase.FOCUS) "End this focus session?" else "End this break?") },
        text = { Text(if (save) "Save the time you’ve already focused, or discard this block." else "Return to the focus timer.") },
        confirmButton = {
            FokusButton(onClick = {
                controller.finish(save, run.id)
                onConfirmed()
            }) { Text(if (save) "Save & end" else "End timer") }
        },
        dismissButton = {
            Row {
                FokusTextButton(onClick = onDismiss) { Text("Cancel") }
                if (save) FokusTextButton(onClick = {
                    controller.finish(false, run.id)
                    onConfirmed()
                }) { Text("Discard") }
            }
        }
    )
}

@Composable
private fun BottomTabs(page: Page, onSelect: (Page) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tokens = LocalFokusTokens.current
    Column(Modifier.fillMaxWidth().background(tokens.navSurface)) {
        HorizontalDivider(thickness = 0.8.dp, color = colors.outlineVariant.copy(alpha = 0.75f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Page.entries.forEach { target ->
                    val selected = target == page
                    val source = remember { MutableInteractionSource() }
                    val pillColor by animateColorAsState(
                        if (selected) colors.primaryContainer else Color.Transparent,
                        tween(200),
                        label = "Nav pill"
                    )
                    val iconTint by animateColorAsState(
                        if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        tween(180),
                        label = "Nav icon"
                    )
                    val textTint by animateColorAsState(
                        if (selected) colors.onSurface else colors.onSurfaceVariant,
                        tween(180),
                        label = "Nav label"
                    )
                    Box(
                        Modifier.weight(1f)
                            .heightIn(min = 56.dp)
                            .selectable(selected, source, indication = null, role = Role.Tab) {
                                if (!selected) onSelect(target)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            Modifier.pressScale(source, 0.93f).padding(vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                Modifier.width(52.dp).height(29.dp).clip(CircleShape).background(pillColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (selected) target.selectedIcon else target.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = iconTint
                                )
                            }
                            Text(
                                target.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = textTint,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
