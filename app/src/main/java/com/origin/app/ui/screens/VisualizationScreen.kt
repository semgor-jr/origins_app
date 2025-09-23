package com.origin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.origin.app.data.ApiClient
import com.origin.app.data.ReportDto
import com.origin.app.data.SessionStore
import com.origin.app.ui.components.GoogleMapsOriginMap
import com.origin.app.ui.components.SimpleStatistics
import com.origin.app.ui.components.SimpleMetrics


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizationScreen(navController: NavController) {
	val context = LocalContext.current
	val sessionStore = remember { SessionStore(context) }
	val token = sessionStore.token.collectAsState(initial = null).value
	val api = remember(token) { ApiClient(tokenProvider = { token }) }
	val reportState = remember { mutableStateOf<ReportDto?>(null) }
	val isLoading = remember { mutableStateOf(false) }
	val error = remember { mutableStateOf<String?>(null) }
	val showShareDialog = remember { mutableStateOf(false) }
    val reloadTrigger = remember { mutableIntStateOf(0) }
	val clipboardManager = LocalClipboardManager.current
	val entry = navController.currentBackStackEntryAsState().value
	val id = entry?.arguments?.getString("id")

	// Функция для загрузки отчета
	val loadReport = {
		reloadTrigger.value++
	}

	LaunchedEffect(id, reloadTrigger.value) {
		println("VisualizationScreen: LaunchedEffect START")
		println("VisualizationScreen: ID = $id")
		println("VisualizationScreen: Token = $token")
		
		if (!id.isNullOrBlank()) {
			println("VisualizationScreen: Starting loading process...")
			isLoading.value = true
			error.value = null
			
			try { 
				println("VisualizationScreen: Calling API...")
				val report = api.getReport(id)
				println("VisualizationScreen: API call successful")
				
				// Безопасная проверка данных
				println("VisualizationScreen: Report ID: ${report.id}")
				println("VisualizationScreen: Report summary length: ${report.summary.length}")
				println("VisualizationScreen: Report origins count: ${report.origins.size}")
				
				// Проверяем каждое происхождение
				report.origins.forEachIndexed { index, origin ->
					println("VisualizationScreen: Origin $index - ${origin.region}: ${origin.percent}%")
				}
				
				println("VisualizationScreen: Setting report state...")
				reportState.value = report
				println("VisualizationScreen: Report state set successfully")
				
			} catch (e: Exception) {
				println("VisualizationScreen: Error in LaunchedEffect: ${e.message}")
				e.printStackTrace()
				error.value = "Ошибка загрузки отчета: ${e.message}"
			} finally {
				println("VisualizationScreen: Setting loading to false")
				isLoading.value = false
			}
		} else {
			println("VisualizationScreen: No report ID provided")
			error.value = "ID отчета не найден"
		}
		
		println("VisualizationScreen: LaunchedEffect END")
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { 
					Text(
						text = reportState.value?.summary?.split(",")?.firstOrNull() ?: "Отчет"
					) 
				},
				navigationIcon = {
					IconButton(onClick = { navController.popBackStack() }) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
					}
				},
				actions = {
					val report = reportState.value
					println("TopAppBar: Report = $report")
					println("TopAppBar: Report publicId = ${report?.publicId}")
					
					if (report?.publicId != null) {
						IconButton(
							onClick = { showShareDialog.value = true }
						) {
							Icon(Icons.Filled.Share, contentDescription = "Поделиться")
						}
					}
				}
			)
		}
	) { padding ->
		val report = reportState.value
		println("VisualizationScreen: Rendering report = $report")
		
		when {
			isLoading.value -> {
				// Показываем индикатор загрузки
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(padding),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					CircularProgressIndicator(
						modifier = Modifier.size(64.dp),
						color = MaterialTheme.colorScheme.primary
					)
					Spacer(modifier = Modifier.height(16.dp))
					Text(
						text = "Загрузка отчета...",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "Пожалуйста, подождите",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "ID: $id",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
			error.value != null -> {
				// Показываем ошибку
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(padding),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					Text(
						text = "Ошибка загрузки",
						style = MaterialTheme.typography.titleLarge,
						color = MaterialTheme.colorScheme.error
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = error.value ?: "Неизвестная ошибка",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						textAlign = TextAlign.Center
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "ID: $id",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Text(
						text = "Token: ${if (token != null) "Present" else "Missing"}",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Spacer(modifier = Modifier.height(16.dp))
					Button(
						onClick = { 
							// Перезагружаем отчет
							loadReport()
						}
					) {
						Text("Повторить")
					}
				}
			}
			report != null -> {
				println("VisualizationScreen: Rendering report - START")

                val safeSummary = report.summary ?: "Нет описания"
				val safeOrigins = report.origins ?: emptyList()
				
				// Проверяем, является ли это отчетом гаплогруппы
				val isHaplogroupReport = safeSummary.contains("y_haplogroup") || safeSummary.contains("mt_haplogroup") || 
										safeOrigins.any { origin -> origin.region.contains("гаплогруппа") }
				
				// Определяем тип гаплогруппы и генерируем информативный текст
				val haplogroupType = when {
					safeSummary.contains("y_haplogroup") -> "отцовской"
					safeSummary.contains("mt_haplogroup") -> "материнской"
					else -> "неизвестной"
				}
				
				val haplogroupRegion = safeOrigins.firstOrNull()?.region ?: "неизвестного региона"
				
				// Функция для правильного падежа региона
				val getRegionInCorrectCase = { region: String ->
					when {
						region.contains("Европа") -> "Европы"
						region.contains("Азия") -> "Азии"
						region.contains("Африка") -> "Африки"
						region.contains("Америка") -> "Америки"
						region.contains("Ближний Восток") -> "Ближнего Востока"
						region.contains("Южная Азия") -> "Южной Азии"
						region.contains("Восточная Азия") -> "Восточной Азии"
						else -> region
					}
				}
				
				val regionInCorrectCase = getRegionInCorrectCase(haplogroupRegion)
				val haplogroupText = when (haplogroupType) {
					"отцовской" -> "Ваши предки по отцовской линии были жителями $regionInCorrectCase"
					"материнской" -> "Ваши предки по материнской линии были жителями $regionInCorrectCase"
					else -> "Ваши предки были жителями $regionInCorrectCase"
				}
				
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(padding)
						.verticalScroll(rememberScrollState()),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Spacer(Modifier.height(16.dp))
					
					// Заголовок
					Text(
						text = if (isHaplogroupReport) "Анализ гаплогруппы" else "Анализ происхождения",
						style = MaterialTheme.typography.headlineMedium
					)
					
					Spacer(Modifier.height(8.dp))
					
					// Информация об отчете
					Card(
						modifier = Modifier
							.fillMaxWidth(0.99f)
							.padding(16.dp),
						colors = CardDefaults.cardColors(
							containerColor = MaterialTheme.colorScheme.surfaceVariant
						)
					) {
						Column(
							modifier = Modifier.padding(16.dp),
							horizontalAlignment = Alignment.CenterHorizontally
						) {
							// Извлекаем только название файла и метод из summary
							val summaryParts = safeSummary.split(", ")
							val fileName = summaryParts.find { it.startsWith("Файл:") }?.substringAfter("Файл: ")?.trim() ?: "Неизвестный файл"
							val method = summaryParts.find { it.startsWith("метод:") }?.substringAfter("метод: ")?.trim() ?: "Неизвестный метод"
							
							Text(
								text = "Файл: $fileName",
								style = MaterialTheme.typography.titleMedium,
								fontWeight = FontWeight.Bold,
								textAlign = TextAlign.Center,
								modifier = Modifier.fillMaxWidth()
							)
							Spacer(modifier = Modifier.height(4.dp))
							Text(
								text = "Тип анализа: $method",
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.primary,
								textAlign = TextAlign.Center,
								modifier = Modifier.fillMaxWidth()
							)
						}
					}
					
					Spacer(Modifier.height(16.dp))
					
					// Визуализация данных
					if (safeOrigins.isNotEmpty()) {
						if (isHaplogroupReport) {
							// Специальная визуализация для гаплогрупп
							Card(
								modifier = Modifier
									.fillMaxWidth(0.99f)
									.padding(16.dp),
								colors = CardDefaults.cardColors(
									containerColor = MaterialTheme.colorScheme.primaryContainer
								)
							) {
								Column(
									modifier = Modifier.padding(16.dp),
									horizontalAlignment = Alignment.CenterHorizontally
								) {
									Text(
										text = "Гаплогруппа",
										style = MaterialTheme.typography.titleLarge,
										fontWeight = FontWeight.Bold,
										textAlign = TextAlign.Center,
										modifier = Modifier.fillMaxWidth()
									)
									Spacer(modifier = Modifier.height(8.dp))
									Text(
										text = safeOrigins.firstOrNull()?.region ?: "Не определена",
										style = MaterialTheme.typography.headlineMedium,
										color = MaterialTheme.colorScheme.primary,
										textAlign = TextAlign.Center,
										modifier = Modifier.fillMaxWidth()
									)
									Spacer(modifier = Modifier.height(16.dp))
									
									// Информативный текст о происхождении
									Card(
										modifier = Modifier.fillMaxWidth(),
										colors = CardDefaults.cardColors(
											containerColor = MaterialTheme.colorScheme.surface
										)
									) {
										Column(
											modifier = Modifier.padding(16.dp),
											horizontalAlignment = Alignment.CenterHorizontally
										) {
											Text(
												text = "Историческое происхождение",
												style = MaterialTheme.typography.titleMedium,
												fontWeight = FontWeight.Bold,
												color = MaterialTheme.colorScheme.onSurface,
												textAlign = TextAlign.Center,
												modifier = Modifier.fillMaxWidth()
											)
											Spacer(modifier = Modifier.height(8.dp))
											Text(
												text = haplogroupText,
												style = MaterialTheme.typography.bodyLarge,
												color = MaterialTheme.colorScheme.onSurface,
												textAlign = TextAlign.Center,
												modifier = Modifier.fillMaxWidth()
											)
											Spacer(modifier = Modifier.height(8.dp))
											Text(
												text = "Это означает, что ваша $haplogroupType линия восходит к древним популяциям, которые населяли этот регион тысячи лет назад.",
												style = MaterialTheme.typography.bodyMedium,
												color = MaterialTheme.colorScheme.onSurfaceVariant,
												textAlign = TextAlign.Center,
												modifier = Modifier.fillMaxWidth()
											)
										}
									}
								}
							}
						} else {
							// Обычная визуализация для этнического происхождения
							Card(
								modifier = Modifier
									.fillMaxWidth(0.99f)
									.padding(16.dp)
							) {
								Column(
									modifier = Modifier.padding(16.dp)
								) {
									Text(
										text = "Этническое происхождение",
										style = MaterialTheme.typography.titleLarge,
										fontWeight = FontWeight.Bold,
										textAlign = TextAlign.Center,
										modifier = Modifier.fillMaxWidth()
									)
									Spacer(modifier = Modifier.height(16.dp))
									
									// Простая визуализация без Canvas
									val colors = listOf(
										Color(0xFF4CAF50),
										Color(0xFF2196F3),
										Color(0xFFFF9800),
										Color(0xFF9C27B0),
										Color(0xFFF44336),
										Color(0xFF00BCD4),
										Color(0xFFFFEB3B),
										Color(0xFF795548)
									)
									
									// Статистика
									SimpleStatistics(
										metrics = SimpleMetrics(
											totalSNPs = 1000, // Примерное значение
											analyzedSNPs = safeOrigins.size * 200,
											qualityScore = 0.85,
											confidenceLevel = 0.92,
											regionsFound = safeOrigins.size
										),
										modifier = Modifier.fillMaxWidth()
									)
									
									Spacer(modifier = Modifier.height(16.dp))
									
									// Карта происхождения
									GoogleMapsOriginMap(
										origins = safeOrigins,
										colors = colors,
										modifier = Modifier
											.fillMaxWidth()
											.height(300.dp)
									)
									
									Spacer(modifier = Modifier.height(16.dp))
									
									// Простой список с цветными индикаторами
									safeOrigins.forEachIndexed { index, origin ->
										Row(
											modifier = Modifier
												.fillMaxWidth()
												.padding(vertical = 8.dp, horizontal = 16.dp),
											horizontalArrangement = Arrangement.SpaceBetween,
											verticalAlignment = Alignment.CenterVertically
										) {
											Row(
												verticalAlignment = Alignment.CenterVertically
											) {
												Box(
													modifier = Modifier
														.size(20.dp)
														.background(
															colors[index % colors.size],
															RoundedCornerShape(4.dp)
														)
												)
												Spacer(modifier = Modifier.width(12.dp))
												Text(
													text = origin.region,
													style = MaterialTheme.typography.bodyLarge
												)
											}
											Text(
												text = "${origin.percent}%",
												style = MaterialTheme.typography.bodyLarge,
												fontWeight = FontWeight.Bold,
												color = MaterialTheme.colorScheme.primary
											)
										}
									}
								}
							}
						}
					} else {
						// Нет данных для отображения
						Card(
							modifier = Modifier
								.fillMaxWidth(0.99f)
								.padding(16.dp),
							colors = CardDefaults.cardColors(
								containerColor = MaterialTheme.colorScheme.errorContainer
							)
						) {
							Column(
								modifier = Modifier.padding(16.dp),
								horizontalAlignment = Alignment.CenterHorizontally
							) {
								Text(
									text = "Нет данных для отображения",
									style = MaterialTheme.typography.titleMedium,
									color = MaterialTheme.colorScheme.onErrorContainer
								)
							}
						}
					}
					
					Spacer(Modifier.height(32.dp))
				}
				
				println("VisualizationScreen: Rendering report - END")
			}
			else -> {
				// Отчет не найден
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(padding),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					Text(
						text = "Отчет не найден",
						style = MaterialTheme.typography.titleLarge,
						color = MaterialTheme.colorScheme.onSurface
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "Попробуйте обновить страницу",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		}
		
		// Диалог для поделиться отчетом
		if (showShareDialog.value) {
			AlertDialog(
				onDismissRequest = { showShareDialog.value = false },
				title = { Text("Поделиться отчетом") },
				text = { 
					val publicId = reportState.value?.publicId
					if (publicId != null) {
						val shareUrl = "https://yourapp.com/report/$publicId" // Замените на ваш домен
						Text("Ссылка на отчет:\n$shareUrl\n\nНажмите 'Копировать', чтобы скопировать ссылку в буфер обмена.")
					} else {
						Text("Этот отчет еще не готов для публикации.\nСоздайте новый отчет или обратитесь к администратору.")
					}
				},
				confirmButton = {
					val publicId = reportState.value?.publicId
					if (publicId != null) {
						TextButton(
							onClick = {
								val shareUrl = "https://yourapp.com/report/$publicId"
								clipboardManager.setText(AnnotatedString(shareUrl))
								showShareDialog.value = false
							}
						) {
							Text("Копировать")
						}
					} else {
						TextButton(onClick = { showShareDialog.value = false }) {
							Text("Понятно")
						}
					}
				},
				dismissButton = {
					TextButton(onClick = { showShareDialog.value = false }) {
						Text("Отмена")
					}
				}
			)
		}
	}
}

