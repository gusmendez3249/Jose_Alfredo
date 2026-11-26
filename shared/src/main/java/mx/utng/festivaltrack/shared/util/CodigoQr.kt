package mx.utng.festivaltrack.shared.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Genera códigos QR reales (escaneables con cualquier lector) con ZXing.
 *
 * Antes la app dibujaba un patrón "parecido" a un QR a partir de un hash del texto: se veía
 * como QR pero ningún lector lo podía leer. El móvil (boletos) y la TV (comprar desde el
 * celular) dibujan con Compose la matriz que regresa [matriz].
 */
object CodigoQr {

    /**
     * Módulos del QR: `true` = cuadro oscuro. Incluye el margen blanco ([margen] módulos por lado),
     * que los lectores necesitan para encontrar el código.
     *
     * Usa corrección de errores media (M): tolera manchas o reflejos en la pantalla.
     */
    fun matriz(contenido: String, margen: Int = 2): Array<BooleanArray> {
        require(contenido.isNotEmpty()) { "El QR necesita contenido" }
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to margen,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        // Tamaño 0: ZXing usa el mínimo (1 píxel por módulo) y así la matriz es la del QR.
        val bits = QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 0, 0, hints)
        return Array(bits.height) { y -> BooleanArray(bits.width) { x -> bits.get(x, y) } }
    }
}
