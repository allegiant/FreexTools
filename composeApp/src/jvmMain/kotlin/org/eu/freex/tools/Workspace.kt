// composeApp/src/jvmMain/kotlin/org/eu/freex/tools/Workspace.kt
package org.eu.freex.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.model.WorkImage
import org.eu.freex.tools.utils.MathUtils
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min

@Composable
fun Workspace(
    modifier: Modifier,
    workImage: WorkImage?,
    binaryBitmap: ImageBitmap?,
    showBinaryPreview: Boolean,
    scale: Float,
    offset: Offset,
    onTransformChange: (Float, Offset) -> Unit,
    onHoverChange: (IntOffset?, Color) -> Unit,
    onColorPick: (String) -> Unit,
    onCropConfirm: (Rect) -> Unit,
    colorRules: List<ColorRule>,
    charRects: List<Rect>,
    onCharRectClick: (Rect) -> Unit
) {
    val currentScaleState = rememberUpdatedState(scale)
    val currentOffsetState = rememberUpdatedState(offset)

    // 裁剪框状态
    var cropStart by remember { mutableStateOf<Offset?>(null) }
    var cropCurrent by remember { mutableStateOf<Offset?>(null) }

    // 鼠标状态
    var currentMousePos by remember { mutableStateOf<Offset?>(null) }
    var currentHoverPixel by remember { mutableStateOf<IntOffset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // 双击检测辅助
    var lastClickTime by remember { mutableStateOf(0L) }

    // 辅助函数：获取当前选区 Rect
    fun getCropRect(): Rect? {
        if (cropStart == null || cropCurrent == null) return null
        return Rect(
            left = min(cropStart!!.x, cropCurrent!!.x),
            top = min(cropStart!!.y, cropCurrent!!.y),
            right = max(cropStart!!.x, cropCurrent!!.x),
            bottom = max(cropStart!!.y, cropCurrent!!.y)
        )
    }

    // 清除选区
    fun clearSelection() {
        cropStart = null
        cropCurrent = null
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2B2B2B))
            .clipToBounds()
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()

        if (workImage != null) {
            val rawImg = workImage.bufferedImage
            val fitScale = min(containerW / rawImg.width, containerH / rawImg.height)
            val fitOffsetX = (containerW - rawImg.width * fitScale) / 2f
            val fitOffsetY = (containerH - rawImg.height * fitScale) / 2f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(workImage) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val mainChange = changes.first()
                                val currentPos = mainChange.position

                                // 1. 滚轮缩放
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = mainChange.scrollDelta
                                    val zoomFactor = if (delta.y < 0) 1.1f else 0.9f
                                    val newScale = (currentScaleState.value * zoomFactor).coerceIn(0.1f, 32f)
                                    onTransformChange(newScale, currentOffsetState.value)
                                    changes.forEach { it.consume() }
                                }

                                // 2. 按下 (Press)
                                else if (event.type == PointerEventType.Press) {
                                    val startPos = currentPos
                                    val startOffset = currentOffsetState.value
                                    val isLeftClick = event.buttons.isPrimaryPressed
                                    val isRightClick = event.buttons.isSecondaryPressed
                                    var hasDragMoved = false

                                    // 【核心逻辑】按下时，记录当前是否有选区
                                    val existingRect = getCropRect()
                                    val isClickInsideSelection = existingRect?.contains(startPos) == true

                                    // 如果不是点击内部，且是左键，则准备开始新选区（重置起点）
                                    // 注意：我们这里不立即清除，而是在拖拽发生后或释放时处理，以支持逻辑判断
                                    if (isLeftClick && !isClickInsideSelection) {
                                        cropStart = startPos
                                        cropCurrent = startPos
                                    }

                                    do {
                                        val dragEvent = awaitPointerEvent()
                                        val dragChange = dragEvent.changes.firstOrNull { it.id == mainChange.id } ?: continue

                                        if (dragChange.pressed) {
                                            val dragAmount = dragChange.position - startPos
                                            if (!hasDragMoved && dragAmount.getDistance() > 5f) {
                                                hasDragMoved = true
                                                isDragging = true
                                                // 拖拽开始，隐藏放大镜
                                                currentMousePos = null
                                                currentHoverPixel = null
                                                onHoverChange(null, Color.Transparent)
                                            }

                                            if (hasDragMoved) {
                                                if (isRightClick) {
                                                    // 右键拖拽 -> 平移画布
                                                    onTransformChange(currentScaleState.value, startOffset + dragAmount)
                                                } else if (isLeftClick) {
                                                    // 左键拖拽 -> 更新选区
                                                    // 只要发生拖拽，就强制进入"选区模式"，覆盖之前的任何选区状态
                                                    if (cropStart == null) cropStart = startPos // 容错
                                                    cropCurrent = dragChange.position
                                                }
                                                dragChange.consume()
                                            }
                                        }
                                    } while (dragEvent.changes.any { it.pressed })

                                    // 3. 释放 (Release) - 未发生拖拽
                                    if (!hasDragMoved && isLeftClick) {
                                        val now = System.currentTimeMillis()
                                        val isDoubleClick = (now - lastClickTime) < 300
                                        lastClickTime = now

                                        if (existingRect != null) {
                                            // --- Case A: 之前已经有选区 ---
                                            if (isClickInsideSelection) {
                                                // 点击选区内部
                                                if (isDoubleClick) {
                                                    // 双击内部 -> 确认裁剪
                                                    val (x1, y1) = MathUtils.mapScreenToImage(existingRect.topLeft, offset, scale, containerW, containerH, fitOffsetX, fitOffsetY, fitScale)
                                                    val (x2, y2) = MathUtils.mapScreenToImage(existingRect.bottomRight, offset, scale, containerW, containerH, fitOffsetX, fitOffsetY, fitScale)
                                                    val imgCropRect = Rect(min(x1, x2).toFloat(), min(y1, y2).toFloat(), max(x1, x2).toFloat(), max(x1, x2).toFloat())
                                                    onCropConfirm(imgCropRect)
                                                    clearSelection()
                                                }
                                                // 单击内部 -> 不做任何事（不取色，也不清除选区）
                                            } else {
                                                // 点击选区外部 -> 清除选区，不取色
                                                clearSelection()
                                            }
                                        } else {
                                            // --- Case B: 之前没有选区 ---
                                            // 执行取色逻辑
                                            val (imgX, imgY) = MathUtils.mapScreenToImage(
                                                startPos, startOffset, currentScaleState.value,
                                                containerW, containerH, fitOffsetX, fitOffsetY, fitScale
                                            )
                                            if (imgX in 0 until rawImg.width && imgY in 0 until rawImg.height) {
                                                val rgb = rawImg.getRGB(imgX, imgY)
                                                onColorPick(String.format("%06X", rgb and 0xFFFFFF))
                                            }
                                        }
                                    }
                                    isDragging = false
                                }

                                // 4. 悬停 (Move)
                                else if (event.type == PointerEventType.Move && !isDragging) {
                                    currentMousePos = currentPos
                                    val (imgX, imgY) = MathUtils.mapScreenToImage(
                                        currentPos, currentOffsetState.value, currentScaleState.value,
                                        containerW, containerH, fitOffsetX, fitOffsetY, fitScale
                                    )

                                    if (currentHoverPixel?.x != imgX || currentHoverPixel?.y != imgY) {
                                        if (imgX in 0 until rawImg.width && imgY in 0 until rawImg.height) {
                                            val rgb = rawImg.getRGB(imgX, imgY)
                                            val awtColor = java.awt.Color(rgb, true)
                                            onHoverChange(
                                                IntOffset(imgX, imgY),
                                                Color(awtColor.red, awtColor.green, awtColor.blue, awtColor.alpha)
                                            )
                                            currentHoverPixel = IntOffset(imgX, imgY)
                                        } else {
                                            onHoverChange(null, Color.Transparent)
                                            currentHoverPixel = null
                                        }
                                    }
                                }

                                // 5. 移出 (Exit)
                                else if (event.type == PointerEventType.Exit) {
                                    currentMousePos = null
                                    currentHoverPixel = null
                                    onHoverChange(null, Color.Transparent)
                                }
                            }
                        }
                    }
            ) {
                // 1. 底层：原图
                Image(
                    bitmap = workImage.bitmap,
                    contentDescription = null,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin.Center
                    },
                    contentScale = ContentScale.Fit
                )

                // 2. 中层：二值化预览覆盖
                if (showBinaryPreview && binaryBitmap != null) {
                    Image(
                        bitmap = binaryBitmap,
                        contentDescription = null,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            transformOrigin = TransformOrigin.Center
                        },
                        contentScale = ContentScale.Fit
                    )
                }

                // 3. 顶层：字符框 (Green)
                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    transformOrigin = TransformOrigin.Center
                }) {
                    withTransform({
                        translate(left = fitOffsetX, top = fitOffsetY)
                        scale(scale = fitScale, pivot = Offset.Zero)
                    }) {
                        charRects.forEach { rect ->
                            drawRect(
                                color = Color.Green,
                                topLeft = rect.topLeft,
                                size = rect.size,
                                style = Stroke(width = 2f / (scale * fitScale))
                            )
                        }
                    }
                }

                // 4. 顶层：当前选区 (Red) + 提示
                val currentRect = getCropRect()
                if (currentRect != null && currentRect.width > 0 && currentRect.height > 0) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color.Red, topLeft = currentRect.topLeft, size = currentRect.size, style = Stroke(2f))
                    }

                    // 仅当选区足够大时显示提示
                    if (currentRect.width > 30 && currentRect.height > 30) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(currentRect.center.x.toInt() - 40, currentRect.center.y.toInt()) }
                                .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            Text("双击区域确认", color = Color.White, style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp))
                        }
                    }
                }
            }

            // 5. 悬浮放大镜 (保持不变)
            FloatingPixelGrid(
                modifier = Modifier
                    .zIndex(10f)
                    .graphicsLayer {
                        val show = !isDragging && currentMousePos != null && currentHoverPixel != null
                        if (show) {
                            alpha = 1f
                            val pos = currentMousePos ?: Offset.Zero
                            translationX = pos.x + 20f
                            translationY = pos.y + 20f
                        } else {
                            alpha = 0f
                            translationX = -10000f
                            translationY = -10000f
                        }
                    },
                rawImage = rawImg,
                centerPixel = currentHoverPixel ?: IntOffset.Zero
            )

        } else {
            Text("请拖拽图片到此处", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun FloatingPixelGrid(
    modifier: Modifier,
    rawImage: BufferedImage,
    centerPixel: IntOffset
) {
    val boxSize = 90.dp
    Card(
        modifier = modifier.size(boxSize),
        elevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
        shape = RoundedCornerShape(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val w = size.width
            val gridCount = 9
            val radius = 4
            val cellSize = w / gridCount.toFloat()

            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    val px = centerPixel.x + dx
                    val py = centerPixel.y + dy
                    val drawX = (dx + radius) * cellSize
                    val drawY = (dy + radius) * cellSize

                    if (px in 0 until rawImage.width && py in 0 until rawImage.height) {
                        val rgbInt = rawImage.getRGB(px, py)
                        val color = Color(
                            red = (rgbInt shr 16 and 0xFF) / 255f,
                            green = (rgbInt shr 8 and 0xFF) / 255f,
                            blue = (rgbInt and 0xFF) / 255f
                        )
                        drawRect(color = color, topLeft = Offset(drawX, drawY), size = Size(cellSize, cellSize))
                    } else {
                        drawRect(color = Color.Black, topLeft = Offset(drawX, drawY), size = Size(cellSize, cellSize))
                    }
                    drawRect(
                        color = Color.Gray.copy(0.3f),
                        topLeft = Offset(drawX, drawY),
                        Size(cellSize, cellSize),
                        style = Stroke(0.5f)
                    )
                }
            }
            val centerDrawX = radius * cellSize
            val centerDrawY = radius * cellSize
            drawRect(
                color = Color.Red,
                topLeft = Offset(centerDrawX, centerDrawY),
                Size(cellSize, cellSize),
                style = Stroke(1.5f)
            )
        }
    }
}