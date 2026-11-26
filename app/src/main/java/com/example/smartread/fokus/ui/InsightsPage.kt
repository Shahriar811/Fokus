package com.example.smartread.fokus.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartread.fokus.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToLong

internal enum class Range(val label: String, val days: Int) {
    TODAY("Today", 1), WEEK("7 days", 7), MONTH("30 days", 30), ALL("All time", 0)
}

private data class Summary(
    val total: Long, val previous: Long?, val sessions: Int, val days: Int, val range: Range
)

@Composable
internal fun InsightsPage(
    state: FokusState,
    range: Range,
    onRange: (Range) -> Unit,
    onHelp: () -> Unit,
    onLog: () -> Unit,
    onSession: (Session) -> Unit,
    controller: FokusController
) {
    val time by liveTime(controller)
    val zone = ZoneId.systemDefault()
    val today = time.wall.localDate(zone)
    var filterId by rememberSaveable { mutableStateOf<String?>(null) }
    val names = remember(state.activities) { (state.activities + generalActivity).associateBy { it.id } }
    val options = remember(state.sessions) { listOf(null) + state.sessions.map { it.activity.id }.distinct() }
    val chosen = filterId.takeIf { it in options }
    val filtered = remember(state.sessions, chosen) {
        if (chosen == null) state.sessions else state.sessions.filter { it.activity.id == chosen }
    }
    val period = remember(filtered, range, today, zone) {
        if (range.days > 0) summarize(filtered, today, range.days, zone) else null
    }
    val records = period?.current ?: filtered
    val total = remember(records) { records.sumOf { it.durationMillis } }
    val activeDays = remember(records, period, zone) {
        period?.activeDays ?: records.distinctBy { it.endMillis.localDate(zone) }.size
    }
    val breakdown = remember(records) {
        records.groupBy { it.activity.id }.values
            .map { group -> group.first().activity to group.sumOf { it.durationMillis } }
            .sortedByDescending { it.second }
    }
    val summary = Summary(total, period?.previousDuration, records.size, activeDays, range)
    val trend = period?.daily?.takeIf { range != Range.TODAY }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "insights-title") { PageTitle("Insights", onHelp) }
        item(key = "insights-range") { Selector(Range.entries, range, { it.label }, onRange) }
        if (options.size > 1) {
            item(key = "insights-filter") {
                ChoiceMenu("Activity", options, chosen, { id ->
                    if (id == null) "All activities"
                    else names[id]?.name ?: state.sessions.first { it.activity.id == id }.activity.name
                }, { filterId = it })
            }
        }
        item(key = "insights-overview") {
            Column {
                AnimatedContent(
                    summary, contentKey = { it.range },
                    transitionSpec = {
                        (fadeIn(tween(205, 50)) togetherWith fadeOut(tween(90)))
                            .using(SizeTransform { _, _ -> tween(265) })
                    },
                    label = "Period summary"
                ) { snapshot ->
                    SummaryCard(snapshot.total, snapshot.previous, snapshot.sessions, snapshot.days, snapshot.range)
                }
                AnimatedContent(
                    trend, contentKey = { it?.size },
                    transitionSpec = {
                        (fadeIn(tween(205, 65)) togetherWith fadeOut(tween(100)))
                            .using(SizeTransform { _, _ -> tween(285) })
                    },
                    label = "Period chart"
                ) { days ->
                    if (days != null) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            FocusChart(days, days.last().date)
                        }
                    }
                }
            }
        }
        if (breakdown.size > 1) {
            item(key = "insights-activity") { Text("Time by activity", style = MaterialTheme.typography.titleLarge) }
            items(breakdown, key = { "total-${it.first.id}" }) { (activity, millis) ->
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CategoryBadge(activity.category)
                        Text(names[activity.id]?.name ?: activity.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(durationLabel(millis), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item(key = "insights-sessions") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Sessions", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                FokusTextButton(onClick = onLog) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log session")
                }
            }
        }
        if (records.isEmpty()) {
            item(key = "insights-empty") {
                EmptyPanel(Icons.Outlined.History, "No sessions here yet", "Finish a focus block or log one you’ve already done.")
            }
        }
        items(records, key = { it.id }) { record -> SessionRow(record, onClick = { onSession(record) }) }
    }
}

