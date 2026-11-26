package mx.utng.festivaltrack.app.auto

import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import mx.utng.festivaltrack.app.R

/** Límites que impone el auto a las plantillas, para no mandar filas que el host recorta. */
object LimitesAuto {

    /** Valor seguro si el host no informa su límite (API de autos menor a 2). */
    private const val FILAS_POR_DEFECTO = 6

    /** Máximo de filas que el auto muestra en una lista. */
    fun filas(carContext: CarContext): Int =
        if (carContext.carAppApiLevel >= 2) {
            runCatching {
                carContext.getCarService(ConstraintManager::class.java)
                    .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
            }.getOrDefault(FILAS_POR_DEFECTO)
        } else {
            FILAS_POR_DEFECTO
        }
}

/**
 * Botones de la barra superior. El auto admite solo un botón con texto en esa barra,
 * así que los demás van con ícono.
 */
object AccionesAuto {

    /** Dorado de FestivalTrack (#E6C27A), el mismo de la app del teléfono y del reloj. */
    val DORADO: CarColor = CarColor.createCustom(0xFFE6C27A.toInt(), 0xFFE6C27A.toInt())

    /** Fondo del botón principal: el dorado del tema del auto (res/values/tema_auto.xml). */
    val DORADO_BOTON: CarColor = CarColor.PRIMARY

    fun icono(carContext: CarContext, recurso: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, recurso)).build()

    /** Ícono teñido de dorado, para las filas y los acentos. */
    fun iconoDorado(carContext: CarContext, recurso: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, recurso)).setTint(DORADO).build()

    fun boton(carContext: CarContext, recurso: Int, alTocar: () -> Unit): Action =
        Action.Builder().setIcon(icono(carContext, recurso)).setOnClickListener { alTocar() }.build()

    /** Botón ⟳ para volver a cargar la pantalla. */
    fun actualizar(carContext: CarContext, alTocar: () -> Unit): Action =
        Action.Builder()
            .setIcon(icono(carContext, R.drawable.ic_auto_actualizar))
            .setOnClickListener { alTocar() }
            .build()
}
