package com.ragdev69.hoshimemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ragdev69.hoshimemo.ui.notes.NoteEditorScreen
import com.ragdev69.hoshimemo.ui.main.MainScreen
import com.ragdev69.hoshimemo.ui.secret.SecretScreen
import com.ragdev69.hoshimemo.ui.sound.LocalSoundConfig
import com.ragdev69.hoshimemo.ui.sound.SoundManager

sealed class Screen(val route: String) {

    object Main : Screen("main")

    object NoteEditor :
        Screen("note_editor?id={id}&title={title}&content={content}") {

        fun createRoute(
            id: String = "",
            title: String = "",
            content: String = ""
        ) =
            "note_editor?id=$id&title=${
                java.net.URLEncoder.encode(title, "UTF-8")
            }&content=${
                java.net.URLEncoder.encode(content, "UTF-8")
            }"
    }

    object Secret : Screen("secret/{code}") {

        fun createRoute(code: String) =
            "secret/$code"
    }
}

@Composable
fun HoshiNavGraph(
    navController: NavHostController
) {
    val soundConfig = LocalSoundConfig.current

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {

        // MAIN SCREEN
        composable(Screen.Main.route) {
            MainScreen(
                navController = navController,
                showBottomBar = true
            )
        }

        // NOTE EDITOR — play nav sound on entry
        composable(Screen.NoteEditor.route) { backStack ->
            LaunchedEffect(Unit) {
                SoundManager.playNav(soundConfig.secretTheme, soundConfig.muted)
            }

            val id =
                backStack.arguments?.getString("id") ?: ""

            val title =
                java.net.URLDecoder.decode(
                    backStack.arguments?.getString("title") ?: "",
                    "UTF-8"
                )

            val content =
                java.net.URLDecoder.decode(
                    backStack.arguments?.getString("content") ?: "",
                    "UTF-8"
                )

            NoteEditorScreen(
                noteId = id,
                noteTitle = title,
                noteContent = content,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // SECRET SCREEN — play nav sound on entry
        composable(Screen.Secret.route) { backStack ->
            LaunchedEffect(Unit) {
                SoundManager.playNav(soundConfig.secretTheme, soundConfig.muted)
            }

            val code =
                backStack.arguments?.getString("code") ?: ""

            SecretScreen(
                code = code,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}