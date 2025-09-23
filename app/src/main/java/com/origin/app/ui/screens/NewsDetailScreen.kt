package com.origin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.origin.app.data.ApiClient
import com.origin.app.data.NewsDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(navController: NavController) {
	val entry = navController.currentBackStackEntryAsState().value
	val id = entry?.arguments?.getString("id") ?: ""
	
	var news by remember { mutableStateOf<NewsDto?>(null) }
	var isLoading by remember { mutableStateOf(true) }
	var error by remember { mutableStateOf<String?>(null) }

	LaunchedEffect(id) {
		if (id.isNotEmpty()) {
			try {
				val apiClient = ApiClient()
				news = apiClient.getNewsById(id)
				isLoading = false
			} catch (e: Exception) {
				error = e.message
				isLoading = false
			}
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(news?.title ?: "Загрузка...") },
				navigationIcon = {
					IconButton(onClick = { navController.popBackStack() }) {
						Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
					}
				}
			)
		}
	) { padding ->
		when {
			isLoading -> {
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator()
				}
			}
			error != null -> {
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center
				) {
					Text(
						text = "Ошибка загрузки новости: $error",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.error
					)
				}
			}
			news != null -> {
				Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
					AsyncImage(
						model = news!!.imageUrl ?: "https://picsum.photos/seed/${news!!.id}/1200/600",
						contentDescription = news!!.title,
						contentScale = ContentScale.Crop,
						modifier = Modifier.height(220.dp)
					)
					Spacer(Modifier.height(8.dp))
					Text(
						news!!.publishedAt,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					news!!.source?.let { source ->
						Text(
							"Источник: $source",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
					Spacer(Modifier.height(12.dp))
					Text(
						news!!.content,
						style = MaterialTheme.typography.bodyLarge
					)
				}
			}
		}
	}
}
