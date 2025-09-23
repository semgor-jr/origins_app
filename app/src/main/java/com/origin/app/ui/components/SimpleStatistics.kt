package com.origin.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class SimpleStatistic(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)

data class SimpleMetrics(
    val totalSNPs: Int,
    val analyzedSNPs: Int,
    val qualityScore: Double,
    val confidenceLevel: Double,
    val regionsFound: Int
)

@Composable
fun SimpleStatistics(
    metrics: SimpleMetrics,
    modifier: Modifier = Modifier
) {
    val statistics = remember(metrics) {
        listOf(
            SimpleStatistic(
                title = "SNP",
                value = "${metrics.analyzedSNPs}",
                icon = Icons.Filled.Science,
                color = Color(0xFF2196F3)
            ),
            SimpleStatistic(
                title = "Качество",
                value = "${(metrics.qualityScore * 100).toInt()}%",
                icon = Icons.Filled.Star,
                color = Color(0xFFFF9800)
            ),
            SimpleStatistic(
                title = "Доверие",
                value = "${(metrics.confidenceLevel * 100).toInt()}%",
                icon = Icons.Filled.Verified,
                color = Color(0xFF4CAF50)
            ),
            SimpleStatistic(
                title = "Регионы",
                value = "${metrics.regionsFound}",
                icon = Icons.Filled.Public,
                color = Color(0xFF9C27B0)
            )
        )
    }
    
    Column(modifier = modifier) {
        Text(
            text = "Статистика анализа",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            statistics.forEach { stat ->
                SimpleStatCard(
                    statistic = stat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SimpleStatCard(
    statistic: SimpleStatistic,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Иконка
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        statistic.color.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statistic.icon,
                    contentDescription = null,
                    tint = statistic.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Значение
            Text(
                text = statistic.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = statistic.color
            )
            
            // Заголовок
            Text(
                text = statistic.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

