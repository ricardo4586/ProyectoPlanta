package com.example.proyectoplata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.Entry

class SharedSensorViewModel : ViewModel() {

    // LiveData para los valores de los sensores individuales (para HomeFragment)
    private val _temperature = MutableLiveData<Double>()
    val temperature: LiveData<Double> = _temperature

    private val _temperatureSuelo = MutableLiveData<Double>()
    val temperatureSuelo: LiveData<Double> = _temperatureSuelo

    private val _humidity = MutableLiveData<Double>()
    val humidity: LiveData<Double> = _humidity

    private val _humiditySuelo = MutableLiveData<Double>()
    val humiditySuelo: LiveData<Double> = _humiditySuelo

    private val _uvIndex = MutableLiveData<Double>()
    val uvIndex: LiveData<Double> = _uvIndex

    private val _light = MutableLiveData<Double>() // Usado para Voltaje UVA
    val light: LiveData<Double> = _light

    // ELIMINADO: private val _intensidadLuzVisible = MutableLiveData<Double>()
    // ELIMINADO: val intensidadLuzVisible: LiveData<Double> = _intensidadLuzVisible

    // ELIMINADO: private val _intensidadLuzInfrarroja = MutableLiveData<Double>()
    // ELIMINADO: val intensidadLuzInfrarroja: LiveData<Double> = _intensidadLuzInfrarroja

    private val _nitrogeno = MutableLiveData<Int>()
    val nitrogeno: LiveData<Int> = _nitrogeno

    private val _fosforo = MutableLiveData<Int>()
    val fosforo: LiveData<Int> = _fosforo

    private val _potasio = MutableLiveData<Int>()
    val potasio: LiveData<Int> = _potasio

    // LiveData para los datos de los gráficos (listas de Entry)
    private val _temperatureEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    val temperatureEntries: LiveData<ArrayList<Entry>> = _temperatureEntries

    private val _temperatureSueloEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    val temperatureSueloEntries: LiveData<ArrayList<Entry>> = _temperatureSueloEntries

    private val _humidityEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    val humidityEntries: LiveData<ArrayList<Entry>> = _humidityEntries

    private val _humiditySueloEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    val humiditySueloEntries: LiveData<ArrayList<Entry>> = _humiditySueloEntries

    private val _uvIndexEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    val uvIndexEntries: LiveData<ArrayList<Entry>> = _uvIndexEntries

    private val _lightEntries = MutableLiveData<ArrayList<Entry>>(ArrayList()) // Usado para Voltaje UVA
    val lightEntries: LiveData<ArrayList<Entry>> = _lightEntries

    // ELIMINADO: private val _intensidadLuzVisibleEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    // ELIMINADO: val intensidadLuzVisibleEntries: LiveData<ArrayList<Entry>> = _intensidadLuzVisibleEntries

    // ELIMINADO: private val _intensidadLuzInfrarrojaEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    // ELIMINADO: val intensidadLuzInfrarrojaEntries: LiveData<ArrayList<Entry>> = _intensidadLuzInfrarrojaEntries

    private val _nitrogenoEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    val nitrogenoEntries: LiveData<ArrayList<Entry>> = _nitrogenoEntries

    private val _fosforoEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    val fosforoEntries: LiveData<ArrayList<Entry>> = _fosforoEntries

    private val _potasioEntries = MutableLiveData<ArrayList<Entry>>(ArrayList())
    val potasioEntries: LiveData<ArrayList<Entry>> = _potasioEntries

    // Funciones para actualizar los valores de los sensores individuales
    fun updateTemperature(newTemperature: Double) {
        _temperature.value = newTemperature
    }

    fun updateTemperatureSuelo(newTemperatureSuelo: Double) {
        _temperatureSuelo.value = newTemperatureSuelo
    }

    fun updateHumidity(newHumidity: Double) {
        _humidity.value = newHumidity
    }

    fun updateHumiditySuelo(newHumiditySuelo: Double) {
        _humiditySuelo.value = newHumiditySuelo
    }

    fun updateUvIndex(newUvIndex: Double) {
        _uvIndex.value = newUvIndex
    }

    fun updateLight(newLight: Double) { // Usado para Voltaje UVA
        _light.value = newLight
    }

    // ELIMINADO: fun updateIntensidadLuzVisible(newIntensidadLuzVisible: Double) { ... }
    // ELIMINADO: fun updateIntensidadLuzInfrarroja(newIntensidadLuzInfrarroja: Double) { ... }

    fun updateNitrogeno(newNitrogeno: Int) {
        _nitrogeno.value = newNitrogeno
    }

    fun updateFosforo(newFosforo: Int) {
        _fosforo.value = newFosforo
    }

    fun updatePotasio(newPotasio: Int) {
        _potasio.value = newPotasio
    }

    // Funciones para actualizar las listas de Entries para los gráficos
    fun updateTemperatureEntries(newEntries: ArrayList<Entry>) {
        _temperatureEntries.value = newEntries
    }

    fun updateTemperatureSueloEntries(newEntries: ArrayList<Entry>) {
        _temperatureSueloEntries.value = newEntries
    }

    fun updateHumidityEntries(newEntries: ArrayList<Entry>) {
        _humidityEntries.value = newEntries
    }

    fun updateHumiditySueloEntries(newEntries: ArrayList<Entry>) {
        _humiditySueloEntries.value = newEntries
    }

    fun updateUvIndexEntries(newEntries: ArrayList<Entry>) {
        _uvIndexEntries.value = newEntries
    }

    fun updateLightEntries(newEntries: ArrayList<Entry>) {
        _lightEntries.value = newEntries
    }

    // ELIMINADO: fun updateIntensidadLuzVisibleEntries(newEntries: ArrayList<Entry>) { ... }
    // ELIMINADO: fun updateIntensidadLuzInfrarrojaEntries(newEntries: ArrayList<Entry>) { ... }

    fun updateNitrogenoEntries(newEntries: ArrayList<Entry>) {
        _nitrogenoEntries.value = newEntries
    }

    fun updateFosforoEntries(newEntries: ArrayList<Entry>) {
        _fosforoEntries.value = newEntries
    }

    fun updatePotasioEntries(newEntries: ArrayList<Entry>) {
        _potasioEntries.value = newEntries
    }
}
