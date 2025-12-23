package org.eu.freex.tools

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.freex.tools.dialogs.ScreenCropperDialog
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.model.WorkImage
import org.eu.freex.tools.utils.ImageUtils
import java.awt.Component
import java.awt.Container
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@Composable
fun App(window: androidx.compose.ui.awt.ComposeWindow?) {
    val imageList = remember { mutableStateListOf<WorkImage>() }
    var currentIndex by remember { mutableStateOf(-1) }
    val currentImage = if (currentIndex in imageList.indices) imageList[currentIndex] else null

    var mainScale by remember { mutableStateOf(1f) }
    var mainOffset by remember { mutableStateOf(Offset.Zero) }
    var hoverPixelPos by remember { mutableStateOf<IntOffset?>(null) }
    var hoverColor by remember { mutableStateOf(Color.Transparent) }
    var isCropMode by remember { mutableStateOf(false) }

    val colorRules = remember { mutableStateListOf<ColorRule>() }
    var defaultBias by remember { mutableStateOf("101010") }
    var showBinaryPreview by remember { mutableStateOf(false) }
    var showScreenCropper by remember { mutableStateOf(false) }
    var fullScreenCapture by remember { mutableStateOf<BufferedImage?>(null) }
    // 存储自动切割出的字符框列表
    var charRects by remember { mutableStateOf<List<Rect>>(emptyList()) }

    // --- 新增：网格切割参数状态 ---
    var isGridMode by remember { mutableStateOf(false) } // 模式切换：自动 vs 网格
    var gridX by remember { mutableStateOf(0) }
    var gridY by remember { mutableStateOf(0) }
    var gridW by remember { mutableStateOf(15) }
    var gridH by remember { mutableStateOf(15) }
    var gridColGap by remember { mutableStateOf(0) }
    var gridRowGap by remember { mutableStateOf(0) }
    var gridColCount by remember { mutableStateOf(1) }
    var gridRowCount by remember { mutableStateOf(1) }
    // ----------------------------

    val scope = rememberCoroutineScope()

    fun addImage(bufferedImage: BufferedImage, name: String) {
        imageList.add(WorkImage(bufferedImage.toComposeImageBitmap(), bufferedImage, name))
        currentIndex = imageList.lastIndex
        mainScale = 1f; mainOffset = Offset.Zero
        isCropMode = false
    }
    LaunchedEffect(isGridMode, gridX, gridY, gridW, gridH, gridColGap, gridRowGap, gridColCount, gridRowCount) {
        if (isGridMode) {
            // 只要参数一变，立即重新计算框框
            charRects = ImageUtils.generateGridRects(
                gridX, gridY, gridW, gridH, gridColGap, gridRowGap, gridColCount, gridRowCount
            )
        }
    }

    fun loadFile(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val img = ImageIO.read(file)
                if (img != null) withContext(Dispatchers.Main) { addImage(img, file.name) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 拖拽挂载 logic
    if (window != null) {
        DisposableEffect(window) {
            val dropTarget = object : DropTarget() {
                override fun dragEnter(dtde: DropTargetDragEvent) { dtde.acceptDrag(DnDConstants.ACTION_COPY) }
                override fun dragOver(dtde: DropTargetDragEvent) { dtde.acceptDrag(DnDConstants.ACTION_COPY) }
                override fun drop(evt: DropTargetDropEvent) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY)
                        val list = evt.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                        list.firstOrNull()?.let {
                            val file = it as File
                            if (file.name.endsWith(".png") || file.name.endsWith(".jpg") || file.name.endsWith(".bmp")) {
                                loadFile(file)
                            }
                        }
                        evt.dropComplete(true)
                    } catch (e: Exception) {
                        e.printStackTrace(); evt.dropComplete(false)
                    }
                }
            }
            // 递归挂载
            fun attachToAll(component: Component) {
                component.dropTarget = dropTarget
                if (component is Container) {
                    for (child in component.components) attachToAll(child)
                }
            }
            attachToAll(window)
            onDispose { window.dropTarget = null }
        }
    }

    if (showScreenCropper && fullScreenCapture != null) {
        ScreenCropperDialog(
            fullScreenImage = fullScreenCapture!!,
            onDismiss = { showScreenCropper = false },
            onCropConfirm = { cropped -> showScreenCropper = false; addImage(cropped, "截图 ${imageList.size + 1}") }
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            onLoadFile = { ImageUtils.pickFile()?.let { loadFile(it) } },
            onScreenCapture = {
                scope.launch(Dispatchers.IO) {
                    delay(300)
                    val capture = ImageUtils.captureFullScreen()
                    withContext(Dispatchers.Main) { fullScreenCapture = capture; showScreenCropper = true }
                }
            },
            isCropMode = isCropMode,
            onToggleCropMode = { isCropMode = !isCropMode }
        )

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Workspace(
                modifier = Modifier.weight(3f),
                workImage = currentImage,
                isCropMode = isCropMode,
                scale = mainScale,
                offset = mainOffset,
                onTransformChange = { s, o -> mainScale = s; mainOffset = o },
                onHoverChange = { pos, col -> hoverPixelPos = pos; hoverColor = col },
                onColorPick = { hex ->
                    if (colorRules.size < 10 && colorRules.none { it.targetHex == hex }) {
                        colorRules.add(ColorRule(targetHex = hex, biasHex = defaultBias))
                    }
                },
                onCropConfirm = { cropRect ->
                    currentImage?.let {
                        val newImg = ImageUtils.cropImage(it.bufferedImage, cropRect)
                        addImage(newImg, "裁剪 ${imageList.size + 1}")
                    }
                },
                colorRules = colorRules,
                // 传进去用于绘制
                charRects = charRects,
                // 点击某个框时的回调（可选，暂时留空后续做选中逻辑）
                onCharRectClick = { rect -> println("Selected: $rect") }
            )

            RightPanel(
                modifier = Modifier.width(340.dp),
                rawImage = currentImage?.bufferedImage,
                hoverPixelPos = hoverPixelPos,
                hoverColor = hoverColor,
                mainScale = mainScale,
                onScaleChange = { mainScale = it },
                colorRules = colorRules,
                defaultBias = defaultBias,
                onDefaultBiasChange = { defaultBias = it },
                onRuleUpdate = { id, newBias ->
                    val index = colorRules.indexOfFirst { it.id == id }
                    if (index != -1) colorRules[index] = colorRules[index].copy(biasHex = newBias)
                },
                // 新增：处理启用状态切换
                onRuleToggle = { id, enabled ->
                    val index = colorRules.indexOfFirst { it.id == id }
                    if (index != -1) colorRules[index] = colorRules[index].copy(isEnabled = enabled)
                },
                onRuleRemove = { id -> colorRules.removeIf { it.id == id } },
                onClearRules = { colorRules.clear() },
                showBinary = showBinaryPreview,
                onTogglePreview = { showBinaryPreview = !showBinaryPreview },
                // 新增回调：执行切割
                onAutoSegment = {
                    currentImage?.let { workImg ->
                        scope.launch(Dispatchers.Default) {
                            val activeRules = colorRules.filter { it.isEnabled }
                            if (activeRules.isNotEmpty()) {
                                // 1. 执行智能识别
                                val rawRects = ImageUtils.scanConnectedComponents(workImg.bufferedImage, activeRules)

                                // 2. 计算网格参数建议值
                                if (rawRects.isNotEmpty()) {
                                    // 简单的排版推算逻辑 (假设是单行横排)
                                    // 按 x 坐标排序
                                    val sortedRects = rawRects.sortedBy { it.left }

                                    // 起点：第一个框的左上角
                                    val first = sortedRects.first()
                                    val newX = first.left.toInt()
                                    val newY = first.top.toInt()

                                    // 宽高：取平均值 (更平滑) 或 最大值 (确保包住)
                                    // 这里使用平均值，微调时更方便
                                    val avgW = sortedRects.map { it.width }.average().toInt()
                                    val avgH = sortedRects.map { it.height }.average().toInt()

                                    // 数量
                                    val count = sortedRects.size

                                    // 间距：计算相邻框的平均间隙
                                    var totalGap = 0f
                                    var gapCount = 0
                                    for (i in 0 until sortedRects.size - 1) {
                                        val gap = sortedRects[i+1].left - sortedRects[i].right
                                        // 过滤掉异常大的间距（比如换行了）
                                        if (gap > 0 && gap < avgW * 2) {
                                            totalGap += gap
                                            gapCount++
                                        }
                                    }
                                    val avgGap = if (gapCount > 0) (totalGap / gapCount).toInt() else 0

                                    // 3. 更新 UI 状态
                                    withContext(Dispatchers.Main) {
                                        // 更新显示的绿框 (智能模式结果)
                                        charRects = rawRects

                                        // 【关键】同时将计算出的参数填入网格设置中
                                        // 这样当你切换到“定距切割”模式时，参数已经准备好了！
                                        gridX = newX
                                        gridY = newY
                                        gridW = avgW
                                        gridH = avgH
                                        gridColGap = avgGap
                                        gridRowGap = 0
                                        gridColCount = count
                                        gridRowCount = 1

                                        // 可选：是否自动切到网格模式？
                                        // 建议保留在智能模式，让用户看一眼识别结果，再手动点切换去微调
                                        // isGridMode = true
                                    }
                                } else {
                                    withContext(Dispatchers.Main) { charRects = emptyList() }
                                }
                            }
                        }
                    }
                },
                // 传递网格参数给右侧面板
                isGridMode = isGridMode,
                onToggleGridMode = { isGridMode = it },
                gridParams = GridParams(gridX, gridY, gridW, gridH, gridColGap, gridRowGap, gridColCount, gridRowCount),
                onGridParamChange = { x, y, w, h, cg, rg, cc, rc ->
                    gridX = x; gridY = y; gridW = w; gridH = h
                    gridColGap = cg; gridRowGap = rg
                    gridColCount = cc; gridRowCount = rc
                },
                // 新增回调：清除切割
                onClearSegments = { charRects = emptyList() }
            )
        }

        BottomBar(
            images = imageList,
            selectedIndex = currentIndex,
            onSelect = { currentIndex = it },
            onDelete = { index ->
                if (index in imageList.indices) {
                    imageList.removeAt(index)
                    if (currentIndex >= imageList.size) currentIndex = imageList.lastIndex
                }
            }
        )
    }
}

// 辅助数据类，方便传递参数
data class GridParams(
    val x: Int, val y: Int, val w: Int, val h: Int,
    val colGap: Int, val rowGap: Int,
    val colCount: Int, val rowCount: Int
)