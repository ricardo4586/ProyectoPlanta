package com.example.proyectoplata.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.proyectoplata.SharedSensorViewModel
import com.example.proyectoplata.databinding.FragmentPhosphorusBinding // Asegúrate de que este binding exista
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class PhosphorusFragment : Fragment() {

    private val TAG = "PhosphorusFragment"
    private var _binding: FragmentPhosphorusBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedSensorViewModel: SharedSensorViewModel
    private lateinit var phosphorusChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
        Log.d(TAG, "onCreate: SharedSensorViewModel inicializado en PhosphorusFragment.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhosphorusBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: PhosphorusFragment layout inflado.")

        phosphorusChart = binding.phosphorusChart
        setupChart(phosphorusChart)
        observePhosphorusData()

        return view
    }

    private fun setupChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = DateAxisFormatter()
                textColor = Color.BLACK
                textSize = 10f
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

    private fun observePhosphorusData() {
        sharedSensorViewModel.fosforoEntries.observe(viewLifecycleOwner) { entries ->
            Log.d(TAG, "Observando fosforoEntries. Número de entradas: ${entries.size}")
            if (entries.isNotEmpty()) {
                entries.sortBy { it.x }
                val dataSet = LineDataSet(entries, "Fósforo (P)").apply {
                    color = Color.parseColor("#00FF00") // Verde brillante
                    setCircleColor(Color.parseColor("#00FF00"))
                    lineWidth = 2f
                    circleRadius = 4f
                    setDrawCircleHole(false)
                    valueTextSize = 0f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#8000FF00")
                }

                val lineData = LineData(dataSet)
                phosphorusChart.data = lineData
                phosphorusChart.invalidate()
                Log.d(TAG, "Gráfico de Fósforo actualizado con ${entries.size} entradas.")
            } else {
                phosphorusChart.clear()
                phosphorusChart.invalidate()
                Log.d(TAG, "No hay entradas para el gráfico de Fósforo.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: PhosphorusFragment view destruida.")
    }

    private class DateAxisFormatter : ValueFormatter() {
        private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val timestamp = value.toLong()
            return mFormat.format(Date(timestamp))
        }
    }
}
