// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/utils/ImageUtils.kt
package org.eu.freex.tools.utils

import androidx.compose.ui.geometry.Rect
import org.eu.freex.tools.model.ColorRule
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
        val x = rect.left.toInt().coerceIn(0, source.width - 1)
        val y = rect.top.toInt().coerceIn(0, source.height - 1)
        val w = rect.width.toInt().coerceIn(1, max(1, source.width - x))
        val h = rect.height.toInt().coerceIn(1, max(1, source.height - y))

        val subImage = source.getSubimage(x, y, w, h)
        val rgbArray = IntArray(w * h)
        subImage.getRGB(0, 0, w, h, rgbArray, 0, w)
        val newImage = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        newImage.setRGB(0, 0, w, h, rgbArray, 0, w)
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

    // 简单的模拟去噪 (这里只是做个灰度处理演示)
    fun dummyDenoise(source: BufferedImage): BufferedImage {
        val w = source.width
        val h = source.height
        val newImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = newImg.createGraphics()
        // 绘制原图
        g.drawImage(source, 0, 0, null)
        // 覆盖一层淡淡的半透明遮罩来模拟 "处理过"
        g.color = Color(200, 200, 200, 50)
        g.fillRect(0, 0, w, h)
        g.dispose()
        return newImg
    }

    // 简单的模拟细化 (画个红叉演示)
    fun dummySkeleton(source: BufferedImage): BufferedImage {
        val w = source.width
        val h = source.height
        val newImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = newImg.createGraphics()
        g.drawImage(source, 0, 0, null)

        g.color = Color.RED
        g.stroke = BasicStroke(2f)
        g.drawLine(0, 0, w, h)
        g.drawLine(0, h, w, 0)
        g.dispose()
        return newImg
    }

    fun scanConnectedComponents(
        source: BufferedImage,
        rules: List<ColorRule>,
        minW: Int = 2,
        minH: Int = 2
    ): List<Rect> {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getRGB(0, 0, w, h, pixels, 0, w)

        val visited = BooleanArray(w * h)
        val result = mutableListOf<Rect>()

        val dxs = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
        val dys = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)

        for (i in pixels.indices) {
            if (visited[i]) continue

            if (ColorUtils.isMatchAny(pixels[i], rules)) {
                var minX = i % w
                var maxX = minX
                var minY = i / w
                var maxY = minY

                val queue = ArrayDeque<Int>()
                queue.add(i)
                visited[i] = true

                while (queue.isNotEmpty()) {
                    val curr = queue.removeFirst()
                    val cx = curr % w
                    val cy = curr / w

                    if (cx < minX) minX = cx
                    if (cx > maxX) maxX = cx
                    if (cy < minY) minY = cy
                    if (cy > maxY) maxY = cy

                    for (k in 0 until 8) {
                        val nx = cx + dxs[k]
                        val ny = cy + dys[k]

                        if (nx in 0 until w && ny in 0 until h) {
                            val nIdx = ny * w + nx
                            if (!visited[nIdx]) {
                                if (ColorUtils.isMatchAny(pixels[nIdx], rules)) {
                                    visited[nIdx] = true
                                    queue.add(nIdx)
                                }
                            }
                        }
                    }
                }

                val rectW = maxX - minX + 1
                val rectH = maxY - minY + 1
                if (rectW >= minW && rectH >= minH) {
                    result.add(Rect(minX.toFloat(), minY.toFloat(), (maxX + 1).toFloat(), (maxY + 1).toFloat()))
                }
            } else {
                visited[i] = true
            }
        }
        return result
    }

    fun generateGridRects(
        startX: Int, startY: Int, width: Int, height: Int,
        colGap: Int, rowGap: Int, colCount: Int, rowCount: Int
    ): List<Rect> {
        val list = mutableListOf<Rect>()
        if (width <= 0 || height <= 0) return list
        for (r in 0 until rowCount) {
            for (c in 0 until colCount) {
                val left = startX + c * (width + colGap)
                val top = startY + r * (height + rowGap)
                val right = left + width
                val bottom = top + height
                list.add(Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()))
            }
        }
        return list
    }

    fun binarizeImage(
        source: BufferedImage,
        rules: List<ColorRule>,
        targetRect: Rect
    ): BufferedImage {
        val x = targetRect.left.toInt().coerceIn(0, source.width - 1)
        val y = targetRect.top.toInt().coerceIn(0, source.height - 1)
        val w = targetRect.width.toInt().coerceIn(1, max(1, source.width - x))
        val h = targetRect.height.toInt().coerceIn(1, max(1, source.height - y))

        val resultImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

        for (row in 0 until h) {
            for (col in 0 until w) {
                val srcX = x + col
                val srcY = y + row
                val rgb = source.getRGB(srcX, srcY)
                val isMatch = ColorUtils.isMatchAny(rgb, rules)
                val newColor = if (isMatch) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                resultImg.setRGB(col, row, newColor)
            }
        }
        return resultImg
    }
}