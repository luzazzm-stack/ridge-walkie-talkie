package app.ridge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun QrCode(
    text: String,
    size: Dp = 180.dp,
    fg: Color = Color(0xFF111111),
    bg: Color = Color.White,
) {
    val matrix = remember(text) {
        if (text.isBlank()) null
        else runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
            )
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 256, 256, hints)
        }.getOrNull()
    }

    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
    ) {
        if (matrix != null) {
            Canvas(Modifier.size(size)) {
                val cellW = this.size.width / matrix.width
                val cellH = this.size.height / matrix.height
                for (y in 0 until matrix.height) {
                    for (x in 0 until matrix.width) {
                        if (matrix.get(x, y)) {
                            drawRect(
                                color = fg,
                                topLeft = Offset(x * cellW, y * cellH),
                                size = Size(cellW + 0.5f, cellH + 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}
