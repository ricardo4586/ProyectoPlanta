package com.example.proyectoplata.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.example.proyectoplata.databinding.FragmentHumidityBinding
import com.example.proyectoplata.models.SensorValueData
import com.example.proyectoplata.SharedSensorViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet // Importa ILineDataSet
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HumidityFragment : Fragment() {

    private val TAG = "HumidityFragment"
    private var _binding: FragmentHumidityBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var humedadAmbientalRef: DatabaseReference
    private lateinit var humedadSueloRef: DatabaseReference
    private lateinit var sharedSensorViewModel: SharedSensorViewModel

    private lateinit var humidityChart: LineChart
    private lateinit var tvHumedadActualAmbiental: TextView
    private lateinit var tvHumedadActualSuelo: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHumidityBinding.inflate(inflater, container, false)
        val view = binding.root

        firebaseDatabase = FirebaseDatabase.getInstance()
        humedadAmbientalRef = firebaseDatabase.getReference("sensores/humedad_ambiental")
        humedadSueloRef = firebaseDatabase.getReference("sensores/humedad_suelo")

        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)

        tvHumedadActualAmbiental = binding.tvHumedadActualAmbiental
        tvHumedadActualSuelo = binding.tvHumedadActualSuelo
        humidityChart = binding.humidityLineChart

        setupChart(humidityChart)
        observeViewModel()
        readHumidityData()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun readHumidityData() {
        humedadAmbientalRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                val entries = ArrayList<Entry>()
                var latestHumidity: Double = -1.0

                for (childSnapshot in snapshot.children) {
                    val sensorValueData = childSnapshot.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        latestHumidity = data.valor
                        val timestamp = convertDateToTimestamp(data.fecha)
                        if (timestamp != -1L) {
                            entries.add(Entry(timestamp.toFloat(), data.valor.toFloat()))
                        }
                    }
                }
                entries.sortBy { it.x }

                sharedSensorViewModel.updateHumidity(latestHumidity)
                sharedSensorViewModel.updateHumidityEntries(entries)

                if (!snapshot.exists()) {
                    Log.d(TAG, "No se encontraron datos de humedad ambiental en Firebase.")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "No hay datos de humedad ambiental disponibles.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "Error al leer datos de humedad ambiental de Firebase: ${error.message}", error.toException())
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error de Firebase Humedad Ambiental: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        })

        humedadSueloRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                val entries = ArrayList<Entry>()
                var latestHumiditySuelo: Double = -1.0

                for (childSnapshot in snapshot.children) {
                    val sensorValueData = childSnapshot.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        latestHumiditySuelo = data.valor
                        val timestamp = convertDateToTimestamp(data.fecha)
                        if (timestamp != -1L) {
                            entries.add(Entry(timestamp.toFloat(), data.valor.toFloat()))
                        }
                    }
                }
                entries.sortBy { it.x }

                sharedSensorViewModel.updateHumiditySuelo(latestHumiditySuelo)
                sharedSensorViewModel.updateHumiditySueloEntries(entries)

                if (!snapshot.exists()) {
                    Log.d(TAG, "No se encontraron datos de humedad del suelo en Firebase.")
                }
            }

            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "Error al leer datos de humedad del suelo de Firebase: ${error.message}", error.toException())
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error de Firebase Humedad Suelo: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun observeViewModel() {
        sharedSensorViewModel.humidity.observe(viewLifecycleOwner, Observer { humidity ->
            tvHumedadActualAmbiental.text = "Humedad Ambiental: $humidity%"
        })

        sharedSensorViewModel.humidityEntries.observe(viewLifecycleOwner, Observer { ambientEntries ->
            val soilEntries = sharedSensorViewModel.humiditySueloEntries.value ?: emptyList()
            updateHumidityChart(ambientEntries, soilEntries)
        })

        sharedSensorViewModel.humiditySuelo.observe(viewLifecycleOwner, Observer { humiditySuelo ->
            tvHumedadActualSuelo.text = "Humedad del Suelo: $humiditySuelo%"
        })

        sharedSensorViewModel.humiditySueloEntries.observe(viewLifecycleOwner, Observer { soilEntries ->
            val ambientEntries = sharedSensorViewModel.humidityEntries.value ?: emptyList()
            updateHumidityChart(ambientEntries, soilEntries)
        })
    }

    private fun setupChart(chart: LineChart) {
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.description.isEnabled = false
        chart.setNoDataText("Cargando datos de Humedad...")
        chart.setNoDataTextColor(resources.getColor(android.R.color.darker_gray))
    }

    private fun updateHumidityChart(ambientEntries: List<Entry>, soilEntries: List<Entry>) {
        if (ambientEntries.isEmpty() && soilEntries.isEmpty()) {
            humidityChart.clear()
            humidityChart.setNoDataText("No hay datos de Humedad disponibles para mostrar.")
            humidityChart.invalidate()
            return
        }

        val dataSets = ArrayList<LineDataSet>()

        if (ambientEntries.isNotEmpty()) {
            val ambientDataSet = LineDataSet(ambientEntries, "Humedad Ambiental")
            ambientDataSet.color = resources.getColor(android.R.color.holo_blue_light)
            ambientDataSet.setCircleColor(resources.getColor(android.R.color.holo_blue_light))
            ambientDataSet.setDrawValues(false)
            ambientDataSet.lineWidth = 2f
            ambientDataSet.circleRadius = 3f
            dataSets.add(ambientDataSet)
        }

        if (soilEntries.isNotEmpty()) {
            val soilDataSet = LineDataSet(soilEntries, "Humedad del Suelo")
            soilDataSet.color = resources.getColor(android.R.color.holo_green_light)
            soilDataSet.setCircleColor(resources.getColor(android.R.color.holo_green_light))
            soilDataSet.setDrawValues(false)
            soilDataSet.lineWidth = 2f
            soilDataSet.circleRadius = 3f
            dataSets.add(soilDataSet)
        }

        // CORRECCIÓN AQUÍ: Casteo explícito a List<ILineDataSet>
        val lineData = LineData(dataSets as List<ILineDataSet>)
        humidityChart.data = lineData
        humidityChart.invalidate()
    }

    private fun convertDateToTimestamp(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time?.div(1000) ?: -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date string for Humidity: $dateString", e)
            -1L
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}