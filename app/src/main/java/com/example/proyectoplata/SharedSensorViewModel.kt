package com.example.proyectoplata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.Entry

/**
 * SharedSensorViewModel:
 * Un ViewModel que mantiene los datos de los sensores y los expone a la UI
 * a través de LiveData. Esto permite que múltiples componentes (fragmentos)
 * observen los mismos datos y reaccionen a los cambios en tiempo real,
 * mientras que los datos persisten a través de cambios de configuración (ej. rotación de pantalla).
 */
class SharedSensorViewModel : ViewModel() {

    // LiveData para los valores actuales de los sensores
    private val _temperature = MutableLiveData<Double>()
    val temperature: LiveData<Double> get() = _temperature

    // LiveData para la TEMPERATURA DEL SUELO (valor actual)
    private val _temperatureSuelo = MutableLiveData<Double>()
    val temperatureSuelo: LiveData<Double> get() = _temperatureSuelo

    private val _humidity = MutableLiveData<Double>()
    val humidity: LiveData<Double> get() = _humidity

    private val _humiditySuelo = MutableLiveData<Double>()
    val humiditySuelo: LiveData<Double> get() = _humiditySuelo

    private val _uvIndex = MutableLiveData<Double>()
    val uvIndex: LiveData<Double> get() = _uvIndex

    private val _light = MutableLiveData<Double>() // Usado para voltaje UVA
    val light: LiveData<Double> get() = _light

    private val _nitrogeno = MutableLiveData<Int>()
    val nitrogeno: LiveData<Int> get() = _nitrogeno

    private val _fosforo = MutableLiveData<Int>()
    val fosforo: LiveData<Int> get() = _fosforo

    private val _potasio = MutableLiveData<Int>()
    val potasio: LiveData<Int> get() = _potasio

    // LiveData para las listas de entradas de gráficos (si usas MPAndroidChart)
    private val _temperatureEntries = MutableLiveData<ArrayList<Entry>>()
    val temperatureEntries: LiveData<ArrayList<Entry>> get() = _temperatureEntries

    // LiveData para las ENTRADAS DE TEMPERATURA DEL SUELO (para el gráfico)
    private val _temperatureSueloEntries = MutableLiveData<ArrayList<Entry>>()
    val temperatureSueloEntries: LiveData<ArrayList<Entry>> get() = _temperatureSueloEntries

    private val _humidityEntries = MutableLiveData<ArrayList<Entry>>()
    val humidityEntries: LiveData<ArrayList<Entry>> get() = _humidityEntries

    private val _humiditySueloEntries = MutableLiveData<ArrayList<Entry>>()
    val humiditySueloEntries: LiveData<ArrayList<Entry>> get() = _humiditySueloEntries

    private val _uvIndexEntries = MutableLiveData<ArrayList<Entry>>()
    val uvIndexEntries: LiveData<ArrayList<Entry>> get() = _uvIndexEntries

    private val _lightEntries = MutableLiveData<ArrayList<Entry>>()
    val lightEntries: LiveData<ArrayList<Entry>> get() = _lightEntries

    private val _nitrogenoEntries = MutableLiveData<ArrayList<Entry>>()
    val nitrogenoEntries: LiveData<ArrayList<Entry>> get() = _nitrogenoEntries

    private val _fosforoEntries = MutableLiveData<ArrayList<Entry>>()
    val fosforoEntries: LiveData<ArrayList<Entry>> get() = _fosforoEntries

    private val _potasioEntries = MutableLiveData<ArrayList<Entry>>()
    val potasioEntries: LiveData<ArrayList<Entry>> get() = _potasioEntries

    // --- Métodos para actualizar los datos actuales ---

    fun updateTemperature(temp: Double) {
        _temperature.value = temp
    }

    // Función para actualizar la TEMPERATURA DEL SUELO
    fun updateTemperatureSuelo(tempSuelo: Double) {
        _temperatureSuelo.value = tempSuelo
    }

    fun updateHumidity(hum: Double) {
        _humidity.value = hum
    }

    fun updateHumiditySuelo(humSuelo: Double) {
        _humiditySuelo.value = humSuelo
    }

    fun updateUvIndex(uv: Double) {
        _uvIndex.value = uv
    }

    fun updateLight(lightVal: Double) {
        _light.value = lightVal
    }

    fun updateNitrogeno(nitro: Int) {
        _nitrogeno.value = nitro
    }

    fun updateFosforo(phos: Int) {
        _fosforo.value = phos
    }

    fun updatePotasio(pot: Int) {
        _potasio.value = pot
    }

    // --- Métodos para actualizar las listas de Entries para gráficos ---

    fun updateTemperatureEntries(entries: ArrayList<Entry>) {
        _temperatureEntries.value = entries
    }

    // Función para actualizar las ENTRADAS DE TEMPERATURA DEL SUELO
    fun updateTemperatureSueloEntries(entries: ArrayList<Entry>) {
        _temperatureSueloEntries.value = entries
    }

    fun updateHumidityEntries(entries: ArrayList<Entry>) {
        _humidityEntries.value = entries
    }

    fun updateHumiditySueloEntries(entries: ArrayList<Entry>) {
        _humiditySueloEntries.value = entries
    }

    fun updateUvIndexEntries(entries: ArrayList<Entry>) {
        _uvIndexEntries.value = entries
    }

    fun updateLightEntries(entries: ArrayList<Entry>) {
        _lightEntries.value = entries
    }

    fun updateNitrogenoEntries(entries: ArrayList<Entry>) {
        _nitrogenoEntries.value = entries
    }

    fun updateFosforoEntries(entries: ArrayList<Entry>) {
        _fosforoEntries.value = entries
    }

    fun updatePotasioEntries(entries: ArrayList<Entry>) {
        _potasioEntries.value = entries
    }
}
