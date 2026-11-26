package mx.utng.festivaltrack.app.ui.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
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
import mx.utng.festivaltrack.shared.data.remote.EventoDto
import mx.utng.festivaltrack.shared.data.remote.FestivalApiService
import mx.utng.festivaltrack.shared.data.remote.SitioArDto
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
        val kmAlFestival: Double = 0.0,
        /** Radio de error de la ubicación en metros (null si no se conoce). */
        val precisionM: Float? = null,
        /** true si el usuario solo dio permiso de ubicación aproximada (puede fallar cientos de metros). */
        val soloAproximada: Boolean = false
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

    // Datos para recalcular distancias cada vez que el teléfono se mueve, sin volver a pedirlos.
    private var eventosCache: List<EventoDto> = emptyList()
    private var sitiosCache: List<SitioArDto> = emptyList()

    /** Origen desde el que se trazó la ruta actual; si te alejas de él, se recalcula. */
    private var origenDeLaRuta: Pair<Double, Double>? = null

    private val cliente = LocationServices.getFusedLocationProviderClient(application)
    private var siguiendo = false
    private val alMoverse = object : LocationCallback() {
        override fun onLocationResult(resultado: LocationResult) {
            resultado.lastLocation?.let { actualizarUbicacion(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy else null) }
        }
    }

    fun cargar() {
        if (_state.value !is DestinosState.Success) _state.value = DestinosState.Loading
        viewModelScope.launch {
            try {
                val eventos = async { api.getEventos() }
                val sitios = async { runCatching { api.getSitiosAr() }.getOrDefault(emptyList()) }
                val ubicacion = obtenerUbicacion()
                eventosCache = eventos.await()
                sitiosCache = sitios.await()
                _state.value = construirEstado(ubicacion?.first, ubicacion?.second, ubicacion?.third)
                seleccionarInicial()
            } catch (e: Exception) {
                _state.value = DestinosState.Error("No se pudieron cargar los destinos. Revisa tu conexión.")
            }
        }
    }

    /** Estado con distancias desde la ubicación dada (o desde el Jardín si no sirve). */
    private fun construirEstado(lat: Double?, lon: Double?, precision: Float?): DestinosState.Success {
        val (cLat, cLon) = CENTRO_DOLORES
        val km = if (lat != null && lon != null) SalidaCalculator.distanciaKm(lat, lon, cLat, cLon) else 0.0
        val origen = when {
            lat == null || lon == null -> OrigenUbicacion.SIN_UBICACION
            km > RADIO_FESTIVAL_KM -> OrigenUbicacion.LEJOS
            else -> OrigenUbicacion.GPS
        }
        val (oLat, oLon) = if (origen == OrigenUbicacion.GPS) lat!! to lon!! else CENTRO_DOLORES
        val destinos = DestinosCalculator.construir(eventosCache, sitiosCache, oLat, oLon)
        return DestinosState.Success(
            destinos, oLat, oLon, origen, km,
            precisionM = precision.takeIf { origen == OrigenUbicacion.GPS },
            soloAproximada = !tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION) &&
                tienePermiso(Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    /**
     * Sigue la ubicación del teléfono mientras el mapa está abierto (cada ~2 s o 3 m).
     * Llamar a [dejarDeSeguir] al salir para no gastar batería.
     */
    @SuppressLint("MissingPermission")
    fun seguirUbicacion() {
        if (siguiendo || !tienePermiso(Manifest.permission.ACCESS_COARSE_LOCATION) &&
            !tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION)
        ) return
        val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000)
            .setMinUpdateDistanceMeters(3f)
            .build()
        runCatching { cliente.requestLocationUpdates(solicitud, alMoverse, Looper.getMainLooper()) }
            .onSuccess { siguiendo = true }
    }

    fun dejarDeSeguir() {
        if (!siguiendo) return
        cliente.removeLocationUpdates(alMoverse)
        siguiendo = false
    }

    /** Nueva posición del teléfono: mueve el punto, recalcula distancias y, si te alejaste, la ruta. */
    fun actualizarUbicacion(lat: Double, lon: Double, precision: Float?) {
        if (_state.value !is DestinosState.Success) return
        val nuevo = construirEstado(lat, lon, precision)
        _state.value = nuevo
        val destino = (_ruta.value as? RutaState.Lista)?.destino ?: return
        val desde = origenDeLaRuta
        if (desde == null || debeRecalcularRuta(desde.first, desde.second, nuevo.origenLat, nuevo.origenLon)) {
            // El destino conserva su nombre; se toma el de la lista nueva por si cambió la distancia.
            trazarRuta(nuevo.destinos.firstOrNull { it.nombre == destino.nombre } ?: destino, silencioso = true)
        }
    }

    override fun onCleared() {
        dejarDeSeguir()
        super.onCleared()
    }

    private fun tienePermiso(permiso: String) =
        ContextCompat.checkSelfPermission(getApplication(), permiso) == PackageManager.PERMISSION_GRANTED

    /** Guarda el destino como ruta activa (el mapa lo muestra y Android Auto lo retoma). */
    fun elegir(destino: Destino) {
        rutaStore.guardar(RutaActiva(destino.nombre, destino.latitud, destino.longitud))
    }

    /**
     * Selecciona el destino en el mapa y calcula la ruta a pie desde el origen.
     * Con [silencioso] (al caminar) se sigue mostrando la ruta anterior mientras llega la nueva.
     */
    fun trazarRuta(destino: Destino, silencioso: Boolean = false) {
        val s = _state.value as? DestinosState.Success ?: return
        elegir(destino)
        rutaJob?.cancel()
        if (!silencioso) _ruta.value = RutaState.Calculando(destino)
        origenDeLaRuta = s.origenLat to s.origenLon
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

    /** Ubicación actual precisa (lat, lon, precisión en m); si tarda, la última conocida. */
    @SuppressLint("MissingPermission")
    private suspend fun obtenerUbicacion(): Triple<Double, Double, Float?>? {
        if (!tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION) && !tienePermiso(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        fun aTriple(l: android.location.Location?) = l?.let { Triple(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy else null) }
        val actual = withTimeoutOrNull(8000) {
            suspendCancellableCoroutine<Triple<Double, Double, Float?>?> { cont ->
                val token = CancellationTokenSource()
                cont.invokeOnCancellation { token.cancel() }
                cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                    .addOnSuccessListener { cont.resume(aTriple(it)) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
        return actual ?: suspendCancellableCoroutine { cont ->
            cliente.lastLocation
                .addOnSuccessListener { cont.resume(aTriple(it)) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    companion object {
        /** Jardín Principal de Dolores Hidalgo, usado cuando no hay ubicación útil del teléfono. */
        val CENTRO_DOLORES = 21.157511 to -100.934529

        /** Más allá de esta distancia el teléfono no está en el festival. */
        const val RADIO_FESTIVAL_KM = 15.0

        /** Metros que hay que alejarse del punto de partida para volver a pedir la ruta por calles. */
        const val RECALCULAR_RUTA_M = 25.0

        /** true si el teléfono se movió lo suficiente como para trazar la ruta de nuevo. */
        fun debeRecalcularRuta(desdeLat: Double, desdeLon: Double, lat: Double, lon: Double): Boolean =
            SalidaCalculator.distanciaKm(desdeLat, desdeLon, lat, lon) * 1000 >= RECALCULAR_RUTA_M
    }
}
