package org.ncgroup.kscan.scanner

import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import org.ncgroup.kscan.BarcodeFormat
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import com.google.zxing.BarcodeFormat as ZxingFormat

private const val PAYLOAD = "X00298D4XT"

private fun render(
    text: String,
    format: ZxingFormat,
    width: Int,
    height: Int,
): BufferedImage {
    val matrix = MultiFormatWriter().encode(text, format, width, height, mapOf(EncodeHintType.MARGIN to 10))
    val image = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)

    for (y in 0 until height) {
        for (x in 0 until width) {
            image.setRGB(x, y, if (matrix[x, y]) 0x000000 else 0xFFFFFF)
        }
    }

    return image
}

// A camera frame: the code covers a fraction of it, over a background lit unevenly.
private fun frame(
    code: BufferedImage,
    codeWidth: Int,
    codeHeight: Int,
    quarterTurn: Boolean = false,
): BufferedImage {
    val frame = BufferedImage(1920, 1080, BufferedImage.TYPE_3BYTE_BGR)
    val graphics = frame.createGraphics()

    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    graphics.paint = GradientPaint(0f, 0f, Color(220, 218, 214), 1920f, 1080f, Color(150, 148, 145))
    graphics.fillRect(0, 0, 1920, 1080)

    if (quarterTurn) graphics.transform(AffineTransform.getRotateInstance(Math.PI / 2, 960.0, 540.0))

    graphics.drawImage(code, 960 - codeWidth / 2, 540 - codeHeight / 2, codeWidth, codeHeight, null)
    graphics.dispose()

    return frame
}

class FrameDecoderTest {

    private fun decoder() = FrameDecoder(zxingReader(listOf(BarcodeFormat.FORMAT_ALL_FORMATS)))

    @Test
    fun `reads a 1D code too small in the frame to read at its own scale`() {
        val code = render(PAYLOAD, ZxingFormat.CODE_128, 900, 300)

        assertEquals(PAYLOAD, decoder().decode(frame(code, codeWidth = 220, codeHeight = 73))?.text)
    }

    @Test
    fun `reads a 1D code held on its side`() {
        val code = render(PAYLOAD, ZxingFormat.CODE_128, 900, 300)

        assertEquals(
            PAYLOAD,
            decoder().decode(frame(code, codeWidth = 320, codeHeight = 106, quarterTurn = true))?.text,
        )
    }

    @Test
    fun `reads a qr code`() {
        val code = render("https://github.com/ismai117/KScan", ZxingFormat.QR_CODE, 600, 600)

        assertEquals(
            "https://github.com/ismai117/KScan",
            decoder().decode(frame(code, codeWidth = 220, codeHeight = 220))?.text,
        )
    }

    @Test
    fun `finds nothing in a frame holding no barcode`() {
        val blank = frame(render("x", ZxingFormat.QR_CODE, 10, 10), codeWidth = 0, codeHeight = 0)

        assertEquals(null, decoder().decode(blank))
    }
}
