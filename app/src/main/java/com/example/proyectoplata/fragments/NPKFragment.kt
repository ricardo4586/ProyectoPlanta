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
import com.example.proyectoplata.databinding.FragmentNpkBinding
import com.example.proyectoplata.models.NPKData // Importa NPKData desde el paquete 'models'
import com.example.proyectoplata.SharedSensorViewModel // Importa SharedSensorViewModel desde el paquete principal
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet // Added for LineData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class NPKFragment : Fragment() {

    private val TAG = "NPKFragment"
    private var _binding: FragmentNpkBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var npkRef: DatabaseReference
    private lateinit var sharedSensorViewModel: SharedSensorViewModel

    private lateinit var npkChart: LineChart
    private lateinit var tvNitrogeno: TextView
    private lateinit var tvFosforo: TextView
    private lateinit var tvPotasio: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNpkBinding.inflate(inflater, container, false)
        val view = binding.root

        firebaseDatabase = FirebaseDatabase.getInstance()
        npkRef = firebaseDatabase.getReference("sensores/npk")

        sharedSensorViewModel = ViewModelProvider(requireActivity()).get(SharedSensorViewModel::class.java)

        tvNitrogeno = binding.tvNitrogeno
        tvFosforo = binding.tvFosforo
        tvPotasio = binding.tvPotasio
        npkChart = binding.npkLineChart

        setupChart(npkChart)

        observeViewModel()

        readNPKData()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun readNPKData() {
        npkRef.addValueEventListener(object : ValueEventListener { // CORRECTED LINE HERE
            override fun onDataChange(@NonNull snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val nitrogenoEntries = ArrayList<Entry>()
                    val fosforoEntries = ArrayList<Entry>()
                    val potasioEntries = ArrayList<Entry>()
                    var latestNPKData: NPKData? = null

                    for (childSnapshot in snapshot.children) {
                        val npkData = childSnapshot.getValue(NPKData::class.java)
                        npkData?.let {
                            val timestamp = convertDateToTimestamp(it.fecha)
                            if (timestamp != -1L) {
                                nitrogenoEntries.add(Entry(timestamp.toFloat(), it.nitrogeno.toFloat()))
                                fosforoEntries.add(Entry(timestamp.toFloat(), it.fosforo.toFloat()))
                                potasioEntries.add(Entry(timestamp.toFloat(), it.potasio.toFloat()))
                            }
                            if (latestNPKData == null || it.fecha > latestNPKData!!.fecha) {
                                latestNPKData = it
                            }
                        }
                    }

                    nitrogenoEntries.sortBy { it.x }
                    fosforoEntries.sortBy { it.x }
                    potasioEntries.sortBy { it.x }

                    // Update LiveData in SharedSensorViewModel
                    sharedSensorViewModel.updateNitrogenoEntries(nitrogenoEntries)
                    sharedSensorViewModel.updateFosforoEntries(fosforoEntries)
                    sharedSensorViewModel.updatePotasioEntries(potasioEntries)

                    latestNPKData?.let {
                        // Assuming updateNitrogeno, updateFosforo, updatePotasio exist for individual latest values
                        sharedSensorViewModel.updateNitrogeno(it.nitrogeno)
                        sharedSensorViewModel.updateFosforo(it.fosforo)
                        sharedSensorViewModel.updatePotasio(it.potasio)
                        Log.d(TAG, "NPK actualizado: N=${it.nitrogeno}, P=${it.fosforo}, K=${it.potasio}")
                    }

                } else {
                    Log.d(TAG, "No se encontraron datos NPK en Firebase.")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "No hay datos NPK disponibles.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(@NonNull error: DatabaseError) {
                Log.e(TAG, "Error al leer datos NPK de Firebase: ${error.message}", error.toException())
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error de Firebase NPK: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun observeViewModel() {
        sharedSensorViewModel.nitrogeno.observe(viewLifecycleOwner, Observer { nitrogeno ->
            tvNitrogeno.text = "Nitrógeno: $nitrogeno"
            // Re-render chart when latest individual value changes (optional, but good for real-time feel)
            updateNPKChart(
                sharedSensorViewModel.nitrogenoEntries.value ?: emptyList(),
                sharedSensorViewModel.fosforoEntries.value ?: emptyList(),
                sharedSensorViewModel.potasioEntries.value ?: emptyList()
            )
        })
        sharedSensorViewModel.fosforo.observe(viewLifecycleOwner, Observer { fosforo ->
            tvFosforo.text = "Fósforo: $fosforo"
            updateNPKChart(
                sharedSensorViewModel.nitrogenoEntries.value ?: emptyList(),
                sharedSensorViewModel.fosforoEntries.value ?: emptyList(),
                sharedSensorViewModel.potasioEntries.value ?: emptyList()
            )
        })
        sharedSensorViewModel.potasio.observe(viewLifecycleOwner, Observer { potasio ->
            tvPotasio.text = "Potasio: $potasio"
            updateNPKChart(
                sharedSensorViewModel.nitrogenoEntries.value ?: emptyList(),
                sharedSensorViewModel.fosforoEntries.value ?: emptyList(),
                sharedSensorViewModel.potasioEntries.value ?: emptyList()
            )
        })

        // Also observe the entries lists to update the chart directly when they change
        sharedSensorViewModel.nitrogenoEntries.observe(viewLifecycleOwner, Observer { _ ->
            updateNPKChart(
                sharedSensorViewModel.nitrogenoEntries.value ?: emptyList(),
                sharedSensorViewModel.fosforoEntries.value ?: emptyList(),
                sharedSensorViewModel.potasioEntries.value ?: emptyList()
            )
        })
        sharedSensorViewModel.fosforoEntries.observe(viewLifecycleOwner, Observer { _ ->
            updateNPKChart(
                sharedSensorViewModel.nitrogenoEntries.value ?: emptyList(),
                sharedSensorViewModel.fosforoEntries.value ?: emptyList(),
                sharedSensorViewModel.potasioEntries.value ?: emptyList()
            )
        })
        sharedSensorViewModel.potasioEntries.observe(viewLifecycleOwner, Observer { _ ->
            updateNPKChart(
                sharedSensorViewModel.nitrogenoEntries.value ?: emptyList(),
                sharedSensorViewModel.fosforoEntries.value ?: emptyList(),
                sharedSensorViewModel.potasioEntries.value ?: emptyList()
            )
        })
    }

    private fun setupChart(chart: LineChart) {
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.description.isEnabled = false
        chart.setNoDataText("Cargando datos NPK...")
        chart.setNoDataTextColor(resources.getColor(android.R.color.darker_gray))
    }

    private fun updateNPKChart(
        nitrogenoEntries: List<Entry>,
        fosforoEntries: List<Entry>,
        potasioEntries: List<Entry>
    ) {
        if (nitrogenoEntries.isEmpty() && fosforoEntries.isEmpty() && potasioEntries.isEmpty()) {
            npkChart.clear()
            npkChart.setNoDataText("No hay datos NPK disponibles para mostrar.")
            npkChart.invalidate()
            return
        }

        val dataSets = ArrayList<LineDataSet>()

        if (nitrogenoEntries.isNotEmpty()) {
            val nitrogenoDataSet = LineDataSet(nitrogenoEntries, "Nitrógeno")
            nitrogenoDataSet.color = resources.getColor(android.R.color.holo_blue_light)
            nitrogenoDataSet.setCircleColor(resources.getColor(android.R.color.holo_blue_light))
            nitrogenoDataSet.setDrawValues(false)
            nitrogenoDataSet.lineWidth = 2f
            nitrogenoDataSet.circleRadius = 3f
            dataSets.add(nitrogenoDataSet)
        }

        if (fosforoEntries.isNotEmpty()) {
            val fosforoDataSet = LineDataSet(fosforoEntries, "Fósforo")
            fosforoDataSet.color = resources.getColor(android.R.color.holo_green_light) // Different color
            fosforoDataSet.setCircleColor(resources.getColor(android.R.color.holo_green_light))
            fosforoDataSet.setDrawValues(false)
            fosforoDataSet.lineWidth = 2f
            fosforoDataSet.circleRadius = 3f
            dataSets.add(fosforoDataSet)
        }

        if (potasioEntries.isNotEmpty()) {
            val potasioDataSet = LineDataSet(potasioEntries, "Potasio")
            potasioDataSet.color = resources.getColor(android.R.color.holo_orange_light) // Different color
            potasioDataSet.setCircleColor(resources.getColor(android.R.color.holo_orange_light))
            potasioDataSet.setDrawValues(false)
            potasioDataSet.lineWidth = 2f
            potasioDataSet.circleRadius = 3f
            dataSets.add(potasioDataSet)
        }

        val lineData = LineData(dataSets as List<ILineDataSet>)
        npkChart.data = lineData
        npkChart.invalidate()
    }

    private fun convertDateToTimestamp(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time?.div(1000) ?: -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date string for NPK: $dateString", e)
            -1L
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}