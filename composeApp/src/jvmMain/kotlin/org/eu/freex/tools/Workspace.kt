// composeApp/src/jvmMain/kotlin/org/eu/freex/tools/Workspace.kt
package org.eu.freex.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onCharRectClick: (Rect) -> Unit // 【关键】传入字符点击回调
) {
    val currentScaleState = rememberUpdatedState(scale)
    val currentOffsetState = rememberUpdatedState(offset)
    var cropStart by remember { mutableStateOf<Offset?>(null) }
    var cropCurrent by remember { mutableStateOf<Offset?>(null) }
    var currentMousePos by remember { mutableStateOf<Offset?>(null) }
    var currentHoverPixel by remember { mutableStateOf<IntOffset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    fun getCropRect(): Rect? {
        if (cropStart == null || cropCurrent == null) return null
        return Rect(
            left = min(cropStart!!.x, cropCurrent!!.x),
            top = min(cropStart!!.y, cropCurrent!!.y),
            right = max(cropStart!!.x, cropCurrent!!.x),
            bottom = max(cropStart!!.y, cropCurrent!!.y)
        )
    }

    fun clearSelection() {
        cropStart = null
        cropCurrent = null
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(Color(0xFF2B2B2B)).clipToBounds()
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
                    .pointerInput(workImage, fitScale, fitOffsetX, fitOffsetY) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val mainChange = changes.first()
                                val currentPos = mainChange.position

                                // --- 1. 滚轮缩放 ---
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = mainChange.scrollDelta
                                    val zoomFactor = if (delta.y < 0) 1.1f else 0.9f
                                    val newScale = (currentScaleState.value * zoomFactor).coerceIn(0.1f, 32f)
                                    onTransformChange(newScale, currentOffsetState.value)
                                    changes.forEach { it.consume() }
                                }
                                // --- 2. 按下 (Press) ---
                                else if (event.type == PointerEventType.Press) {
                                    val startPos = currentPos
                                    val startOffset = currentOffsetState.value
                                    val isLeftClick = event.buttons.isPrimaryPressed
                                    val isRightClick = event.buttons.isSecondaryPressed
                                    var hasDragMoved = false

                                    // 如果不是右键拖拽，且不在现有选区内，则开始新选区
                                    val existingRect = getCropRect()
                                    val isClickInsideSelection = existingRect?.contains(startPos) == true

                                    if (isLeftClick && !isClickInsideSelection) {
                                        // 这里有个细节：如果点击的位置是字符框，不要开始画选区，但这在Press阶段很难判断
                                        // 我们可以先记录，等到 Release 阶段判断是否是点击事件
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
                                            }
                                            if (hasDragMoved) {
                                                if (isRightClick) {
                                                    onTransformChange(currentScaleState.value, startOffset + dragAmount)
                                                } else if (isLeftClick) {
                                                    if (cropStart == null) cropStart = startPos
                                                    cropCurrent = dragChange.position
                                                }
                                                dragChange.consume()
                                            }
                                        }
                                    } while (dragEvent.changes.any { it.pressed })

                                    // --- 3. 释放 (Release) - 处理点击/双击 ---
                                    if (!hasDragMoved && isLeftClick) {
                                        val now = System.currentTimeMillis()
                                        val isDoubleClick = (now - lastClickTime) < 300
                                        lastClickTime = now

                                        // 计算点击在图片上的坐标
                                        val (imgX, imgY) = MathUtils.mapScreenToImage(
                                            startPos, currentOffsetState.value, currentScaleState.value,
                                            containerW, containerH, fitOffsetX, fitOffsetY, fitScale
                                        )
                                        val clickPointInImage = Offset(imgX.toFloat(), imgY.toFloat())

                                        if (isDoubleClick) {
                                            // 【逻辑修正】双击优先级判断

                                            // 优先级 A: 是否点击了已生成的字符框 (绿色框) -> 映射
                                            val clickedCharRect = charRects.find { it.contains(clickPointInImage) }
                                            if (clickedCharRect != null) {
                                                onCharRectClick(clickedCharRect)
                                            }
                                            // 优先级 B: 是否点击了手动框选区域 (红色框) -> 确认裁剪
                                            else if (existingRect != null && isClickInsideSelection) {
                                                // 计算选区在图片上的真实坐标
                                                val (x1, y1) = MathUtils.mapScreenToImage(existingRect.topLeft, currentOffsetState.value, currentScaleState.value, containerW, containerH, fitOffsetX, fitOffsetY, fitScale)
                                                val (x2, y2) = MathUtils.mapScreenToImage(existingRect.bottomRight, currentOffsetState.value, currentScaleState.value, containerW, containerH, fitOffsetX, fitOffsetY, fitScale)
                                                val imgCropRect = Rect(min(x1, x2).toFloat(), min(y1, y2).toFloat(), max(x1, x2).toFloat(), max(y1, y2).toFloat())

                                                onCropConfirm(imgCropRect)
                                                clearSelection()
                                            }
                                        } else {
                                            // 单击逻辑 (取色 or 清除选区)
                                            if (existingRect != null && !isClickInsideSelection) {
                                                clearSelection()
                                            } else if (existingRect == null) {
                                                // 取色
                                                if (imgX in 0 until rawImg.width && imgY in 0 until rawImg.height) {
                                                    val rgb = rawImg.getRGB(imgX, imgY)
                                                    onColorPick(String.format("%06X", rgb and 0xFFFFFF))
                                                }
                                            }
                                        }
                                    }
                                    isDragging = false
                                }
                                // --- 4. 移动 (Move) ---
                                else if (event.type == PointerEventType.Move && !isDragging) {
                                    currentMousePos = currentPos
                                    val (imgX, imgY) = MathUtils.mapScreenToImage(
                                        currentPos, currentOffsetState.value, currentScaleState.value,
                                        containerW, containerH, fitOffsetX, fitOffsetY, fitScale
                                    )
                                    // ... 更新 hover 状态 (同原代码) ...
                                    if (currentHoverPixel?.x != imgX || currentHoverPixel?.y != imgY) {
                                        if (imgX in 0 until rawImg.width && imgY in 0 until rawImg.height) {
                                            val rgb = rawImg.getRGB(imgX, imgY)
                                            val awtColor = java.awt.Color(rgb, true)
                                            onHoverChange(IntOffset(imgX, imgY), Color(awtColor.red, awtColor.green, awtColor.blue, awtColor.alpha))
                                            currentHoverPixel = IntOffset(imgX, imgY)
                                        } else {
                                            onHoverChange(null, Color.Transparent)
                                            currentHoverPixel = null
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                // 绘制层 (原图 -> 二值化 -> 字符框 -> 裁剪框)
                // 1. 原图
                Image(
                    bitmap = workImage.bitmap, contentDescription = null, filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y; transformOrigin = TransformOrigin.Center
                    }, contentScale = ContentScale.Fit
                )
                // 2. 二值化层
                if (showBinaryPreview && binaryBitmap != null) {
                    Image(
                        bitmap = binaryBitmap, contentDescription = null, filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y; transformOrigin = TransformOrigin.Center
                        }, contentScale = ContentScale.Fit
                    )
                }
                // 3. 字符框 (绿色)
                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y; transformOrigin = TransformOrigin.Center
                }) {
                    withTransform({ translate(left = fitOffsetX, top = fitOffsetY); scale(scale = fitScale, pivot = Offset.Zero) }) {
                        charRects.forEach { rect ->
                            drawRect(color = Color.Green, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = 2f / (scale * fitScale)))
                        }
                    }
                }
                // 4. 手动裁剪选区 (红色)
                val currentRect = getCropRect()
                if (currentRect != null && currentRect.width > 0 && currentRect.height > 0) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color.Red, topLeft = currentRect.topLeft, size = currentRect.size, style = Stroke(2f))
                    }
                    if (currentRect.width > 30 && currentRect.height > 30) {
                        Box(
                            modifier = Modifier.offset { IntOffset(currentRect.center.x.toInt() - 40, currentRect.center.y.toInt()) }
                                .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp)).padding(4.dp)
                        ) {
                            Text("双击确认裁剪", color = Color.White, style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp))
                        }
                    }
                }
            }
            // 放大镜组件 (FloatingPixelGrid) 保持不变 ...
            FloatingPixelGrid(
                modifier = Modifier.zIndex(10f).graphicsLayer {
                    val show = !isDragging && currentMousePos != null && currentHoverPixel != null
                    alpha = if (show) 1f else 0f
                    if (show) { val pos = currentMousePos ?: Offset.Zero; translationX = pos.x + 20f; translationY = pos.y + 20f }
                },
                rawImage = rawImg, centerPixel = currentHoverPixel ?: IntOffset.Zero
            )
        } else {
            Text("请拖拽图片到此处", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
        }
    }
}

