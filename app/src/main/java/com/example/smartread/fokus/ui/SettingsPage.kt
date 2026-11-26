package com.example.smartread.fokus.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartread.fokus.Appearance
import com.example.smartread.fokus.Preferences
import kotlin.math.roundToInt

data class DeviceAccess(val notifications: Boolean, val doNotDisturb: Boolean, val exactAlarms: Boolean)

internal enum class Help(val title: String, val text: String) {
    TIMER("Pomodoro timer", "Choose an activity and start a focus block. The defaults are 25 minutes of focus and a 5-minute break. Fokus saves completed focus blocks automatically. Use the stop button to save a shorter block or discard it."),
    ACTIVITIES("Activities", "Make an activity for each book, course, task, or workout you want to track. Tap its play button to bring it to the timer. Archived activities stay in your session history."),
    TREE("Focus tree", "Every saved focus session waters your tree, including manually logged sessions. Longer sessions add more water. Each growth stage needs both focus time and days with focus. Your streak counts consecutive days. Missing a day leaves your tree’s growth intact."),
    INSIGHTS("Productivity insights", "See focus time for the selected period and compare it with the preceding period of the same length. Sessions count on the date they ended. Open any session for its start time, end time, and duration. Use Log session to add time you tracked outside Fokus."),
    NOTIFICATIONS("Timer alerts", "Allow notifications to get an alert when a focus block or break ends. You can change alert sound and visibility in Android settings."),
    QUIET("Focus mode", "Fokus uses its own Android Do Not Disturb rule to silence interruptions during a running focus block. The rule turns off when you pause, stop, or start a break. Android controls exceptions such as alarms. Precise background alerts help the rule turn off on time when the phone is idle."),
    AOD("In-app AOD", "Keeps your focus and break blocks inside a pitch-black Always-On Display in Fokus without letting the phone lock. Designed for both AMOLED and IPS/non-AMOLED displays."),
    DISPLAY("Lock-screen progress", "Shows a countdown in your notifications. With lock-screen notifications enabled, supported phones can also show it on the lock screen or always-on display. Some phones show only an icon on the always-on display."),
    EXACT("Precise background alerts", "Allow alarms and reminders so Android can finish a countdown on time while the phone is asleep. Without this access, Android may deliver your alert and end Focus mode later.")
}

