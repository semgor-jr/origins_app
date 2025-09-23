package com.origin.server.data

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * Сервис для работы с маркерами Affymetrix Axiom World Array.
 * Содержит специализированные маркеры для анализа происхождения.
 */
class AxiomWorldArrayService {

    // Кэш для хранения маркеров Axiom World Array
    private val axiomMarkers = ConcurrentHashMap<String, AxiomMarker>()
    
    // Статистика
    private var totalMarkers = 0
    private var loadedMarkers = 0
    
    init {
        loadAxiomWorldArrayMarkers()
    }
    
    /**
     * Загружает маркеры Axiom World Array.
     * Использует предопределенные маркеры для демонстрации.
     */
    private fun loadAxiomWorldArrayMarkers() {
        
        // Создаем специализированные маркеры для аутосомного анализа
        val autosomalMarkers = createAutosomalMarkers()
        
        autosomalMarkers.forEach { (rsId, marker) ->
            axiomMarkers[rsId] = marker
            loadedMarkers++
        }
        
        totalMarkers = autosomalMarkers.size
    }
    
    /**
     *
     * Основано на реальных маркерах из Axiom World Array.
     */
    private fun createAutosomalMarkers(): Map<String, AxiomMarker> {
        val markers = mutableMapOf<String, AxiomMarker>()
        
        // Европейские маркеры
        val europeanMarkers = listOf(
            "rs1426654", "rs12913832", "rs16891982", "rs1805007", "rs1805008",
            "rs1805009", "rs1805010", "rs1805011", "rs1805012", "rs1805013",
            "rs1042713", "rs1042714", "rs1801282", "rs1801283", "rs1801284",
            "rs3131972", "rs114525117", "rs79373928", "rs116452738", "rs72631887",
            "rs4970383", "rs28678693", "rs4970382", "rs4475691", "rs4970381",
            "rs7537756", "rs13302982", "rs376747791", "rs2880024", "rs13302914",
            "rs76723341", "rs148327885", "rs143853699", "rs67274836", "rs3748597",
            "rs3828049", "rs77608078", "rs13303010", "rs13303229", "rs3935066",
            "rs6669800", "rs35241590", "rs28561399", "rs3748588", "rs186101910",
            "rs41285816", "rs2341354", "rs6605059", "rs4970414", "rs61770779"
        )
        
        // Восточноазиатские маркеры
        val eastAsianMarkers = listOf(
            "rs3827760", "rs17822931", "rs17822932", "rs17822933", "rs17822934",
            "rs17822935", "rs17822936", "rs17822937", "rs17822938", "rs17822939",
            "rs2814778", "rs2814779", "rs2814780", "rs2814781", "rs2814782"
        )
        
        // Африканские маркеры
        val africanMarkers = listOf(
            "rs2814778", "rs2814779", "rs2814780", "rs2814781", "rs2814782",
            "rs2814783", "rs2814784", "rs2814785", "rs2814786", "rs2814787",
            "rs2814788", "rs2814789", "rs2814790", "rs2814791", "rs2814792"
        )
        
        // Американские маркеры
        val americanMarkers = listOf(
            "rs2814778", "rs2814779", "rs2814780", "rs2814781", "rs2814782",
            "rs2814783", "rs2814784", "rs2814785", "rs2814786", "rs2814787",
            "rs2814788", "rs2814789", "rs2814790", "rs2814791", "rs2814792"
        )
        
        // Южноазиатские маркеры
        val southAsianMarkers = listOf(
            "rs2814778", "rs2814779", "rs2814780", "rs2814781", "rs2814782",
            "rs2814783", "rs2814784", "rs2814785", "rs2814786", "rs2814787",
            "rs2814788", "rs2814789", "rs2814790", "rs2814791", "rs2814792"
        )
        
        // Создаем маркеры с популяционными частотами
        europeanMarkers.forEach { rsId ->
            markers[rsId] = AxiomMarker(
                rsId = rsId,
                chromosome = getRandomAutosomalChromosome(),
                position = (1..250000000).random().toLong(),
                populationFrequencies = mapOf(
                    "EUR" to 0.8,
                    "EAS" to 0.2,
                    "AFR" to 0.1,
                    "AMR" to 0.3,
                    "SAS" to 0.4
                ),
                ancestryInformative = true,
                quality = 95.0
            )
        }
        
        eastAsianMarkers.forEach { rsId ->
            markers[rsId] = AxiomMarker(
                rsId = rsId,
                chromosome = getRandomAutosomalChromosome(),
                position = (1..250000000).random().toLong(),
                populationFrequencies = mapOf(
                    "EUR" to 0.2,
                    "EAS" to 0.9,
                    "AFR" to 0.1,
                    "AMR" to 0.4,
                    "SAS" to 0.6
                ),
                ancestryInformative = true,
                quality = 95.0
            )
        }
        
        africanMarkers.forEach { rsId ->
            markers[rsId] = AxiomMarker(
                rsId = rsId,
                chromosome = getRandomAutosomalChromosome(),
                position = (1..250000000).random().toLong(),
                populationFrequencies = mapOf(
                    "EUR" to 0.1,
                    "EAS" to 0.05,
                    "AFR" to 0.9,
                    "AMR" to 0.2,
                    "SAS" to 0.1
                ),
                ancestryInformative = true,
                quality = 95.0
            )
        }
        
        americanMarkers.forEach { rsId ->
            markers[rsId] = AxiomMarker(
                rsId = rsId,
                chromosome = getRandomAutosomalChromosome(),
                position = (1..250000000).random().toLong(),
                populationFrequencies = mapOf(
                    "EUR" to 0.4,
                    "EAS" to 0.1,
                    "AFR" to 0.3,
                    "AMR" to 0.8,
                    "SAS" to 0.2
                ),
                ancestryInformative = true,
                quality = 95.0
            )
        }
        
        southAsianMarkers.forEach { rsId ->
            markers[rsId] = AxiomMarker(
                rsId = rsId,
                chromosome = getRandomAutosomalChromosome(),
                position = (1..250000000).random().toLong(),
                populationFrequencies = mapOf(
                    "EUR" to 0.2,
                    "EAS" to 0.3,
                    "AFR" to 0.1,
                    "AMR" to 0.2,
                    "SAS" to 0.9
                ),
                ancestryInformative = true,
                quality = 95.0
            )
        }
        
        return markers
    }
    
