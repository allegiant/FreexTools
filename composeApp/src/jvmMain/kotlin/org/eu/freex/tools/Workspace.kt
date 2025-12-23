package org.eu.freex.tools


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
    isCropMode: Boolean,
    scale: Float,
    offset: Offset,
    onTransformChange: (Float, Offset) -> Unit,
    onHoverChange: (IntOffset?, Color) -> Unit,
    onColorPick: (String) -> Unit,
    onCropConfirm: (Rect) -> Unit,
    colorRules: List<ColorRule>
) {
    // 1. 使用 rememberUpdatedState以此在不重启 pointerInput 的情况下获取最新值
    val currentScaleState = rememberUpdatedState(scale)
    val currentOffsetState = rememberUpdatedState(offset)

    var cropStart by remember { mutableStateOf<Offset?>(null) }
    var cropCurrent by remember { mutableStateOf<Offset?>(null) }
    var currentMousePos by remember { mutableStateOf<Offset?>(null) }
    var currentHoverPixel by remember { mutableStateOf<IntOffset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(isCropMode) {
        if (!isCropMode) { cropStart = null; cropCurrent = null }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
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
                    // 【核心修复】
                    // Keys 只填 workImage 和 isCropMode。
                    // 绝对不要填 scale 和 offset，否则一动就重置！
                    .pointerInput(workImage, isCropMode) {
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

                                // 2. 按下 (点击或拖拽开始)
                                else if (event.type == PointerEventType.Press) {
                                    val startPos = currentPos
                                    // 记录按下瞬间的 offset 作为基准
                                    val startOffset = currentOffsetState.value
                                    var hasDragMoved = false

                                    if (isCropMode) {
                                        cropStart = startPos
                                        cropCurrent = startPos
                                    }

                                    // 拖拽循环
                                    do {
                                        val dragEvent = awaitPointerEvent()
                                        val dragChange = dragEvent.changes.firstOrNull { it.id == mainChange.id } ?: continue

                                        if (dragChange.pressed) {
                                            val dragAmount = dragChange.position - startPos

                                            // 拖拽阈值判断
                                            if (!hasDragMoved && dragAmount.getDistance() > 5f) {
                                                hasDragMoved = true
                                                isDragging = true
                                                // 拖拽开始，清理悬停
                                                currentMousePos = null
                                                currentHoverPixel = null
                                                onHoverChange(null, Color.Transparent)
                                            }

                                            if (hasDragMoved) {
                                                if (isCropMode) {
                                                    cropCurrent = dragChange.position
                                                } else {
                                                    // 【核心】使用 currentScaleState.value 获取最新缩放
                                                    // 使用 startOffset + dragAmount 计算新位置（绝对位移，不累积误差）
                                                    onTransformChange(currentScaleState.value, startOffset + dragAmount)
                                                }
                                                dragChange.consume()
                                            }
                                        }
                                    } while (dragEvent.changes.any { it.pressed })

                                    // 3. 点击 (抬起且未拖拽)
                                    if (!hasDragMoved && !isCropMode) {
                                        // 此时 offset 没变，可以直接用 startOffset
                                        val (imgX, imgY) = MathUtils.mapScreenToImage(
                                            startPos, startOffset, currentScaleState.value,
                                            containerW, containerH, fitOffsetX, fitOffsetY, fitScale
                                        )
                                        if (imgX in 0 until rawImg.width && imgY in 0 until rawImg.height) {
                                            val rgb = rawImg.getRGB(imgX, imgY)
                                            onColorPick(String.format("%06X", rgb and 0xFFFFFF))
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

                                // 5. 移出
                                else if (event.type == PointerEventType.Exit) {
                                    currentMousePos = null
                                    currentHoverPixel = null
                                    onHoverChange(null, Color.Transparent)
                                }
                            }
                        }
                    }
            ) {
                // Image 和其他 UI 保持不变，直接复制即可
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

                if (isCropMode && cropStart != null && cropCurrent != null) {
                    val rect = Rect(
                        left = min(cropStart!!.x, cropCurrent!!.x),
                        top = min(cropStart!!.y, cropCurrent!!.y),
                        right = max(cropStart!!.x, cropCurrent!!.x),
                        bottom = max(cropStart!!.y, cropCurrent!!.y)
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color.Red, topLeft = rect.topLeft, size = rect.size, style = Stroke(2f))
                    }
                    Button(
                        onClick = {
                            val (x1, y1) = MathUtils.mapScreenToImage(rect.topLeft, offset, scale, containerW, containerH, fitOffsetX, fitOffsetY, fitScale)
                            val (x2, y2) = MathUtils.mapScreenToImage(rect.bottomRight, offset, scale, containerW, containerH, fitOffsetX, fitOffsetY, fitScale)
                            val cropRect = Rect(min(x1, x2).toFloat(), min(y1, y2).toFloat(), max(x1, x2).toFloat(), max(y1, y2).toFloat())
                            onCropConfirm(cropRect)
                            cropStart = null; cropCurrent = null
                        },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                    ) { Text("确认裁剪") }
                }

                FloatingPixelGrid(
                    modifier = Modifier
                        .zIndex(10f)
                        .graphicsLayer {
                            val show = !isCropMode && !isDragging && currentMousePos != null && currentHoverPixel != null
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
            }
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