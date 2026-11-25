package mx.utng.festivaltrack.app.ui.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import mx.utng.festivaltrack.app.auto.Destino
import mx.utng.festivaltrack.app.auto.DestinosCalculator
import mx.utng.festivaltrack.app.auto.RutaActiva
import mx.utng.festivaltrack.app.auto.RutaActivaStore
import mx.utng.festivaltrack.app.auto.SalidaCalculator
import mx.utng.festivaltrack.app.ui.utils.Ruta
import mx.utng.festivaltrack.app.ui.utils.RutaPeatonal
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import kotlin.coroutines.resume

/** De dónde se calculan las distancias. */
enum class OrigenUbicacion {
    /** Ubicación real del teléfono. */
    GPS,
    /** Sin permiso o sin señal: se usa el Jardín Principal. */
    SIN_UBICACION,
    /** El teléfono está lejos de Dolores Hidalgo: se usa el Jardín Principal. */
    LEJOS
}

sealed class DestinosState {
    object Loading : DestinosState()
    data class Success(
        val destinos: List<Destino>,
        val origenLat: Double,
        val origenLon: Double,
        val origen: OrigenUbicacion,
        /** Kilómetros del teléfono al festival cuando [origen] es [OrigenUbicacion.LEJOS]. */
        val kmAlFestival: Double = 0.0
    ) : DestinosState() {
        val ubicacionAproximada: Boolean get() = origen != OrigenUbicacion.GPS
    }
    data class Error(val mensaje: String) : DestinosState()
}

sealed class RutaState {
    object SinDestino : RutaState()
    data class Calculando(val destino: Destino) : RutaState()
    data class Lista(val destino: Destino, val ruta: Ruta) : RutaState()
}

/**
 * Lugares del festival con distancia y tiempo desde la ubicación real del teléfono.
 *
 * Lo usan el mapa (ruta a pie por calles) y la lista "GPS del festival". El destino elegido se
 * guarda como ruta activa: el mapa lo preselecciona y el modo automóvil lo retoma.
 */
class DestinosViewModel(application: Application) : AndroidViewModel(application) {

    private val api = FestivalApiService.create()
    private val rutaStore = RutaActivaStore(application)

    private val _state = MutableStateFlow<DestinosState>(DestinosState.Loading)
    val state: StateFlow<DestinosState> = _state

    private val _ruta = MutableStateFlow<RutaState>(RutaState.SinDestino)
    val ruta: StateFlow<RutaState> = _ruta

    private var rutaJob: Job? = null

    fun cargar() {
        if (_state.value !is DestinosState.Success) _state.value = DestinosState.Loading
        viewModelScope.launch {
            try {
                val eventos = async { api.getEventos() }
                val sitios = async { runCatching { api.getSitiosAr() }.getOrDefault(emptyList()) }
                val ubicacion = obtenerUbicacion()
                val (cLat, cLon) = CENTRO_DOLORES
                val km = ubicacion?.let { SalidaCalculator.distanciaKm(it.first, it.second, cLat, cLon) } ?: 0.0
                val origen = when {
                    ubicacion == null -> OrigenUbicacion.SIN_UBICACION
                    km > RADIO_FESTIVAL_KM -> OrigenUbicacion.LEJOS
                    else -> OrigenUbicacion.GPS
                }
                val (lat, lon) = if (origen == OrigenUbicacion.GPS) ubicacion!! else CENTRO_DOLORES
                val destinos = DestinosCalculator.construir(eventos.await(), sitios.await(), lat, lon)
                _state.value = DestinosState.Success(destinos, lat, lon, origen, km)
                seleccionarInicial()
            } catch (e: Exception) {
                _state.value = DestinosState.Error("No se pudieron cargar los destinos. Revisa tu conexión.")
            }
        }
    }

    /** Guarda el destino como ruta activa (el mapa lo muestra y Android Auto lo retoma). */
    fun elegir(destino: Destino) {
        rutaStore.guardar(RutaActiva(destino.nombre, destino.latitud, destino.longitud))
    }

    /** Selecciona el destino en el mapa y calcula la ruta a pie desde el origen. */
    fun trazarRuta(destino: Destino) {
        val s = _state.value as? DestinosState.Success ?: return
        elegir(destino)
        rutaJob?.cancel()
        _ruta.value = RutaState.Calculando(destino)
        rutaJob = viewModelScope.launch {
            val ruta = RutaPeatonal.calcular(s.origenLat, s.origenLon, destino.latitud, destino.longitud)
            _ruta.value = RutaState.Lista(destino, ruta)
        }
    }

    /**
     * Al entrar al mapa: preselecciona la ruta activa guardada (la que eligió el usuario en
     * "GPS del festival") o, si no hay, el destino del próximo evento.
     */
    fun seleccionarInicial() {
        val s = _state.value as? DestinosState.Success ?: return
        if (s.destinos.isEmpty()) return
        val guardada = rutaStore.obtener()
        val destino = guardada?.let { r ->
            s.destinos.minByOrNull { SalidaCalculator.distanciaKm(it.latitud, it.longitud, r.latitud, r.longitud) }
                ?.takeIf { SalidaCalculator.distanciaKm(it.latitud, it.longitud, r.latitud, r.longitud) < 0.05 }
        } ?: s.destinos.first()
        val actual = (_ruta.value as? RutaState.Lista)?.destino ?: (_ruta.value as? RutaState.Calculando)?.destino
        if (actual != destino) trazarRuta(destino)
    }

    /** Ubicación actual precisa; si tarda, la última conocida. */
    @SuppressLint("MissingPermission")
    private suspend fun obtenerUbicacion(): Pair<Double, Double>? {
        val context = getApplication<Application>()
        val conPermiso = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!conPermiso) return null
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val actual = withTimeoutOrNull(8000) {
            suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                val token = CancellationTokenSource()
                cont.invokeOnCancellation { token.cancel() }
                cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                    .addOnSuccessListener { cont.resume(it?.let { l -> l.latitude to l.longitude }) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
        return actual ?: suspendCancellableCoroutine { cont ->
            cliente.lastLocation
                .addOnSuccessListener { cont.resume(it?.let { l -> l.latitude to l.longitude }) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    companion object {
        /** Jardín Principal de Dolores Hidalgo, usado cuando no hay ubicación útil del teléfono. */
        val CENTRO_DOLORES = 21.1563 to -100.9338

        /** Más allá de esta distancia el teléfono no está en el festival. */
        const val RADIO_FESTIVAL_KM = 15.0
    }
}