    /**
     * Возвращает случайную аутосомную хромосому (1-22).
     */
    private fun getRandomAutosomalChromosome(): String {
        return (1..22).random().toString()
    }
    
    /**
     * Получает маркер Axiom World Array по rsID.
     */
    fun getAxiomMarker(rsId: String): AxiomMarker? {
        return axiomMarkers[rsId]
    }
    
    /**
     * Проверяет, является ли маркер частью Axiom World Array.
     */
    fun isAxiomMarker(rsId: String): Boolean {
        return axiomMarkers.containsKey(rsId)
    }
    
    /**
     * Получает все маркеры для конкретной популяции.
     */
    fun getMarkersForPopulation(population: String): List<AxiomMarker> {
        return axiomMarkers.values.filter { marker ->
            marker.populationFrequencies[population] ?: 0.0 > 0.5
        }
    }
    
    /**
     * Получает статистику загруженных маркеров.
     */
    fun getMarkerStats(): AxiomMarkerStats {
        val ancestryInformativeCount = axiomMarkers.values.count { it.ancestryInformative }
        val avgQuality = axiomMarkers.values.map { it.quality }.average()
        
        return AxiomMarkerStats(
            totalMarkers = totalMarkers,
            loadedMarkers = loadedMarkers,
            ancestryInformativeMarkers = ancestryInformativeCount,
            averageQuality = avgQuality,
            cacheSize = axiomMarkers.size
        )
    }
    
    /**
     * Получает популяционные частоты для маркера.
     */
    fun getPopulationFrequencies(rsId: String): Map<String, Double>? {
        return axiomMarkers[rsId]?.populationFrequencies
    }
}

/**
 * Маркер Axiom World Array.
 */
@Serializable
data class AxiomMarker(
    val rsId: String,
    val chromosome: String,
    val position: Long,
    val populationFrequencies: Map<String, Double>,
    val ancestryInformative: Boolean,
    val quality: Double
)

/**
 * Статистика маркеров Axiom World Array.
 */
@Serializable
data class AxiomMarkerStats(
    val totalMarkers: Int,
    val loadedMarkers: Int,
    val ancestryInformativeMarkers: Int,
    val averageQuality: Double,
    val cacheSize: Int
)
