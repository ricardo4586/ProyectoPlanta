package com.example.proyectoplata.fragments // ¡MUY IMPORTANTE! Esta debe ser la primera línea.

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.proyectoplata.R
import com.example.proyectoplata.SharedSensorViewModel // Importa tu ViewModel
import com.example.proyectoplata.databinding.FragmentTemperatureBinding // Importa el binding de tu layout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class TemperatureFragment : Fragment() {

    private val TAG = "TemperatureFragment"
    private var _binding: FragmentTemperatureBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedSensorViewModel: SharedSensorViewModel
    private lateinit var temperatureChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate de inicializar el ViewModel en el alcance de la actividad
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
        Log.d(TAG, "onCreate: SharedSensorViewModel inicializado en TemperatureFragment.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemperatureBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: TemperatureFragment layout inflado.")

        temperatureChart = binding.temperatureChart
        setupChart(temperatureChart)
        observeTemperatureData() // Inicia la observación de los datos

        return view
    }

    private fun setupChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                // Granularidad de 1 hora (60 minutos * 60 segundos * 1000 milisegundos)
                granularity = (60 * 60 * 1000).toFloat()
                valueFormatter = DateAxisFormatter()
                textColor = ContextCompat.getColor(requireContext(), R.color.black)
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = ContextCompat.getColor(requireContext(), R.color.black)
                textSize = 10f
                axisMinimum = 0f // Ejemplo: Temperatura mínima en 0°C
                axisMaximum = 40f // Ejemplo: Temperatura máxima en 40°C
            }

            axisRight.isEnabled = false // Deshabilitar el eje Y derecho

            legend.apply {
                isEnabled = true // Habilitar la leyenda
                textColor = ContextCompat.getColor(requireContext(), R.color.black)
                textSize = 12f
            }

            // Animación para el gráfico
            animateX(1500)
        }
    }

    private fun observeTemperatureData() {
        // *** ESTE ES EL CAMBIO CRUCIAL: Observa 'temperatureEntries' ***
        // Tu SharedSensorViewModel tiene 'temperatureEntries', NO 'temperatureAmbientalEntries'.
        sharedSensorViewModel.temperatureEntries.observe(viewLifecycleOwner) { entries ->
            Log.d(TAG, "Observando temperatureEntries. Número de entradas: ${entries?.size ?: 0}")
            if (entries != null && entries.isNotEmpty()) {
                val dataSet = LineDataSet(entries, "Temperatura (°C)").apply {
                    color = ContextCompat.getColor(requireContext(), R.color.chart_line_color) // Color de la línea
                    valueTextColor = ContextCompat.getColor(requireContext(), R.color.black) // Color del texto de los valores
                    valueTextSize = 9f // Tamaño del texto de los valores
                    setDrawValues(false) // No dibujar los valores en los puntos
                    setDrawCircles(false) // No dibujar círculos en los puntos de datos
                    mode = LineDataSet.Mode.CUBIC_BEZIER // Curva suavizada
                    setDrawFilled(true) // Rellenar el área debajo de la línea
                    fillColor = ContextCompat.getColor(requireContext(), R.color.chart_fill_color) // Color de relleno
                    fillAlpha = 80 // Transparencia del relleno (0-255)
                }

                val lineData = LineData(dataSet)
                temperatureChart.data = lineData
                temperatureChart.invalidate() // Refresca el gráfico
                Log.d(TAG, "Gráfico de temperatura ambiental actualizado con ${entries.size} entradas.")
            } else {
                // Si no hay datos, limpia el gráfico
                temperatureChart.clear()
                temperatureChart.invalidate()
                Log.d(TAG, "No hay entradas para el gráfico de temperatura ambiental.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpia la referencia al binding para evitar fugas de memoria
        Log.d(TAG, "onDestroyView: TemperatureFragment view destruida.")
    }

    // Clase interna para formatear las etiquetas del eje X como fechas
    private class DateAxisFormatter : ValueFormatter() {
        private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val timestamp = value.toLong()
            return mFormat.format(Date(timestamp))
        }
    }
}