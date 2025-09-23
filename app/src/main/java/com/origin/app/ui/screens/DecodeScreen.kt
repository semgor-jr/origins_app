package com.origin.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.origin.app.data.ApiClient
import com.origin.app.data.CreateReportRequest
import com.origin.app.data.DecodingMethod
import com.origin.app.data.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import com.origin.app.data.SessionStore

@Composable
fun DecodeScreen(navController: NavController) {
	val session = remember { SessionStore(navController.context) }
	val token = session.token.collectAsState(initial = null).value
	val api = remember(token) { ApiClient(tokenProvider = { token }) }
	
	// Состояние формы
	var reportName by remember { mutableStateOf("") }
	var reportDescription by remember { mutableStateOf("") }
	var selectedMethod by remember { mutableStateOf<DecodingMethod?>(null) }
	var pickedFile by remember { mutableStateOf<Uri?>(null) }
	var fileName by remember { mutableStateOf("") }
	var isLoading by remember { mutableStateOf(false) }
	var loadingStep by remember { mutableStateOf("") }
	var loadingProgress by remember { mutableFloatStateOf(0f) }
	var error by remember { mutableStateOf("") }
	var decodingMethods by remember { mutableStateOf<List<DecodingMethod>>(emptyList()) }
	var userData by remember { mutableStateOf<UserDto?>(null) }
	// var showConfirmationDialog by remember { mutableStateOf(false) } // Убрано - теперь автоматическая перезапись
	
	val scope = rememberCoroutineScope()
	
	// Загружаем доступные методы расшифровки и данные пользователя
	LaunchedEffect(token) {
		if (token != null) {
			try {
				decodingMethods = api.getDecodingMethods()
				userData = api.getMe()
			} catch (e: Exception) {
				error = "Ошибка загрузки данных: ${e.message}"
			}
		}
	}
	
	// Launcher для выбора файла
	val fileLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.GetContent(),
		onResult = { uri ->
			pickedFile = uri
			if (uri != null) {
				scope.launch {
					fileName = withContext(Dispatchers.IO) {
						var name = "report.vcf"
						navController.context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
							val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
							if (cursor.moveToFirst() && idx >= 0) {
								name = cursor.getString(idx)
							}
						}
						name
					}
				}
			}
		}
	)
	
	// Функция создания отчета
	val performCreateReport: () -> Unit = {
		scope.launch {
			isLoading = true
			error = ""
			loadingProgress = 0f
			try {
				// Шаг 1: Создаем отчет
				loadingStep = "Создаю отчет..."
				loadingProgress = 0.1f
				val request = CreateReportRequest(
					name = reportName,
					description = reportDescription,
					decodingMethod = selectedMethod!!.id
				)
				val report = api.createReport(request)
				
				// Шаг 2: Читаем файл
				loadingStep = "Читаю VCF файл..."
				loadingProgress = 0.3f
				val fileBytes = withContext(Dispatchers.IO) {
					navController.context.contentResolver.openInputStream(pickedFile!!)!!.readBytes()
				}
				
				// Шаг 3: Загружаем файл на сервер
				loadingStep = "Загружаю файл на сервер..."
				loadingProgress = 0.5f
				val finalReport = api.uploadVcfToReport(report.id, fileBytes, fileName)
				
				// Шаг 4: Анализируем данные
				loadingStep = "Анализирую генетические данные..."
				loadingProgress = 0.7f
				// Небольшая задержка для демонстрации процесса анализа
				kotlinx.coroutines.delay(2000)
				
				// Шаг 5: Обновляем данные пользователя
				loadingStep = "Обновляю профиль..."
				loadingProgress = 0.9f
				try {
					userData = api.getMe()
				} catch (e: Exception) {
					println("Ошибка обновления данных пользователя: ${e.message}")
				}
				
				// Шаг 6: Завершение
				loadingStep = "Отчет готов!"
				loadingProgress = 1.0f
				kotlinx.coroutines.delay(500)
				
				// Переходим к визуализации
				println("DecodeScreen: Navigating to report ${finalReport.id}")
				navController.navigate("visual/${finalReport.id}")
			} catch (e: Exception) {
				error = "Ошибка создания отчета: ${e.message}"
			} finally {
				isLoading = false
				loadingStep = ""
				loadingProgress = 0f
			}
		}
	}
	
	val createReport: () -> Unit = {
		if (reportName.isBlank()) {
			error = "Введите название отчета"
		} else if (selectedMethod == null) {
			error = "Выберите метод расшифровки"
		} else if (pickedFile == null) {
			error = "Выберите VCF файл"
		} else {
			// Теперь всегда создаем/обновляем отчет без диалога подтверждения
			performCreateReport()
		}
	}
	
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp)
			.verticalScroll(rememberScrollState()),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		Text(
			text = "Создать новый отчет",
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.Bold,
			textAlign = TextAlign.Center
		)
		
		// Название отчета
		OutlinedTextField(
			value = reportName,
			onValueChange = { reportName = it },
			label = { Text("Название отчета") },
			placeholder = { Text("Например: Мой генетический анализ") },
			modifier = Modifier.fillMaxWidth(),
			singleLine = true
		)
		
		// Описание отчета
		OutlinedTextField(
			value = reportDescription,
			onValueChange = { reportDescription = it },
			label = { Text("Описание (необязательно)") },
			placeholder = { Text("Дополнительная информация об отчете") },
			modifier = Modifier.fillMaxWidth(),
			minLines = 3,
			maxLines = 5
		)
		
		// Выбор метода расшифровки
		Text(
			text = "Метод расшифровки",
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.Medium
		)
		
		decodingMethods.forEach { method ->
			val isCompleted = when (method.id) {
				"autosomal_analysis" -> userData?.autosomalData?.isNotEmpty() == true
				"y_haplogroup" -> userData?.yHaplogroup != null
				"mt_haplogroup" -> userData?.mtHaplogroup != null
				else -> false
			}
			
			Card(
				modifier = Modifier.fillMaxWidth(),
				shape = RoundedCornerShape(12.dp),
				colors = CardDefaults.cardColors(
					containerColor = when {
						isCompleted -> Color(0xFFE8F5E8) // Зеленый для завершенных
						selectedMethod?.id == method.id -> MaterialTheme.colorScheme.primaryContainer
						else -> MaterialTheme.colorScheme.surface
					}
				),
				onClick = { selectedMethod = method }
			) {
				Column(
					modifier = Modifier.padding(16.dp)
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween,
						modifier = Modifier.fillMaxWidth()
					) {
						Text(
							text = method.name,
							style = MaterialTheme.typography.titleSmall,
							fontWeight = FontWeight.Medium
						)
						if (isCompleted) {
							Icon(
								Icons.Default.Check,
								contentDescription = "Завершено",
								tint = Color(0xFF2E7D32),
								modifier = Modifier.size(20.dp)
							)
						}
					}
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = method.description,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					if (isCompleted) {
						Spacer(modifier = Modifier.height(4.dp))
						Text(
							text = "✓ Анализ завершен",
							style = MaterialTheme.typography.labelSmall,
							color = Color(0xFF2E7D32),
							fontWeight = FontWeight.Medium
						)
					}
				}
			}
		}
		
		
		OutlinedButton(
			onClick = { fileLauncher.launch("*/*") },
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(12.dp)
		) {
			Icon(
				imageVector = Icons.Default.Upload,
				contentDescription = null,
				modifier = Modifier.size(20.dp)
			)
			Spacer(modifier = Modifier.width(8.dp))
			Text(
				text = fileName.ifEmpty { "Выберите VCF файл" }
			)
		}
		
		// Ошибка
		if (error.isNotEmpty()) {
			Card(
				modifier = Modifier.fillMaxWidth(),
				colors = CardDefaults.cardColors(
					containerColor = MaterialTheme.colorScheme.errorContainer
				)
			) {
				Text(
					text = error,
					modifier = Modifier.padding(16.dp),
					color = MaterialTheme.colorScheme.onErrorContainer
				)
			}
		}
		
		// Кнопка создания отчета
		Button(
			onClick = createReport,
			modifier = Modifier.fillMaxWidth(),
			enabled = !isLoading,
			shape = RoundedCornerShape(12.dp)
		) {
			if (isLoading) {
				CircularProgressIndicator(
					modifier = Modifier.size(20.dp),
					color = MaterialTheme.colorScheme.onPrimary
				)
				Spacer(modifier = Modifier.width(8.dp))
				Text("Создаю отчет...")
			} else {
				Text("Создать отчет")
			}
		}
		
		// Детальная информация о процессе загрузки
		if (isLoading && loadingStep.isNotEmpty()) {
			Card(
				modifier = Modifier.fillMaxWidth(),
				colors = CardDefaults.cardColors(
					containerColor = MaterialTheme.colorScheme.primaryContainer
				),
				shape = RoundedCornerShape(12.dp)
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Text(
						text = "Обработка отчета",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onPrimaryContainer
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = loadingStep,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onPrimaryContainer,
						textAlign = TextAlign.Center
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "${(loadingProgress * 100).toInt()}%",
						style = MaterialTheme.typography.bodyLarge,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.primary
					)
					Spacer(modifier = Modifier.height(12.dp))
					LinearProgressIndicator(
						progress = loadingProgress,
						modifier = Modifier.fillMaxWidth(),
						color = MaterialTheme.colorScheme.primary,
						trackColor = MaterialTheme.colorScheme.surface
					)
				}
			}
		}
	}
	
	// Диалог подтверждения убран - теперь автоматическая перезапись
}