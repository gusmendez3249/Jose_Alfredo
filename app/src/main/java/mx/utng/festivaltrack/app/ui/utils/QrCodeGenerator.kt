package mx.utng.festivaltrack.app.ui.utils

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
import kotlin.math.abs

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

            // Hash the content string to produce unique module patterns
            val hash = content.hashCode()
            
            // Function to check finder patterns (top-left, top-right, bottom-left)
            fun isFinderPattern(r: Int, c: Int): Boolean {
                if (r < 7 && c < 7) return true // Top-left
                if (r < 7 && c >= sizeCount - 7) return true // Top-right
                if (r >= sizeCount - 7 && c < 7) return true // Bottom-left
                return false
            }

            // Draw Modules
            for (r in 0 until sizeCount) {
                for (c in 0 until sizeCount) {
                    val drawSquare: Boolean
                    if (isFinderPattern(r, c)) {
                        // Top-left finder
                        val inTLBorder = (r == 0 || r == 6 || c == 0 || c == 6) && r < 7 && c < 7
                        val inTLCenter = (r in 2..4) && (c in 2..4) && r < 7 && c < 7
                        // Top-right finder
                        val inTRBorder = (r == 0 || r == 6 || c == sizeCount - 7 || c == sizeCount - 1) && r < 7 && c >= sizeCount - 7
                        val inTRCenter = (r in 2..4) && (c in (sizeCount - 5)..(sizeCount - 3)) && r < 7 && c >= sizeCount - 7
                        // Bottom-left finder
                        val inBLBorder = (r == sizeCount - 7 || r == sizeCount - 1 || c == 0 || c == 6) && r >= sizeCount - 7 && c < 7
                        val inBLCenter = (r in (sizeCount - 5)..(sizeCount - 3)) && (c in 2..4) && r >= sizeCount - 7 && c < 7

                        drawSquare = inTLBorder || inTLCenter || inTRBorder || inTRCenter || inBLBorder || inBLCenter
                    } else {
                        // Deterministic pseudo-random module based on position and hash
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
