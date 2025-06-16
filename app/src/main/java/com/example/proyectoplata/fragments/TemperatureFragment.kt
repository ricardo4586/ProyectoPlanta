package com.example.proyectoplata.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.proyectoplata.databinding.FragmentTemperatureBinding // Asegúrate de que este binding esté correctamente configurado
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.ArrayList

class TemperatureFragment : Fragment() {

    private val TAG = "TemperatureFragment" // Etiqueta para Logcat

    private var _binding: FragmentTemperatureBinding? = null
    private val binding get() = _binding!!

    // Variables para almacenar los valores de temperatura
    private var currentTemperaturaAmbiental: Float = -1f // Valor inicial para indicar que no hay datos
    private var currentTemperaturaSuelo: Float = -1f     // Valor inicial para indicar que no hay datos

    // Listas para los datos de los gráficos (para mostrar el último punto)
    private val temperaturaAmbientalEntries = ArrayList<Entry>()
    private val temperaturaSueloEntries = ArrayList<Entry>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTemperatureBinding.inflate(inflater, container, false)
        val view = binding.root

        // Inicializar gráficos
        setupChart(binding.lineChartTemperaturaAmbiental, "Temperatura Ambiental (°C)")
        setupChart(binding.lineChartTemperaturaSuelo, "Temperatura del Suelo (°C)")

        // Recuperar datos de los argumentos (Bundle)
        arguments?.let {
            currentTemperaturaAmbiental = it.getFloat(ARG_TEMP_AMBIENTAL, -1f)
            currentTemperaturaSuelo = it.getFloat(ARG_TEMP_SUELO, -1f)
            Log.d(TAG, "Datos recibidos: TempAmb=${currentTemperaturaAmbiental}, TempSuelo=${currentTemperaturaSuelo}")
        } ?: run {
            Log.d(TAG, "No se recibieron argumentos para TemperatureFragment.")
        }

        updateUI() // Actualiza los gráficos y TextViews con los datos recibidos

        return view
    }

    /**
     * Actualiza la interfaz de usuario con los datos de temperatura actuales.
     */
    private fun updateUI() {
        if (currentTemperaturaAmbiental != -1f) {
            binding.tvTemperaturaAmbiental.text = String.format("Temperatura Ambiental: %.1f °C", currentTemperaturaAmbiental)
            temperaturaAmbientalEntries.clear()
            temperaturaAmbientalEntries.add(Entry(0f, currentTemperaturaAmbiental))
            updateChart(binding.lineChartTemperaturaAmbiental, temperaturaAmbientalEntries, "Temperatura Ambiental")
        } else {
            binding.tvTemperaturaAmbiental.text = "Temperatura Ambiental: N/D"
            temperaturaAmbientalEntries.clear() // Borrar datos anteriores si no hay nuevos
            updateChart(binding.lineChartTemperaturaAmbiental, temperaturaAmbientalEntries, "Temperatura Ambiental")
        }

        if (currentTemperaturaSuelo != -1f) {
            binding.tvTemperaturaSuelo.text = String.format("Temperatura del Suelo: %.1f °C", currentTemperaturaSuelo)
            temperaturaSueloEntries.clear()
            temperaturaSueloEntries.add(Entry(0f, currentTemperaturaSuelo))
            updateChart(binding.lineChartTemperaturaSuelo, temperaturaSueloEntries, "Temperatura del Suelo")
        } else {
            binding.tvTemperaturaSuelo.text = "Temperatura del Suelo: N/D"
            temperaturaSueloEntries.clear() // Borrar datos anteriores si no hay nuevos
            updateChart(binding.lineChartTemperaturaSuelo, temperaturaSueloEntries, "Temperatura del Suelo")
        }
    }

    /**
     * Configura las propiedades iniciales de un gráfico de línea.
     */
    private fun setupChart(chart: LineChart, descriptionText: String) {
        chart.description.text = descriptionText
        chart.description.textSize = 12f
        chart.description.textColor = Color.DKGRAY
        chart.setNoDataText("No hay datos disponibles")
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.setDrawGridBackground(false)

        val xAxis = chart.xAxis
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawLabels(false) // No mostrar etiquetas en el eje X para un solo punto
        xAxis.setDrawAxisLine(true)
        xAxis.textColor = Color.BLACK

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.setAxisMinimum(-10f) // Rango específico para temperatura
        leftAxis.setAxisMaximum(50f)
        leftAxis.granularity = 1f
        leftAxis.textColor = Color.BLACK

        chart.axisRight.isEnabled = false // Deshabilitar el eje Y derecho

        chart.legend.isEnabled = true
        chart.legend.textColor = Color.BLACK

        chart.animateX(1500)
    }

    /**
     * Actualiza los datos y redibuja un gráfico de línea.
     */
    private fun updateChart(chart: LineChart, entries: ArrayList<Entry>, label: String) {
        val dataSet: LineDataSet
        if (chart.data != null && chart.data.dataSetCount > 0) {
            dataSet = chart.data.getDataSetByIndex(0) as LineDataSet
            dataSet.values = entries
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
        } else {
            dataSet = LineDataSet(entries, label)
            dataSet.color = Color.parseColor("#42A5F5") // Azul claro
            dataSet.setCircleColor(Color.parseColor("#FF7043")) // Naranja para el punto
            dataSet.lineWidth = 2.5f
            dataSet.circleRadius = 5f
            dataSet.setDrawCircleHole(false)
            dataSet.valueTextSize = 12f
            dataSet.setDrawFilled(true)
            dataSet.fillColor = Color.parseColor("#B3E5FC") // Un azul más claro para el relleno
            dataSet.fillAlpha = 80
            dataSet.mode = LineDataSet.Mode.LINEAR
            dataSet.setDrawValues(true)

            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(dataSet)
            val data = LineData(dataSets)
            chart.setData(data)
        }
        chart.invalidate()
        chart.moveViewToX(entries.size.toFloat()) // Mover la vista al último punto
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TEMP_AMBIENTAL = "temperaturaAmbiental"
        private const val ARG_TEMP_SUELO = "temperaturaSuelo"

        /**
         * Método factoría para crear una nueva instancia de TemperatureFragment con argumentos.
         */
        @JvmStatic
        fun newInstance(tempAmbiental: Float, tempSuelo: Float) =
            TemperatureFragment().apply {
                arguments = Bundle().apply {
                    putFloat(ARG_TEMP_AMBIENTAL, tempAmbiental)
                    putFloat(ARG_TEMP_SUELO, tempSuelo)
                }
            }
    }
}