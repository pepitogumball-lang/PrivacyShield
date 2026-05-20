package com.privacyshield

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.privacyshield.bothub.viewmodel.BotHubViewModel
import com.privacyshield.data.AppViewModel
import com.privacyshield.ui.screens.AppsScreen
import com.privacyshield.ui.screens.BotHubScreen
import com.privacyshield.ui.screens.BotPersonaEditorScreen
import com.privacyshield.ui.screens.ContactEditorScreen
import com.privacyshield.ui.screens.ConversationScreen
import com.privacyshield.ui.screens.HomeScreen
import com.privacyshield.ui.screens.PerformanceScreen
import com.privacyshield.ui.screens.ProtectedAppsScreen
import com.privacyshield.ui.screens.RemoteScreen
import com.privacyshield.ui.screens.SettingsScreen
import com.privacyshield.ui.theme.BackgroundDark
import com.privacyshield.ui.theme.CyanAccent
import com.privacyshield.ui.theme.PrivacyShieldTheme
import com.privacyshield.ui.theme.SurfaceDark
import com.privacyshield.ui.theme.TextSecondary
import com.privacyshield.util.PermissionManager

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Apps : Screen("apps", "Apps", Icons.Default.Apps)
    object Protected : Screen("protected", "Shield", Icons.Default.Lock)
    object Performance : Screen("performance", "Perf", Icons.Default.Speed)
    object BotHub : Screen("bothub", "Bots", Icons.Default.Android)
    object Remote : Screen("remote", "Remote", Icons.Default.Tv)
    object Settings : Screen("settings", "More", Icons.Default.Settings)
}

private val bottomNavItems = listOf(
    Screen.Home,
    Screen.Apps,
    Screen.Protected,
    Screen.Performance,
    Screen.BotHub,
    Screen.Remote,
    Screen.Settings
)

private val hideNavPrefixes = listOf("persona_editor", "contact_editor", "chat")

class MainActivity : ComponentActivity() {

    private val notificationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted is handled silently; app works without notifications */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (PermissionManager.needsNotificationPermission() &&
            !PermissionManager.isNotificationPermissionGranted(this)
        ) {
            @Suppress("InlinedApi")
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            PrivacyShieldTheme {
                PrivacyShieldApp()
            }
        }
    }
}

@Composable
private fun PrivacyShieldApp() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel()
    val botHubViewModel: BotHubViewModel = viewModel()

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route ?: ""

            val showBottomBar = remember(currentRoute) {
                hideNavPrefixes.none { currentRoute.startsWith(it) }
            }

            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanAccent,
                                selectedTextColor = CyanAccent,
                                indicatorColor = CyanAccent.copy(alpha = 0.12f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel = appViewModel) }
            composable(Screen.Apps.route) { AppsScreen(viewModel = appViewModel) }
            composable(Screen.Protected.route) { ProtectedAppsScreen(viewModel = appViewModel) }
            composable(Screen.Performance.route) { PerformanceScreen(viewModel = appViewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel = appViewModel) }
            composable(Screen.Remote.route) { RemoteScreen() }
            composable(Screen.BotHub.route) {
                BotHubScreen(navController = navController, viewModel = botHubViewModel)
            }
            composable("persona_editor/{personaId}") { backStackEntry ->
                val personaId = backStackEntry.arguments?.getString("personaId") ?: "new"
                BotPersonaEditorScreen(
                    personaId = personaId,
                    navController = navController,
                    viewModel = botHubViewModel
                )
            }
            composable("contact_editor/{contactId}") { backStackEntry ->
                val contactId = backStackEntry.arguments?.getString("contactId") ?: "new"
                ContactEditorScreen(
                    contactId = contactId,
                    navController = navController,
                    viewModel = botHubViewModel
                )
            }
            composable("chat/{conversationId}") { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId")
                if (conversationId != null) {
                    ConversationScreen(
                        conversationId = conversationId,
                        navController = navController,
                        viewModel = botHubViewModel
                    )
                }
            }
        }
    }
}
