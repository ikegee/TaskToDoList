package com.example.tasktodolist.data

import java.time.LocalDate
import java.util.UUID

/**
 * Task domain model.
 *
 * Ported from TaskLinkedList (data class modernized 2026-07-13).
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
