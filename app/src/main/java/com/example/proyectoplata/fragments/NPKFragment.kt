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

class NPKFragment : Fragment() {

    private lateinit var lineChartNPK: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_npk, container, false)

        // Asegúrate de que el ID en fragment_npk.xml sea 'lineChartNPKFragment'
        lineChartNPK = view.findViewById(R.id.lineChartNPKFragment)
        setupNPKChart()

        return view
    }

    private fun setupNPKChart() {
        val npkData = ArrayList<Entry>()
        // Datos de ejemplo para el gráfico NPK (pueden ser valores de N, P, K o un índice combinado)
        // Aquí usamos un valor simple como ejemplo
        npkData.add(Entry(0f, 0.5f))
        npkData.add(Entry(1f, 0.7f))
        npkData.add(Entry(2f, 0.6f))
        npkData.add(Entry(3f, 0.8f))
        npkData.add(Entry(4f, 0.75f))

        val dataSet = LineDataSet(npkData, "NPK (Índice)")
        dataSet.color = ColorTemplate.MATERIAL_COLORS[3] // Cambia el color si quieres
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        lineChartNPK.data = lineData
        lineChartNPK.description.isEnabled = false
        lineChartNPK.setTouchEnabled(true)
        lineChartNPK.setPinchZoom(true)
        lineChartNPK.invalidate()
    }
}