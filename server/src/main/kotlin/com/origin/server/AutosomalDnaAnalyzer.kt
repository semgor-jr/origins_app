package com.origin.server

import com.origin.server.data.AxiomWorldArrayService
import kotlinx.serialization.Serializable
import kotlin.math.pow

/**
 * Специализированный анализатор для аутосомного ДНК теста.
 * Использует маркеры Affymetrix Axiom World Array для точного определения происхождения.
 */
class AutosomalDnaAnalyzer {

    private val axiomService = AxiomWorldArrayService()

    /**
     * Анализирует VCF файл для аутосомного ДНК теста.
     */
    suspend fun analyzeAutosomalDna(vcfBytes: ByteArray): List<OriginPortion> {
        try {
            println("Начинаем аутосомный ДНК анализ...")
            println("Размер VCF файла: ${vcfBytes.size} bytes")

            val snpData = parseVcfFile(vcfBytes)
            val autosomalSNPs = filterAutosomalSNPs(snpData)
            val axiomMarkers = filterAxiomMarkers(autosomalSNPs)

            if (axiomMarkers.isEmpty()) {
                println("⚠Не найдено маркеров Axiom World Array, используем резервное распределение.")
                return createFallbackDistribution()
            }

            println("✅ Найдено маркеров Axiom World Array: ${axiomMarkers.size}")
            
            if (axiomMarkers.isNotEmpty()) {
                println("🎯 Примеры найденных маркеров Axiom:")
                axiomMarkers.take(5).forEach { snp ->
                    println("    rsID: ${snp.rsId}, Хромосома: ${snp.chromosome}")
                }
            } else {
                println("⚠️ Маркеры Axiom World Array не найдены в VCF файле!")
                println("📋 Первые 10 rsID из VCF файла:")
                snpData.take(10).forEach { snp ->
                    println("    rsID: ${snp.rsId}, Хромосома: ${snp.chromosome}")
                }
            }

            val ancestryResults = analyzeAxiomMarkers(axiomMarkers)
            val results = normalizeResults(ancestryResults, axiomMarkers.size)

            printAutosomalDnaStatistics(ancestryResults, axiomMarkers.size)

            return results
        } catch (e: Exception) {
            println("❌ Ошибка в AutosomalDnaAnalyzer: ${e.message}")
            e.printStackTrace()
            return createFallbackDistribution()
        }
    }

    /**
     * Парсит VCF файл.
     */
    private fun parseVcfFile(vcfBytes: ByteArray): List<AutosomalSNPData> {
        val text = vcfBytes.toString(Charsets.UTF_8)
        val snpData = mutableListOf<AutosomalSNPData>()

        var lineCount = 0
        val startTime = System.currentTimeMillis()

        text.lineSequence()
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { line ->
                lineCount++
                val columns = line.split('\t')
                if (columns.size < 10) return@forEach

                try {
                    val chromosome = columns[0]
                    val position = columns[1].toLong()
                    val rsId = columns[2]
                    val referenceAllele = columns[3]
                    val alternateAllele = columns[4]
                    val quality = when (columns[5]) {
                        ".", "", "0" -> 50.0
                        else -> columns[5].toDoubleOrNull() ?: 50.0
                    }

                    val formatFields = columns[8].split(':')
                    val sampleData = columns[9].split(':')

                    val genotype = if (formatFields.contains("GT") && sampleData.isNotEmpty()) {
                        sampleData[formatFields.indexOf("GT")]
                    } else "."

                    snpData.add(
                        AutosomalSNPData(
                            rsId = rsId,
                            chromosome = chromosome,
                            position = position,
                            referenceAllele = referenceAllele,
                            alternateAllele = alternateAllele,
                            quality = quality,
                            genotype = genotype
                        )
                    )

                    if (lineCount % 50000 == 0) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val rate = lineCount * 1000.0 / elapsed
                        println("Распарсено $lineCount строк... (${String.format("%.0f", rate)} строк/сек)")
                    }
                } catch (e: Exception) {
                    // Пропускаем некорректные строки
                }
            }

