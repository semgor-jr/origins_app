package com.origin.app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.origin.app.data.OriginPortion
import com.origin.app.data.ReportDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

class PdfExporter(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    
    suspend fun exportReportToPdf(report: ReportDto, userName: String): String? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            drawReportContent(canvas, report, userName)
            
            pdfDocument.finishPage(page)
            
            val fileName = "genetic_report_${System.currentTimeMillis()}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val fileOutputStream = FileOutputStream(file)
            
            pdfDocument.writeTo(fileOutputStream)
            pdfDocument.close()
            fileOutputStream.close()
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Отчет сохранен: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
            
            file.absolutePath
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка при создании PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            null
        }
    }
    
    private fun drawReportContent(canvas: Canvas, report: ReportDto, userName: String) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 14f
        }
        
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val smallPaint = Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            textSize = 12f
        }
        
        var yPosition = 50f
        
        // Заголовок
        canvas.drawText("Генетический анализ происхождения", 50f, yPosition, titlePaint)
        yPosition += 40f
        
        // Информация о пользователе
        canvas.drawText("Пользователь: $userName", 50f, yPosition, paint)
        yPosition += 25f
        
        canvas.drawText("Дата создания: ${dateFormat.format(Date())}", 50f, yPosition, paint)
        yPosition += 25f
        
        canvas.drawText("ID отчета: ${report.id}", 50f, yPosition, paint)
        yPosition += 40f
        
        // Описание отчета
        if (report.summary.isNotEmpty()) {
            canvas.drawText("Описание анализа:", 50f, yPosition, headerPaint)
            yPosition += 30f
            
            val lines = wrapText(report.summary, 500f, paint)
            lines.forEach { line ->
                canvas.drawText(line, 50f, yPosition, paint)
                yPosition += 20f
            }
            yPosition += 20f
        }
        
        // Результаты анализа
        canvas.drawText("Результаты анализа происхождения:", 50f, yPosition, headerPaint)
        yPosition += 30f
        
        // Таблица результатов
        drawResultsTable(canvas, report.origins, yPosition, paint)
        
        // Статистика
        yPosition += 200f
        canvas.drawText("Статистика:", 50f, yPosition, headerPaint)
        yPosition += 30f
        
        val totalPercentage = report.origins.sumOf { it.percent }
        canvas.drawText("Общее количество регионов: ${report.origins.size}", 50f, yPosition, paint)
        yPosition += 20f
        
        canvas.drawText("Общий процент: ${totalPercentage}%", 50f, yPosition, paint)
        yPosition += 20f
        
        val primaryRegion = report.origins.maxByOrNull { it.percent }
        if (primaryRegion != null) {
            canvas.drawText("Основное происхождение: ${primaryRegion.region} (${primaryRegion.percent}%)", 50f, yPosition, paint)
            yPosition += 20f
        }
        
        // Подпись
        yPosition += 50f
        canvas.drawText("Отчет создан приложением OriginApp", 50f, yPosition, smallPaint)
        yPosition += 20f
        canvas.drawText("Для получения дополнительной информации посетите наш сайт", 50f, yPosition, smallPaint)
    }
    
    private fun drawResultsTable(canvas: Canvas, origins: List<OriginPortion>, startY: Float, paint: Paint) {
        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val tablePaint = Paint().apply {
            isAntiAlias = true
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        
        var yPosition = startY
        
        // Заголовок таблицы
        canvas.drawRect(50f, yPosition - 20f, 550f, yPosition + 25f, tablePaint)
        canvas.drawText("Регион", 60f, yPosition, headerPaint)
        canvas.drawText("Процент", 400f, yPosition, headerPaint)
        yPosition += 30f
        
        // Строки таблицы
        origins.forEach { origin ->
            canvas.drawRect(50f, yPosition - 20f, 550f, yPosition + 5f, tablePaint)
            canvas.drawText(origin.region, 60f, yPosition, paint)
            canvas.drawText("${origin.percent}%", 400f, yPosition, paint)
            
            // Цветная полоса
            val barWidth = (origin.percent / 100f) * 200f
            val barPaint = Paint().apply {
                color = getRegionColor(origin.region)
                style = Paint.Style.FILL
            }
            canvas.drawRect(450f, yPosition - 15f, 450f + barWidth, yPosition - 5f, barPaint)
            
            yPosition += 25f
        }
    }
    
    private fun getRegionColor(region: String): Int {
        return when (region) {
            "Европа" -> "#4CAF50".toColorInt()
            "Восточная Азия" -> "#03A9F4".toColorInt()
            "Ближний Восток" -> "#FFC107".toColorInt()
            "Африка" -> "#F44336".toColorInt()
            "Южная Азия" -> "#9C27B0".toColorInt()
            "Америка" -> "#FF9800".toColorInt()
            else -> Color.GRAY
        }
    }
    
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    lines.add(word)
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
}

