package com.origin.server

import java.util.concurrent.ThreadLocalRandom

object EmailService {
    
    /**
     * Генерирует 6-значный код подтверждения
     */
    fun generateVerificationCode(): String {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000))
    }
    
    /**
     * Отправляет код подтверждения на email
     * В продакшене здесь будеь вызов SMTP сервера
     */
    fun sendVerificationCode(email: String, code: String) {
        // Для демонстрации выводим код в консоль
        println("=== EMAIL VERIFICATION CODE ===")
        println("To: $email")
        println("Subject: Код подтверждения регистрации")
        println("Your verification code: $code")
        println("Code expires in 10 minutes")
        println("===============================")
    }
    
    /**
     * Проверяет, является ли строка валидным email
     */
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        return emailRegex.matches(email)
    }
}

