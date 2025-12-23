package org.eu.freex.tools.utils


import androidx.compose.ui.geometry.Offset
import kotlin.math.floor

object MathUtils {
    // 将屏幕坐标映射回图片的真实像素坐标
    fun mapScreenToImage(
        screenPos: Offset,
        panOffset: Offset,
        scale: Float,
        containerW: Float,
        containerH: Float,
        fitOffsetX: Float,
        fitOffsetY: Float,
        fitScale: Float
    ): Pair<Int, Int> {
        // 1. 去除平移
        val xNoPan = screenPos.x - panOffset.x
        val yNoPan = screenPos.y - panOffset.y

        // 2. 去除中心缩放 (相对于容器中心)
        val centerX = containerW / 2
        val centerY = containerH / 2
        val xNoZoom = (xNoPan - centerX) / scale + centerX
        val yNoZoom = (yNoPan - centerY) / scale + centerY

        // 3. 去除 Fit 布局偏移和缩放
        // 【核心修改】使用 floor 向下取整，保证像素坐标落在 [N, N+1) 区间内都归为 N
        val imgX = floor((xNoZoom - fitOffsetX) / fitScale).toInt()
        val imgY = floor((yNoZoom - fitOffsetY) / fitScale).toInt()

        return imgX to imgY
    }
}