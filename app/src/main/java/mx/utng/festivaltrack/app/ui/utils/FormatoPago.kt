package mx.utng.festivaltrack.app.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.time.YearMonth
import java.util.Locale

/** Formatos y validaciones del pago con tarjeta (simulado: la tarjeta no se envía al servidor). */
object FormatoPago {

    /** 4500 → "$4,500". */
    fun precio(monto: Int): String = "$" + String.format(Locale.US, "%,d", monto)

    /** Deja solo dígitos, hasta [max]. */
    fun digitos(texto: String, max: Int): String = texto.filter(Char::isDigit).take(max)

    /**
     * Revisa los datos de la tarjeta; regresa el mensaje del primer problema o null si están bien.
     * [vencimiento] son 4 dígitos MMAA.
     */
    fun validar(titular: String, numero: String, vencimiento: String, cvv: String, hoy: YearMonth = YearMonth.now()): String? {
        if (titular.isBlank() || numero.isBlank() || vencimiento.isBlank() || cvv.isBlank()) {
            return "Llena todos los datos de la tarjeta"
        }
        if (numero.length !in 15..16) return "El número de tarjeta debe tener 16 dígitos"
        if (vencimiento.length != 4) return "Escribe el vencimiento como MM/AA"
        val mes = vencimiento.take(2).toInt()
        if (mes !in 1..12) return "El mes de vencimiento no es válido"
        val vence = YearMonth.of(2000 + vencimiento.takeLast(2).toInt(), mes)
        if (vence.isBefore(hoy)) return "La tarjeta está vencida"
        if (cvv.length !in 3..4) return "El CVV debe tener 3 o 4 dígitos"
        return null
    }

    /** Muestra "4242424242424242" como "4242 4242 4242 4242". */
    val numeroTarjeta = VisualTransformation { texto ->
        val conEspacios = texto.text.chunked(4).joinToString(" ")
        TransformedText(AnnotatedString(conEspacios), object : OffsetMapping {
            // Un espacio después de cada grupo de 4, nunca al final.
            override fun originalToTransformed(offset: Int) = if (offset == 0) 0 else offset + (offset - 1) / 4
            override fun transformedToOriginal(offset: Int) = (offset - offset / 5).coerceIn(0, texto.text.length)
        })
    }

    /** Muestra "1228" como "12/28". */
    val vencimiento = VisualTransformation { texto ->
        val t = texto.text
        val conBarra = if (t.length > 2) t.take(2) + "/" + t.drop(2) else t
        TransformedText(AnnotatedString(conBarra), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset > 2 && t.length > 2) offset + 1 else offset
            override fun transformedToOriginal(offset: Int) = (if (offset > 2) offset - 1 else offset).coerceIn(0, t.length)
        })
    }
}
