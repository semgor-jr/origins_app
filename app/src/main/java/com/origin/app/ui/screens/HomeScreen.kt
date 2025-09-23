package com.origin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import com.origin.app.data.ApiClient
import com.origin.app.data.NewsDto

@Composable
fun HomeScreen(navController: NavController) {
	var news by remember { mutableStateOf<List<NewsDto>>(emptyList()) }
	var isLoading by remember { mutableStateOf(true) }
	var error by remember { mutableStateOf<String?>(null) }

	LaunchedEffect(Unit) {
		try {
			val apiClient = ApiClient()
			news = apiClient.getNews()
			isLoading = false
		} catch (e: Exception) {
			error = e.message
			isLoading = false
		}
	}
	Box(modifier = Modifier.fillMaxSize()) {
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
						text = "Ошибка загрузки новостей: $error",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.error
					)
				}
			}
			else -> {
				LazyColumn(
					modifier = Modifier.fillMaxSize(),
					horizontalAlignment = Alignment.CenterHorizontally,
					contentPadding = PaddingValues(vertical = 12.dp)
				) {
					items(news) { item ->
						Card(
							modifier = Modifier
								.widthIn(max = 720.dp)
								.fillMaxWidth(0.95f)
								.padding(vertical = 8.dp),
							shape = RoundedCornerShape(16.dp),
							elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
							onClick = { navController.navigate("news/${item.id}") }
						) {
							SubcomposeAsyncImage(
								model = item.imageUrl ?: "https://picsum.photos/seed/${item.id}/800/400",
								contentDescription = item.title,
								modifier = Modifier
									.fillMaxWidth()
									.height(200.dp)
									.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
								contentScale = ContentScale.Crop,
								loading = {
									Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
										CircularProgressIndicator()
									}
								}
							)
							Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
								Text(
									text = item.title,
									style = MaterialTheme.typography.titleMedium,
									maxLines = 2,
									overflow = TextOverflow.Ellipsis
								)
								Spacer(Modifier.height(4.dp))
								Text(
									text = item.content,
									style = MaterialTheme.typography.bodyMedium,
									maxLines = 2,
									overflow = TextOverflow.Ellipsis
								)
							}
						}
					}
				}
			}
		}
	}
}
