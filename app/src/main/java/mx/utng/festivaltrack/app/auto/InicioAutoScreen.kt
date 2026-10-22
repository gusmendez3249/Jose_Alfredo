package mx.utng.festivaltrack.app.auto

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

/**
 * Pantalla inicial del modo automóvil.
 *
 * Android Auto solo permite plantillas (lista, cuadrícula, navegación), por lo que el menú
 * se arma como una [ListTemplate]. Cada módulo nuevo del modo automóvil se agrega aquí
 * como una fila más.
 */
class InicioAutoScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val opciones = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Alerta de salida y próximos eventos")
                    .addText("Cuándo salir para llegar a tiempo al siguiente concierto")
                    .setBrowsable(true)
                    .setOnClickListener { screenManager.push(AlertaSalidaScreen(carContext)) }
                    .build()
            )
            .addItem(filaRuta())
            .addItem(
                Row.Builder()
                    .setTitle("Audio en curso")
                    .addText("Disponible próximamente")
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("FestivalTrack")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(opciones)
            .build()
    }

    /** Ruta elegida en el teléfono (pantalla 7); al tocarla abre la navegación del auto. */
    private fun filaRuta(): Row {
        val ruta = RutaActivaStore(carContext).obtener()
            ?: return Row.Builder()
                .setTitle("Ruta al festival")
                .addText("Elige tu destino en el teléfono: Mapa → ícono del auto")
                .build()
        return Row.Builder()
            .setTitle("Ruta activa: ${ruta.nombre}")
            .addText("Toca para iniciar la navegación")
            .setOnClickListener {
                try {
                    carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:${ruta.latitud},${ruta.longitud}")))
                } catch (e: Exception) {
                    CarToast.makeText(carContext, "No hay una app de navegación disponible", CarToast.LENGTH_LONG).show()
                }
            }
            .build()
    }
}
