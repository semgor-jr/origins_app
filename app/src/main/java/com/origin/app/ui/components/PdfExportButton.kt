package com.origin.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.origin.app.data.ReportDto
import com.origin.app.data.SessionStore
import com.origin.app.utils.PdfExporter
import kotlinx.coroutines.launch

@Composable
fun PdfExportButton(
    report: ReportDto,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionStore = remember { SessionStore(context) }
    val userName by sessionStore.name.collectAsState(initial = "Пользователь")
    val pdfExporter = remember { PdfExporter(context) }
    
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                scope.launch {
                    isExporting = true
                    exportMessage = null
                    
                    try {
                        val filePath = pdfExporter.exportReportToPdf(report, userName ?: "Пользователь")
                        exportMessage = if (filePath != null) {
                            "Отчет успешно сохранен!"
                        } else {
                            "Ошибка при сохранении отчета"
                        }
                    } catch (e: Exception) {
                        exportMessage = "Ошибка: ${e.message}"
                    } finally {
                        isExporting = false
                    }
                }
            },
            enabled = !isExporting,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            if (isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Создание PDF...")
            } else {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Экспорт в PDF",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Экспорт в PDF")
            }
        }
        
        if (exportMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exportMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (exportMessage!!.contains("успешно")) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Отчет будет сохранен в папку Documents",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
