package com.example.tasktodolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tasktodolist.data.TaskList
import com.example.tasktodolist.ui.screens.AddTaskScreen
import com.example.tasktodolist.ui.screens.EditTaskScreen
import com.example.tasktodolist.ui.screens.TaskListScreen
import com.example.tasktodolist.ui.theme.TaskToDoListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load SharedPreferences into the shared TaskList once before UI reads tasks
        TaskList.instance.init(this)
        enableEdgeToEdge()
        setContent {
            TaskToDoListTheme {
                TaskToDoNavHost(
                    onExitApp = { finishAffinity() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun TaskToDoNavHost(
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "list",
        modifier = modifier,
    ) {
        composable("list") {
            TaskListScreen(
                onAddClick = { navController.navigate("add") },
                onTaskClick = { taskId -> navController.navigate("edit/$taskId") },
                onExitClick = onExitApp,
            )
        }
        composable("add") {
            AddTaskScreen(
                onNavigateBack = { navController.popBackStack() },
                onExitClick = onExitApp,
            )
        }
        composable(
            route = "edit/{taskId}",
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
            EditTaskScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() },
                onExitClick = onExitApp,
            )
        }
    }
}
