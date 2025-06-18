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
import com.example.proyectoplata.databinding.FragmentUvIndexBinding // Asegúrate de que este binding exista
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class UvIndexFragment : Fragment() {

    private val TAG = "UvIndexFragment"
    private var _binding: FragmentUvIndexBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedSensorViewModel: SharedSensorViewModel
    private lateinit var uvIndexChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
        Log.d(TAG, "onCreate: SharedSensorViewModel inicializado en UvIndexFragment.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUvIndexBinding.inflate(inflater, container, false)
        val view = binding.root
        Log.d(TAG, "onCreateView: UvIndexFragment layout inflado.")

        uvIndexChart = binding.uvIndexChart
        setupChart(uvIndexChart)
        observeUvIndexData()

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

    private fun observeUvIndexData() {
        sharedSensorViewModel.uvIndexEntries.observe(viewLifecycleOwner) { entries ->
            Log.d(TAG, "Observando uvIndexEntries. Número de entradas: ${entries.size}")
            if (entries.isNotEmpty()) {
                entries.sortBy { it.x }
                val dataSet = LineDataSet(entries, "Índice UV").apply {
                    color = Color.parseColor("#FFC107") // Amarillo
                    setCircleColor(Color.parseColor("#FFC107"))
                    lineWidth = 2f
                    circleRadius = 4f
                    setDrawCircleHole(false)
                    valueTextSize = 0f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#80FFC107")
                }

                val lineData = LineData(dataSet)
                uvIndexChart.data = lineData
                uvIndexChart.invalidate()
                Log.d(TAG, "Gráfico de índice UV actualizado con ${entries.size} entradas.")
            } else {
                uvIndexChart.clear()
                uvIndexChart.invalidate()
                Log.d(TAG, "No hay entradas para el gráfico de índice UV.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView: UvIndexFragment view destruida.")
    }

    private class DateAxisFormatter : ValueFormatter() {
        private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val timestamp = value.toLong()
            return mFormat.format(Date(timestamp))
        }
    }
}
