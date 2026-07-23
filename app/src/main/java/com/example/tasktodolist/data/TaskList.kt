package com.example.tasktodolist.data

import android.content.Context
import android.util.Log
import java.util.LinkedList
import java.util.UUID

/**
 * Shared store for tasks: in-memory [LinkedList] plus disk via [TaskPreferences].
 *
 * Call [init] once (e.g. from [com.example.tasktodolist.MainActivity.onCreate])
 * before reading tasks. Screens should use [instance] for add/edit/display.
 *
 * Ported from TaskLinkedList.
 */
class TaskList private constructor() {

    val tasks = LinkedList<Task>()
    private var preferences: TaskPreferences? = null

    /**
     * Must be called once with any [Context] before reading tasks.
     * Safe to call again — only the first call loads from disk.
     */
    fun init(context: Context) {
        if (preferences != null) return
        preferences = TaskPreferences(context.applicationContext)
        tasks.clear()
        tasks.addAll(preferences!!.loadTasks())
        Log.d(TAG, "init: loaded ${tasks.size} task(s) from SharedPreferences")
    }

    fun addTask(task: Task) {
        tasks.add(task)
        persist()
        Log.d(TAG, "addTask: true, size=${tasks.size}")
    }

    fun getTask(id: UUID): Task? = tasks.find { it.id == id }

    fun updateTask(task: Task): Boolean {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index == -1) {
            Log.d(TAG, "updateTask: not found ${task.id}")
            return false
        }
        tasks[index] = task
        persist()
        Log.d(TAG, "updateTask: ${task.id}")
        return true
    }

    fun removeTask(id: UUID): Boolean {
        val removed = tasks.removeAll { it.id == id }
        if (removed) {
            persist()
        }
        Log.d(
            TAG,
            if (removed) "removeTask: $id, size=${tasks.size}" else "removeTask: not found $id",
        )
        return removed
    }

    /**
     * Removes every task from memory and SharedPreferences.
     * Not used on Exit — Exit keeps tasks on disk so they reload next launch.
     * Use only when intentionally wiping all tasks.
     */
    fun clearTasks() {
        val previousSize = tasks.size
        tasks.clear()
        preferences?.clear()
        Log.d(TAG, "clearTasks: cleared $previousSize task(s), size=${tasks.size}")
    }

    private fun persist() {
        preferences?.saveTasks(tasks)
            ?: Log.w(TAG, "persist: skipped — call init(context) first")
    }

    companion object {
        private val TAG = TaskList::class.java.simpleName
        val instance: TaskList = TaskList()
    }
}
