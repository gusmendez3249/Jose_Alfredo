package mx.utng.festivaltrack.app.auto.mapa

import android.graphics.Rect
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer

/**
 * Único receptor de la superficie del auto para toda la sesión.
 *
 * El auto entrega la superficie del mapa una sola vez: si cada pantalla registrara su propio mapa,
 * la segunda nunca la recibiría y se quedaría congelada la imagen de la primera. Aquí se guarda la
 * superficie y se le pasa al mapa de la pantalla que está a la vista ([activar]).
 */
object SuperficieAuto : SurfaceCallback {

    private var registrado = false
    private var contenedor: SurfaceContainer? = null
    private var areaVisible: Rect? = null
    private var areaEstable: Rect? = null
    private var activo: MapaAutoRenderer? = null

    /** El [mapa] de la pantalla que acaba de quedar a la vista empieza a dibujar. */
    fun activar(carContext: CarContext, mapa: MapaAutoRenderer) {
        if (!registrado) {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
            registrado = true
        }
        if (activo !== mapa) activo?.soltarSuperficie()
        activo = mapa
        contenedor?.let(mapa::onSurfaceAvailable)
        areaEstable?.let(mapa::onStableAreaChanged)
        areaVisible?.let(mapa::onVisibleAreaChanged)
    }

    /** El [mapa] dejó de verse: ya no dibuja (si otra pantalla no lo reemplazó antes). */
    fun desactivar(mapa: MapaAutoRenderer) {
        mapa.soltarSuperficie()
        if (activo === mapa) activo = null
    }

    /** Al terminar la sesión del auto se vuelve a registrar en la siguiente. */
    fun reiniciar() {
        registrado = false
        contenedor = null
        areaVisible = null
        areaEstable = null
        activo = null
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        contenedor = surfaceContainer
        activo?.onSurfaceAvailable(surfaceContainer)
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        contenedor = null
        activo?.onSurfaceDestroyed(surfaceContainer)
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        areaVisible = Rect(visibleArea)
        activo?.onVisibleAreaChanged(visibleArea)
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        areaEstable = Rect(stableArea)
        activo?.onStableAreaChanged(stableArea)
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        activo?.onScroll(distanceX, distanceY)
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        activo?.onScale(focusX, focusY, scaleFactor)
    }
}
