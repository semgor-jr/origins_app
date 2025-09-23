package com.origin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import com.origin.app.data.ApiClient
import com.origin.app.data.ReportDto
import com.origin.app.data.SessionStore
import com.origin.app.data.UserDto
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
fun ProfileScreen(navController: NavController) {
	val session = remember { SessionStore(navController.context) }
	val token by session.token.collectAsState(initial = null)
	val scope = rememberCoroutineScope()
    val api = remember(token) { ApiClient(tokenProvider = { token }) }
	val nameState = remember { mutableStateOf("") }
	val userState = remember { mutableStateOf<UserDto?>(null) }
	val reportsState = remember { mutableStateOf<List<ReportDto>>(emptyList()) }
	val status = remember { mutableStateOf("") }

	// Функция для обновления данных
	val refreshData = {
		if (token != null) {
			scope.launch {
				try {
					val me = api.getMe()
					nameState.value = me.name
					userState.value = me
					reportsState.value = api.getReports()
				} catch (e: Exception) { 
					status.value = e.message ?: "Ошибка загрузки данных"
				}
			}
		}
	}

	LaunchedEffect(token) {
		refreshData()
	}
	
	// Обновляем данные при возврате на экран
	DisposableEffect(navController) {
		val listener = OnDestinationChangedListener { _, destination, _ ->
			if (destination.route == "profile") {
				refreshData()
			}
		}
		navController.addOnDestinationChangedListener(listener)
		onDispose {
			navController.removeOnDestinationChangedListener(listener)
		}
	}

	Box(Modifier.fillMaxSize()) {
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 16.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			contentPadding = PaddingValues(bottom = 80.dp)
		) {
			item {
				Spacer(Modifier.height(12.dp))
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Spacer(Modifier.width(40.dp)) // Для центрирования
					Text("Профиль", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
					IconButton(onClick = refreshData) {
						Icon(Icons.Default.Refresh, contentDescription = "Обновить данные")
					}
				}
				Spacer(Modifier.height(16.dp))
			}

			// Основная информация профиля
			item {
				Card(
					modifier = Modifier.fillMaxWidth(),
					elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
				) {
					Column(Modifier.padding(16.dp)) {
						Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier.padding(bottom = 16.dp)
						) {
							Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp))
							Spacer(Modifier.width(8.dp))
							Text("Основная информация", style = MaterialTheme.typography.titleMedium)
						}
						
						OutlinedTextField(
							value = nameState.value, 
							onValueChange = { nameState.value = it }, 
							label = { Text("Имя") },
							modifier = Modifier.fillMaxWidth()
						)
						Spacer(Modifier.height(8.dp))
						Button(
							onClick = {
								try {
									runBlocking {
										val updated = api.updateMe(nameState.value.trim())
										session.save(token = token, name = updated.name)
									}
									status.value = "Сохранено"
								} catch (e: Exception) { status.value = e.message ?: "Ошибка сохранения" }
							},
							modifier = Modifier.fillMaxWidth()
						) { Text("Сохранить") }
					}
				}
				Spacer(Modifier.height(16.dp))
			}

			// Данные генетического анализа
			userState.value?.let { user ->
				item {
					Card(
						modifier = Modifier.fillMaxWidth(),
						elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
					) {
						Column(Modifier.padding(16.dp)) {
							Text(
								text = "Генетический паспорт", 
								style = MaterialTheme.typography.titleMedium,
								textAlign = TextAlign.Center,
								modifier = Modifier.fillMaxWidth()
							)
							Spacer(Modifier.height(12.dp))

							// Аутосомный анализ
							if (user.autosomalData.isNotEmpty()) {
								Card(
									colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
									modifier = Modifier.fillMaxWidth()
								) {
									Column(Modifier.padding(12.dp)) {
										Text(
											text = "🧬 Аутосомный анализ", 
											style = MaterialTheme.typography.titleSmall, 
											fontWeight = FontWeight.Bold,
											textAlign = TextAlign.Center,
											modifier = Modifier.fillMaxWidth()
										)
										Spacer(Modifier.height(8.dp))
										LazyRow(
											horizontalArrangement = Arrangement.spacedBy(8.dp)
										) {
											items(user.autosomalData) { origin ->
												Card(
													colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
													modifier = Modifier.padding(4.dp)
												) {
													Text(
														text = "${origin.region}: ${origin.percent}%",
														modifier = Modifier.padding(8.dp),
														style = MaterialTheme.typography.labelMedium
													)
												}
											}
										}
									}
								}
								Spacer(Modifier.height(12.dp))
							}

							// Y-гаплогруппа
							user.yHaplogroup?.let { yHaplo ->
								Card(
									colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
									modifier = Modifier.fillMaxWidth()
								) {
									Column(Modifier.padding(12.dp)) {
										Text(
											text = "👨 Y-гаплогруппа", 
											style = MaterialTheme.typography.titleSmall, 
											fontWeight = FontWeight.Bold,
											textAlign = TextAlign.Center,
											modifier = Modifier.fillMaxWidth()
										)
										Spacer(Modifier.height(4.dp))
										Text(
											text = yHaplo, 
											style = MaterialTheme.typography.bodyLarge,
											textAlign = TextAlign.Center,
											modifier = Modifier.fillMaxWidth()
										)
									}
								}
								Spacer(Modifier.height(12.dp))
							}

							// мт-гаплогруппа
							user.mtHaplogroup?.let { mtHaplo ->
								Card(
									colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
									modifier = Modifier.fillMaxWidth()
								) {
									Column(Modifier.padding(12.dp)) {
										Text(
											text = "👩 мт-гаплогруппа", 
											style = MaterialTheme.typography.titleSmall, 
											fontWeight = FontWeight.Bold,
											textAlign = TextAlign.Center,
											modifier = Modifier.fillMaxWidth()
										)
										Spacer(Modifier.height(4.dp))
										Text(
											text = mtHaplo, 
											style = MaterialTheme.typography.bodyLarge,
											textAlign = TextAlign.Center,
											modifier = Modifier.fillMaxWidth()
										)
									}
								}
								Spacer(Modifier.height(12.dp))
							}

							// Кнопка экспорта PDF
							if (user.autosomalData.isNotEmpty() || user.yHaplogroup != null || user.mtHaplogroup != null) {
								Button(
									onClick = { 
										//Реализовать экспорт PDF
										status.value = "Экспорт PDF в разработке"
									},
									modifier = Modifier.fillMaxWidth()
								) {
									Icon(Icons.Default.Download, contentDescription = null)
									Spacer(Modifier.width(8.dp))
									Text("Экспортировать в PDF")
								}
							} else {
								Text(
									"Данные анализа отсутствуют. Создайте отчет для получения результатов.",
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									textAlign = TextAlign.Center,
									modifier = Modifier.fillMaxWidth()
								)
							}
						}
					}
					Spacer(Modifier.height(16.dp))
				}
			}

			// Отчеты
			item {
				Text("Мои отчеты", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
				Spacer(Modifier.height(8.dp))
			}

			items(reportsState.value) { r ->
				Card(
					modifier = Modifier
						.widthIn(max = 720.dp)
						.fillMaxWidth(0.95f)
						.padding(vertical = 6.dp),
					elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
					onClick = { 
						println("ProfileScreen: Navigating to report ${r.id}")
						navController.navigate("visual/${r.id}") 
					}
				) {
					Column(Modifier.padding(12.dp)) {
						Text(r.name ?: "Без названия", style = MaterialTheme.typography.titleSmall)
					}
				}
			}

			// Статус
			if (status.value.isNotEmpty()) {
				item {
					Spacer(Modifier.height(16.dp))
					Text(status.value, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
				}
			}

			// Кнопка "Настройки"
			item {
				Spacer(Modifier.height(24.dp))
				Button(
					onClick = { 
						navController.navigate("settings")
					},
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp)
				) { 
					Icon(Icons.Default.Settings, contentDescription = null)
					Spacer(Modifier.width(8.dp))
					Text("Настройки") 
				}
				Spacer(Modifier.height(8.dp))
			}

			// Кнопка "Выйти"
			item {
				Button(
					onClick = { 
						scope.launch { 
							session.clearSession()
							navController.navigate("auth") { 
								popUpTo("home") { inclusive = true } 
							}
						}
					},
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp)
				) { 
					Text("Выйти") 
				}
				Spacer(Modifier.height(16.dp))
			}
		}
	}
}
