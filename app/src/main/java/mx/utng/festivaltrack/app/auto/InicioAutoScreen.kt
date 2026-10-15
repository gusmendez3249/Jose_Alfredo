package mx.utng.festivaltrack.app.auto

import androidx.car.app.CarContext
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
            .addItem(
                Row.Builder()
                    .setTitle("Ruta al festival")
                    .addText("Disponible próximamente")
                    .build()
            )
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
}
