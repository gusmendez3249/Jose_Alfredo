package mx.utng.festivaltrack.app.auto

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.model.Action
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Ubicación y navegación compartidas por las pantallas del modo automóvil.
 */
object UbicacionAuto {

    /** Jardín Principal de Dolores Hidalgo, usado cuando no hay ubicación del auto o del teléfono. */
    val CENTRO_DOLORES = 21.157511 to -100.934529

    /** Una ubicación guardada más vieja que esto ya no sirve: el auto pudo moverse. */
    private const val EDAD_MAXIMA_MS = 60_000L

    /** Tiempo máximo para esperar una ubicación nueva del GPS. */
    private const val ESPERA_GPS_MS = 5_000L

    fun tienePermiso(carContext: CarContext): Boolean =
        ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Ubicación actual, o null si no hay permiso o señal.
     *
     * La última ubicación guardada solo se usa si es reciente; si no, se pide una nueva al GPS
     * (con un límite de espera), para no calcular distancias desde donde estaba el auto antes.
     */
    @SuppressLint("MissingPermission")
    suspend fun obtener(carContext: CarContext): Pair<Double, Double>? {
        if (!tienePermiso(carContext)) return null
        val cliente = LocationServices.getFusedLocationProviderClient(carContext)
        val ultima = esperar(cliente.lastLocation)
        if (ultima != null && edadMs(ultima) <= EDAD_MAXIMA_MS) return ultima.latitude to ultima.longitude

        val cancelar = CancellationTokenSource()
        val nueva = withTimeoutOrNull(ESPERA_GPS_MS) {
            esperar(cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelar.token))
        }
        if (nueva == null) cancelar.cancel()
        return (nueva ?: ultima)?.let { it.latitude to it.longitude }
    }

    /** Última ubicación conocida, sin esperar al GPS (puede ser vieja); null sin permiso o señal. */
    @SuppressLint("MissingPermission")
    suspend fun ultima(carContext: CarContext): Pair<Double, Double>? {
        if (!tienePermiso(carContext)) return null
        return esperar(LocationServices.getFusedLocationProviderClient(carContext).lastLocation)
            ?.let { it.latitude to it.longitude }
    }

    /**
     * Para que la pantalla no se quede cargando mientras llega el GPS: llama a [alObtener] al
     * instante con la última ubicación conocida y otra vez si llega una más reciente y distinta.
     * Recibe null si no hay permiso ni señal (la pantalla usa el centro de Dolores Hidalgo).
     */
    suspend fun enDosPasos(carContext: CarContext, alObtener: (Pair<Double, Double>?) -> Unit) {
        val rapida = ultima(carContext)
        alObtener(rapida)
        val fresca = obtener(carContext)
        if (fresca != null && fresca != rapida) alObtener(fresca)
    }

    /**
     * Sigue la ubicación del auto mientras la pantalla del mapa está a la vista (cada ~2 s o 5 m).
     * Regresa la función que detiene el seguimiento; sin permiso no hace nada.
     */
    @SuppressLint("MissingPermission")
    fun seguir(carContext: CarContext, alMoverse: (Location) -> Unit): () -> Unit {
        if (!tienePermiso(carContext)) return {}
        val cliente = LocationServices.getFusedLocationProviderClient(carContext)
        val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                resultado.lastLocation?.let(alMoverse)
            }
        }
        cliente.requestLocationUpdates(solicitud, callback, Looper.getMainLooper())
        return { cliente.removeLocationUpdates(callback) }
    }

    /**
     * Botón "Usar mi ubicación" para las pantallas que calculan distancias.
     * Solo se muestra si falta el permiso; al concederlo se ejecuta [alConceder].
     */
    fun accionPermiso(carContext: CarContext, alConceder: () -> Unit): Action =
        Action.Builder()
            .setTitle("Usar mi ubicación")
            .setOnClickListener {
                carContext.requestPermissions(
                    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                ) { concedidos, _ -> if (concedidos.isNotEmpty()) alConceder() }
            }
            .build()

    /** Abre la ruta en la app de navegación del auto (Google Maps, Waze…). */
    fun navegar(carContext: CarContext, uriGeo: String) {
        try {
            carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, Uri.parse(uriGeo)))
        } catch (e: Exception) {
            CarToast.makeText(carContext, "No hay una app de navegación disponible", CarToast.LENGTH_LONG).show()
        }
    }

    private fun edadMs(ubicacion: Location): Long =
        (SystemClock.elapsedRealtimeNanos() - ubicacion.elapsedRealtimeNanos) / 1_000_000

    private suspend fun esperar(tarea: Task<Location?>): Location? = suspendCancellableCoroutine { cont ->
        tarea.addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
            .addOnCanceledListener { cont.resume(null) }
    }
}
