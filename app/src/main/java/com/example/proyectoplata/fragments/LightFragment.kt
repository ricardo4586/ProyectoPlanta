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
import com.example.proyectoplata.R // Asegúrate de tener esta importación para R.layout y R.id

class LightFragment : Fragment() { // Mantener el constructor vacío aquí

    private lateinit var lineChartLight: LineChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout del fragmento
        val view = inflater.inflate(R.layout.fragment_light, container, false)

        // Inicializa el gráfico de luz
        // *** CAMBIO CLAVE AQUÍ: El ID debe coincidir con el del XML del fragmento ***
        lineChartLight = view.findViewById(R.id.lineChartLightFragment) // Asegúrate de que el ID en fragment_light.xml sea 'lineChartLightFragment'

        // Llenar el gráfico con datos
        setupLightChart() // Renombramos setDataToChart a setupLightChart para consistencia

        return view
    }

    // Lógica para llenar el gráfico con datos
    private fun setupLightChart() {
        val lightData = ArrayList<Entry>()
        // Datos de ejemplo para el gráfico de luz (ej. lux o un valor de 0 a 1023 de un sensor analógico)
        lightData.add(Entry(0f, 300f))
        lightData.add(Entry(1f, 450f))
        lightData.add(Entry(2f, 280f))
        lightData.add(Entry(3f, 500f))
        lightData.add(Entry(4f, 380f))
        lightData.add(Entry(5f, 600f)) // Más datos de ejemplo

        val dataSet = LineDataSet(lightData, "Luz (Lux)") // Etiqueta del conjunto de datos
        dataSet.color = ColorTemplate.MATERIAL_COLORS[2] // Un color diferente para la luz
        dataSet.valueTextColor = Color.BLACK // Color del texto de los valores
        dataSet.setDrawCircles(true) // Dibuja círculos en los puntos de datos
        dataSet.setDrawValues(false) // No muestra los valores numéricos sobre los puntos

        val lineData = LineData(dataSet)
        lineChartLight.data = lineData
        lineChartLight.description.isEnabled = false // Oculta la descripción por defecto
        lineChartLight.setTouchEnabled(true) // Permite interacción táctil
        lineChartLight.setPinchZoom(true) // Permite hacer zoom con los dedos
        lineChartLight.invalidate() // Refresca el gráfico para que se dibuje
    }
}