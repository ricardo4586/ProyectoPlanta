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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.proyectoplata.R // Necesario para R.layout.marker_view_layout
import com.example.proyectoplata.SharedSensorViewModel
import com.example.proyectoplata.databinding.FragmentNitrogenBinding
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

class NitrogenFragment : Fragment() {

    private val TAG = "NitrogenFragment"
    private var _binding: FragmentNitrogenBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedSensorViewModel: SharedSensorViewModel
    private lateinit var nitrogenChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
        Log.d(TAG, "onCreate: SharedSensorViewModel inicializado en NitrogenFragment.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNitrogenBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: NitrogenFragment layout inflado.")

        nitrogenChart = binding.nitrogenChart
        setupChart(nitrogenChart)
        observeNitrogenData()

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
                valueFormatter = DateAxisFormatter() // Este formatter mostrará solo HH:mm
                textColor = Color.BLACK
                textSize = 10f
                // Opcional: Si las etiquetas aún se superponen, puedes rotarlas:
                // setLabelRotationAngle(45f) // Rota las etiquetas 45 grados.
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = Color.BLACK
                textSize = 10f
                // Opcional: Formateador para el eje Y si quieres añadir la unidad (por ejemplo, "mg/kg" o "ppm")
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        // Asumiendo que el Nitrógeno se mide en alguna unidad como mg/kg o ppm
                        return "${String.format("%.0f", value)} ppm" // Ejemplo: ppm (partes por millón)
                    }
                }
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
            animateX(1500)
        }
    }

    private fun observeNitrogenData() {
        sharedSensorViewModel.nitrogenoEntries.observe(viewLifecycleOwner) { entries ->
            Log.d(TAG, "Observando nitrogenoEntries. Número de entradas: ${entries.size}")
            if (entries.isNotEmpty()) {
                entries.sortBy { it.x }
                val dataSet = LineDataSet(entries, "Nitrógeno (N)").apply {
                    color = Color.parseColor("#8A2BE2") // Azul violeta
                    setCircleColor(Color.parseColor("#8A2BE2"))
                    lineWidth = 2f
                    circleRadius = 4f
                    setDrawCircleHole(false)
                    valueTextSize = 0f // No muestra el valor numérico en cada punto del gráfico principal
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#808A2BE2") // 50% de opacidad
                }

                val lineData = LineData(dataSet)
                nitrogenChart.data = lineData
                nitrogenChart.invalidate()
                Log.d(TAG, "Gráfico de Nitrógeno actualizado con ${entries.size} entradas.")
            } else {
                nitrogenChart.clear()
                nitrogenChart.invalidate()
                Log.d(TAG, "No hay entradas para el gráfico de Nitrógeno.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: NitrogenFragment view destruida.")
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

    /**
     * Clase MarkerView personalizada para mostrar detalles (fecha completa, hora y valor)
     * cuando el usuario toca un punto en el gráfico.
     * Esta clase está anidada dentro de NitrogenFragment.
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
            return MPPointF(-(width / 2).toFloat(), -height.toFloat() - 10f) // -10f para un pequeño margen
        }

        // Se llama cada vez que el MarkerView es redibujado,
        // actualizando el contenido con los datos del punto seleccionado
        override fun refreshContent(e: Entry, highlight: Highlight) {
            val timestamp = e.x.toLong()
            val nitrogenValue = e.y

            tvMarkerDateTime.text = dateTimeFormat.format(Date(timestamp))
            // Asumiendo que el Nitrógeno se mide en alguna unidad como ppm
            tvMarkerValue.text = "${String.format("%.0f", nitrogenValue)} ppm" // Formatear y añadir unidad

            super.refreshContent(e, highlight)
        }
    }
}