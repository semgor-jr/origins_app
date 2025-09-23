package com.origin.app.data

import com.origin.app.BuildConfig

/**
 * Конфигурация приложения для разных окружений
 */
object AppConfig {
    
    /**
     * Базовый URL API сервера
     */
    val apiBaseUrl: String
        get() = BuildConfig.API_BASE_URL

    /**
     * Режим отладки
     */
    val isDebugMode: Boolean
        get() = BuildConfig.DEBUG_MODE

    /**
     * Таймауты для API запросов
     */
    val requestTimeout: Long
        get() = if (isDebugMode) 300_000L else 60_000L // 5 минут для отладки, 1 минута для продакшена
    
    val connectTimeout: Long
        get() = if (isDebugMode) 60_000L else 30_000L // 1 минута для отладки, 30 секунд для продакшена

}