// ... FloatingPixelGrid 代码保持不变 (80dp 版本) ...
@Composable
fun FloatingPixelGrid(
    modifier: Modifier,
    rawImage: BufferedImage,
    centerPixel: IntOffset
) {
    // ... 保持您上一次请求的 5x5 80dp 缩小版代码 ...
    var hexText = "#------"
    var coordsText = "(-, -)"
    var pixelColor = Color.Transparent

    if (centerPixel.x in 0 until rawImage.width && centerPixel.y in 0 until rawImage.height) {
        val rgbInt = rawImage.getRGB(centerPixel.x, centerPixel.y)
        val r = (rgbInt shr 16) and 0xFF
        val g = (rgbInt shr 8) and 0xFF
        val b = rgbInt and 0xFF

        pixelColor = Color(r / 255f, g / 255f, b / 255f)
        hexText = String.format("#%02X%02X%02X", r, g, b)
        coordsText = "(${centerPixel.x}, ${centerPixel.y})"
    }

    Card(
        modifier = modifier.width(80.dp).wrapContentHeight(),
        elevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            Box(Modifier.aspectRatio(1f).background(Color.Black)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val gridCount = 5
                    val radius = 2
                    val cellSize = w / gridCount.toFloat()

                    for (dx in -radius..radius) {
                        for (dy in -radius..radius) {
                            val px = centerPixel.x + dx
                            val py = centerPixel.y + dy
                            val drawX = (dx + radius) * cellSize
                            val drawY = (dy + radius) * cellSize

                            if (px in 0 until rawImage.width && py in 0 until rawImage.height) {
                                val rgbInt = rawImage.getRGB(px, py)
                                val color = Color((rgbInt shr 16 and 0xFF)/255f, (rgbInt shr 8 and 0xFF)/255f, (rgbInt and 0xFF)/255f)
                                drawRect(color = color, topLeft = Offset(drawX, drawY), size = Size(cellSize, cellSize))
                            } else {
                                drawRect(color = Color.Black, topLeft = Offset(drawX, drawY), size = Size(cellSize, cellSize))
                            }
                            drawRect(color = Color.Gray.copy(0.2f), topLeft = Offset(drawX, drawY), Size(cellSize, cellSize), style = Stroke(0.5f))
                        }
                    }
                    val centerDrawX = radius * cellSize
                    val centerDrawY = radius * cellSize
                    drawRect(color = Color.Red, topLeft = Offset(centerDrawX, centerDrawY), Size(cellSize, cellSize), style = Stroke(1.0f))
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 3.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(pixelColor).border(0.5.dp, Color.Gray))
                    Spacer(Modifier.width(3.dp))
                    Text(text = hexText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Text(text = coordsText, fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    }
}