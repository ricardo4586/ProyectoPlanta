package com.example.proyectoplata.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.proyectoplata.R
import com.example.proyectoplata.SharedSensorViewModel
import com.example.proyectoplata.databinding.FragmentHistoryBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log

import androidx.core.content.ContextCompat // AÑADIDO: Import para ContextCompat

class HistoryFragment : Fragment() {

    private val TAG = "HistoryFragment"
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedSensorViewModel: SharedSensorViewModel
    private lateinit var historyLineChart: LineChart
    private lateinit var spinnerSensorType: Spinner
    private lateinit var tvSelectedDateRange: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoDataMessage: TextView

    // Rango de fechas actual (timestamps en milisegundos)
    private var startDate: Long = 0L
    private var endDate: Long = 0L

    // Tipo de sensor seleccionado (índice del spinner)
    private var selectedSensorTypeIndex: Int = 0

    // Formateador para el MarkerView (lo reusamos de los fragmentos de gráfico)
    private val dateTimeFormatMarker = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
        Log.d(TAG, "onCreate: SharedSensorViewModel inicializado en HistoryFragment.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: HistoryFragment layout inflado.")

        historyLineChart = binding.historyLineChart
        spinnerSensorType = binding.spinnerSensorType
        tvSelectedDateRange = binding.tvSelectedDateRange
        progressBar = binding.progressBarHistory
        tvNoDataMessage = binding.tvNoDataMessage

        setupSpinner()
        setupDateRangeButtons()
        setupChart(historyLineChart)

        // Establecer el rango inicial a "Últimas 24 horas" al iniciar
        setLast24HoursRange()
        // Cargar los datos iniciales
        loadHistoricalData(selectedSensorTypeIndex, startDate, endDate)

