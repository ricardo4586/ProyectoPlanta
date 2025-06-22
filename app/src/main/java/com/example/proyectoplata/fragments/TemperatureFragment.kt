package com.example.proyectoplata.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView // Necesario para el MarkerView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.proyectoplata.R // Necesario para R.layout.marker_view_layout y colores
import com.example.proyectoplata.SharedSensorViewModel
import com.example.proyectoplata.databinding.FragmentTemperatureBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView // Necesario para MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight // Necesario para MarkerView
import com.github.mikephil.charting.utils.MPPointF // Necesario para MarkerView
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
        observeTemperatureData()

        return view
    }

    private fun setupChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)

            // Configuración del MarkerView personalizado
            val mv = CustomMarkerView(requireContext(), R.layout.marker_view_layout)
            mv.chartView = chart // Necesario para que el MarkerView sepa a qué gráfico pertenece
            setMarker(mv) // Asigna el MarkerView al gráfico

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                // Granularidad de 1 hora (3600000 milisegundos) para evitar la superposición
                granularity = 3600000f
                labelCount = 5 // Número máximo de etiquetas a intentar mostrar
                valueFormatter = DateAxisFormatter() // Este formatter mostrará solo HH:mm
                textColor = ContextCompat.getColor(requireContext(), R.color.black)
                textSize = 10f
                // Opcional: Si las etiquetas aún se superponen, puedes rotarlas:
                // setLabelRotationAngle(45f) // Rota las etiquetas 45 grados.
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = ContextCompat.getColor(requireContext(), R.color.black)
                textSize = 10f
                axisMinimum = 0f // Ejemplo: Temperatura mínima en 0°C
                axisMaximum = 40f // Ejemplo: Temperatura máxima en 40°C
                // Formateador para el eje Y para añadir el símbolo de grados Celsius
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${String.format("%.1f", value)} °C" // Formatear a un decimal y añadir '°C'
                    }
                }
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
        sharedSensorViewModel.temperatureEntries.observe(viewLifecycleOwner) { entries ->
            Log.d(TAG, "Observando temperatureEntries. Número de entradas: ${entries?.size ?: 0}")
            if (entries != null && entries.isNotEmpty()) {
                entries.sortBy { it.x } // Asegura que los datos estén ordenados por tiempo
                val dataSet = LineDataSet(entries, "Temperatura (°C)").apply {
                    color = ContextCompat.getColor(requireContext(), R.color.chart_line_color) // Color de la línea
                    valueTextColor = ContextCompat.getColor(requireContext(), R.color.black) // Color del texto de los valores
                    valueTextSize = 0f // No dibujar los valores en los puntos, el MarkerView se encargará
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

    // Clase interna para formatear las etiquetas del eje X como fechas (HH:mm)
    private class DateAxisFormatter : ValueFormatter() {
        private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val timestamp = value.toLong()
            return mFormat.format(Date(timestamp))
        }
    }

    /**
     * Clase MarkerView personalizada para mostrar detalles (fecha completa, hora y valor)
     * cuando el usuario toca un punto en el gráfico.
     * Esta clase está anidada dentro de TemperatureFragment.
     */
    @SuppressLint("ViewConstructor")
    class CustomMarkerView(context: Context, layoutResource: Int) :
        MarkerView(context, layoutResource) {

        private val tvMarkerDateTime: TextView = findViewById(R.id.tv_marker_date_time)
        private val tvMarkerValue: TextView = findViewById(R.id.tv_marker_value)

        // Formateador para la fecha y hora completa que se mostrará en el MarkerView
        private val dateTimeFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

        // Ajusta el offset para posicionar el marcador correctamente sobre el punto
        override fun getOffset(): MPPointF {
            // Devuelve el offset del marcador. Por defecto, lo centramos en el punto (x,y)
            // y lo movemos hacia arriba para que no tape el punto.
            return MPPointF(-(width / 2).toFloat(), -height.toFloat() - 10f) // -10f para un pequeño margen
        }

        // Se llama cada vez que el MarkerView es redibujado,
        // actualizando el contenido con los datos del punto seleccionado
        override fun refreshContent(e: Entry, highlight: Highlight) {
            val timestamp = e.x.toLong()
            val temperatureValue = e.y

            tvMarkerDateTime.text = dateTimeFormat.format(Date(timestamp))
            tvMarkerValue.text = "${String.format("%.1f", temperatureValue)} °C" // Formatear a un decimal y añadir '°C'

            super.refreshContent(e, highlight)
        }
    }
}