package com.origin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.origin.app.ui.screens.DecodeScreen
import com.origin.app.ui.screens.HomeScreen
import com.origin.app.ui.screens.ProfileScreen
import com.origin.app.ui.screens.VisualizationScreen
import com.origin.app.ui.theme.OriginTheme
import androidx.compose.ui.graphics.vector.ImageVector
import com.origin.app.data.SessionStore
import com.origin.app.ui.screens.AuthScreen
import com.origin.app.ui.screens.ForgotPasswordScreen
import com.origin.app.ui.screens.NewsDetailScreen
import com.origin.app.ui.screens.SettingsScreen
import com.origin.app.ui.screens.VerifyEmailScreen

sealed class Dest(val route: String, val title: String, val icon: ImageVector) {
	data object Home: Dest("home", "Главный", Icons.Filled.Home)
	data object Decode: Dest("decode", "Расшифровка", Icons.Filled.Science)
	data object Profile: Dest("profile", "Профиль", Icons.Filled.Person)
	data object Auth: Dest("auth", "Авторизация", Icons.Filled.Person)
}

class MainActivity : ComponentActivity() {
	private lateinit var sessionStore: SessionStore
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		sessionStore = SessionStore(this)
		setContent {
			OriginTheme(sessionStore = sessionStore) { App(sessionStore) }
		}
	}
}

@Composable
fun App(sessionStore: SessionStore) {
	val navController = rememberNavController()
	val items = listOf(Dest.Home, Dest.Decode, Dest.Profile)
	val backStackEntry by navController.currentBackStackEntryAsState()
	val currentRoute = backStackEntry?.destination?.route
	val token by sessionStore.token.collectAsState(initial = null)

	LaunchedEffect(token) {
		if (token.isNullOrEmpty()) {
			navController.navigate(Dest.Auth.route) {
				popUpTo(navController.graph.findStartDestination().id) { saveState = true }
				launchSingleTop = true
				restoreState = true
			}
		} else {
			// Если пользователь авторизован, переходим в профиль
			navController.navigate(Dest.Profile.route) {
				popUpTo(navController.graph.findStartDestination().id) { saveState = true }
				launchSingleTop = true
				restoreState = true
			}
		}
	}

	Scaffold(
		bottomBar = {
			if (currentRoute != Dest.Auth.route) {
				NavigationBar {
					items.forEach { dest ->
						NavigationBarItem(
							selected = currentRoute == dest.route,
							onClick = {
								navController.navigate(dest.route) {
									popUpTo(navController.graph.findStartDestination().id) { saveState = true }
									launchSingleTop = true
									restoreState = true
								}
							},
							icon = { Icon(dest.icon, contentDescription = dest.title) },
							label = { Text(dest.title) }
						)
					}
				}
			}
		}
	) { padding ->
		NavHost(
			navController = navController,
			startDestination = Dest.Home.route,
			modifier = Modifier.padding(padding)
		) {
			composable(Dest.Auth.route) { AuthScreen(navController, sessionStore) }
			composable("auth/login") { AuthScreen(navController, sessionStore, "login") }
			composable("verify-email/{email}") { backStackEntry ->
				val email = backStackEntry.arguments?.getString("email") ?: ""
				VerifyEmailScreen(navController, sessionStore, email)
			}
			composable(Dest.Home.route) { HomeScreen(navController) }
			composable(Dest.Decode.route) { DecodeScreen(navController) }
			composable(Dest.Profile.route) { ProfileScreen(navController) }
			composable("settings") { SettingsScreen(navController, sessionStore) }
			composable("forgot-password") { ForgotPasswordScreen(navController) }
			composable("visual/{id}") { VisualizationScreen(navController) }
			composable("news/{id}") { NewsDetailScreen(navController) }
		}
	}
}
