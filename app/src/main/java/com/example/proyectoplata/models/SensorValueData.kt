package com.example.proyectoplata.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SensorValueData(
    val fecha: String = "",
    val valor: Double = 0.0 // ¡Esto es importante! Debe ser Double
)
    