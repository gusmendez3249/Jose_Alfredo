package mx.utng.festivaltrack.app.auto.mapa

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat

/**
 * Íconos numerados para las filas de la lista: iguales a los marcadores del mapa,
 * para que el conductor relacione "2" en la lista con "2" en el mapa de un vistazo.
 */
object IconosMapa {

    private const val TAM = 96

    fun numero(etiqueta: String, destacado: Boolean): CarIcon {
        val bitmap = Bitmap.createBitmap(TAM, TAM, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centro = TAM / 2f
        val relleno = Paint(Paint.ANTI_ALIAS_FLAG)
        relleno.color = Color.rgb(28, 27, 31)
        canvas.drawCircle(centro, centro, centro, relleno)
        relleno.color = if (destacado) MapaAutoRenderer.COLOR_DORADO else Color.rgb(250, 245, 230)
        canvas.drawCircle(centro, centro, centro - 6f, relleno)
        val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 27, 31)
            textSize = 44f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(etiqueta, centro, centro + texto.textSize / 3f, texto)
        return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
    }
}
