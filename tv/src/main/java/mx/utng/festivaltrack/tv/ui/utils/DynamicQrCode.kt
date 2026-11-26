package mx.utng.festivaltrack.tv.ui.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mx.utng.festivaltrack.shared.util.CodigoQr

/**
 * Código QR real del [content] para escanear con el celular desde el sillón
 * (por ejemplo la página de compra de un concierto).
 *
 * Antes se dibujaba un patrón parecido a un QR que ningún lector podía leer; ahora la matriz
 * sale de [CodigoQr] (ZXing), la misma que usan los boletos del móvil.
 */
@Composable
fun DynamicQrCode(
    content: String,
    modifier: Modifier = Modifier,
    foregroundColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val matriz = remember(content) { runCatching { CodigoQr.matriz(content, margen = 1) }.getOrNull() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(10.dp)
    ) {
        if (matriz == null) return@Box
        Canvas(modifier = Modifier.fillMaxSize()) {
            val celda = size.width / matriz.size
            for (fila in matriz.indices) {
                for (columna in matriz[fila].indices) {
                    if (matriz[fila][columna]) {
                        drawRect(
                            color = foregroundColor,
                            topLeft = Offset(columna * celda, fila * celda),
                            size = Size(celda + 0.5f, celda + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
