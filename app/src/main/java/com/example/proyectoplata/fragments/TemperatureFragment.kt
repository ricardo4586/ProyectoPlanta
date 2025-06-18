package com.example.proyectoplata.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat // ¡Importante: Añadida esta importación!
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

import kotlin.random.Random
import com.example.proyectoplata.R

class TemperatureFragment : Fragment() {

    private lateinit var temperatureChart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout para este fragmento
        val view = inflater.inflate(R.layout.fragment_temperature, container, false)
        temperatureChart = view.findViewById(R.id.temperatureChart)
        setupChart() // Configura el gráfico
        return view
    }

    private fun setupChart() {
        val entries = ArrayList<Entry>()
        // Genera datos simulados: 24 puntos de datos para representar 24 horas
        val startTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 horas atrás
        for (i in 0 until 24) {
            // Genera una temperatura aleatoria entre 15 y 30 grados
            val temperature = 15f + Random.nextFloat() * 15f
            // Añade el punto de datos con el tiempo (en milisegundos) y la temperatura
            // El tiempo se incrementa en una hora por cada punto
            entries.add(Entry((startTime + i * 60 * 60 * 1000).toFloat(), temperature))
        }

        // Crea un conjunto de datos (DataSet) a partir de las entradas
        val dataSet = LineDataSet(entries, "Temperatura (°C)").apply {
            // Usando ContextCompat.getColor para obtener colores de resources/colors.xml
            color = ContextCompat.getColor(requireContext(), R.color.chart_line_color)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.black) // O Color.BLACK directamente
            valueTextSize = 9f // Tamaño del texto de los valores
            setDrawValues(false) // No dibujar los valores en los puntos
            setDrawCircles(false) // No dibujar círculos en los puntos
            mode = LineDataSet.Mode.CUBIC_BEZIER // Curva suave
            setDrawFilled(true) // Rellenar el área debajo de la línea
            fillColor = ContextCompat.getColor(requireContext(), R.color.chart_fill_color) // Color del relleno con transparencia
            fillAlpha = 80 // Transparencia del relleno
        }

        // Crea un objeto LineData con el conjunto de datos
        val lineData = LineData(dataSet)

        // Configura el gráfico
        temperatureChart.apply {
            data = lineData // Asigna los datos al gráfico
            description.isEnabled = false // Deshabilita la descripción
            setTouchEnabled(true) // Habilita la interacción táctil
            setPinchZoom(true) // Habilita el zoom con dos dedos

            // Configuración del eje X (horizontal - tiempo)
            xAxis.apply {
                valueFormatter = object : ValueFormatter() {
                    private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        // Formatea el valor del tiempo (milisegundos) a una hora legible
                        return mFormat.format(Date(value.toLong()))
                    }
                }
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM // Posición inferior
                setDrawGridLines(false) // No dibujar líneas de cuadrícula verticales
                textColor = ContextCompat.getColor(requireContext(), R.color.black) // Color del texto del eje X
                granularity = (60 * 60 * 1000).toFloat() // Intervalo de una hora
            }

            // Configuración del eje Y izquierdo (vertical - temperatura)
            axisLeft.apply {
                setDrawGridLines(true) // Dibujar líneas de cuadrícula horizontales
                textColor = ContextCompat.getColor(requireContext(), R.color.black) // Color del texto del eje Y
                axisMinimum = 0f // Valor mínimo del eje Y
                axisMaximum = 40f // Valor máximo del eje Y
            }

            // Deshabilita el eje Y derecho
            axisRight.isEnabled = false

            legend.apply {
                isEnabled = true // Habilita la leyenda
                textColor = ContextCompat.getColor(requireContext(), R.color.black) // Color del texto de la leyenda
            }

            animateX(1500) // Animar el gráfico en el eje X
            invalidate() // Refrescar el gráfico
        }
    }
}