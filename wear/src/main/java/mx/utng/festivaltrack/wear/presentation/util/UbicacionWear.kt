package mx.utng.festivaltrack.wear.presentation.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/** Posición del reloj (o del teléfono con el que está vinculado). */
data class Posicion(val latitud: Double, val longitud: Double, val precisionM: Float?)

/** Estado de la ubicación para las pantallas. */
data class EstadoUbicacion(
    val posicion: Posicion?,
    /** false si el usuario negó el permiso. */
    val conPermiso: Boolean
)

private fun tienePermiso(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * Ubicación en vivo mientras la pantalla está abierta: pide el permiso si falta, entrega la
 * última posición conocida de inmediato y luego se actualiza cada ~3 s o 5 m.
 * Al salir de la pantalla deja de pedir ubicaciones para ahorrar batería.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberUbicacionEnVivo(): EstadoUbicacion {
    val context = LocalContext.current
    var permiso by remember { mutableStateOf(tienePermiso(context)) }
    var posicion by remember { mutableStateOf<Posicion?>(null) }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        permiso = r.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (!permiso) {
            pedirPermiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    DisposableEffect(permiso) {
        if (!permiso) return@DisposableEffect onDispose { }
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                resultado.lastLocation?.let {
                    posicion = Posicion(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy else null)
                }
            }
        }
        runCatching {
            cliente.lastLocation.addOnSuccessListener { l ->
                if (l != null && posicion == null) posicion = Posicion(l.latitude, l.longitude, if (l.hasAccuracy()) l.accuracy else null)
            }
            val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000)
                .setMinUpdateDistanceMeters(5f)
                .build()
            cliente.requestLocationUpdates(solicitud, callback, Looper.getMainLooper())
        }
        onDispose { cliente.removeLocationUpdates(callback) }
    }
    return EstadoUbicacion(posicion, permiso)
}
