package mx.utng.festivaltrack.app.auto

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Ubicación y navegación compartidas por las pantallas del modo automóvil.
 */
object UbicacionAuto {

    /** Jardín Principal de Dolores Hidalgo, usado cuando no hay ubicación del teléfono. */
    val CENTRO_DOLORES = 21.1561 to -100.9318

    fun tienePermiso(carContext: CarContext): Boolean =
        ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Última ubicación del teléfono, o null si no hay permiso o señal. */
    @SuppressLint("MissingPermission")
    suspend fun obtener(carContext: CarContext): Pair<Double, Double>? {
        if (!tienePermiso(carContext)) return null
        val ubicacion = suspendCancellableCoroutine { cont ->
            LocationServices.getFusedLocationProviderClient(carContext).lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
        return ubicacion?.let { it.latitude to it.longitude }
    }

    /** Abre la ruta en la app de navegación del auto (Google Maps, Waze…). */
    fun navegar(carContext: CarContext, uriGeo: String) {
        try {
            carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, Uri.parse(uriGeo)))
        } catch (e: Exception) {
            CarToast.makeText(carContext, "No hay una app de navegación disponible", CarToast.LENGTH_LONG).show()
        }
    }
}
