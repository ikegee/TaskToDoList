package com.example.tasktodolist.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.util.LinkedList
import androidx.core.content.edit

/**
 * SharedPreferences helper for persisting [LinkedList]<[Task]> as JSON.
 *
 * Uses Gson with [LocalDateSerializer] so [Task.dateObj] round-trips correctly.
 * Call from [TaskList] only — UI should not touch prefs directly.
 *
 * Ported from TaskLinkedList. Same prefs name/key so JSON format is compatible
 * if you ever copy prefs data between installs.
 */
class TaskPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateSerializer())
        .create()

    /**
     * Serializes the full task list and writes it under [KEY_TASKS].
     * Uses [android.content.SharedPreferences.Editor.apply] (async, non-blocking).
     */
    fun saveTasks(tasks: LinkedList<Task>) {
        val json = gson.toJson(tasks)
        prefs.edit { putString(KEY_TASKS, json) }
        Log.d(TAG, "saveTasks: size=${tasks.size}, jsonLength=${json.length}")
    }

    /**
     * Loads tasks from prefs, or an empty list if nothing is stored / parse fails.
     */
    fun loadTasks(): LinkedList<Task> {
        val json = prefs.getString(KEY_TASKS, null)
        if (json.isNullOrBlank()) {
            Log.d(TAG, "loadTasks: no saved data")
            return LinkedList()
        }
        return try {
            val type = object : TypeToken<LinkedList<Task>>() {}.type
            val loaded: LinkedList<Task>? = gson.fromJson(json, type)
            val result = loaded ?: LinkedList()
            Log.d(TAG, "loadTasks: size=${result.size}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "loadTasks: failed to parse JSON", e)
            LinkedList()
        }
    }

    /** Removes the stored task list (intentional wipe only). */
    fun clear() {
        prefs.edit { remove(KEY_TASKS) }
        Log.d(TAG, "clear: removed $KEY_TASKS")
    }

    companion object {
        private val TAG = TaskPreferences::class.java.simpleName
        private const val PREFS_NAME = "task_prefs"
        private const val KEY_TASKS = "tasks_json"
    }
}