@Composable
private fun SummaryCard(total: Long, previous: Long?, sessions: Int, days: Int, range: Range) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.primaryContainer, contentColor = colors.onPrimaryContainer, shape = panelShape) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Focus time", style = MaterialTheme.typography.labelLarge)
            Text(durationLabel(total), style = MaterialTheme.typography.headlineLarge)
            if (previous != null) {
                val difference = total - previous
                val baseline = if (range == Range.TODAY) "yesterday" else "the previous ${range.days} days"
                val comparison = when {
                    previous == 0L -> if (total == 0L) "0m recorded $baseline" else "Up from 0m $baseline"
                    difference == 0L -> "Same focus time as $baseline"
                    else -> {
                        val percent = (abs(difference).toDouble() / previous * 100).roundToLong()
                        val change = if (percent == 0L) "<1%" else "$percent%"
                        "$change ${if (difference > 0) "more" else "less"} than $baseline (${durationLabel(previous)})"
                    }
                }
                Text(comparison, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(Modifier.padding(vertical = 3.dp), color = colors.onPrimaryContainer.copy(alpha = 0.15f))
            Text("${sessionCountLabel(sessions)}  ·  $days active ${if (days == 1) "day" else "days"}",
                style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FocusChart(days: List<DailyTotal>, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val groups = remember(days) { days.chunked(if (days.size > 7) 5 else 1) }
    val amounts = remember(groups) { groups.map { group -> group.sumOf { it.duration } } }
    val peak = amounts.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    var selection by rememberSaveable(days.size, today.toEpochDay()) { mutableIntStateOf(groups.lastIndex) }
    val chosen = groups[selection]
    val dayFormat = DateTimeFormatter.ofPattern("MMM d")
    val dateLabel = if (chosen.size == 1) chosen.first().date.format(dayFormat) else
        "${chosen.first().date.format(dayFormat)} – ${chosen.last().date.format(dayFormat)}"
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Focus trend", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(durationLabel(amounts[selection]), color = colors.primary, style = MaterialTheme.typography.labelLarge)
        }
        Muted(dateLabel)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            groups.forEachIndexed { index, group ->
                val part by animateFloatAsState((amounts[index].toFloat() / peak).coerceIn(0f, 1f),
                    tween(310), label = "Focus bar")
                val tint by animateColorAsState(
                    if (index == selection) colors.primary else colors.primary.copy(alpha = 0.46f),
                    tween(185), label = "Chart selection"
                )
                val day = group.last().date
                val axis = if (group.size == 1) day.format(DateTimeFormatter.ofPattern("EEEEE")) else day.dayOfMonth.toString()
                val description = if (group.size == 1) day.format(dayFormat) else
                    "${group.first().date.format(dayFormat)} to ${day.format(dayFormat)}"
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { selection = index }
                        .semantics { contentDescription = "$description, ${durationLabel(amounts[index])}" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.widthIn(max = 28.dp).fillMaxWidth(0.63f).height(72.dp)
                            .background(colors.surfaceVariant, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(part).background(tint, RoundedCornerShape(6.dp)))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(axis, style = MaterialTheme.typography.labelMedium,
                        color = if (selection == index) colors.primary else colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: Session, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val date = session.endMillis.localDate().format(DateTimeFormatter.ofPattern("MMM d"))
    val end = DateFormat.getTimeFormat(context).format(Date(session.endMillis))
    FokusSurface(onClick = onClick, color = colors.surface, shape = panelShape, modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)) {
        Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(session.activity.category)
            RowLabel(session.activity.name, "$date · $end", Modifier.weight(1f))
            Text(durationLabel(session.durationMillis), style = MaterialTheme.typography.labelLarge, color = colors.primary)
        }
    }
}

@Composable
internal fun SessionDetail(session: Session, onDismiss: () -> Unit, onDelete: () -> Unit, onSaveNote: (String) -> Unit) {
    val context = LocalContext.current
    var confirm by rememberSaveable { mutableStateOf(false) }
    var editingNote by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable(session.id) { mutableStateOf(session.note) }
    fun timestamp(millis: Long): String {
        val date = Date(millis)
        return "${DateFormat.getMediumDateFormat(context).format(date)} · ${DateFormat.getTimeFormat(context).format(date)}"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (confirm) "Delete this session?" else session.activity.name) },
        text = {
            if (confirm) {
                Text("Its time will be removed from your history, totals, and tree growth.")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RowLabel(durationLabel(session.durationMillis, includeSeconds = true), if (session.manual) "Manually logged" else "Timer session")
                    RowLabel("Started", timestamp(session.startMillis))
                    RowLabel("Ended", timestamp(session.endMillis))
                    if (editingNote) {
                        OutlinedTextField(
                            note, { if (it.length <= 500) note = it }, label = { Text("Notes") },
                            placeholder = { Text("e.g. Pages 20–38") }, modifier = Modifier.fillMaxWidth(), maxLines = 4
                        )
                    } else {
                        if (session.note.isNotBlank()) Text(session.note)
                        FokusTextButton(onClick = {
                            note = session.note
                            editingNote = true
                        }) { Text(if (session.note.isBlank()) "Add notes" else "Edit notes") }
                    }
                }
            }
        },
        confirmButton = {
            when {
                confirm -> FokusButton(onClick = onDelete) { Text("Delete session") }
                editingNote -> FokusButton(onClick = {
                    onSaveNote(note)
                    editingNote = false
                }) { Text("Save notes") }
                else -> FokusButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            FokusTextButton(onClick = {
                when {
                    editingNote -> editingNote = false
                    else -> confirm = !confirm
                }
            }) { Text(if (confirm || editingNote) "Cancel" else "Delete") }
        }
    )
}

@Composable
internal fun SessionEditor(
    activities: List<Activity>,
    selected: Activity,
    onDismiss: () -> Unit,
    onSave: (Activity, Long, Long, String) -> Unit
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val options = listOf(generalActivity) + activities
    var activityId by rememberSaveable { mutableStateOf(selected.id) }
    val activity = options.firstOrNull { it.id == activityId } ?: generalActivity
    var minutes by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var endMinute by rememberSaveable { mutableLongStateOf(System.currentTimeMillis() / 60_000) }
    val dateTime = Instant.ofEpochMilli(endMinute * 60_000).atZone(zone)
    val end = endMinute * 60_000
    val duration = minutes.toIntOrNull()?.takeIf { it in 1..1440 }?.times(60_000L)
    val future = end > System.currentTimeMillis()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log a session") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ChoiceMenu("Activity", options, activity, { it.name }, { activityId = it.id }, { it.category.icon })
                OutlinedTextField(
                    value = minutes, onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) minutes = it },
                    label = { Text("Duration in minutes") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = minutes.isNotEmpty() && duration == null,
                    supportingText = if (minutes.isNotEmpty() && duration == null) { { Text("Enter 1–1440 minutes.") } } else null
                )
                FokusOutlinedButton(
                    onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            endMinute = LocalDate.of(year, month + 1, day).atTime(dateTime.toLocalTime())
                                .atZone(zone).toEpochSecond() / 60
                        }, dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth).apply {
                            datePicker.maxDate = System.currentTimeMillis()
                        }.show()
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Ended · ${DateFormat.getMediumDateFormat(context).format(Date(end))}") }
                FokusOutlinedButton(
                    onClick = {
                        TimePickerDialog(context, { _, hour, minute ->
                            endMinute = dateTime.toLocalDate().atTime(LocalTime.of(hour, minute))
                                .atZone(zone).toEpochSecond() / 60
                        }, dateTime.hour, dateTime.minute, DateFormat.is24HourFormat(context)).show()
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("End time · ${DateFormat.getTimeFormat(context).format(Date(end))}") }
                if (future) Text("Choose an end time in the past.", color = MaterialTheme.colorScheme.error)
                OutlinedTextField(
                    value = note, onValueChange = { if (it.length <= 500) note = it },
                    label = { Text("Notes (optional)") }, placeholder = { Text("e.g. Pages 20–38") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3
                )
            }
        },
        confirmButton = {
            FokusButton(enabled = !future && duration != null, onClick = {
                if (duration != null) onSave(activity, end - duration, duration, note)
            }) { Text("Save session") }
        },
        dismissButton = { FokusTextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
