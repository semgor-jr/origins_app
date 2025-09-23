package com.origin.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.origin.app.data.OriginPortion
import androidx.core.graphics.toColorInt


@Composable
fun GoogleMapsOriginMap(
    origins: List<OriginPortion>,
    colors: List<Color> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Проверяем, есть ли данные для отображения
    if (origins.isEmpty()) {
        Text(
            text = "Нет данных для отображения на карте",
            modifier = modifier
        )
        return
    }
    
    println("GoogleMapsOriginMap: Rendering ${origins.size} origins")
    origins.forEach { origin ->
        println("GoogleMapsOriginMap: Origin - ${origin.region}: ${origin.percent}%")
    }

    val regionColors = mapOf(
        "Европа" to "#4CAF50".toColorInt(),
        "Восточная Азия" to "#03A9F4".toColorInt(),
        "Ближний Восток" to "#FFC107".toColorInt(),
        "Африка" to "#F44336".toColorInt(),
        "Южная Азия" to "#9C27B0".toColorInt(),
        "Америка" to "#FF9800".toColorInt(),
    )
    
    // Создаем MapView
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            onResume()
        }
    }
    
    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    ) { view ->
        view.getMapAsync { googleMap ->
            // Настройки карты
            googleMap.uiSettings.apply {
                isZoomControlsEnabled = true
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
            }
            
            // Устанавливаем тип карты
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            
            // Координаты центров регионов для кругов
            val regionCenters = mapOf(
                "Европа" to LatLng(54.0, 15.0),
                "Восточная Азия" to LatLng(35.0, 120.0),
                "Ближний Восток" to LatLng(35.0, 42.0),
                "Африка" to LatLng(0.0, 20.0),
                "Южная Азия" to LatLng(25.0, 80.0),
                "Америка" to LatLng(15.0, -90.0),
            )
            
            // Добавляем круги для найденных регионов
            val boundsBuilder = LatLngBounds.builder()
            origins.forEachIndexed { index, origin ->
                val center = regionCenters[origin.region]
                // Используем переданные цвета по индексу, если они есть, иначе fallback цвета
                val color = if (colors.isNotEmpty() && index < colors.size) {
                    colors[index].toArgb()
                } else {
                    regionColors[origin.region] ?: android.graphics.Color.GRAY
                }
                
                center?.let { centerPoint ->
                    // Размер круга зависит от процента происхождения
                    val baseRadius = 500000.0 // базовый радиус в метрах
                    val radius = baseRadius * (origin.percent.toFloat() / 100f * 2f + 0.5f)
                    
                    // Прозрачность зависит от процента происхождения
                    val alpha = (origin.percent.toFloat() / 100f * 0.6f + 0.2f).coerceIn(0.2f, 0.8f)
                    val fillColor = android.graphics.Color.argb(
                        (alpha * 255).toInt(),
                        android.graphics.Color.red(color),
                        android.graphics.Color.green(color),
                        android.graphics.Color.blue(color)
                    )
                    
                    // Добавляем круг
                    googleMap.addCircle(
                        CircleOptions()
                            .center(centerPoint)
                            .radius(radius)
                            .fillColor(fillColor)
                            .strokeColor(color)
                            .strokeWidth(3f)
                    )
                    
                    // Добавляем центр круга в границы для камеры
                    boundsBuilder.include(centerPoint)
                }
            }
            
            // Устанавливаем камеру, чтобы показать все регионы
            if (origins.isNotEmpty()) {
                val bounds = boundsBuilder.build()
                val padding = 100 // отступ в пикселях
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding)
                )
            } else {
                // Если нет данных, показываем мир
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(20.0, 0.0), 2f)
                )
            }
        }
    }
    
    // Управляем жизненным циклом MapView
    DisposableEffect(mapView) {
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
}

