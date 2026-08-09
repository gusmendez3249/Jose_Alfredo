package mx.utng.festivaltrack.tv.ui.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DynamicQrCode(
    content: String,
    modifier: Modifier = Modifier,
    foregroundColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizeCount = 21
            val cellWidth = size.width / sizeCount
            val cellHeight = size.height / sizeCount

            val hash = content.hashCode()
            
            fun isFinderPattern(r: Int, c: Int): Boolean {
                if (r < 7 && c < 7) return true
                if (r < 7 && c >= sizeCount - 7) return true
                if (r >= sizeCount - 7 && c < 7) return true
                return false
            }

            for (r in 0 until sizeCount) {
                for (c in 0 until sizeCount) {
                    val drawSquare: Boolean
                    if (isFinderPattern(r, c)) {
                        val inTLBorder = (r == 0 || r == 6 || c == 0 || c == 6) && r < 7 && c < 7
                        val inTLCenter = (r in 2..4) && (c in 2..4) && r < 7 && c < 7
                        val inTRBorder = (r == 0 || r == 6 || c == sizeCount - 7 || c == sizeCount - 1) && r < 7 && c >= sizeCount - 7
                        val inTRCenter = (r in 2..4) && (c in (sizeCount - 5)..(sizeCount - 3)) && r < 7 && c >= sizeCount - 7
                        val inBLBorder = (r == sizeCount - 7 || r == sizeCount - 1 || c == 0 || c == 6) && r >= sizeCount - 7 && c < 7
                        val inBLCenter = (r in (sizeCount - 5)..(sizeCount - 3)) && (c in 2..4) && r >= sizeCount - 7 && c < 7

                        drawSquare = inTLBorder || inTLCenter || inTRBorder || inTRCenter || inBLBorder || inBLCenter
                    } else {
                        val bitShift = (r * sizeCount + c) % 31
                        val bitVal = (hash ushr bitShift) and 1
                        val altBit = ((r * 7 + c * 13 + hash) % 3) == 0
                        drawSquare = bitVal == 1 || altBit
                    }

                    if (drawSquare) {
                        drawRect(
                            color = foregroundColor,
                            topLeft = Offset(c * cellWidth, r * cellHeight),
                            size = Size(cellWidth + 0.5f, cellHeight + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
