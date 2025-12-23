package org.eu.freex.tools.utils


import androidx.compose.ui.geometry.Rect
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.max

object ImageUtils {
    fun captureFullScreen(): BufferedImage {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        return Robot().createScreenCapture(Rectangle(screenSize))
    }

    fun cropImage(source: BufferedImage, rect: Rect): BufferedImage {
        // 1. 坐标防越界处理（保持你原有的逻辑）
        val x = rect.left.toInt().coerceIn(0, source.width - 1)
        val y = rect.top.toInt().coerceIn(0, source.height - 1)
        val w = rect.width.toInt().coerceIn(1, max(1, source.width - x))
        val h = rect.height.toInt().coerceIn(1, max(1, source.height - y))

        // 2. 获取子图像视图（这只是一个 View，数据还是原来的）
        val subImage = source.getSubimage(x, y, w, h)

        // 3. 【核心修复】创建一个全新的 BufferedImage 并进行深拷贝
        // 处理类型兼容性：如果是 TYPE_CUSTOM (0) 或系统截图可能的不兼容类型，强制转为 ARGB
        val safeType = if (source.type == BufferedImage.TYPE_CUSTOM || source.type == 0) {
            BufferedImage.TYPE_INT_ARGB
        } else {
            source.type
        }

        val newImage = BufferedImage(w, h, safeType)
        val g = newImage.createGraphics()

        // 将裁剪区域画到新图的 (0,0) 位置，彻底重置数据结构
        g.drawImage(subImage, 0, 0, null)
        g.dispose()

        return newImage
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