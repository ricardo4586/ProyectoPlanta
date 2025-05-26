package com.example.proyectoplata.fragments // Asegúrate de que este sea el paquete correcto

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.example.proyectoplata.R // Necesario para R.layout y R.id

class TemperatureFragment : Fragment() {

    private lateinit var lineChartTemperature: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout para este fragmento
        val view = inflater.inflate(R.layout.fragment_temperature, container, false)

        // Inicializa el gráfico de temperatura con el ID CORRECTO del XML
        lineChartTemperature = view.findViewById(R.id.lineChartTemperatureFragment)
        setupTemperatureChart() // Llama a la función para configurar los datos del gráfico

        return view
    }

    private fun setupTemperatureChart() {
        val temperatureData = ArrayList<Entry>()
        // Datos de ejemplo para el gráfico de temperatura
        temperatureData.add(Entry(0f, 22.5f))
        temperatureData.add(Entry(1f, 23.0f))
        temperatureData.add(Entry(2f, 21.8f))
        temperatureData.add(Entry(3f, 22.2f))
        temperatureData.add(Entry(4f, 23.5f))

        val dataSet = LineDataSet(temperatureData, "Temperatura (°C)")
        dataSet.color = ColorTemplate.MATERIAL_COLORS[0]
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(false) // No muestra los valores sobre los puntos

        val lineData = LineData(dataSet)
        lineChartTemperature.data = lineData
        lineChartTemperature.description.isEnabled = false // Oculta la descripción por defecto
        lineChartTemperature.setTouchEnabled(true) // Permite interactuar con el gráfico
        lineChartTemperature.setPinchZoom(true) // Permite zoom con dos dedos
        lineChartTemperature.invalidate() // Refresca el gráfico
    }
}