package com.example.tasktodolist.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tasktodolist.R
import com.example.tasktodolist.data.LocalDateSerializer
import com.example.tasktodolist.data.TaskList
import com.example.tasktodolist.ui.components.AppTopBar
import com.example.tasktodolist.ui.components.appContentWindowInsets
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

private const val MAX_TITLE_LENGTH = 50
private const val MAX_DESCRIPTION_LENGTH = 200

/**
 * Author:  G.E. Eidsness
 * Project: TaskToDoList File: AddTaskScreen.kt
 * Edit-task form (legacy EditTaskActivity parity Created: 2017-08-15).
 * [taskId] is the UUID string from the navigation route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val categories = stringArrayResource(R.array.category_array)
    val priorities = stringArrayResource(R.array.priority_array)
    val errorEnterTitle = stringResource(R.string.error_enter_title)
    val errorEnterDescription = stringResource(R.string.error_enter_description)
    val errorTaskNotFound = stringResource(R.string.error_task_not_found)
    val emailChooserTitle = stringResource(R.string.email_chooser_title)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val taskIdUuid = remember(taskId) {
        runCatching { UUID.fromString(taskId) }.getOrNull()
    }
    val existingTask = remember(taskIdUuid) {
        taskIdUuid?.let { TaskList.instance.getTask(it) }
    }

    LaunchedEffect(existingTask, taskIdUuid) {
        if (taskIdUuid == null || existingTask == null) {
            // Toast: no Scaffold/SnackbarHost yet on the not-found path
            Toast.makeText(context, errorTaskNotFound, Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    if (existingTask == null) {
        // Brief placeholder while snackbar + pop run for missing tasks
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Prefill from store; caps match Add / legacy maxLength
    var title by remember(existingTask.id) {
        mutableStateOf(existingTask.title.take(MAX_TITLE_LENGTH))
    }
    var description by remember(existingTask.id) {
        mutableStateOf(existingTask.description.take(MAX_DESCRIPTION_LENGTH))
    }
    var selectedDate by remember(existingTask.id) {
        mutableStateOf(existingTask.dateObj)
    }
    var category by remember(existingTask.id) {
        mutableStateOf(
            existingTask.category.ifBlank {
                categories.firstOrNull { it == "Home" } ?: categories.firstOrNull().orEmpty()
            },
        )
    }
    var priority by remember(existingTask.id) {
        mutableStateOf(
            existingTask.priority.ifBlank {
                priorities.firstOrNull { it == "Low" } ?: priorities.firstOrNull().orEmpty()
            },
        )
    }
    var isTasked by remember(existingTask.id) {
        mutableStateOf(existingTask.isTasked)
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = appContentWindowInsets(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.edit_task_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = onExitClick) {
                        Text(text = stringResource(R.string.action_exit))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= MAX_TITLE_LENGTH) title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.task_title_label)) },
                placeholder = { Text(stringResource(R.string.task_title_hint)) },
                singleLine = true,
                supportingText = {
                    Text("${title.length}/$MAX_TITLE_LENGTH")
                },
            )

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= MAX_DESCRIPTION_LENGTH) description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.task_description_label)) },
                placeholder = { Text(stringResource(R.string.task_description_hint)) },
                minLines = 3,
                supportingText = {
                    Text("${description.length}/$MAX_DESCRIPTION_LENGTH")
                },
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${stringResource(R.string.task_due_date)}: ${LocalDateSerializer.format(selectedDate)}",
                )
            }

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.task_category)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = it },
            ) {
                OutlinedTextField(
                    value = priority,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.task_priority)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false },
                ) {
                    priorities.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                priority = option
                                priorityExpanded = false
                            },
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isTasked,
                    onCheckedChange = { isTasked = it },
                )
                Text(
                    text = stringResource(
                        if (isTasked) {
                            R.string.task_checked_completed
                        } else {
                            R.string.task_checked_pending
                        },
                    ),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    val trimmedDescription = description.trim()
                    when {
                        trimmedTitle.isEmpty() -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(errorEnterTitle)
                            }
                        }
                        trimmedDescription.isEmpty() -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(errorEnterDescription)
                            }
                        }
                        else -> {
                            existingTask.title = trimmedTitle
                            existingTask.description = trimmedDescription
                            existingTask.dateObj = selectedDate
                            existingTask.category = category
                            existingTask.priority = priority
                            existingTask.isTasked = isTasked
                            val updated = TaskList.instance.updateTask(existingTask)
                            if (!updated) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(errorTaskNotFound)
                                }
                                onNavigateBack()
                            } else {
                                onNavigateBack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.action_update))
            }

            OutlinedButton(
                onClick = {
                    TaskList.instance.removeTask(existingTask.id)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.action_delete))
            }

            OutlinedButton(
                onClick = {
                    emailTask(
                        context = context,
                        subject = title,
                        body = description,
                        chooserTitle = emailChooserTitle,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.action_email_task))
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun emailTask(
    context: android.content.Context,
    subject: String,
    body: String,
    chooserTitle: String,
) {
    val email = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(Intent.createChooser(email, chooserTitle))
    } catch (_: ActivityNotFoundException) {
        // No email client available; chooser may also no-op on some devices
    }
}
