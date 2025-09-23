package com.origin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.origin.app.data.ApiClient
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController
) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) } // 1 - ввод email, 2 - ввод пароля и кода
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient() }
    
    val isEmailValid = email.matches(Regex("^.+@.+\\..+$"))
    val isPasswordValid = newPassword.length >= 6
    val isConfirmValid = newPassword == confirmPassword && confirmPassword.isNotEmpty()
    val isCodeValid = verificationCode.length == 6
    val isFormValid = when (step) {
        1 -> isEmailValid
        2 -> isPasswordValid && isConfirmValid && isCodeValid
        else -> false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок с кнопкой назад
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            
            Text(
                text = "Восстановление пароля",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // Информационная карточка
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ℹ️ Информация",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (step == 1) 
                        "Введите email, на который зарегистрирован аккаунт. Мы отправим код для восстановления пароля."
                    else 
                        "Введите новый пароль и код подтверждения, который мы отправили на ваш email.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        if (step == 1) {
            // Шаг 1: Ввод email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                isError = email.isNotEmpty() && !isEmailValid,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (email.isNotEmpty() && !isEmailValid) {
                Text(
                    text = "Введите корректный email",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Button(
                enabled = isFormValid,
                onClick = {
                    scope.launch {
                        status = ""
                        try {
                            api.requestPasswordReset(email.trim())
                            status = "Код отправлен на ваш email"
                            step = 2
                        } catch (e: Exception) {
                            status = e.message ?: "Ошибка"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отправить код")
            }
        } else {
            // Шаг 2: Ввод нового пароля и кода
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Новый пароль") },
                visualTransformation = PasswordVisualTransformation(),
                isError = newPassword.isNotEmpty() && !isPasswordValid,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (newPassword.isNotEmpty() && !isPasswordValid) {
                Text(
                    text = "Пароль должен содержать минимум 6 символов",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Подтвердите пароль") },
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmPassword.isNotEmpty() && !isConfirmValid,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (confirmPassword.isNotEmpty() && !isConfirmValid) {
                Text(
                    text = "Пароли не совпадают",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { verificationCode = it },
                label = { Text("Код подтверждения") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                isError = verificationCode.isNotEmpty() && !isCodeValid,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (verificationCode.isNotEmpty() && !isCodeValid) {
                Text(
                    text = "Код должен содержать 6 цифр",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Button(
                enabled = isFormValid,
                onClick = {
                    scope.launch {
                        status = ""
                        try {
                            api.resetPassword(email.trim(), verificationCode, newPassword)
                            status = "Пароль успешно изменен! Теперь вы можете войти в систему."
                            // Переходим на экран входа через 2 секунды
                            kotlinx.coroutines.delay(2000)
                            navController.navigate("auth/login") {
                                popUpTo("forgot-password") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            status = e.message ?: "Ошибка"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Установить новый пароль")
            }
        }
        
        // Статус
        if (status.isNotEmpty()) {
            Text(
                text = status,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
                color = if (status.contains("успешно") || status.contains("отправлен")) 
                    MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.error
            )
        }
    }
}


