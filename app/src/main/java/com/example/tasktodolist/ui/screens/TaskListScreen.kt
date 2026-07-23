package com.example.tasktodolist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.tasktodolist.R
import com.example.tasktodolist.data.LocalDateSerializer
import com.example.tasktodolist.data.Task
import com.example.tasktodolist.data.TaskList
import com.example.tasktodolist.ui.components.AppTopBar
import com.example.tasktodolist.ui.components.appContentWindowInsets

/**
 * Task list screen (legacy DisplayTasksActivity parity).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onAddClick: () -> Unit,
    onTaskClick: (taskId: String) -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Snapshot the mutable LinkedList whenever we resume / first compose / local edit
    val tasks = remember(refreshKey) {
        TaskList.instance.tasks.toList()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = appContentWindowInsets(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.tasks_title),
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_add),
                        )
                    }
                    TextButton(onClick = onExitClick) {
                        Text(text = stringResource(R.string.action_exit))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (tasks.isEmpty()) {
            EmptyTasksState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.edit_task_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(
                    items = tasks,
                    key = { it.id },
                ) { task ->
                    TaskListItem(
                        task = task,
                        onClick = { onTaskClick(task.id.toString()) },
                        onToggleCompleted = { completed ->
                            task.isTasked = completed
                            TaskList.instance.updateTask(task)
                            refreshKey++
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.no_tasks),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.no_tasks_guidance),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskListItem(
    task: Task,
    onClick: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusLabel = if (task.isTasked) {
        stringResource(R.string.task_checked_completed)
    } else {
        stringResource(R.string.task_checked_pending)
    }
    val priorityVisual = priorityVisuals(task.priority)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val iconModifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
                .then(
                    if (priorityVisual.iconBackground != null) {
                        Modifier
                            .clip(CircleShape)
                            .background(priorityVisual.iconBackground)
                            .padding(4.dp)
                    } else {
                        Modifier
                    },
                )
            Icon(
                imageVector = priorityVisual.icon,
                contentDescription = stringResource(R.string.priority_prefix, task.priority),
                tint = priorityVisual.iconTint,
                modifier = iconModifier,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = LocalDateSerializer.format(task.dateObj),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = stringResource(R.string.category_prefix, task.category),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = stringResource(R.string.priority_prefix, task.priority),
                    style = MaterialTheme.typography.bodySmall,
                    color = priorityVisual.accentColor,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.isTasked,
                        onCheckedChange = onToggleCompleted,
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/**
 * Priority visuals: Low=calm (tertiary), Medium=orange/yellow bg + black icon, High=error.
 */
private data class PriorityVisual(
    val icon: ImageVector,
    val iconTint: Color,
    val accentColor: Color,
    val iconBackground: Color? = null,
)

@Composable
private fun priorityVisuals(priority: String): PriorityVisual {
    val scheme = MaterialTheme.colorScheme
    return when (priority) {
        "Low" -> PriorityVisual(
            icon = Icons.Filled.KeyboardArrowDown,
            iconTint = scheme.tertiary,
            accentColor = scheme.tertiary,
        )
        "Medium" -> {
            val bg = colorResource(R.color.medium_priority_bg)
            val fg = colorResource(R.color.medium_priority_fg)
            PriorityVisual(
                icon = Icons.Filled.Warning,
                iconTint = fg,
                accentColor = bg,
                iconBackground = bg,
            )
        }
        "High" -> PriorityVisual(
            icon = Icons.Filled.Error,
            iconTint = scheme.error,
            accentColor = scheme.error,
        )
        else -> PriorityVisual(
            icon = Icons.Filled.Warning,
            iconTint = scheme.onSurfaceVariant,
            accentColor = scheme.onSurfaceVariant,
        )
    }
}
