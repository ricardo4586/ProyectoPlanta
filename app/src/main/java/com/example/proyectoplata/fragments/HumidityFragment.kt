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
import com.example.proyectoplata.R // Puede que necesites esta importación explícita

class HumidityFragment : Fragment() {

    private lateinit var lineChartHumidity: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_humidity, container, false)

        // Asegúrate de que el ID en fragment_humidity.xml sea 'lineChartHumidityFragment'
        lineChartHumidity = view.findViewById(R.id.lineChartHumidityFragment)
        setupHumidityChart()

        return view
    }

    private fun setupHumidityChart() {
        val humidityData = ArrayList<Entry>()
        // Datos de ejemplo para el gráfico de humedad
        humidityData.add(Entry(0f, 60f))
        humidityData.add(Entry(1f, 65f))
        humidityData.add(Entry(2f, 70f))
        humidityData.add(Entry(3f, 68f))
        humidityData.add(Entry(4f, 72f))

        val dataSet = LineDataSet(humidityData, "Humedad (%)")
        dataSet.color = ColorTemplate.MATERIAL_COLORS[1] // Cambia el color si quieres
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