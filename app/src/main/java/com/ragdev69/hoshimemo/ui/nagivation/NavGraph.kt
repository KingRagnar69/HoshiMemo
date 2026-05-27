package com.ragdev69.hoshimemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ragdev69.hoshimemo.ui.notes.NoteEditorScreen
import com.ragdev69.hoshimemo.ui.main.MainScreen

sealed class Screen(val route: String) {
    object Main       : Screen("main")
    object NoteEditor : Screen("note_editor?id={id}&title={title}&content={content}") {
        fun createRoute(id: String = "", title: String = "", content: String = "") =
            "note_editor?id=$id&title=${
                java.net.URLEncoder.encode(title, "UTF-8")
            }&content=${
                java.net.URLEncoder.encode(content, "UTF-8")
            }"
    }
}

@Composable
fun HoshiNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }
        composable(Screen.NoteEditor.route) { backStack ->
            val id      = backStack.arguments?.getString("id") ?: ""
            val title   = java.net.URLDecoder.decode(
                backStack.arguments?.getString("title") ?: "", "UTF-8"
            )
            val content = java.net.URLDecoder.decode(
                backStack.arguments?.getString("content") ?: "", "UTF-8"
            )
            NoteEditorScreen(
                noteId      = id,
                noteTitle   = title,
                noteContent = content,
                onBack      = { navController.popBackStack() }
            )
        }
    }
}