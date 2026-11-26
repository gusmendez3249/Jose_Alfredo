package mx.utng.festivaltrack.shared.util

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodigoQrTest {

    /** Dibuja la matriz como imagen (4 px por módulo) y la lee con el lector de ZXing. */
    private fun leer(matriz: Array<BooleanArray>): String {
        val escala = 4
        val alto = matriz.size * escala
        val ancho = matriz[0].size * escala
        val pixeles = IntArray(ancho * alto) { i ->
            val y = i / ancho / escala
            val x = i % ancho / escala
            if (matriz[y][x]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val imagen = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(ancho, alto, pixeles)))
        return QRCodeReader().decode(imagen).text
    }

    @Test
    fun `el QR del boleto se puede leer y trae la firma exacta`() {
        val firma = "FESTIVAL-TICKET-2024::cmwwiitte0004140rxu60kqrn"
        assertEquals(firma, leer(CodigoQr.matriz(firma)))
    }

    @Test
    fun `el QR de compra lleva la direccion completa`() {
        val url = "http://192.168.1.20:5173/checkout/cmwwhsne80005t6rrz27z8q0o"
        assertEquals(url, leer(CodigoQr.matriz(url)))
    }

    @Test
    fun `la matriz es cuadrada e incluye margen blanco`() {
        val m = CodigoQr.matriz("hola", margen = 2)
        assertEquals(m.size, m[0].size)
        assertTrue(m.first().none { it })
        assertTrue(m.all { !it.first() })
    }

    @Test
    fun `textos distintos dan codigos distintos`() {
        val a = CodigoQr.matriz("FESTIVAL-TICKET-2024::a").map { it.toList() }
        val b = CodigoQr.matriz("FESTIVAL-TICKET-2024::b").map { it.toList() }
        assertTrue(a != b)
    }
}
