package com.example.proyectoplata.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.proyectoplata.R // Asegúrate de que R esté importado

/**
 * Fragmento de ejemplo para la vista general de Gráficos de Sensores.
 * Aquí podrías poner un dashboard con mini-gráficos o botones para navegar
 * a cada gráfico específico.
 */
class SensorGraphsOverviewFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla un layout simple para este placeholder
        val view = inflater.inflate(R.layout.fragment_graph_placeholder, container, false)
        view.findViewById<TextView>(R.id.graph_title_placeholder).text = "Vista General de Gráficos de Sensores"
        return view
    }
}