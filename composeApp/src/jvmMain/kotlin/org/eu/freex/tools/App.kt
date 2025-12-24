// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/App.kt
package org.eu.freex.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.freex.tools.dialogs.ScreenCropperDialog
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.model.GridParams
import org.eu.freex.tools.model.WorkImage
import org.eu.freex.tools.utils.ColorUtils
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
    var currentModule by remember { mutableStateOf(AppModule.IMAGE_PROCESSING) }

    // --- 数据状态 ---
    val sourceImages = remember { mutableStateListOf<WorkImage>() }
    var currentIndex by remember { mutableStateOf(-1) }
    val resultImages = remember { mutableStateListOf<WorkImage>() }

    val currentImage = if (currentIndex in sourceImages.indices) sourceImages[currentIndex] else null

    // --- UI/工具状态 ---
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
    var charRects by remember { mutableStateOf<List<Rect>>(emptyList()) }

    // 网格参数
    var isGridMode by remember { mutableStateOf(false) }
    var gridX by remember { mutableStateOf(0) }
    var gridY by remember { mutableStateOf(0) }
    var gridW by remember { mutableStateOf(15) }
    var gridH by remember { mutableStateOf(15) }
    var gridColGap by remember { mutableStateOf(0) }
    var gridRowGap by remember { mutableStateOf(0) }
    var gridColCount by remember { mutableStateOf(1) }
    var gridRowCount by remember { mutableStateOf(1) }
    var binaryBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val scope = rememberCoroutineScope()

    // --- 帮助函数 ---
    fun addSourceImage(bufferedImage: BufferedImage, name: String) {
        sourceImages.add(WorkImage(bufferedImage.toComposeImageBitmap(), bufferedImage, name))
        currentIndex = sourceImages.lastIndex
        mainScale = 1f; mainOffset = Offset.Zero; isCropMode = false
    }

    fun addResultImage(bufferedImage: BufferedImage, name: String) {
        resultImages.add(WorkImage(bufferedImage.toComposeImageBitmap(), bufferedImage, name))
    }

    // 监听网格参数变化自动更新框
    LaunchedEffect(isGridMode, gridX, gridY, gridW, gridH, gridColGap, gridRowGap, gridColCount, gridRowCount) {
        if (isGridMode) {
            charRects = ImageUtils.generateGridRects(gridX, gridY, gridW, gridH, gridColGap, gridRowGap, gridColCount, gridRowCount)
        }
    }

    fun loadFile(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val img = ImageIO.read(file)
                if (img != null) withContext(Dispatchers.Main) { addSourceImage(img, file.name) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 拖拽文件逻辑
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
                    } catch (e: Exception) { e.printStackTrace(); evt.dropComplete(false) }
                }
            }
            fun attachToAll(component: Component) {
                component.dropTarget = dropTarget
                if (component is Container) for (child in component.components) attachToAll(child)
            }
            attachToAll(window)
            onDispose { window.dropTarget = null }
        }
    }

    // 截图弹窗逻辑
    if (showScreenCropper && fullScreenCapture != null) {
        ScreenCropperDialog(
            fullScreenImage = fullScreenCapture!!,
            onDismiss = { showScreenCropper = false },
            onCropConfirm = { cropped -> showScreenCropper = false; addSourceImage(cropped, "截图 ${sourceImages.size + 1}") }
        )
    }

    // 只有当开启预览时才计算，节省性能？
    // 或者一直计算以便切换时无延迟。建议一直计算，因为规则变动频率不高。
    LaunchedEffect(currentImage, colorRules.toList(), colorRules.size) {
        if (currentImage == null) {
            binaryBitmap = null
            return@LaunchedEffect
        }

        // 只有当开启预览时才计算，节省性能？
        // 或者一直计算以便切换时无延迟。建议一直计算，因为规则变动频率不高。
        withContext(Dispatchers.Default) {
            val rawImage = currentImage.bufferedImage
            val activeRules = colorRules.filter { it.isEnabled }

            if (activeRules.isEmpty()) {
                binaryBitmap = null
            } else {
                val w = rawImage.width
                val h = rawImage.height
                val resultImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

                // 这里可以使用 ImageUtils 里的高性能算法优化，暂时用简单循环
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val rgb = rawImage.getRGB(x, y)
                        val isMatch = ColorUtils.isMatchAny(rgb, activeRules)
                        // 匹配显示保留原色或白色？通常二值化是 黑白。
                        // 这里我们做成：匹配的部分显示高亮色(如白色)，不匹配的显示黑色或透明
                        resultImg.setRGB(x, y, if (isMatch) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
                    }
                }
                binaryBitmap = resultImg.toComposeImageBitmap()
            }
        }
    }

    // --- 主界面布局 ---
    Column(Modifier.fillMaxSize()) {
        TopBar(
            currentModule = currentModule,
            onModuleChange = { currentModule = it }
        )

        Box(Modifier.weight(1f)) {
            when (currentModule) {
                AppModule.IMAGE_PROCESSING -> {
                    ImageProcessingWorkbench(
                        sourceImages = sourceImages,
                        currentImageIndex = currentIndex,
                        resultImages = resultImages,
                        currentImage = currentImage,
                        mainScale = mainScale,
                        mainOffset = mainOffset,
                        hoverPixelPos = hoverPixelPos,
                        hoverColor = hoverColor,
                        isCropMode = isCropMode,
                        colorRules = colorRules,
                        defaultBias = defaultBias,
                        showBinaryPreview = showBinaryPreview,
                        isGridMode = isGridMode,
                        gridParams = GridParams(gridX, gridY, gridW, gridH, gridColGap, gridRowGap, gridColCount, gridRowCount),
                        charRects = charRects,

                        onSelectImage = { currentIndex = it },
                        onAddFile = { ImageUtils.pickFile()?.let { loadFile(it) } },
                        onScreenCapture = {
                            scope.launch(Dispatchers.IO) {
                                delay(300) // 等待窗口最小化动画
                                val capture = ImageUtils.captureFullScreen()
                                withContext(Dispatchers.Main) { fullScreenCapture = capture; showScreenCropper = true }
                            }
                        },
                        onDeleteSource = { idx ->
                            if (idx in sourceImages.indices) {
                                sourceImages.removeAt(idx)
                                if (currentIndex >= sourceImages.size) currentIndex = sourceImages.lastIndex
                            }
                        },
                        onDeleteResult = { idx -> if (idx in resultImages.indices) resultImages.removeAt(idx) },

                        onTransformChange = { s, o -> mainScale = s; mainOffset = o },
                        onHoverChange = { p, c -> hoverPixelPos = p; hoverColor = c },
                        onColorPick = { hex ->
                            if (colorRules.size < 10 && colorRules.none { it.targetHex == hex }) {
                                colorRules.add(ColorRule(targetHex = hex, biasHex = defaultBias))
                            }
                        },
                        onCropConfirm = { rect ->
                            currentImage?.let {
                                val newImg = ImageUtils.cropImage(it.bufferedImage, rect)
                                addResultImage(newImg, "Crop_${resultImages.size}")
                            }
                        },
                        onToggleCropMode = { isCropMode = !isCropMode },
                        onScaleChange = { mainScale = it },
                        onDefaultBiasChange = { defaultBias = it },
                        onRuleUpdate = { id, bias -> colorRules.find { it.id == id }?.let { colorRules[colorRules.indexOf(it)] = it.copy(biasHex = bias) } },
                        onRuleToggle = { id, enabled -> colorRules.find { it.id == id }?.let { colorRules[colorRules.indexOf(it)] = it.copy(isEnabled = enabled) } },
                        onRuleRemove = { id -> colorRules.removeIf { it.id == id } },
                        onClearRules = { colorRules.clear() },
                        onTogglePreview = { showBinaryPreview = !showBinaryPreview },
                        onAutoSegment = {
                            currentImage?.let { workImg ->
                                scope.launch(Dispatchers.Default) {
                                    val activeRules = colorRules.filter { it.isEnabled }
                                    if (activeRules.isNotEmpty()) {
                                        val rawRects = ImageUtils.scanConnectedComponents(workImg.bufferedImage, activeRules)
                                        withContext(Dispatchers.Main) {
                                            if (rawRects.isNotEmpty()) {
                                                charRects = rawRects
                                                // 自动填充网格参数逻辑可在此处优化
                                                val first = rawRects.minByOrNull { it.left } ?: rawRects[0]
                                                gridX = first.left.toInt()
                                                gridY = first.top.toInt()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onClearSegments = { charRects = emptyList() },
                        onToggleGridMode = { isGridMode = it },
                        onGridParamChange = { x, y, w, h, cg, rg, cc, rc ->
                            gridX = x; gridY = y; gridW = w; gridH = h
                            gridColGap = cg; gridRowGap = rg
                            gridColCount = cc; gridRowCount = rc
                        },
                        onGridExtract = {
                            currentImage?.let { img ->
                                charRects.forEach { rect ->
                                    val cut = ImageUtils.cropImage(img.bufferedImage, rect)
                                    addResultImage(cut, "Grid_${resultImages.size}")
                                }
                            }
                        },
                        binaryBitmap = binaryBitmap,
                    )
                }
                AppModule.FONT_MANAGER -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("字库管理模块 - 开发中...", color = Color.Gray)
                    }
                }
            }
        }
    }
}