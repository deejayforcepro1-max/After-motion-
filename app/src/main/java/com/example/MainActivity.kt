package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AfterMotionDatabase
import com.example.data.CompositionRepository
import com.example.ui.editor.EditorScreen
import com.example.ui.projects.ProjectsScreen
import com.example.ui.theme.AfterMotionTheme
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.ProjectsViewModel

private const val ROUTE_PROJECTS = "projects"
private const val ROUTE_EDITOR = "editor/{projectId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val db = AfterMotionDatabase.getInstance(applicationContext)
        val repository = CompositionRepository(db.compositionDao())

        setContent {
            AfterMotionTheme {
                val navController = rememberNavController()
                val projectsFactory = remember { ProjectsViewModel.Factory(repository) }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_PROJECTS
                ) {
                    composable(ROUTE_PROJECTS) {
                        val projectsVm: ProjectsViewModel = viewModel(factory = projectsFactory)
                        ProjectsScreen(
                            viewModel = projectsVm,
                            onOpenProject = { id ->
                                navController.navigate("editor/$id")
                            }
                        )
                    }

                    composable(
                        route = ROUTE_EDITOR,
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                        val editorFactory = remember(projectId) {
                            EditorViewModel.Factory(projectId, repository)
                        }
                        val editorVm: EditorViewModel = viewModel(factory = editorFactory)
                        EditorScreen(
                            viewModel = editorVm,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
