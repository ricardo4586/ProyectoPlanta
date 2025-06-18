package com.example.proyectoplata.models

/**
 * NPKData:
 * Clase de modelo de datos que representa los valores de Nitrógeno (N),
 * Fósforo (P) y Potasio (K) para la lectura de sensores.
 *
 * Los nombres de las propiedades (nitrogeno, fosforo, potasio) deben
 * coincidir exactamente con los nombres de los campos en tu base de datos Firebase.
 *
 * @property nitrogeno Valor de Nitrógeno.
 * @property fosforo Valor de Fósforo.
 * @property potasio Valor de Potasio.
 */
data class NPKData(
    val nitrogeno: Int = 0,
    val fosforo: Int = 0,
    val potasio: Int = 0
)
