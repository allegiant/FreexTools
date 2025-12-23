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
    var cropStart by remember { mutableStateOf<Offset?>(null) }
    var cropCurrent by remember { mutableStateOf<Offset?>(null) }

    // 鼠标在屏幕上的物理位置 (用于显示悬浮窗)
    var currentMousePos by remember { mutableStateOf<Offset?>(null) }
    // 鼠标对应的图片像素位置 (用于高亮像素)
    var currentHoverPixel by remember { mutableStateOf<IntOffset?>(null) }

    // 重置裁剪状态
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
                    // ----------------------------------------------------------------
                    // 【核心修复】合并所有手势逻辑：点击、拖拽、悬停、滚轮
                    // ----------------------------------------------------------------
                    .pointerInput(workImage, isCropMode, scale, offset) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val mainChange = changes.first() // 获取主要触点

                                // 1. 实时更新鼠标位置 (悬停逻辑)
                                // ------------------------------------
                                val localPos = mainChange.position
                                currentMousePos = localPos

                                // 计算当前指向的像素
                                val (imgX, imgY) = MathUtils.mapScreenToImage(
                                    localPos, offset, scale, containerW, containerH, fitOffsetX, fitOffsetY, fitScale
                                )

                                // 只有位置发生变化才回调 (减少 recomposition 带来的抖动)
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

                                // 2. 处理滚轮缩放
                                // ------------------------------------
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = mainChange.scrollDelta
                                    val zoomFactor = if (delta.y < 0) 1.1f else 0.9f
                                    onTransformChange((scale * zoomFactor).coerceIn(0.1f, 32f), offset)
                                    mainChange.consume()
                                }

                                // 3. 处理 按下 -> 拖拽/点击
                                // ------------------------------------
                                // 检测到按下
                                if (event.type == PointerEventType.Press) {
                                    // 记录按下时的初始位置
                                    val startPos = mainChange.position
                                    var isDrag = false

                                    // 裁剪模式下记录起点
                                    if (isCropMode) {
                                        cropStart = startPos
                                        cropCurrent = startPos
                                    }

                                    // 进入拖拽循环 (drag loop)
                                    // 只要手指没抬起，就一直在这个循环里
                                    do {
                                        val dragEvent = awaitPointerEvent()
                                        val dragChange = dragEvent.changes.first()

                                        // 计算从上一帧到现在的位移
                                        val dragAmount = dragChange.position - dragChange.previousPosition

                                        // 如果位移超过一点点，视为拖拽
                                        if (dragAmount.getDistance() > 0.5f) {
                                            isDrag = true

                                            if (isCropMode) {
                                                // 裁剪：更新终点
                                                cropCurrent = dragChange.position
                                            } else {
                                                // 平移：更新 Offset
                                                onTransformChange(scale, offset + dragAmount)
                                            }
                                            dragChange.consume() // 消费掉事件，防止冒泡
                                        }
                                    } while (dragEvent.changes.any { it.pressed })

                                    // 循环结束(手指抬起)，如果期间没有发生拖拽，则视为【点击】
                                    if (!isDrag && !isCropMode) {
                                        // 执行取色逻辑
                                        if (imgX in 0 until rawImg.width && imgY in 0 until rawImg.height) {
                                            val rgb = rawImg.getRGB(imgX, imgY)
                                            onColorPick(String.format("%06X", rgb and 0xFFFFFF))
                                        }
                                    }
                                }

                                // 4. 鼠标移出处理
                                // ------------------------------------
                                if (event.type == PointerEventType.Exit) {
                                    currentMousePos = null
                                    currentHoverPixel = null
                                    onHoverChange(null, Color.Transparent)
                                }
                            }
                        }
                    }
            ) {
                // A. 渲染底图
                Image(
                    bitmap = workImage.bitmap,
                    contentDescription = null,
                    filterQuality = FilterQuality.None, // 保持像素锐利
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale; scaleY = scale
                        translationX = offset.x; translationY = offset.y
                        transformOrigin = TransformOrigin.Center
                    },
                    contentScale = ContentScale.Fit
                )

                // B. 渲染裁剪框
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
                        // 2. 必须使用 zIndex 确保它浮在最上层，否则可能被遮挡
                        .zIndex(10f)
                        // 3. 所有的位置、显示/隐藏逻辑全部移入 graphicsLayer
                        .graphicsLayer {
                            // 在这里读取状态，只会触发“绘制阶段”更新，不会触发“布局”或“组合”，极度丝滑
                            val mousePos = currentMousePos
                            val hoverPixel = currentHoverPixel
                            val cropMode = isCropMode

                            // 判断是否需要显示
                            val shouldShow = !cropMode && mousePos != null && hoverPixel != null

                            if (shouldShow) {
                                alpha = 1f // 显示
                                // 使用 translation 移动，支持浮点数，不会跳格
                                // 注意：这里需要再次判空或使用安全调用，虽然 shouldShow 已经保证了，但为了编译安全
                                if (mousePos != null) {
                                    translationX = mousePos.x + 20f
                                    translationY = mousePos.y + 20f
                                }
                            } else {
                                alpha = 0f // 隐身（看不见，但对象还在，开销极小）
                                // 隐身时移出屏幕防止误触（可选，但在 graphicsLayer 里只改绘制位置通常不影响点击事件流）
                                translationX = -10000f
                                translationY = -10000f
                            }
                        },
                    rawImage = rawImg,
                    // 4. 给参数兜底：因为我们移除了外层的 null 判断，这里必须给一个默认值。
                    // 当 alpha = 0 时，用户看不见这个颜色，所以给 Transparent 或 Black 都行，只要不崩即可。
                    centerPixel = (currentHoverPixel ?: Color.Transparent) as IntOffset
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