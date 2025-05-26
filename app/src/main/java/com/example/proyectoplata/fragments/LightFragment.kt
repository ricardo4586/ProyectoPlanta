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

class LightFragment : Fragment() {

    private lateinit var lineChartLight: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_light, container, false)

        lineChartLight = view.findViewById(R.id.lineChartLightFragment)
        setupLightChart()

        return view
    }

    private fun setupLightChart() {
        val lightData = ArrayList<Entry>()
        lightData.add(Entry(0f, 300f))
        lightData.add(Entry(1f, 450f))
        lightData.add(Entry(2f, 280f))
        lightData.add(Entry(3f, 500f))
        lightData.add(Entry(4f, 380f))
        lightData.add(Entry(5f, 600f))

        val dataSet = LineDataSet(lightData, "Luz (Lux)")
        dataSet.color = ColorTemplate.MATERIAL_COLORS[2]
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        lineChartLight.data = lineData
        lineChartLight.description.isEnabled = false
        lineChartLight.setTouchEnabled(true)
        lineChartLight.setPinchZoom(true)
        lineChartLight.invalidate()
    }
}