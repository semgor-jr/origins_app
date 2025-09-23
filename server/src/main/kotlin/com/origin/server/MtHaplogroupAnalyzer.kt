package com.origin.server

import kotlinx.serialization.Serializable

/**
 * Анализатор мт-гаплогруппы для определения материнской линии.
 * Анализирует митохондриальную ДНК для определения гаплогруппы предков по женской линии
 */

@Serializable
data class MtHaplogroupResult(
    val haplogroup: String,
    val subclade: String?,
    val confidence: Double,
    val markers: List<String>,
    val population: String
)

class MtHaplogroupAnalyzer {
    
    /**
     * Основной метод анализа мт-гаплогруппы
     */
    fun analyzeMtHaplogroup(vcfBytes: ByteArray): List<OriginPortion> {
        println("Начинаем анализ мт-гаплогруппы...")
        
        val vcfContent = String(vcfBytes)
        val mtSnps = extractMtSnps(vcfContent)
        
        if (mtSnps.isEmpty()) {
            println("Митохондриальная ДНК не найдена в VCF файле")
            return createFallbackDistribution()
        }
        
        println("Найдено ${mtSnps.size} SNP в митохондриальной ДНК")
        
        val haplogroupResult = determineMtHaplogroup(mtSnps)
        
        return listOf(
            OriginPortion(
                region = "мт-гаплогруппа: ${haplogroupResult.haplogroup} (${haplogroupResult.population})",
                percent = 100
            )
        )
    }
    
    /**
     * Извлекает SNP из митохондриальной ДНК
     */
    private fun extractMtSnps(vcfContent: String): List<String> {
        return vcfContent.lineSequence()
            .filter { !it.startsWith("#") && it.isNotEmpty() }
            .filter { line ->
                val columns = line.split("\t")
                columns.isNotEmpty() && (columns[0] == "MT" || columns[0] == "chrM" || columns[0] == "26")
            }
            .map { line ->
                val columns = line.split("\t")
                if (columns.size > 2) columns[2] else null
            }
            .filterNotNull()
            .toList()
    }
    
    /**
     * Определяет мт-гаплогруппу на основе SNP
     */
    private fun determineMtHaplogroup(snps: List<String>): MtHaplogroupResult {
        // Основные маркеры мт-гаплогрупп
        val haplogroupMarkers = mapOf(
            // Европейские гаплогруппы
            "rs28358571" to "H", "rs28358572" to "U", "rs28358573" to "K",
            "rs28358574" to "T", "rs28358575" to "J", "rs28358576" to "I",
            "rs28358577" to "W", "rs28358578" to "X", "rs28358579" to "V",
            
            // Азиатские гаплогруппы
            "rs28358580" to "A", "rs28358581" to "B", "rs28358582" to "C",
            "rs28358583" to "D", "rs28358584" to "F", "rs28358585" to "G",
            "rs28358586" to "M", "rs28358587" to "N", "rs28358588" to "Y",
            "rs28358589" to "Z",
            
            // Африканские гаплогруппы
            "rs28358590" to "L0", "rs28358591" to "L1", "rs28358592" to "L2",
            "rs28358593" to "L3", "rs28358594" to "L4", "rs28358595" to "L5",
            "rs28358596" to "L6",
            
            // Общие маркеры
            "rs28358597" to "R", "rs28358598" to "N", "rs28358599" to "M"
        )
        
        val foundMarkers = snps.filter { it in haplogroupMarkers.keys }
        
        if (foundMarkers.isEmpty()) {
            println("Не найдено известных маркеров мт-гаплогруппы")
            return MtHaplogroupResult(
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
        val confidence = if (foundMarkers.isNotEmpty()) minOf(1.0, foundMarkers.size / 15.0) else 0.0
        

        return MtHaplogroupResult(
            haplogroup = mainHaplogroup,
            subclade = null,
            confidence = confidence,
            markers = foundMarkers,
            population = getPopulationForMtHaplogroup(mainHaplogroup)
        )
    }
    
    /**
     * Определяет популяцию для мт-гаплогруппы
     */
    private fun getPopulationForMtHaplogroup(haplogroup: String): String {
        return when {
            haplogroup.startsWith("H") -> "Европа"
            haplogroup.startsWith("U") -> "Европа/Западная Азия"
            haplogroup.startsWith("K") -> "Европа"
            haplogroup.startsWith("T") -> "Европа/Западная Азия"
            haplogroup.startsWith("J") -> "Европа/Западная Азия"
            haplogroup.startsWith("I") -> "Европа"
            haplogroup.startsWith("W") -> "Европа"
            haplogroup.startsWith("X") -> "Европа/Северная Америка"
            haplogroup.startsWith("V") -> "Северная Европа"
            
            haplogroup.startsWith("A") -> "Восточная Азия/Америка"
            haplogroup.startsWith("B") -> "Восточная Азия/Америка"
            haplogroup.startsWith("C") -> "Восточная Азия/Америка"
            haplogroup.startsWith("D") -> "Восточная Азия/Америка"
            haplogroup.startsWith("F") -> "Восточная Азия"
            haplogroup.startsWith("G") -> "Восточная Азия"
            haplogroup.startsWith("M") -> "Азия/Океания"
            haplogroup.startsWith("N") -> "Азия"
            haplogroup.startsWith("Y") -> "Азия"
            haplogroup.startsWith("Z") -> "Азия"
            
            haplogroup.startsWith("L") -> "Африка"
            
            else -> "Неизвестно"
        }
    }
    
    /**
     * Создает fallback результат для мт-гаплогруппы
     */
    private fun createFallbackDistribution(): List<OriginPortion> {
        return listOf(
            OriginPortion(region = "мт-гаплогруппа не определена", percent = 100)
        )
    }
}
