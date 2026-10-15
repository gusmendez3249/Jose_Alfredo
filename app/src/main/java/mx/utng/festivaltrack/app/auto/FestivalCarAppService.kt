package mx.utng.festivaltrack.app.auto

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Punto de entrada de FestivalTrack en Android Auto.
 *
 * Android Auto se conecta a este servicio (declarado en el manifest con la categoría POI)
 * y le pide una [Session] por cada conexión con la pantalla del automóvil.
 */
class FestivalCarAppService : CarAppService() {

    /**
     * En depuración se acepta cualquier host (incluido el Desktop Head Unit);
     * en producción solo los hosts oficiales de Android Auto.
     */
    override fun createHostValidator(): HostValidator {
        val esDepuracion = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return if (esDepuracion) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session = FestivalCarSession()
}

/**
 * Sesión del modo automóvil: dura mientras el teléfono está conectado al auto.
 * Su primera pantalla es [InicioAutoScreen].
 */
class FestivalCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = InicioAutoScreen(carContext)
}
