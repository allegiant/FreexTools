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
        // 1. 坐标防越界处理
        val x = rect.left.toInt().coerceIn(0, source.width - 1)
        val y = rect.top.toInt().coerceIn(0, source.height - 1)
        val w = rect.width.toInt().coerceIn(1, max(1, source.width - x))
        val h = rect.height.toInt().coerceIn(1, max(1, source.height - y))

        // 2. 获取子图像视图
        val subImage = source.getSubimage(x, y, w, h)

        // 3. 【终极修复】使用 IntArray 直接拷贝像素数据
        // 这绕过了 Graphics2D 所有的渲染管道和兼容性问题
        val rgbArray = IntArray(w * h)
        // 从 subImage 读取像素到数组 (Array, offset, scansize)
        subImage.getRGB(0, 0, w, h, rgbArray, 0, w)

        // 创建统一格式的新图片 (ARGB 兼容性最好)
        val newImage = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        // 将像素数组写回新图片
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

    /**
     * 连通域扫描算法 (自动切割字符)
     * @param source 原图
     * @param rules 启用的颜色规则
     * @param minW 最小宽度过滤
     * @param minH 最小高度过滤
     */
    fun scanConnectedComponents(
        source: BufferedImage,
        rules: List<ColorRule>,
        minW: Int = 2,
        minH: Int = 2
    ): List<Rect> {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        // 一次性获取像素数组，性能远高于 getRGB
        source.getRGB(0, 0, w, h, pixels, 0, w)

        val visited = BooleanArray(w * h)
        val result = mutableListOf<Rect>()

        // 8邻域偏移量 (上下左右+对角线)，保证字符笔画断点能连上
        val dxs = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
        val dys = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)

        for (i in pixels.indices) {
            if (visited[i]) continue

            // 如果当前点匹配颜色规则，开始种子生长(BFS)
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

                    // 更新边界
                    if (cx < minX) minX = cx
                    if (cx > maxX) maxX = cx
                    if (cy < minY) minY = cy
                    if (cy > maxY) maxY = cy

                    // 搜索相邻点
                    for (k in 0 until 8) {
                        val nx = cx + dxs[k]
                        val ny = cy + dys[k]

                        if (nx in 0 until w && ny in 0 until h) {
                            val nIdx = ny * w + nx
                            if (!visited[nIdx]) {
                                // 只有匹配的点才加入队列
                                if (ColorUtils.isMatchAny(pixels[nIdx], rules)) {
                                    visited[nIdx] = true
                                    queue.add(nIdx)
                                }
                            }
                        }
                    }
                }

                // 生成包围盒 (Rect right/bottom 是坐标+1)
                val rectW = maxX - minX + 1
                val rectH = maxY - minY + 1
                if (rectW >= minW && rectH >= minH) {
                    result.add(Rect(minX.toFloat(), minY.toFloat(), (maxX + 1).toFloat(), (maxY + 1).toFloat()))
                }
            } else {
                visited[i] = true // 非匹配点直接标记已访问
            }
        }
        return result
    }

    /**
     * 网格切割算法 (定距切割)
     */
    fun generateGridRects(
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        colGap: Int, // 列间距
        rowGap: Int, // 行间距
        colCount: Int,
        rowCount: Int
    ): List<Rect> {
        val list = mutableListOf<Rect>()
        if (width <= 0 || height <= 0) return list

        for (r in 0 until rowCount) {
            for (c in 0 until colCount) {
                // 计算每个格子的左上角坐标
                // 公式：起点 + 索引 * (文字宽 + 间距)
                val left = startX + c * (width + colGap)
                val top = startY + r * (height + rowGap)
                val right = left + width
                val bottom = top + height

                list.add(Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()))
            }
        }
        return list
    }
}

