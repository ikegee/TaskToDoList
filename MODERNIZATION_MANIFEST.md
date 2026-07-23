# TaskToDoList — Modernization Manifest (from TaskLinkedList)

**Purpose:** Feed these steps (one phase at a time, or the whole doc) into TaskToDoList’s AI Chat to finish modernizing the app as a Jetpack Compose rewrite of **TaskLinkedList**.

**Workspace:** `C:\Users\ike\AndroidStudioProjects\TaskToDoList`  
**Reference (source of behavior):** `C:\Users\ike\AndroidStudioProjects\TaskLinkedList`

---

## Snapshot (do not re-do)

| Layer | TaskLinkedList (legacy) | TaskToDoList (target) | Status |
|-------|-------------------------|------------------------|--------|
| Build | Groovy Gradle, Views, AppCompat | Kotlin DSL, Version Catalog, Compose Material3 | Done |
| Domain | `Task` data class | `data/Task.kt` | Done |
| Store | `TaskList` singleton + `LinkedList` | `data/TaskList.kt` | Done |
| Persist | Gson + SharedPreferences `task_prefs` / `tasks_json` | `data/TaskPreferences.kt` | Done |
| Dates | `LocalDateSerializer` | `data/LocalDateSerializer.kt` | Done |
| UI | 3 Activities + ListView + XML | Placeholder `Greeting` only | **Not done** |
| Nav | Implicit intents between activities | Compose Navigation | **Not done** |
| Date pick | `TaskDatePicker` DialogFragment | Material3 date picker | **Not done** |

**Package:** `com.example.tasktodolist`  
**Data package:** `com.example.tasktodolist.data`  
**Keep:** `TaskList.instance.init(context)` before any UI reads tasks.  
**Keep:** Exit must **not** call `clearTasks()` — tasks stay on disk.

---

## Global rules for the AI (paste at the start of every phase)

```
You are modernizing TaskToDoList (Jetpack Compose) to match TaskLinkedList feature parity.

RULES:
1. Work only in TaskToDoList unless explicitly asked to read TaskLinkedList for reference.
2. Do not rewrite data/* (Task, TaskList, TaskPreferences, LocalDateSerializer) unless a bug blocks UI.
3. Preserve behavior: LinkedList in-memory store, SharedPreferences JSON persistence, UUID ids.
4. Exit / close app: finish activity / finishAffinity only — never wipe prefs.
5. Title max 50 chars, description max 200 chars.
6. Categories: Personal, Home, Work. Priorities: Low, Medium, High.
7. Prefer Material3, single-activity Compose, Navigation Compose.
8. After each phase: app must compile; note any files changed.
9. Prefer small, reviewable diffs; no drive-by refactors of unrelated code.
10. Use string resources in res/values/strings.xml (not hardcoded UI strings where practical).
```

---

## Phase 0 — Orient (read-only)

**Goal:** Confirm baseline before coding.

**Prompt to feed:**

```
Read and summarize the current TaskToDoList project:
- app/src/main/java/com/example/tasktodolist/MainActivity.kt
- app/src/main/java/com/example/tasktodolist/data/*
- app/build.gradle.kts, gradle/libs.versions.toml
- app/src/main/AndroidManifest.xml

Then skim TaskLinkedList for behavior to port (do not copy Views code verbatim):
- DisplayTasksActivity.kt (launcher list + menu Add + Exit)
- AddTaskActivity.kt (create task form)
- EditTaskActivity.kt (update / delete / email / cancel / exit)
- TaskListAdapter.kt (list row fields + priority icon mapping)
- strings.xml (labels, category_array, priority_array)

Reply with: (1) what’s already ported, (2) missing UI screens, (3) navigation plan for single-activity Compose.
```

**Done when:** Clear list of missing screens and nav graph.

---

## Phase 1 — Dependencies & strings

**Goal:** Add Navigation Compose (if missing) and port string resources.

**Prompt to feed:**

```
PHASE 1 — Dependencies and strings for TaskToDoList.

1) Add Navigation Compose via version catalog (gradle/libs.versions.toml + app/build.gradle.kts)
   if not already present. Prefer a recent stable androidx.navigation:navigation-compose
   compatible with the existing Compose BOM.

2) Expand app/src/main/res/values/strings.xml from TaskLinkedList parity:
   - app_name: TaskToDoList
   - Labels: title, description, due date, category, priority
   - Hints for title/description
   - Status: Completed / Pending
   - Actions: Add, Update, Delete, Cancel, Exit, Email Task, New task
   - Empty list: "Tasks Empty" / guidance to add
   - List tip: "Click Task to Edit" (or "Tap task to edit")
   - string-array category_array: Personal, Home, Work
   - string-array priority_array: Low, Medium, High
   - Validation: "Enter Title", "Enter Description", max-length messages

3) Do not change data layer.
4) Confirm project still compiles.
```

