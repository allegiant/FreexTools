package org.eu.freex.tools.utils


import androidx.compose.ui.geometry.Rect
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.max

object ImageUtils {
    fun captureFullScreen(): BufferedImage {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        return Robot().createScreenCapture(Rectangle(screenSize))
    }

    fun cropImage(source: BufferedImage, rect: Rect): BufferedImage {
        val x = rect.left.toInt().coerceIn(0, source.width - 1)
        val y = rect.top.toInt().coerceIn(0, source.height - 1)
        val w = rect.width.toInt().coerceIn(1, max(1, source.width - x))
        val h = rect.height.toInt().coerceIn(1, max(1, source.height - y))
        return source.getSubimage(x, y, w, h)
    }

    fun pickFile(): File? {
        val dialog = FileDialog(null as Frame?, "选择图片", FileDialog.LOAD)
        dialog.setFilenameFilter { _, name ->
            name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".bmp")
        }
        dialog.isVisible = true
        return if (dialog.directory != null && dialog.file != null) {
            File(dialog.directory + dialog.file)
        } else null
    }
}