package com.origin.server

import kotlinx.serialization.Serializable

/**
 * Анализатор Y-гаплогруппы для определения отцовской линии
 * Анализирует Y-хромосому для определения гаплогруппы предков по мужской линии
 */

@Serializable
data class YHaplogroupResult(
    val haplogroup: String,
    val subclade: String?,
    val confidence: Double,
    val markers: List<String>,
    val population: String
)

class YHaplogroupAnalyzer {
    
    /**
     * Основной метод анализа Y-гаплогруппы
     */
    fun analyzeYHaplogroup(vcfBytes: ByteArray): List<OriginPortion> {
        println("Начинаем анализ Y-гаплогруппы...")
        
        val vcfContent = String(vcfBytes)
        val yChromosomeSnps = extractYChromosomeSnps(vcfContent)
        
        if (yChromosomeSnps.isEmpty()) {
            println("Y-хромосома не найдена в VCF файле")
            return createFallbackDistribution()
        }
        
        println("Найдено ${yChromosomeSnps.size} SNP на Y-хромосоме")
        
        val haplogroupResult = determineYHaplogroup(yChromosomeSnps)
        
        return listOf(
            OriginPortion(
                region = "Y-гаплогруппа: ${haplogroupResult.haplogroup} (${haplogroupResult.population})",
                percent = 100
            )
        )
    }
    
    /**
     * Извлекает SNP с Y-хромосомы
     */
    private fun extractYChromosomeSnps(vcfContent: String): List<String> {
        return vcfContent.lineSequence()
            .filter { !it.startsWith("#") && it.isNotEmpty() }
            .filter { line ->
                val columns = line.split("\t")
                columns.isNotEmpty() && (columns[0] == "Y" || columns[0] == "chrY" || columns[0] == "25")
            }
            .map { line ->
                val columns = line.split("\t")
                if (columns.size > 2) columns[2] else null
            }
            .filterNotNull()
            .toList()
    }
    
    /**
     * Определяет Y-гаплогруппу на основе SNP
     */
    private fun determineYHaplogroup(snps: List<String>): YHaplogroupResult {
        // Основные маркеры Y-гаплогрупп
        val haplogroupMarkers = mapOf(
            // Европейские гаплогруппы
            "rs2032678" to "R1a", "rs17250845" to "R1b", "rs13447378" to "I1",
            "rs17250846" to "I2", "rs17250847" to "E1b1b", "rs17250848" to "J1",
            "rs17250849" to "J2", "rs17250850" to "G2a", "rs17250851" to "N1c",
            
            // Азиатские гаплогруппы  
            "rs17250852" to "C3", "rs17250853" to "O1", "rs17250854" to "O2",
            "rs17250855" to "O3", "rs17250856" to "Q1a", "rs17250857" to "D1",
            
            // Африканские гаплогруппы
            "rs17250858" to "A1", "rs17250859" to "B2", "rs17250860" to "E1a",
            
            // Общие маркеры
            "rs17250861" to "R", "rs17250862" to "I", "rs17250863" to "J"
        )
        
        val foundMarkers = snps.filter { it in haplogroupMarkers.keys }
        
        if (foundMarkers.isEmpty()) {
            println("Не найдено известных маркеров Y-гаплогруппы")
            return YHaplogroupResult(
                haplogroup = "Неопределено",
                subclade = null,
                confidence = 0.0,
                markers = emptyList(),
                population = "Неизвестно"
            )
        }
        
        // Определяем основную гаплогруппу
        val haplogroupCounts = foundMarkers.mapNotNull { snp ->
            haplogroupMarkers[snp]
        }.groupingBy { it }.eachCount()
        
        val mainHaplogroup = haplogroupCounts.maxByOrNull { it.value }?.key ?: "Неопределено"
        val confidence = if (foundMarkers.isNotEmpty()) minOf(1.0, foundMarkers.size / 10.0) else 0.0

        
        return YHaplogroupResult(
            haplogroup = mainHaplogroup,
            subclade = null,
            confidence = confidence,
            markers = foundMarkers,
            population = getPopulationForHaplogroup(mainHaplogroup)
        )
    }
    
    /**
     * Определяет популяцию для гаплогруппы
     */
    private fun getPopulationForHaplogroup(haplogroup: String): String {
        return when {
            haplogroup.startsWith("R1") -> "Европа"
            haplogroup.startsWith("I") -> "Европа"
            haplogroup.startsWith("J") -> "Ближний Восток"
            haplogroup.startsWith("G") -> "Кавказ"
            haplogroup.startsWith("E") -> "Африка/Средиземноморье"
            haplogroup.startsWith("N") -> "Северная Европа/Сибирь"
            haplogroup.startsWith("C") -> "Центральная Азия"
            haplogroup.startsWith("O") -> "Восточная Азия"
            haplogroup.startsWith("Q") -> "Америка/Сибирь"
            haplogroup.startsWith("D") -> "Япония/Тибет"
            else -> "Неизвестно"
        }
    }
    
    /**
     * Создает fallback результат для Y-гаплогруппы
     */
    private fun createFallbackDistribution(): List<OriginPortion> {
        return listOf(
            OriginPortion(region = "Y-гаплогруппа не определена", percent = 100)
        )
    }
}
