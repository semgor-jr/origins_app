package com.origin.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.origin.app.data.SessionStore

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun OriginTheme(
	useDarkTheme: Boolean = isSystemInDarkTheme(),
	sessionStore: SessionStore? = null,
	content: @Composable () -> Unit
) {
	val fontSize by sessionStore?.fontSize?.collectAsState(initial = 1.0f) ?: remember { mutableStateOf(1.0f) }
	
	val typography = Typography(
		displayLarge = TextStyle(fontSize = (57 * fontSize).sp),
		displayMedium = TextStyle(fontSize = (45 * fontSize).sp),
		displaySmall = TextStyle(fontSize = (36 * fontSize).sp),
		headlineLarge = TextStyle(fontSize = (32 * fontSize).sp),
		headlineMedium = TextStyle(fontSize = (28 * fontSize).sp),
		headlineSmall = TextStyle(fontSize = (24 * fontSize).sp),
		titleLarge = TextStyle(fontSize = (22 * fontSize).sp),
		titleMedium = TextStyle(fontSize = (16 * fontSize).sp),
		titleSmall = TextStyle(fontSize = (14 * fontSize).sp),
		bodyLarge = TextStyle(fontSize = (16 * fontSize).sp),
		bodyMedium = TextStyle(fontSize = (14 * fontSize).sp),
		bodySmall = TextStyle(fontSize = (12 * fontSize).sp),
		labelLarge = TextStyle(fontSize = (14 * fontSize).sp),
		labelMedium = TextStyle(fontSize = (12 * fontSize).sp),
		labelSmall = TextStyle(fontSize = (11 * fontSize).sp)
	)
	
	MaterialTheme(
		colorScheme = if (useDarkTheme) DarkColors else LightColors,
		typography = typography,
		content = content
	)
}
