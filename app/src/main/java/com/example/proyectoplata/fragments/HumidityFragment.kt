package com.example.proyectoplata.fragments

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

class HumidityFragment : Fragment() { // <-- ¡IMPORTANTE! Nombre de la clase corregido

    private lateinit var lineChartHumidity: LineChart // <-- Nombre de la variable corregido

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_humidity, container, false) // <-- Layout corregido

        lineChartHumidity = view.findViewById(R.id.lineChartHumidityFragment) // <-- ID del LineChart corregido
        setupHumidityChart() // <-- Nombre del método corregido

        return view
    }

    private fun setupHumidityChart() { // <-- Nombre del método corregido
        val humidityData = ArrayList<Entry>() // <-- Nombre de la lista de datos corregido
        // Datos de ejemplo para humedad (ajusta estos valores si tienes datos reales)
        humidityData.add(Entry(0f, 65f))
        humidityData.add(Entry(1f, 68f))
        humidityData.add(Entry(2f, 70f))
        humidityData.add(Entry(3f, 67f))
        humidityData.add(Entry(4f, 72f))

        val dataSet = LineDataSet(humidityData, "Humedad (%)") // <-- Etiqueta del DataSet corregida
        dataSet.color = ColorTemplate.MATERIAL_COLORS[1] // Usar un color diferente para distinguirlo
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        lineChartHumidity.data = lineData
        lineChartHumidity.description.isEnabled = false
        lineChartHumidity.setTouchEnabled(true)
        lineChartHumidity.setPinchZoom(true)
        lineChartHumidity.invalidate()
    }
}