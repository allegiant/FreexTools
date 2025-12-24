package org.eu.freex.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.freex.tools.dialogs.ScreenCropperDialog
import org.eu.freex.tools.model.ActiveSource
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.model.GridParams
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

// 定义当前参数的作用域：全局模式 或 局部(单图)重载模式
enum class RuleScope { GLOBAL, LOCAL }

@Composable
fun App(window: androidx.compose.ui.awt.ComposeWindow?) {
    var currentModule by remember { mutableStateOf(AppModule.IMAGE_PROCESSING) }

    // --- 1. 工程数据 (Project Data) ---
    val sourceImages = remember { mutableStateListOf<WorkImage>() }

    // 全局规则 (Global Rules)
    val globalColorRules = remember { mutableStateListOf<ColorRule>() }
    var globalBias by remember { mutableStateOf("101010") }
    // 全局固定裁剪框 (例如固定的UI位置)
    val globalFixedRects = remember { mutableStateListOf<Rect>() }

    // --- 2. 运行时状态 (Runtime State) ---
    var selectedSourceIndex by remember { mutableStateOf(-1) }

    // 预览流结果 (代替原来的暂存区，这里存放的是实时计算出的结果)
    val previewResults = remember { mutableStateListOf<WorkImage>() }

    // 当前操作的作用域 (全局 vs 局部)
    var currentScope by remember { mutableStateOf(RuleScope.GLOBAL) }

    // 获取当前选中的源图片
    val currentSourceImage = if (selectedSourceIndex in sourceImages.indices) sourceImages[selectedSourceIndex] else null

    // --- 3. 计算当前生效的规则 (Effective Rules) ---
    // 逻辑：如果有局部重载，用局部的；否则用全局的
    val activeColorRules = currentSourceImage?.localColorRules ?: globalColorRules
    val activeBias = currentSourceImage?.localBias ?: globalBias

    // 当前生效的裁剪框 (仅用于显示，实际计算在 LaunchedEffect 中)
    val activeRectsState = remember { mutableStateOf<List<Rect>>(emptyList()) }

    // 画布与工具状态
    var mainScale by remember { mutableStateOf(1f) }
    var mainOffset by remember { mutableStateOf(Offset.Zero) }
    var hoverPixelPos by remember { mutableStateOf<IntOffset?>(null) }
    var hoverColor by remember { mutableStateOf(Color.Transparent) }

    var showScreenCropper by remember { mutableStateOf(false) }
    var fullScreenCapture by remember { mutableStateOf<BufferedImage?>(null) }
    var binaryBitmap by remember { mutableStateOf<ImageBitmap?>(null) } // 全图预览

    // 网格参数
    var isGridMode by remember { mutableStateOf(false) }
    // 注意：GridParams 初始化参数
    var gridParams by remember { mutableStateOf(GridParams(0, 0, 15, 15, 0, 0, 1, 1)) }

    val scope = rememberCoroutineScope()

    // --- 核心逻辑 A: 实时计算流水线 (The Pipeline) ---
    // 当源图切换、规则变更、或者网格参数变更时，自动重算所有结果
    LaunchedEffect(
        currentSourceImage,
        // 监听规则列表内容的变化
        activeColorRules.toList(),
        activeColorRules.size,
        // 监听固定框变化
        globalFixedRects.toList(),
        globalFixedRects.size,
        isGridMode,
        gridParams
    ) {
        if (currentSourceImage == null) {
            previewResults.clear()
            binaryBitmap = null
            activeRectsState.value = emptyList()
            return@LaunchedEffect
        }

        withContext(Dispatchers.Default) {
            val rawImg = currentSourceImage.bufferedImage

            // 1. 计算二值化全图 (用于中间画布叠加显示)
            val activeRules = activeColorRules.filter { it.isEnabled }
            if (activeRules.isNotEmpty()) {
                val fullRect = Rect(0f, 0f, rawImg.width.toFloat(), rawImg.height.toFloat())
                val binImg = ImageUtils.binarizeImage(rawImg, activeRules, fullRect)
                binaryBitmap = binImg.toComposeImageBitmap()
            } else {
                binaryBitmap = null
            }

            // 2. 确定裁剪区域 (Rects)
            val rects = mutableListOf<Rect>()

            // 2.1 优先使用手动指定的局部框 (如果有)
            if (!currentSourceImage.localCropRects.isNullOrEmpty()) {
                rects.addAll(currentSourceImage.localCropRects)
            }
            // 2.2 否则使用生成逻辑
            else {
                if (isGridMode) {
                    // 模式 A: 网格切割
                    rects.addAll(ImageUtils.generateGridRects(
                        gridParams.x, gridParams.y, gridParams.w, gridParams.h,
                        gridParams.colGap, gridParams.rowGap, gridParams.colCount, gridParams.rowCount
                    ))
                } else {
                    // 模式 B: 智能识别 (基于二值化规则)
                    if (activeRules.isNotEmpty()) {
                        val autoRects = ImageUtils.scanConnectedComponents(rawImg, activeRules)
                        rects.addAll(autoRects)
                    }
                    // 模式 C: 全局固定框 (叠加)
                    rects.addAll(globalFixedRects)
                }
            }

            // 更新画布显示的框
            activeRectsState.value = rects

            // 3. 生成预览流结果 (Pipeline Output)
            // 实时切割出小图，放入左下角预览区
            val newPreviews = rects.mapIndexed { index, rect ->
                val processedImg = if (activeRules.isNotEmpty()) {
                    ImageUtils.binarizeImage(rawImg, activeRules, rect)
                } else {
                    ImageUtils.cropImage(rawImg, rect)
                }
                WorkImage(
                    bitmap = processedImg.toComposeImageBitmap(),
                    bufferedImage = processedImg,
                    name = "${currentSourceImage.name}_$index",
                    // 【修正】参数名对应 WorkImage 的新定义
                    localColorRules = activeRules.map { it.copy() }, // 携带生成它的基因
                    localBias = activeBias
                )
            }

            withContext(Dispatchers.Main) {
                previewResults.clear()
                previewResults.addAll(newPreviews)
            }
        }
    }

    // --- 辅助函数 ---

    // 更新规则的通用方法 (处理 Global vs Local)
    fun updateRules(action: (MutableList<ColorRule>) -> Unit) {
        if (currentScope == RuleScope.GLOBAL) {
            action(globalColorRules)
            // 全局修改会自动触发 LaunchedEffect
        } else {
            // Local 模式：需要拷贝一份规则给当前图片，形成 Override
            if (currentSourceImage != null) {
                val newRules = (currentSourceImage.localColorRules ?: globalColorRules).map { it.copy() }.toMutableList()
                action(newRules)
                // 更新源图列表中的该图片对象
                val newImage = currentSourceImage.copy(localColorRules = newRules)
                if (selectedSourceIndex in sourceImages.indices) {
                    sourceImages[selectedSourceIndex] = newImage
                }
            }
        }
    }

    fun addSourceImage(bufferedImage: BufferedImage, name: String) {
        sourceImages.add(WorkImage(bufferedImage.toComposeImageBitmap(), bufferedImage, name))
        selectedSourceIndex = sourceImages.lastIndex
        // 默认切回 Global 模式，除非用户刻意想保留在 Local
        // currentScope = RuleScope.GLOBAL
    }

    fun loadFile(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val img = ImageIO.read(file)
                if (img != null) withContext(Dispatchers.Main) { addSourceImage(img, file.name) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 拖拽文件支持
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

    // 截图弹窗
    if (showScreenCropper && fullScreenCapture != null) {
        ScreenCropperDialog(
            fullScreenImage = fullScreenCapture!!,
            onDismiss = { showScreenCropper = false },
            onCropConfirm = { cropped -> showScreenCropper = false; addSourceImage(cropped, "截图 ${sourceImages.size + 1}") }
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(currentModule = currentModule, onModuleChange = { currentModule = it })

        Box(Modifier.weight(1f)) {
            when (currentModule) {
                AppModule.IMAGE_PROCESSING -> {
                    ImageProcessingWorkbench(
                        sourceImages = sourceImages,
                        resultImages = previewResults, // 左下角现在显示的是实时预览流
                        currentImage = currentSourceImage,

                        selectedSourceIndex = selectedSourceIndex,
                        selectedResultIndex = -1, // 预览流一般不需要选中高亮，或者仅用于定位
                        activeSource = ActiveSource.SOURCE,

                        mainScale = mainScale,
                        mainOffset = mainOffset,
                        hoverPixelPos = hoverPixelPos,
                        hoverColor = hoverColor,

                        // 传入计算好的生效规则
                        colorRules = activeColorRules,
                        defaultBias = activeBias,
                        binaryBitmap = binaryBitmap,

                        isGridMode = isGridMode,
                        gridParams = gridParams,
                        charRects = activeRectsState.value,

                        // 新增参数：作用域控制
                        currentScope = currentScope,
                        onToggleScope = {
                            currentScope = if (currentScope == RuleScope.GLOBAL) RuleScope.LOCAL else RuleScope.GLOBAL
                        },

                        // --- 回调 ---
                        onSelectSource = { idx -> selectedSourceIndex = idx },
                        // 点击预览结果暂不做复杂处理
                        onSelectResult = { },

                        onAddFile = { ImageUtils.pickFile()?.let { loadFile(it) } },
                        onScreenCapture = {
                            scope.launch(Dispatchers.IO) {
                                delay(300)
                                val capture = ImageUtils.captureFullScreen()
                                withContext(Dispatchers.Main) { fullScreenCapture = capture; showScreenCropper = true }
                            }
                        },

                        onDeleteSource = { idx ->
                            if (idx in sourceImages.indices) {
                                sourceImages.removeAt(idx)
                                if (selectedSourceIndex >= sourceImages.size) selectedSourceIndex = sourceImages.lastIndex
                            }
                        },
                        // 在流水线模式下，无法单独删除某一个结果，因为它是算出来的
                        onDeleteResult = { },

                        onTransformChange = { s, o -> mainScale = s; mainOffset = o },
                        onHoverChange = { p, c -> hoverPixelPos = p; hoverColor = c },

                        // 取色
                        onColorPick = { hex ->
                            // 只有当规则未满时添加
                            val targetList = if (currentScope == RuleScope.GLOBAL) globalColorRules else (currentSourceImage?.localColorRules ?: globalColorRules)
                            if (targetList.size < 10 && targetList.none { it.targetHex == hex }) {
                                updateRules { it.add(ColorRule(targetHex = hex, biasHex = activeBias)) }
                            }
                        },

                        // 手动裁剪 (添加框)
                        onCropConfirm = { rect ->
                            // 手动裁剪现在意味着：添加一个固定识别区域
                            if (currentScope == RuleScope.GLOBAL) {
                                globalFixedRects.add(rect)
                            } else {
                                // Local 模式：给当前图添加私有框
                                currentSourceImage?.let { img ->
                                    val newRects = (img.localCropRects ?: emptyList()) + rect
                                    sourceImages[selectedSourceIndex] = img.copy(localCropRects = newRects)
                                }
                            }
                        },

                        onScaleChange = { mainScale = it },
                        onDefaultBiasChange = { newBias ->
                            if (currentScope == RuleScope.GLOBAL) {
                                globalBias = newBias
                            } else {
                                currentSourceImage?.let {
                                    sourceImages[selectedSourceIndex] = it.copy(localBias = newBias)
                                }
                            }
                        },

                        // 规则操作
                        onRuleUpdate = { id, bias -> updateRules { list -> list.find { it.id == id }?.let { list[list.indexOf(it)] = it.copy(biasHex = bias) } } },
                        onRuleToggle = { id, enabled -> updateRules { list -> list.find { it.id == id }?.let { list[list.indexOf(it)] = it.copy(isEnabled = enabled) } } },
                        onRuleRemove = { id -> updateRules { list -> list.removeIf { it.id == id } } },
                        onClearRules = { updateRules { it.clear() } },

                        onAutoSegment = { /* 自动分割是实时计算的，不需要显式触发 */ },
                        onClearSegments = {
                            // 清空框 -> 清空 GlobalFixedRects 或 LocalRects
                            if (currentScope == RuleScope.GLOBAL) globalFixedRects.clear()
                            else currentSourceImage?.let { sourceImages[selectedSourceIndex] = it.copy(localCropRects = emptyList()) }
                        },

                        onToggleGridMode = { isGridMode = it },
                        onGridParamChange = { x, y, w, h, cg, rg, cc, rc ->
                            gridParams = GridParams(x, y, w, h, cg, rg, cc, rc)
                        },

                        // [新增] 保存工程/导出结果
                        onGridExtract = {
                            // 这里可以实现将 previewResults 写入文件的逻辑
                            println("Exporting ${previewResults.size} images from current pipeline...")
                        }
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