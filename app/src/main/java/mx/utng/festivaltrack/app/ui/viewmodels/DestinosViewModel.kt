package mx.utng.festivaltrack.app.ui.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import mx.utng.festivaltrack.app.auto.Destino
import mx.utng.festivaltrack.app.auto.DestinosCalculator
import mx.utng.festivaltrack.app.auto.RutaActiva
import mx.utng.festivaltrack.app.auto.RutaActivaStore
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import kotlin.coroutines.resume

sealed class DestinosState {
    object Loading : DestinosState()
    data class Success(val destinos: List<Destino>, val ubicacionAproximada: Boolean) : DestinosState()
    data class Error(val mensaje: String) : DestinosState()
}

/**
 * Pantalla 7 "Elige tu destino": calcula distancia y tiempo a cada lugar del festival
 * y guarda la ruta elegida para el modo automóvil.
 */
class DestinosViewModel(application: Application) : AndroidViewModel(application) {

    private val api = FestivalApiService.create()
    private val rutaStore = RutaActivaStore(application)

    private val _state = MutableStateFlow<DestinosState>(DestinosState.Loading)
    val state: StateFlow<DestinosState> = _state

    fun cargar() {
        _state.value = DestinosState.Loading
        viewModelScope.launch {
            try {
                val eventos = async { api.getEventos() }
                val sitios = async { runCatching { api.getSitiosAr() }.getOrDefault(emptyList()) }
                val ubicacion = obtenerUbicacion()
                val (lat, lon) = ubicacion ?: CENTRO_DOLORES
                val destinos = DestinosCalculator.construir(eventos.await(), sitios.await(), lat, lon)
                _state.value = DestinosState.Success(destinos, ubicacionAproximada = ubicacion == null)
            } catch (e: Exception) {
                _state.value = DestinosState.Error("No se pudieron cargar los destinos. Revisa tu conexión.")
            }
        }
    }

    /** Guarda el destino como ruta activa; Android Auto la retoma al conectar el teléfono. */
    fun elegir(destino: Destino) {
        rutaStore.guardar(RutaActiva(destino.nombre, destino.latitud, destino.longitud))
    }

    @SuppressLint("MissingPermission")
    private suspend fun obtenerUbicacion(): Pair<Double, Double>? {
        val context = getApplication<Application>()
        val conPermiso = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!conPermiso) return null
        return suspendCancellableCoroutine { cont ->
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { cont.resume(it?.let { l -> l.latitude to l.longitude }) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    private companion object {
        /** Jardín Principal de Dolores Hidalgo, usado cuando no hay ubicación del teléfono. */
        val CENTRO_DOLORES = 21.1561 to -100.9318
    }
}
