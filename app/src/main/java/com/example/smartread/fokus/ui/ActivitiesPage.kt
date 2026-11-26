package com.example.smartread.fokus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartread.fokus.*

@Composable
internal fun ActivitiesPage(
    state: FokusState,
    onAdd: () -> Unit,
    onEdit: (Activity) -> Unit,
    onFocus: (Activity) -> Unit,
    onArchive: (Activity) -> Unit,
    onHelp: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var archived by rememberSaveable { mutableStateOf(false) }
    val results = remember(state.activities, archived, query) {
        state.activities.filter { activity ->
            activity.archived == archived && (activity.name.contains(query.trim(), ignoreCase = true) ||
                activity.category.label.contains(query.trim(), ignoreCase = true))
        }
    }
    val totals = remember(state.sessions) { state.sessions.groupBy { it.activity.id } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageTitle("Activities", onHelp) }
        item {
            FokusButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("New activity")
            }
        }
        if (state.activities.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true, shape = panelShape,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, label = { Text("Search activities") },
                    trailingIcon = if (query.isEmpty()) null else {
                        { FokusIconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") } }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Selector(listOf(false, true), archived, { if (it) "Archived" else "Active" }, { archived = it }) }
        }
        if (results.isEmpty()) {
            item {
                when {
                    query.isNotBlank() -> EmptyPanel(Icons.Outlined.Search, "No matches", "Try a different name or category.")
                    archived -> EmptyPanel(Icons.Outlined.Archive, "No archived activities", "Archived activities appear here.")
                    else -> EmptyPanel(Icons.Outlined.Layers, "Add your first activity",
                        "Start with a book, course, task, workout, or anything you want to track.")
                }
            }
        }
        items(results, key = { it.id }) { activity ->
            val records = totals[activity.id].orEmpty()
            ActivityRow(
                activity, records.size, records.sumOf { it.durationMillis },
                onFocus = { onFocus(activity) }, onEdit = { onEdit(activity) }, onArchive = { onArchive(activity) }
            )
        }
    }
}

@Composable
private fun ActivityRow(
    activity: Activity,
    sessions: Int,
    duration: Long,
    onFocus: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CategoryBadge(activity.category)
            RowLabel(activity.name, "${activity.category.label} · ${durationLabel(duration)} · ${sessionCountLabel(sessions)}", Modifier.weight(1f))
            if (!activity.archived) {
                FokusIconButton(
                    onClick = onFocus,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Outlined.PlayArrow, "Focus on ${activity.name}")
                }
            }
            Box {
                FokusIconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, "Manage ${activity.name}") }
                DropdownMenu(open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") }, onClick = {
                            open = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (activity.archived) "Restore" else "Archive") },
                        onClick = {
                            open = false
                            onArchive()
                        },
                        leadingIcon = { Icon(if (activity.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ActivityEditor(existing: Activity?, onDismiss: () -> Unit, onSave: (String, Category) -> Unit) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category ?: Category.TASK) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New activity" else "Edit activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { if (it.length <= 120) name = it },
                    label = { Text("Name") }, placeholder = { Text("e.g. Read chapter 4") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ChoiceMenu("Category", Category.entries, category, { it.label }, { category = it }, { it.icon })
            }
        },
        confirmButton = { FokusButton(enabled = name.isNotBlank(), onClick = { onSave(name, category) }) { Text("Save activity") } },
        dismissButton = { FokusTextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun <T> ChoiceMenu(
    label: String, items: List<T>, selected: T, describe: (T) -> String,
    onSelect: (T) -> Unit, icon: ((T) -> androidx.compose.ui.graphics.vector.ImageVector)? = null
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FokusOutlinedButton(
            onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
        ) {
            Text("$label · ${describe(selected)}", Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 340.dp)) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(describe(item)) },
                    leadingIcon = icon?.let { { Icon(it(item), null) } },
                    onClick = {
                        open = false
                        onSelect(item)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityPicker(
    activities: List<Activity>,
    selected: Activity,
    onDismiss: () -> Unit,
    onSelect: (Activity) -> Unit,
    onCreate: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = remember(activities, query) {
        (listOf(generalActivity) + activities.filterNot { it.archived }).filter {
            it.name.contains(query.trim(), ignoreCase = true) || it.category.label.contains(query.trim(), ignoreCase = true)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 470.dp),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            item { Text("Choose activity", style = MaterialTheme.typography.headlineMedium) }
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true, shape = panelShape,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(matches, key = { it.id }) { activity ->
                FokusSurface(onClick = { onSelect(activity) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadge(activity.category)
                        RowLabel(activity.name, activity.category.label, Modifier.weight(1f))
                        if (selected.id == activity.id) Icon(Icons.Outlined.Check, "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                FokusTextButton(onClick = onCreate) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("New activity")
                }
            }
        }
    }
}
