package com.example.proyectoplata.models // Esta línea es CRUCIAL.

// Ajusta las propiedades de esta clase según los datos reales de tus sensores
data class SensorData(
    val fecha: String = "",
    val temperaturaAmbiental: Double = 0.0,
    val humedadAmbiental: Double = 0.0,
    val voltajeUVA: Double = 0.0,
    val indiceUV: Double = 0.0,
    val intensidadLuzVisible: Double = 0.0,
    val intensidadLuzInfrarroja: Double = 0.0
    // Si tu nodo raíz de sensores en Firebase también incluye NPK, añádelos aquí:
    // val nitrogeno: Int = 0,
    // val fosforo: Int = 0,
    // val potasio: Int = 0
)