**Done when:** Strings + nav dependency present; build OK.

---

## Phase 2 — Navigation shell (replace Greeting)

**Goal:** Single-activity app with three routes.

**Prompt to feed:**

```
PHASE 2 — Compose navigation shell.

Replace the Greeting placeholder in MainActivity with:
- TaskList.instance.init(this) before setContent (already present — keep it)
- enableEdgeToEdge + TaskToDoListTheme + Scaffold
- NavHost with routes:
  - "list"   (start destination) — TaskListScreen stub
  - "add"    — AddTaskScreen stub
  - "edit/{taskId}" — EditTaskScreen stub (taskId as UUID string)

Create package ui/screens/ with stub composables that show a title Text for each screen.
Use rememberNavController(). Wire FAB or TopAppBar later in Phase 3.

Keep MainActivity as the only Activity (update AndroidManifest if needed; remove nothing critical).
Compile must succeed.
```

**Done when:** Three navigable stub screens; launcher opens list.

---

## Phase 3 — Task list screen (DisplayTasksActivity parity)

**Source behavior:** `DisplayTasksActivity.kt` + `TaskListAdapter` + `list_task_items.xml`

**Prompt to feed:**

```
PHASE 3 — TaskListScreen (modern DisplayTasksActivity).

Implement ui/screens/TaskListScreen.kt:

UI:
- TopAppBar: title "Tasks" (or string resource); action icon Add (+) → navigate to "add"
- LazyColumn of tasks from TaskList.instance.tasks
- Each row (TaskListAdapter parity):
  - Title (bold)
  - Description
  - Date: task.dateObj formatted via LocalDateSerializer.format (ISO yyyy-MM-dd)
  - Category: "Category: {value}"
  - Priority: "Priority: {value}" + visual priority indicator (color/icon for Low/Medium/High)
  - Status: checkbox or badge — Completed vs Pending (task.isTasked); display-only on list
- Empty state: show "Tasks Empty" and hint to tap + to add
- Bottom or TopAppBar overflow / button: Exit → activity.finishAffinity() (do NOT clearTasks)

Interaction:
- Tap row → navigate to "edit/{task.id}"
- After returning from add/edit, list must refresh. Because TaskList is a mutable LinkedList,
  use a simple refresh trigger (e.g. mutableStateOf version counter, or collect a snapshot
  list on each composition resume). Prefer:
    val tasks = TaskList.instance.tasks.toList()
  recomposed when a parent refreshKey changes; increment refreshKey on LaunchedEffect / DisposableEffect
  when the screen becomes visible again (Lifecycle Resume).

Do not use ListView, BaseAdapter, or XML layouts.
Material3 cards or list items preferred.
Compile and keep data layer unchanged.
```

**Done when:** List shows persisted tasks; empty state; add nav; exit without wipe.

---

## Phase 4 — Add task screen (AddTaskActivity parity)

**Source behavior:** `AddTaskActivity.kt`

**Prompt to feed:**

```
PHASE 4 — AddTaskScreen.

Implement ui/screens/AddTaskScreen.kt:

Form fields:
- Title: OutlinedTextField, maxLength 50, required
- Description: OutlinedTextField, maxLength 200, required
- Due date: button/field showing LocalDateSerializer.format(selectedDate); default LocalDate.now()
  Tap opens Material3 DatePickerDialog (compose); write LocalDate into selectedDate
- Category: dropdown / ExposedDropdownMenuBox from R.array.category_array (default first or Home)
- Priority: dropdown from R.array.priority_array (default Low)
- Completed checkbox: labels Completed / Pending (isTasked)

Actions:
- Add: validate title/description non-blank; create Task(...); TaskList.instance.addTask(task); popBackStack()
- Cancel: discard and popBackStack()
- Exit: finishAffinity() without clearTasks

Use Scaffold + TopAppBar with back navigation.
On success, list screen must show the new task after resume/refresh.
Compile; no XML forms.
```

**Done when:** Can add a task, return to list, see it, and it survives process death via prefs.

---

## Phase 5 — Edit task screen (EditTaskActivity parity)

**Source behavior:** `EditTaskActivity.kt`

**Prompt to feed:**

```
PHASE 5 — EditTaskScreen.

Implement ui/screens/EditTaskScreen(taskId: String, ...):

Load: val id = UUID.fromString(taskId); val task = TaskList.instance.getTask(id)
If null: snackbar/toast "Task not found" and popBackStack().

Prefill all form fields like Add screen (title, description, date, category, priority, isTasked).
Enforce same max lengths (50 / 200).

Actions:
- Update: validate; mutate fields on the Task (or copy); TaskList.instance.updateTask(task); popBackStack()
- Delete: TaskList.instance.removeTask(id); popBackStack()
- Cancel: popBackStack() without save
- Email Task: Intent.ACTION_SEND type message/rfc822; EXTRA_SUBJECT=title; EXTRA_TEXT=description;
  startActivity(Intent.createChooser(...)). Use a placeholder recipient only if needed
  (legacy used joeblow@gmail.ca — prefer chooser without hard failure).
- Exit: finishAffinity() without clearTasks

TopAppBar title "Edit Task"; back = cancel.
Compile; list refresh after update/delete.
```