@Composable
internal fun SettingsPage(
    preferences: Preferences,
    access: DeviceAccess,
    onUpdate: ((Preferences) -> Preferences) -> Unit,
    onDurations: () -> Unit,
    onHelp: (Help) -> Unit,
    onNotifications: () -> Unit,
    onQuietAccess: () -> Unit,
    onExactAlarms: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageTitle("Settings") }
        item {
            Text("Timer", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsIcon(Icons.Outlined.Timer)
                    RowLabel("Durations", "${preferences.focusMinutes} min focus · ${preferences.breakMinutes} min break", Modifier.weight(1f))
                    FokusTextButton(onClick = onDurations) { Text("Edit") }
                }
                HorizontalDivider(Modifier.padding(vertical = 9.dp))
                SettingSwitch(Icons.Outlined.Coffee, "Auto-start breaks", "Start a break when focus ends.", preferences.autoBreak) { value ->
                    onUpdate { it.copy(autoBreak = value) }
                }
            }
        }
        item {
            Text("Focus & display", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Panel {
                SettingSwitch(
                    Icons.Outlined.DoNotDisturbOn, "Focus mode", "Quiet notifications during focus blocks.",
                    preferences.quietMode, help = { onHelp(Help.QUIET) }
                ) { value ->
                    onUpdate { it.copy(quietMode = value) }
                    if (value && !access.doNotDisturb) onQuietAccess()
                }
                if (preferences.quietMode && !access.doNotDisturb) {
                    FokusTextButton(onClick = onQuietAccess) { Text("Allow Do Not Disturb access") }
                }
                HorizontalDivider(Modifier.padding(vertical = 9.dp))
                SettingSwitch(
                    Icons.Outlined.LockClock, "Lock-screen progress", "Show the timer on supported displays.",
                    preferences.lockScreen, help = { onHelp(Help.DISPLAY) }
                ) { value ->
                    onUpdate { it.copy(lockScreen = value) }
                    if (value && !access.notifications) onNotifications()
                }
                if (preferences.lockScreen && !access.notifications) {
                    FokusTextButton(onClick = onNotifications) { Text("Allow notifications") }
                }
                HorizontalDivider(Modifier.padding(vertical = 9.dp))
                SettingSwitch(
                    Icons.Outlined.Brightness4, "In-app AOD",
                    "Run focus and break in pitch-black AOD without locking.",
                    preferences.aodMode, help = { onHelp(Help.AOD) }
                ) { value -> onUpdate { it.copy(aodMode = value) } }
                HorizontalDivider(Modifier.padding(vertical = 9.dp))
                SettingSwitch(
                    Icons.Outlined.StayCurrentPortrait, "Keep screen on",
                    "While the timer runs on the home screen.", preferences.keepScreenOn
                ) { value -> onUpdate { it.copy(keepScreenOn = value) } }
            }
        }
        item {
            Text("Alerts", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Panel {
                PermissionRow(Icons.Outlined.NotificationsNone, "Timer alerts", access.notifications,
                    { onHelp(Help.NOTIFICATIONS) }, onNotifications)
                HorizontalDivider(Modifier.padding(vertical = 9.dp))
                PermissionRow(Icons.Outlined.Alarm, "Precise background alerts", access.exactAlarms,
                    { onHelp(Help.EXACT) }, onExactAlarms, canManage = android.os.Build.VERSION.SDK_INT >= 31)
            }
        }
        item {
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Selector(Appearance.entries, preferences.appearance, { it.label }, { value ->
                onUpdate { it.copy(appearance = value) }
            })
        }
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SettingSwitch(
    icon: ImageVector, title: String, description: String, checked: Boolean, help: (() -> Unit)? = null,
    onChange: (Boolean) -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        SettingsIcon(icon)
        RowLabel(title, description, Modifier.weight(1f))
        if (help != null) InfoButton(title, help)
        Switch(checked = checked, onCheckedChange = onChange, interactionSource = source,
            modifier = Modifier.pressScale(source, 0.94f).heightIn(min = 48.dp).semantics { contentDescription = title },
            thumbContent = if (checked) { { Icon(Icons.Outlined.Check, null, Modifier.size(15.dp)) } } else null
        )
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector, title: String, granted: Boolean, onHelp: () -> Unit, onOpen: () -> Unit,
    canManage: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsIcon(icon)
        RowLabel(title, if (granted) "Allowed" else "Access needed", Modifier.weight(1f))
        InfoButton(title, onHelp)
        if (canManage) FokusTextButton(onClick = onOpen) { Text(if (granted) "Manage" else "Allow") }
    }
}

@Composable
internal fun DurationEditor(preferences: Preferences, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var focus by rememberSaveable { mutableStateOf(preferences.focusMinutes.toString()) }
    var rest by rememberSaveable { mutableStateOf(preferences.breakMinutes.toString()) }
    val focusValue = focus.toIntOrNull()?.takeIf { it in 1..240 }
    val restValue = rest.toIntOrNull()?.takeIf { it in 1..120 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timer durations") },
        text = {
            val scroll = androidx.compose.foundation.rememberScrollState()
            Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                DurationField(focus, { focus = it }, "Focus minutes", 240, focusValue == null)
                DurationField(rest, { rest = it }, "Break minutes", 120, restValue == null)
                FokusTextButton(onClick = {
                    focus = "25"
                    rest = "5"
                }) { Text("Restore 25 / 5") }
                Muted("Changes apply to new blocks.")
            }
        },
        confirmButton = {
            FokusButton(enabled = focusValue != null && restValue != null, onClick = {
                if (focusValue != null && restValue != null) onSave(focusValue, restValue)
            }) { Text("Save durations") }
        },
        dismissButton = { FokusTextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationField(value: String, onChange: (String) -> Unit, label: String, max: Int, invalid: Boolean) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val source = remember { MutableInteractionSource() }
    val dragging by source.collectIsDraggedAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (dragging || pressed) 1.12f else 1f, tween(150), label = "Slider thumb")
    OutlinedTextField(
        value, { if (it.length <= 3 && it.all(Char::isDigit)) onChange(it) },
        singleLine = true, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(), isError = invalid && value.isNotEmpty(),
        supportingText = { Text("1–$max minutes") }
    )
    Slider(
        value = dragValue ?: (value.toFloatOrNull() ?: 1f).coerceIn(1f, max.toFloat()),
        onValueChange = {
            dragValue = it
            onChange(it.roundToInt().toString())
        },
        onValueChangeFinished = { dragValue = null },
        valueRange = 1f..max.toFloat(),
        modifier = Modifier.semantics { contentDescription = "Adjust ${label.lowercase()}" },
        interactionSource = source,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = source,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )
        }
    )
}
