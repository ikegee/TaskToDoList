package com.example.tasktodolist.data

import java.time.LocalDate
import java.util.UUID

/**
 * Author: G.E. Eidsness
 * Project: TaskToDoList File: Task.kt
 * Task domain model.
 *
 * Ported from TaskLinkedList (Created: 2017-08-15).
 */
data class Task(
    val id: UUID = UUID.randomUUID(),
    var dateObj: LocalDate = LocalDate.now(),
    var title: String = "",
    var description: String = "",
    var isTasked: Boolean = false,
    var category: String = "",
    var priority: String = "",
)