**Done when:** Full edit/delete/email/cancel/exit parity.

---

## Phase 6 — Polish & Material3 UX

**Prompt to feed:**

```
PHASE 6 — Polish TaskToDoList UI.

1) Consistent TopAppBars, contentWindowInsets / padding from Scaffold for edge-to-edge.
2) Priority colors: Low=calm, Medium=warn, High=error (Material3 colorScheme).
3) Optional: swipe-to-delete on list with confirm dialog (only if simple; not required for parity).
4) Optional: toggle completed from list by tapping status (if you add it, call updateTask + persist).
5) Remove unused Greeting / Preview dead code if replaced.
6) Update README if present, or skip.
7) Ensure dark theme + dynamic color still work with TaskToDoListTheme.
```

**Done when:** UI feels modern; no regressions.

---

## Phase 7 — Verification checklist

**Prompt to feed:**

```
PHASE 7 — Verify TaskToDoList against TaskLinkedList behavior.

Manual / logical checklist (run app or reason through code):
[ ] Cold start loads tasks from SharedPreferences (TaskList.init)
[ ] Empty list shows empty message
[ ] Add task with validation (empty title/description blocked)
[ ] Date picker changes due date; saved as LocalDate ISO
[ ] Category/priority spinners work
[ ] List shows title, description, date, category, priority, status
[ ] Tap opens edit with correct task
[ ] Update persists and shows on list
[ ] Delete removes from list and prefs
[ ] Exit does NOT wipe data; relaunch shows same tasks
[ ] Email intent launches chooser
[ ] No AppCompat Activities for add/edit/list (Compose only)
[ ] ./gradlew assembleDebug succeeds

Fix any failures found.
```

**Done when:** All boxes pass.

---

## Optional Phase 8 — Architecture upgrades (only if user asks)

Do **not** run unless explicitly requested. These go beyond parity:

1. Replace singleton with `ViewModel` + `StateFlow` snapshot of tasks  
2. Migrate SharedPreferences → DataStore  
3. Replace `LinkedList` with `SnapshotStateList` / immutable list in UI layer only (keep LinkedList in store if required by project brief)  
4. Room database instead of JSON prefs  
5. Unit tests for TaskList add/update/remove/persist  
6. UI tests for list empty → add → appear  

---

## Mapping: legacy file → Compose target

| TaskLinkedList | TaskToDoList target |
|----------------|---------------------|
| `DisplayTasksActivity.kt` | `ui/screens/TaskListScreen.kt` + list items |
| `AddTaskActivity.kt` | `ui/screens/AddTaskScreen.kt` |
| `EditTaskActivity.kt` | `ui/screens/EditTaskScreen.kt` |
| `TaskListAdapter.kt` | `TaskListItem` composable inside list screen |
| `TaskDatePicker.kt` | Material3 `DatePickerDialog` in add/edit |
| `activity_*.xml`, `list_task_items.xml` | Compose layouts (no XML screens) |
| `main_menu.xml` (Add) | TopAppBar / FAB action |
| `Task.kt`, `TaskList.kt`, `TaskPreferences.kt`, `LocalDateSerializer.kt` | Already under `data/` — keep |

---

## Suggested feed order (copy-paste sequence)

1. Global rules block  
2. Phase 0  
3. Phase 1  
4. Phase 2  
5. Phase 3  ← primary modernization of DisplayTasksActivity  
6. Phase 4  
7. Phase 5  
8. Phase 6  
9. Phase 7  

**One-shot alternative** (if the AI can handle a large task): paste Global rules + Phases 1–7 and say:  
“Implement phases 1–7 in order; stop after each phase only if the build fails.”

---

## Critical behavior notes (from DisplayTasksActivity)

- Launcher was `DisplayTasksActivity` → Compose start destination is the **list** screen.  
- `taskList.init(this)` once, then bind UI to `taskList.tasks`.  
- Adapter held the **same** `LinkedList` reference as `TaskList.tasks` — do not clear+addAll the same instance when refreshing.  
- `onResume` only `notifyDataSetChanged` → Compose: re-snapshot list on lifecycle resume.  
- Item click passes many intent extras; modern approach: pass **only `taskId`** and load from `TaskList.getTask(id)`.  
- Close button: `finishAffinity()` without clearing prefs.

---

*Generated for feeding TaskToDoList AI Chat to complete the Compose modernization of TaskLinkedList.*
