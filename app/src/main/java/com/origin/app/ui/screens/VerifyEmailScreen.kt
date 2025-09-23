package com.origin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.origin.app.data.ApiClient
import com.origin.app.data.SessionStore
import com.origin.app.data.VerifyEmailRequest
import kotlinx.coroutines.launch

@Composable
fun VerifyEmailScreen(
    navController: NavController,
    sessionStore: SessionStore,
    email: String
) {
    var verificationCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient() }

    val isCodeValid = verificationCode.length == 6 && verificationCode.all { it.isDigit() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Заголовок
        Text(
            text = "Подтверждение email",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Мы отправили код подтверждения на:",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Поле ввода кода
        OutlinedTextField(
            value = verificationCode,
            onValueChange = { 
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                    verificationCode = it
                }
            },
            label = { Text("Код подтверждения") },
            placeholder = { Text("123456") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = verificationCode.isNotEmpty() && !isCodeValid,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 16.dp)
        )
        
        if (verificationCode.isNotEmpty() && !isCodeValid) {
            Text(
                text = "Введите 6-значный код",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        }
        
        // Кнопка подтверждения
        Button(
            enabled = isCodeValid && !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    error = ""
                    success = ""
                    
                    try {
                        val response = api.verifyEmail(VerifyEmailRequest(email, verificationCode))
                        success = "Email успешно подтвержден! Теперь вы можете войти в систему."
                        
                        // Небольшая задержка для показа сообщения
                        kotlinx.coroutines.delay(1000)
                        
                        // Переходим на страницу входа
                        navController.navigate("auth/login") { 
                            popUpTo("verify-email") { inclusive = true } 
                        }
                        
                    } catch (e: Exception) {
                        error = e.message ?: "Ошибка подтверждения email"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Подтвердить")
        }
        
        // Кнопка повторной отправки кода
        TextButton(
            onClick = {
                // TODO: Реализовать повторную отправку кода
                error = "Обратитесь к администратору для повторной отправки кода"
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Не получили код? Отправить повторно")
        }
        
        // Кнопка назад
        TextButton(
            onClick = {
                navController.popBackStack()
            }
        ) {
            Text("Назад к регистрации")
        }
        
        // Сообщения об ошибках и успехе
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        
        if (success.isNotEmpty()) {
            Text(
                text = success,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

