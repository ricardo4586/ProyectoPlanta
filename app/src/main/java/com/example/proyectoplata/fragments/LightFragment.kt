package com.example.proyectoplata.fragments

import android.graphics.Color
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
import com.example.proyectoplata.databinding.FragmentLightBinding
import com.example.proyectoplata.SharedSensorViewModel
import com.example.proyectoplata.models.SensorValueData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet // Importar esta interfaz
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LightFragment : Fragment() {

    private val TAG = "LightFragment"
    private var _binding: FragmentLightBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var indiceUvRef: DatabaseReference
    private lateinit var voltajeUvaRef: DatabaseReference

    private lateinit var lightLineChart: LineChart
    private lateinit var sharedSensorViewModel: SharedSensorViewModel

    // TextViews para mostrar los valores actuales
    private lateinit var tvIndiceUvActual: TextView
    private lateinit var tvVoltajeUvaActual: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLightBinding.inflate(inflater, container, false)
        val view = binding.root

        // Inicializar referencias de Firebase
        firebaseDatabase = FirebaseDatabase.getInstance()
        indiceUvRef = firebaseDatabase.getReference("sensores/indice_uv")
        voltajeUvaRef = firebaseDatabase.getReference("sensores/voltaje_uva")

        // Inicializar TextViews
        tvIndiceUvActual = binding.tvIndiceUvActual
        tvVoltajeUvaActual = binding.tvVoltajeUvaActual

        lightLineChart = binding.lightLineChart
        setupChart(lightLineChart)

        observeViewModel() // Inicia la observación de los datos del ViewModel
        readLightData()    // Inicia la lectura de los datos desde Firebase

        return view
    }

    private fun setupChart(chart: LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setNoDataText("No hay datos de luz disponibles para mostrar.")

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.setLabelCount(4, true) // Muestra 4 etiquetas en el eje X
        xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            private val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return format.format(Date(value.toLong() * 1000)) // Multiplicar por 1000 para ms
            }
        }

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true) // Rejilla para el eje Y
        leftAxis.axisMinimum = 0f // Iniciar el eje Y en 0

        chart.axisRight.isEnabled = false // Deshabilitar el eje Y derecho
        chart.legend.setDrawInside(false) // Leyenda fuera del gráfico
    }

    private fun readLightData() {
        // Listener para Indice UV
        indiceUvRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                val uvEntries = ArrayList<Entry>()
                for (childSnapshot in snapshot.children) {
                    val sensorValueData = childSnapshot.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        val timestamp = convertDateToTimestamp(data.fecha)
                        if (timestamp != -1L) {
                            uvEntries.add(Entry(timestamp.toFloat(), data.valor.toFloat()))
                        }
                    }
                }
                sharedSensorViewModel.updateUvIndexEntries(uvEntries)
                Log.d(TAG, "UV Entries size: ${uvEntries.size}") // Log para depuración

                // Actualizar el TextView del último valor
                val lastChild = snapshot.children.lastOrNull()
                lastChild?.let {
                    val sensorValueData = it.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        sharedSensorViewModel.updateUvIndex(data.valor)
                    }
                }
            }

            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "Error al leer índice UV: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error Firebase UV: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listener para Voltaje UVA
        voltajeUvaRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                val uvaEntries = ArrayList<Entry>()
                for (childSnapshot in snapshot.children) {
                    val sensorValueData = childSnapshot.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        val timestamp = convertDateToTimestamp(data.fecha)
                        if (timestamp != -1L) {
                            uvaEntries.add(Entry(timestamp.toFloat(), data.valor.toFloat()))
                        } else {
                            // Log si la fecha no se pudo convertir
                            Log.w(TAG, "Fecha no válida o nula para UVA: ${data.fecha}")
                        }
                    } ?: run {
                        // Log si SensorValueData es nulo (ej. si el nodo está vacío)
                        Log.w(TAG, "SensorValueData nulo para un hijo en voltaje_uva: ${childSnapshot.key}")
                    }
                }
                sharedSensorViewModel.updateLightEntries(uvaEntries) // 'light' es para Voltaje UVA
                Log.d(TAG, "UVA Entries size: ${uvaEntries.size}") // Log para depuración

                // Actualizar el TextView del último valor
                val lastChild = snapshot.children.lastOrNull()
                lastChild?.let {
                    val sensorValueData = it.getValue(SensorValueData::class.java)
                    sensorValueData?.let { data ->
                        sharedSensorViewModel.updateLight(data.valor) // 'light' es para Voltaje UVA
                    }
                }
            }

            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "Error al leer voltaje UVA: ${error.message}", error.toException())
                if (isAdded) Toast.makeText(requireContext(), "Error Firebase UVA: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun observeViewModel() {
        sharedSensorViewModel.uvIndex.observe(viewLifecycleOwner, Observer { uv ->
            tvIndiceUvActual.text = "Índice UV: $uv"
        })

        sharedSensorViewModel.light.observe(viewLifecycleOwner, Observer { lightVal ->
            tvVoltajeUvaActual.text = "Voltaje UVA: $lightVal V"
        })

        // Observa los datos del gráfico de UV y UVA
        sharedSensorViewModel.uvIndexEntries.observe(viewLifecycleOwner, Observer { uvEntries ->
            // Asegura que uvaEntries también esté actualizada al llamar updateLightChart
            updateLightChart(uvEntries, sharedSensorViewModel.lightEntries.value ?: ArrayList())
        })

        sharedSensorViewModel.lightEntries.observe(viewLifecycleOwner, Observer { uvaEntries ->
            // Asegura que uvEntries también esté actualizada al llamar updateLightChart
            updateLightChart(sharedSensorViewModel.uvIndexEntries.value ?: ArrayList(), uvaEntries)
        })
    }

    private fun updateLightChart(uvEntries: ArrayList<Entry>, uvaEntries: ArrayList<Entry>) {
        if (uvEntries.isEmpty() && uvaEntries.isEmpty()) {
            lightLineChart.clear()
            lightLineChart.setNoDataText("No hay datos de luz disponibles.")
            lightLineChart.invalidate()
            return
        }

        val dataSets = ArrayList<ILineDataSet>() // CAMBIO CLAVE AQUÍ: Declarar como ILineDataSet

        // DataSet para Índice UV
        if (uvEntries.isNotEmpty()) {
            val uvDataSet = LineDataSet(uvEntries, "Índice UV")
            uvDataSet.color = Color.parseColor("#FFA500") // Naranja
            uvDataSet.setCircleColor(Color.parseColor("#FFA500"))
            uvDataSet.setDrawValues(false)
            uvDataSet.lineWidth = 2f
            uvDataSet.circleRadius = 3f
            dataSets.add(uvDataSet)
        }

        // DataSet para Voltaje UVA
        if (uvaEntries.isNotEmpty()) {
            val uvaDataSet = LineDataSet(uvaEntries, "Voltaje UVA")
            uvaDataSet.color = Color.parseColor("#8A2BE2") // Azul Violeta
            uvaDataSet.setCircleColor(Color.parseColor("#8A2BE2"))
            uvaDataSet.setDrawValues(false)
            uvaDataSet.lineWidth = 2f
            uvaDataSet.circleRadius = 3f
            dataSets.add(uvaDataSet)
        }

        val lineData = LineData(dataSets) // El constructor ahora recibe ArrayList<ILineDataSet>
        lightLineChart.data = lineData
        lightLineChart.invalidate()
        lightLineChart.animateX(800) // Animación al cargar el gráfico
    }

    // Función para convertir la fecha de Firebase (ISO 8601) a timestamp en segundos
    private fun convertDateToTimestamp(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC") // Importante si la fecha es UTC
            format.parse(dateString)?.time?.div(1000) ?: -1L // Dividir por 1000 para segundos
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date string: $dateString", e)
            -1L
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}