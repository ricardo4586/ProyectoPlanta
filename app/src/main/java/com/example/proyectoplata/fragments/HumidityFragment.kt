package com.example.proyectoplata.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView // Importar TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.proyectoplata.R // Asegúrate de que este import sea correcto para acceder a R.layout.marker_view_layout
import com.example.proyectoplata.SharedSensorViewModel
import com.example.proyectoplata.databinding.FragmentHumidityBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView // Importar MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight // Importar Highlight
import com.github.mikephil.charting.utils.MPPointF // Importar MPPointF
import java.text.SimpleDateFormat
import java.util.*

class HumidityFragment : Fragment() {

    private val TAG = "HumidityFragment"
    private var _binding: FragmentHumidityBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedSensorViewModel: SharedSensorViewModel
    private lateinit var humidityChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
        Log.d(TAG, "onCreate: SharedSensorViewModel inicializado en HumidityFragment.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHumidityBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: HumidityFragment layout inflado.")

        humidityChart = binding.humidityChart
        setupChart(humidityChart)
        observeHumidityData()

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
                // Ajustes para evitar la distorsión del eje X
                granularity = 3600000f // 1 hora en milisegundos. Ajusta si necesitas más o menos etiquetas.
                labelCount = 5 // Número máximo de etiquetas que se intentarán mostrar.
                valueFormatter = DateAxisFormatter() // Este formatter ahora mostrará solo HH:mm
                textColor = Color.BLACK
                textSize = 10f
                // Opcional: Si las etiquetas aún se superponen, puedes rotarlas:
                // setLabelRotationAngle(45f) // Rota las etiquetas 45 grados.
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = Color.BLACK
                textSize = 10f
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
            animateX(1500)
        }
    }

    private fun observeHumidityData() {
        sharedSensorViewModel.humidityEntries.observe(viewLifecycleOwner) { entries ->
            Log.d(TAG, "Observando humidityEntries. Número de entradas: ${entries.size}")
            if (entries.isNotEmpty()) {
                entries.sortBy { it.x } // Asegura que los datos estén ordenados por tiempo
                val dataSet = LineDataSet(entries, "Humedad Ambiental (%)").apply {
                    color = Color.parseColor("#00BFFF") // Azul cielo
                    setCircleColor(Color.parseColor("#00BFFF"))
                    lineWidth = 2f
                    circleRadius = 4f
                    setDrawCircleHole(false)
                    valueTextSize = 0f // No muestra el valor numérico en cada punto del gráfico principal
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#8000BFFF")
                }

                val lineData = LineData(dataSet)
                humidityChart.data = lineData
                humidityChart.invalidate()
                Log.d(TAG, "Gráfico de humedad ambiental actualizado con ${entries.size} entradas.")
            } else {
                humidityChart.clear()
                humidityChart.invalidate()
                Log.d(TAG, "No hay entradas para el gráfico de humedad ambiental.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: HumidityFragment view destruida.")
    }

    /**
     * Formateador para el eje X del gráfico. Muestra solo la hora para mantener la legibilidad.
     * La fecha completa se mostrará en el MarkerView al tocar un punto.
     */
    private class DateAxisFormatter : ValueFormatter() {
        // Formato para el eje X: solo hora (ej. 15:30)
        private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val timestamp = value.toLong()
            return mFormat.format(Date(timestamp))
        }
    }
}

/**
 * Clase MarkerView personalizada para mostrar detalles (fecha completa, hora y valor)
 * cuando el usuario toca un punto en el gráfico.
 */
@SuppressLint("ViewConstructor")
class CustomMarkerView(context: Context, layoutResource: Int) :
    MarkerView(context, layoutResource) {

    private val tvMarkerDateTime: TextView = findViewById(R.id.tv_marker_date_time)
    private val tvMarkerValue: TextView = findViewById(R.id.tv_marker_value)

    // Formateador para la fecha y hora completa que se mostrará en el MarkerView
    // (ej. 21 jun 11:42 o 2025-06-21 11:42)
    // Elige el formato que mejor te parezca para la ventana emergente.
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) // Ejemplo: 21 jun 2025 11:42

    // Ajusta el offset para posicionar el marcador correctamente sobre el punto
    override fun getOffset(): MPPointF {
        // Devuelve el offset del marcador. Por defecto, lo centramos en el punto (x,y)
        // y lo movemos hacia arriba para que no tape el punto.
        return MPPointF(-(width / 2).toFloat(), -height.toFloat() - 10f) // -10f para un pequeño margen
    }

    // Se llama cada vez que el MarkerView es redibujado,
    // actualizando el contenido con los datos del punto seleccionado
    override fun refreshContent(e: Entry, highlight: Highlight) {
        // e.x es el timestamp, e.y es el valor de humedad
        val timestamp = e.x.toLong()
        val humidityValue = e.y

        tvMarkerDateTime.text = dateTimeFormat.format(Date(timestamp))
        tvMarkerValue.text = "${String.format("%.1f", humidityValue)} %" // Formatear a un decimal y añadir '%'

        super.refreshContent(e, highlight)
    }
}