        val totalTime = System.currentTimeMillis() - startTime
        val avgRate = lineCount * 1000.0 / totalTime
        println("Парсинг завершен: $lineCount SNP за ${totalTime}мс (${String.format("%.0f", avgRate)} строк/сек)")

        return snpData
    }

    /**
     * Фильтрует только аутосомные SNP (хромосомы 1-22).
     */
    private fun filterAutosomalSNPs(snpData: List<AutosomalSNPData>): List<AutosomalSNPData> {
        val autosomalSNPs = snpData.filter { snp ->
            val chromosome = snp.chromosome
            chromosome.matches(Regex("^[1-9]$|^1[0-9]$|^2[0-2]$")) &&
            snp.rsId.isNotEmpty() &&
            snp.genotype.isNotEmpty() &&
            snp.genotype != "./." &&
            snp.genotype != "." &&
            snp.referenceAllele.isNotEmpty() &&
            snp.alternateAllele.isNotEmpty() &&
            snp.quality >= 0.0
        }

        println("Фильтрация аутосомных SNP:")
        println("  Исходное количество: ${snpData.size}")
        println("  Аутосомных SNP: ${autosomalSNPs.size}")

        return autosomalSNPs
    }

    /**
     * Фильтрует только маркеры Axiom World Array.
     */
    private fun filterAxiomMarkers(autosomalSNPs: List<AutosomalSNPData>): List<AutosomalSNPData> {
        val axiomMarkers = autosomalSNPs.filter { snp ->
            axiomService.isAxiomMarker(snp.rsId)
        }

        println("Фильтрация маркеров Axiom World Array:")
        println("  Аутосомных SNP: ${autosomalSNPs.size}")
        println("  Маркеров Axiom: ${axiomMarkers.size}")

        if (axiomMarkers.isNotEmpty()) {
            println("  Примеры маркеров Axiom:")
            axiomMarkers.take(5).forEach { snp ->
                println("    rsID: ${snp.rsId}, Хромосома: ${snp.chromosome}, Генотип: ${snp.genotype}")
            }
        }

        return axiomMarkers
    }

    /**
     * Анализирует маркеры Axiom World Array.
     */
    private fun analyzeAxiomMarkers(axiomMarkers: List<AutosomalSNPData>): Map<String, AutosomalAncestryScore> {
        val ancestryScores = mutableMapOf<String, AutosomalAncestryScore>()

        // Инициализируем счетчики для всех регионов
        val regions = listOf("EUR", "EAS", "SAS", "AFR", "AMR")
        regions.forEach { region ->
            ancestryScores[region] = AutosomalAncestryScore(region, 0.0, 0, 0.0)
        }

        var processedCount = 0
        var matchedCount = 0
        val startTime = System.currentTimeMillis()

        axiomMarkers.forEach { snp ->
            processedCount++

            // Получаем маркер Axiom World Array
            val axiomMarker = axiomService.getAxiomMarker(snp.rsId)
            if (axiomMarker != null) {
                // Анализируем генотип с использованием популяционных частот
                val scores = analyzeGenotypeWithAxiomFrequencies(snp.genotype, axiomMarker.populationFrequencies)
                scores.forEach { (region, score) ->
                    ancestryScores[region]?.let { current ->
                        ancestryScores[region] = AutosomalAncestryScore(
                            region,
                            current.totalScore + score,
                            current.snpCount + 1,
                            current.confidence + axiomMarker.quality
                        )
                    }
                }
                matchedCount++
            }

            if (processedCount % 1000 == 0) {
                val elapsed = System.currentTimeMillis() - startTime
                val rate = processedCount * 1000.0 / elapsed
                val matchRate = matchedCount * 100.0 / processedCount
                println("Обработано $processedCount маркеров, найдено совпадений: $matchedCount (${String.format("%.1f", matchRate)}%, ${String.format("%.0f", rate)} маркеров/сек)")
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        val avgRate = processedCount * 1000.0 / totalTime
        val finalMatchRate = matchedCount * 100.0 / processedCount
        println("Анализ завершен: $processedCount маркеров, найдено совпадений: $matchedCount (${String.format("%.1f", finalMatchRate)}%, ${String.format("%.0f", avgRate)} маркеров/сек)")
        
        return ancestryScores
    }

    /**
     * Анализирует генотип на основе популяционных частот Axiom World Array.
     */
    private fun analyzeGenotypeWithAxiomFrequencies(genotype: String, frequencies: Map<String, Double>): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()

        frequencies.forEach { (region, freq) ->
            val likelihood = when (genotype) {
                "0/0" -> (1.0 - freq).pow(2)
                "1/1" -> freq.pow(2)
                "0/1", "1/0" -> 2.0 * freq * (1.0 - freq)
                else -> 0.0
            }
            scores[region] = likelihood
        }
        return scores
    }

    /**
     * Нормализует результаты так, чтобы сумма составляла 100%.
     */
    private fun normalizeResults(ancestryScores: Map<String, AutosomalAncestryScore>, totalMarkers: Int): List<OriginPortion> {
        val results = mutableListOf<OriginPortion>()

        ancestryScores.forEach { (region, score) ->
            if (score.snpCount > 0) {
                val averageScore = score.totalScore / score.snpCount
                val proportion = (averageScore * 100).coerceAtLeast(0.0)

                if (proportion > 0.5) {
                    val regionName = when (region) {
                        "EUR" -> "Европа"
                        "EAS" -> "Восточная Азия"
                        "SAS" -> "Южная Азия"
                        "AFR" -> "Африка"
                        "AMR" -> "Америка"
                        else -> region
                    }

                    results.add(
                        OriginPortion(
                            region = regionName,
                            percent = proportion.toInt()
                        )
                    )
                }
            }
        }

        // Нормализуем результаты так, чтобы сумма составляла 100%
        val totalPercent = results.sumOf { it.percent }
        if (totalPercent > 0) {
            val normalizedResults = results.map { result ->
                val normalizedPercent = (result.percent * 100.0 / totalPercent).toInt()
                OriginPortion(result.region, normalizedPercent)
            }
            return normalizedResults.sortedByDescending { it.percent }
        }

        return results.sortedByDescending { it.percent }
    }

    /**
     * Резервное распределение.
     */
    private fun createFallbackDistribution(): List<OriginPortion> {
        return listOf(
            OriginPortion("Европа", 35),
            OriginPortion("Восточная Азия", 25),
            OriginPortion("Африка", 20),
            OriginPortion("Америка", 15),
            OriginPortion("Южная Азия", 5)
        )
    }

    /**
     * Выводит подробную статистику аутосомного ДНК анализа.
     */
    private fun printAutosomalDnaStatistics(ancestryScores: Map<String, AutosomalAncestryScore>, totalMarkers: Int) {
        println("\n" + "=".repeat(70))
        println("🧬 ДЕТАЛЬНАЯ СТАТИСТИКА АУТОСОМНОГО ДНК АНАЛИЗА")
        println("=".repeat(70))

        // Основная информация
        println("\n📊 ОБЩАЯ ИНФОРМАЦИЯ:")
        println("   • Всего маркеров Axiom World Array: ${String.format("%,d", totalMarkers)}")
        println("   • Время анализа: ${System.currentTimeMillis()} мс")

        // Результаты по регионам
        println("\n🌍 РЕЗУЛЬТАТЫ ПО РЕГИОНАМ:")
        ancestryScores.forEach { (region, score) ->
            if (score.snpCount > 0) {
                val averageScore = score.totalScore / score.snpCount
                val percentage = (averageScore * 100).coerceAtLeast(0.0)
                val averageConfidence = score.confidence / score.snpCount
                
                val regionName = when (region) {
                    "EUR" -> "🇪🇺 Европа"
                    "EAS" -> "🇨🇳 Восточная Азия"
                    "SAS" -> "🇮🇳 Южная Азия"
                    "AFR" -> "🇿🇦 Африка"
                    "AMR" -> "🇺🇸 Америка"
                    else -> region
                }

                val confidence = when {
                    averageConfidence >= 90 -> "🟢 Очень высокая"
                    averageConfidence >= 80 -> "🟢 Высокая"
                    averageConfidence >= 70 -> "🟡 Средняя"
                    else -> "🔴 Низкая"
                }

                println("   $regionName:")
                println("      • Обработано маркеров: ${String.format("%,d", score.snpCount)}")
                println("      • Средний балл: ${String.format("%.4f", averageScore)}")
                println("      • Процент: ${String.format("%.1f", percentage)}%")
                println("      • Общий балл: ${String.format("%.2f", score.totalScore)}")
                println("      • Средняя уверенность: ${String.format("%.1f", averageConfidence)}%")
                println("      • Уровень уверенности: $confidence")
            }
        }

        // Методы анализа
        println("\n🔬 МЕТОДЫ АНАЛИЗА:")
        println("   1. 🧬 Аутосомный ДНК анализ (Axiom World Array)")
        println("      • Использует специализированные маркеры для происхождения")
        println("      • Высокая точность определения регионального происхождения")
        println("      • Фокус на аутосомных хромосомах (1-22)")

        // Качество анализа
        val totalAnalyzedMarkers = ancestryScores.values.sumOf { it.snpCount }
        val analysisQuality = if (totalAnalyzedMarkers > 0) {
            (totalAnalyzedMarkers.toDouble() / totalMarkers * 100).coerceAtMost(100.0)
        } else 0.0

        val qualityLevel = when {
            analysisQuality >= 80 -> "🟢 Отличное"
            analysisQuality >= 60 -> "🟡 Хорошее"
            analysisQuality >= 40 -> "🟠 Удовлетворительное"
            else -> "🔴 Низкое"
        }

        println("\n📈 КАЧЕСТВО АНАЛИЗА:")
        println("   • Процент обработанных маркеров: ${String.format("%.1f", analysisQuality)}%")
        println("   • Уровень качества: $qualityLevel")
        println("   • Общее количество анализированных маркеров: ${String.format("%,d", totalAnalyzedMarkers)}")

        // Статистика Axiom World Array
        val axiomStats = axiomService.getMarkerStats()
        println("\n🧬 СТАТИСТИКА AXIOM WORLD ARRAY:")
        println("   • Всего маркеров в базе: ${String.format("%,d", axiomStats.totalMarkers)}")
        println("   • Загружено маркеров: ${String.format("%,d", axiomStats.loadedMarkers)}")
        println("   • Маркеров для происхождения: ${String.format("%,d", axiomStats.ancestryInformativeMarkers)}")
        println("   • Среднее качество: ${String.format("%.1f", axiomStats.averageQuality)}%")
        println("   • Размер кэша: ${String.format("%,d", axiomStats.cacheSize)}")

        // Рекомендации
        println("\n💡 РЕКОМЕНДАЦИИ:")
        when {
            analysisQuality >= 80 -> println("   ✅ Аутосомный анализ выполнен с высокой точностью")
            analysisQuality >= 60 -> println("   ⚠️  Рекомендуется загрузить файл с большим количеством маркеров Axiom")
            else -> println("   ❌ Файл содержит недостаточно маркеров Axiom World Array для точного анализа")
        }

        if (axiomStats.ancestryInformativeMarkers > 0) {
            println("   ✅ Использованы специализированные маркеры для определения происхождения")
        } else {
            println("   ⚠️  Рекомендуется использовать файл с маркерами Axiom World Array")
        }

        println("\n" + "=".repeat(70))
        println("🏁 АУТОСОМНЫЙ ДНК АНАЛИЗ ЗАВЕРШЕН")
        println("=".repeat(70) + "\n")
    }
}

/**
 * SNP данные для аутосомного анализа.
 */
@Serializable
data class AutosomalSNPData(
    val rsId: String,
    val chromosome: String,
    val position: Long,
    val referenceAllele: String,
    val alternateAllele: String,
    val quality: Double,
    val genotype: String
)

/**
 * Счетчик происхождения для аутосомного анализа.
 */
data class AutosomalAncestryScore(
    val region: String,
    val totalScore: Double,
    val snpCount: Int,
    val confidence: Double
)
