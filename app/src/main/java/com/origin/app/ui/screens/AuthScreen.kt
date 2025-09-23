package com.origin.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.origin.app.data.ApiClient
import com.origin.app.data.LoginRequest
import com.origin.app.data.SessionStore
import kotlinx.coroutines.launch

private fun isValidEmail(s: String): Boolean =
	s.matches(Regex("^.+@.+\\..+$"))

@Composable
fun AuthScreen(navController: NavController, sessionStore: SessionStore, initialMode: String = "register") {
	var name by remember { mutableStateOf("") }
	var email by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }
	var confirmPassword by remember { mutableStateOf("") }
	var mode by remember { mutableStateOf(initialMode) }
	var status by remember { mutableStateOf("") }
	val scope = rememberCoroutineScope()
	val api = remember { ApiClient() }

	val isEmailValid = isValidEmail(email)
	val isNameValid = if (mode == "register") name.trim().isNotEmpty() else true
	val isPasswordValid = password.length >= 6
	val isConfirmValid = if (mode == "register") confirmPassword == password && confirmPassword.isNotEmpty() else true
	val isFormValid = isEmailValid && isNameValid && isPasswordValid && isConfirmValid

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		// Заголовок
		Text(
			text = if (mode == "register") "Регистрация" else "Вход",
			textAlign = TextAlign.Center,
			modifier = Modifier.padding(bottom = 24.dp)
		)

		// Поля ввода
		if (mode == "register") {
			OutlinedTextField(
				value = name,
				onValueChange = { name = it },
				label = { Text("Имя") },
				isError = !isNameValid && name.isNotEmpty(),
				modifier = Modifier
					.fillMaxWidth(0.8f)
					.padding(bottom = 8.dp)
			)
			if (!isNameValid && name.isNotEmpty()) {
				Text(
					text = "Введите имя",
					modifier = Modifier.padding(bottom = 8.dp),
					textAlign = TextAlign.Center
				)
			}
		}

		OutlinedTextField(
			value = email,
			onValueChange = { email = it },
			label = { Text("Email") },
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
			isError = email.isNotEmpty() && !isEmailValid,
			modifier = Modifier
				.fillMaxWidth(0.8f)
				.padding(bottom = 8.dp)
		)
		if (email.isNotEmpty() && !isEmailValid) {
			Text(
				text = "Неверный email",
				modifier = Modifier.padding(bottom = 8.dp),
				textAlign = TextAlign.Center
			)
		}

		OutlinedTextField(
			value = password,
			onValueChange = { password = it },
			label = { Text("Пароль (мин. 6 символов)") },
			visualTransformation = PasswordVisualTransformation(),
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
			isError = password.isNotEmpty() && !isPasswordValid,
			modifier = Modifier
				.fillMaxWidth(0.8f)
				.padding(bottom = 8.dp)
		)
		if (password.isNotEmpty() && !isPasswordValid) {
			Text(
				text = "Слишком короткий пароль",
				modifier = Modifier.padding(bottom = 8.dp),
				textAlign = TextAlign.Center
			)
		}

		if (mode == "register") {
			OutlinedTextField(
				value = confirmPassword,
				onValueChange = { confirmPassword = it },
				label = { Text("Повторите пароль") },
				visualTransformation = PasswordVisualTransformation(),
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
				isError = confirmPassword.isNotEmpty() && !isConfirmValid,
				modifier = Modifier
					.fillMaxWidth(0.8f)
					.padding(bottom = 8.dp)
			)
			if (confirmPassword.isNotEmpty() && !isConfirmValid) {
				Text(
					text = "Пароли не совпадают",
					modifier = Modifier.padding(bottom = 8.dp),
					textAlign = TextAlign.Center
				)
			}
		}

		// Кнопки
		Button(
			enabled = isFormValid,
			onClick = {
				scope.launch {
					status = ""
					try {
						if (mode == "register") {
                            // Переходим на экран подтверждения email
							navController.navigate("verify-email/${email.trim()}")
						} else {
							val r = api.login(LoginRequest(email.trim(), password))
							sessionStore.save(token = r.token, name = r.user.name)
							navController.navigate("home") { popUpTo("home") { inclusive = true } }
						}
					} catch (e: Exception) {
						status = e.message ?: "Ошибка"
					}
				}
			},
			modifier = Modifier
				.fillMaxWidth(0.8f)
				.padding(bottom = 8.dp)
		) { 
			Text(if (mode == "register") "Зарегистрироваться" else "Войти") 
		}

		Button(
			onClick = { mode = if (mode == "register") "login" else "register" },
			modifier = Modifier
				.fillMaxWidth(0.8f)
				.padding(bottom = 8.dp)
		) { 
			Text(if (mode == "register") "У меня есть аккаунт" else "Создать аккаунт") 
		}
		
		// Кнопка "Забыли пароль?" только в режиме входа
		if (mode == "login") {
			Button(
				onClick = { navController.navigate("forgot-password") },
				modifier = Modifier
					.fillMaxWidth(0.8f)
					.padding(bottom = 8.dp)
			) { 
				Text("Забыли пароль?") 
			}
		}

		// Статус
		if (status.isNotEmpty()) {
			Text(
				text = status,
				modifier = Modifier.padding(top = 8.dp),
				textAlign = TextAlign.Center
			)
		}
	}
}