        return view
    }

    private fun setupSpinner() {
        val sensorTypes = resources.getStringArray(R.array.sensor_types_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sensorTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSensorType.adapter = adapter

        spinnerSensorType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSensorTypeIndex = position
                Log.d(TAG, "Sensor seleccionado: ${sensorTypes[position]}")
                // Vuelve a cargar los datos con el nuevo tipo de sensor y el rango actual
                loadHistoricalData(selectedSensorTypeIndex, startDate, endDate)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No hacer nada
            }
        }
    }

    private fun setupDateRangeButtons() {
        binding.btnLast24Hours.setOnClickListener {
            setLast24HoursRange()
            loadHistoricalData(selectedSensorTypeIndex, startDate, endDate)
        }
        binding.btnLast7Days.setOnClickListener {
            setLastDaysRange(7)
            loadHistoricalData(selectedSensorTypeIndex, startDate, endDate)
        }
        binding.btnLast30Days.setOnClickListener {
            setLastDaysRange(30)
            loadHistoricalData(selectedSensorTypeIndex, startDate, endDate)
        }
        binding.btnCustomRange.setOnClickListener {
            showDateRangePicker()
        }
    }

    private fun setLast24HoursRange() {
        val calendar = Calendar.getInstance()
        endDate = calendar.timeInMillis
        calendar.add(Calendar.HOUR_OF_DAY, -24)
        startDate = calendar.timeInMillis
        updateSelectedDateRangeText()
    }

    private fun setLastDaysRange(days: Int) {
        val calendar = Calendar.getInstance()
        endDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        startDate = calendar.timeInMillis
        updateSelectedDateRangeText()
    }

    private fun showDateRangePicker() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            startDate = selection.first ?: 0L
            endDate = selection.second ?: 0L

            // Asegúrate de que endDate incluya hasta el final del día seleccionado
            // Sumar un día completo menos 1 milisegundo para incluir todo el día
            endDate += (24 * 60 * 60 * 1000) - 1

            updateSelectedDateRangeText()
            loadHistoricalData(selectedSensorTypeIndex, startDate, endDate)
        }
        picker.show(childFragmentManager, picker.toString())
    }

    private fun updateSelectedDateRangeText() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) // Cambié a 'yyyy' para mostrar el año
        val startFormatted = dateFormat.format(Date(startDate))
        val endFormatted = dateFormat.format(Date(endDate))

        tvSelectedDateRange.text = "Rango actual: $startFormatted - $endFormatted"
    }

    private fun setupChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)

            // Configuración del MarkerView personalizado - CREARLO SÓLO UNA VEZ AQUÍ
            val mv = CustomMarkerView(requireContext(), R.layout.marker_view_layout)
            mv.chartView = chart
            setMarker(mv) // Asigna el MarkerView al gráfico

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 3600000f // 1 hora en milisegundos
                labelCount = 5
                valueFormatter = DateAxisFormatter()
                textColor = Color.BLACK
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = Color.BLACK
                textSize = 10f
                // El valueFormatter se establecerá dinámicamente en loadHistoricalData
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
            animateX(1500)
        }
    }

    private fun loadHistoricalData(sensorTypeIndex: Int, startTimestamp: Long, endTimestamp: Long) {
        progressBar.visibility = View.VISIBLE
        tvNoDataMessage.visibility = View.GONE
        historyLineChart.visibility = View.GONE

        // Obtener el LiveData correspondiente según el sensorTypeIndex
        val entriesLiveData = when (sensorTypeIndex) {
            0 -> sharedSensorViewModel.humidityEntries // Humedad Ambiental
            1 -> sharedSensorViewModel.humiditySueloEntries // Humedad del Suelo
            2 -> sharedSensorViewModel.temperatureEntries // Temperatura
            3 -> sharedSensorViewModel.lightEntries // Luz (UVB)
            4 -> sharedSensorViewModel.nitrogenoEntries // Nitrógeno
            5 -> sharedSensorViewModel.fosforoEntries // Fósforo
            6 -> sharedSensorViewModel.potasioEntries // Potasio
            7 -> sharedSensorViewModel.uvIndexEntries // Índice UV
            else -> sharedSensorViewModel.humidityEntries // Valor por defecto
        }

        // Observar los datos filtrados por el rango de tiempo
        entriesLiveData.observe(viewLifecycleOwner) { allEntries ->
            val filteredEntries = allEntries?.filter { entry ->
                entry.x.toLong() in startTimestamp..endTimestamp
            }?.sortedBy { it.x } ?: emptyList() // Asegura que estén ordenados

            Log.d(TAG, "Cargando datos para ${resources.getStringArray(R.array.sensor_types_array)[sensorTypeIndex]} del ${Date(startTimestamp)} al ${Date(endTimestamp)}. Entradas filtradas: ${filteredEntries.size}")

            progressBar.visibility = View.GONE

            if (filteredEntries.isNotEmpty()) {
                tvNoDataMessage.visibility = View.GONE
                historyLineChart.visibility = View.VISIBLE

                val (label, colorRes, valueFormatString) = getSensorChartProperties(sensorTypeIndex)
                // Separar el formato numérico de la unidad para el MarkerView
                val (markerValueFormat, markerUnitLabel) = parseValueFormatAndUnit(valueFormatString)

                val dataSet = LineDataSet(filteredEntries, label).apply {
                    color = ContextCompat.getColor(requireContext(), colorRes)
                    setCircleColor(ContextCompat.getColor(requireContext(), colorRes))
                    lineWidth = 2f
                    circleRadius = 4f
                    setDrawCircleHole(false)
                    valueTextSize = 0f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawFilled(true)
                    fillColor = ContextCompat.getColor(requireContext(), colorRes)
                    fillAlpha = 80
                }

                historyLineChart.data = LineData(dataSet)

                // Actualizar las propiedades del MarkerView existente
                val mv = historyLineChart.marker as? CustomMarkerView
                mv?.currentUnitFormat = markerValueFormat
                mv?.currentUnitLabel = markerUnitLabel

                // Actualizar el formateador del eje Y dinámicamente
                historyLineChart.axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format(valueFormatString, value)
                    }
                }
                historyLineChart.invalidate()
                historyLineChart.animateX(1500)
            } else {
                historyLineChart.clear()
                historyLineChart.invalidate()
                historyLineChart.visibility = View.GONE
                tvNoDataMessage.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Define las propiedades del gráfico (etiqueta, color y formato de valor) según el tipo de sensor.
     * @param sensorTypeIndex El índice del sensor seleccionado en el spinner.
     * @return Un Triple que contiene (etiqueta del dataset, ID del recurso de color, string de formato para el valor del eje Y).
     */
    private fun getSensorChartProperties(sensorTypeIndex: Int): Triple<String, Int, String> {
        return when (sensorTypeIndex) {
            0 -> Triple("Humedad Ambiental (%)", R.color.chart_humidity_ambiental, "%.1f %%")
            1 -> Triple("Humedad del Suelo (%)", R.color.chart_humidity_soil, "%.0f %%")
            2 -> Triple("Temperatura (°C)", R.color.chart_temperature, "%.1f °C")
            3 -> Triple("Luz (V)", R.color.chart_light, "%.2f V")
            4 -> Triple("Nitrógeno (N)", R.color.chart_nitrogen, "%.0f ppm")
            5 -> Triple("Fósforo (P)", R.color.chart_phosphorus, "%.0f ppm")
            6 -> Triple("Potasio (K)", R.color.chart_potassium, "%.0f ppm")
            7 -> Triple("Índice UV", R.color.chart_uv_index, "%.0f")
            else -> Triple("Datos", R.color.black, "%.1f") // Default
        }
    }

    // Helper function to parse the format string into the numerical format and unit label
    private fun parseValueFormatAndUnit(fullFormatString: String): Pair<String, String> {
        // This regex looks for a format specifier like "%.1f" or "%.0f"
        // and captures the remaining part as the unit label.
        val regex = Regex("(%\\.\\d+f|%f)\\s*(.*)") // Regex para capturar el especificador de formato (ej. "%.1f") y el resto como unidad
        val matchResult = regex.find(fullFormatString)

        return if (matchResult != null && matchResult.groupValues.size >= 3) {
            val formatSpecifier = matchResult.groupValues[1] // ej. "%.1f", "%.0f"
            val unitLabel = matchResult.groupValues[2]       // ej. "°C", " %", " ppm"
            Pair(formatSpecifier, unitLabel)
        } else {
            // Default fallback if the format string doesn't match the expected pattern
            Pair("%.1f", "") // Default to one decimal place and no unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: HistoryFragment view destruida.")
    }

    // Clase interna para formatear las etiquetas del eje X como fechas
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
     * Esta clase está anidada dentro de HistoryFragment, para que sea un componente auto-contenido.
     */
    @SuppressLint("ViewConstructor")
    class CustomMarkerView(context: Context, layoutResource: Int) :
        MarkerView(context, layoutResource) {

        private val tvMarkerDateTime: TextView = findViewById(R.id.tv_marker_date_time)
        private val tvMarkerValue: TextView = findViewById(R.id.tv_marker_value)

        private val dateTimeFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

        // Propiedad adicional para el CustomMarkerView para manejar la unidad
        var currentUnitFormat: String = "%.1f" // Formato por defecto (e.g., "%.1f")
        var currentUnitLabel: String = ""      // Etiqueta de la unidad, e.g., "°C", " ppm"

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2).toFloat(), -height.toFloat() - 10f)
        }

        // ESTA ES LA ÚNICA VERSIÓN DE refreshContent QUE DEBES TENER
        override fun refreshContent(e: Entry, highlight: Highlight) {
            val timestamp = e.x.toLong()
            val value = e.y

            tvMarkerDateTime.text = dateTimeFormat.format(Date(timestamp))
            // Usar el formato y la etiqueta de unidad configurados
            tvMarkerValue.text = "${String.format(currentUnitFormat, value)}$currentUnitLabel"

            super.refreshContent(e, highlight)
        }
    } // <-- AQUI DEBE ESTAR EL CIERRE DE CustomMarkerView
} // <-- AQUI DEBE ESTAR EL CIERRE DE HistoryFragment