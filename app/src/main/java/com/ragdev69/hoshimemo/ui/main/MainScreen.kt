package com.ragdev69.hoshimemo.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ragdev69.hoshimemo.ui.notes.NotesScreen
import com.ragdev69.hoshimemo.ui.todo.TodoScreen
import com.ragdev69.hoshimemo.ui.trash.TrashScreen
import com.ragdev69.hoshimemo.ui.settings.SettingsScreen
import com.ragdev69.hoshimemo.ui.profile.ProfileScreen
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors

sealed class BottomTab(val label: String, val icon: ImageVector) {
    object Notes    : BottomTab("Notes",    Icons.Filled.Notes)
    object Todo     : BottomTab("To-Do",    Icons.Filled.CheckCircle)
    object Trash    : BottomTab("Trash",    Icons.Filled.Delete)
    object Profile  : BottomTab("Profile",  Icons.Filled.Person)
    object Settings : BottomTab("Settings", Icons.Filled.Settings)
}

@Composable
fun MainScreen(navController: NavHostController) {
    val colors = LocalHoshiColors.current
    var selectedTab by remember { mutableStateOf<BottomTab>(BottomTab.Notes) }

    val tabs = listOf(
        BottomTab.Notes,
        BottomTab.Todo,
        BottomTab.Trash,
        BottomTab.Profile,
        BottomTab.Settings
    )

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            NavigationBar(
                containerColor = colors.navBar,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                tabs.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick  = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) colors.primary else colors.textSecondary
                            )
                        },
                        label = {
                            Text(
                                text  = tab.label,
                                color = if (selected) colors.primary else colors.textSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = colors.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                            slideInHorizontally { it / 20 } togetherWith
                            fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    BottomTab.Notes    -> NotesScreen(navController = navController)
                    BottomTab.Todo     -> TodoScreen()
                    BottomTab.Trash    -> TrashScreen()
                    BottomTab.Profile  -> ProfileScreen()
                    BottomTab.Settings -> SettingsScreen()
                }
            }
        }
    }
}