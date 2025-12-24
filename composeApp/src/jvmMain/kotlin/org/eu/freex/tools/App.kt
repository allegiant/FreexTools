// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/App.kt
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
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.freex.tools.dialogs.CharMappingDialog
import org.eu.freex.tools.dialogs.ScreenCropperDialog
import org.eu.freex.tools.model.ActiveSource
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.model.FontItem
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

enum class RuleScope { GLOBAL, LOCAL }

@Composable
fun App(window: androidx.compose.ui.awt.ComposeWindow?) {
    var currentModule by remember { mutableStateOf(AppModule.IMAGE_PROCESSING) }

    // --- 1. 工程数据 (Project Data) ---
    val sourceImages = remember { mutableStateListOf<WorkImage>() }

    // 全局规则
    val globalColorRules = remember { mutableStateListOf<ColorRule>() }
    var globalBias by remember { mutableStateOf("101010") }
    val globalFixedRects = remember { mutableStateListOf<Rect>() }

    // --- 2. 运行时状态 ---
    var selectedSourceIndex by remember { mutableStateOf(-1) }
    var currentScope by remember { mutableStateOf(RuleScope.GLOBAL) }

    // 获取当前有效的源图
    val currentSourceImage = if (selectedSourceIndex in sourceImages.indices) sourceImages[selectedSourceIndex] else null

    // --- 3. 流水线核心逻辑 (Pipeline) ---
    // 存放中间步骤 (不含源图，不含二值化)
    val pipelineSteps = remember { mutableStateListOf<WorkImage>() }
    // 动态计算的二值化结果
    var binaryPreview by remember { mutableStateOf<WorkImage?>(null) }
    // 底部流水线选中的索引 (0=原图, 1..N=步骤, N+1=二值化)
    var selectedPipelineIndex by remember { mutableStateOf(0) }

    // 【关键】计算二值化的基础图：取流水线最后一步，如果没有则取原图
    val activeBaseImage = remember(currentSourceImage, pipelineSteps.size, pipelineSteps.toList()) {
        if (pipelineSteps.isNotEmpty()) {
            pipelineSteps.last()
        } else {
            currentSourceImage
        }
    }

    // 构建展示链 (Display Chain)
    val displayChain = remember(currentSourceImage, pipelineSteps.toList(), binaryPreview) {
        val list = mutableListOf<WorkImage>()
        if (currentSourceImage != null) {
            list.add(currentSourceImage.copy(label = "原图"))
        }
        list.addAll(pipelineSteps)
        if (binaryPreview != null) {
            list.add(binaryPreview!!)
        }
        list
    }

    // 确定当前工作区显示的图片
    val currentWorkImage = if (displayChain.isNotEmpty() && selectedPipelineIndex in displayChain.indices) {
        displayChain[selectedPipelineIndex]
    } else {
        activeBaseImage
    }

    // 切换源文件时，重置流水线
    LaunchedEffect(selectedSourceIndex) {
        pipelineSteps.clear()
        binaryPreview = null
        selectedPipelineIndex = 0
    }

    // --- 4. 规则计算 ---
    val activeColorRules = currentSourceImage?.localColorRules ?: globalColorRules
    val activeBias = currentSourceImage?.localBias ?: globalBias

    // 生效的裁剪框
    val activeRectsState = remember { mutableStateOf<List<Rect>>(emptyList()) }

    // 画布状态
    var mainScale by remember { mutableStateOf(1f) }
    var mainOffset by remember { mutableStateOf(Offset.Zero) }
    var hoverPixelPos by remember { mutableStateOf<IntOffset?>(null) }
    var hoverColor by remember { mutableStateOf(Color.Transparent) }

    var showScreenCropper by remember { mutableStateOf(false) }
    var fullScreenCapture by remember { mutableStateOf<BufferedImage?>(null) }

    // 映射相关
    val fontLibrary = remember { mutableStateListOf<FontItem>() }
    var mappingBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var mappingBufferedImg by remember { mutableStateOf<BufferedImage?>(null) }
    var showMappingDialog by remember { mutableStateOf(false) }

    // 网格参数
    var isGridMode by remember { mutableStateOf(false) }
    var gridParams by remember { mutableStateOf(GridParams(0, 0, 15, 15, 0, 0, 1, 1)) }

    val scope = rememberCoroutineScope()

    // --- 自动计算二值化预览 (Binary Preview) ---
    // 当 Base Image 变化或规则变化时触发
    LaunchedEffect(
        activeBaseImage,
        activeColorRules.toList(),
        activeColorRules.size
    ) {
        if (activeBaseImage != null && activeColorRules.any { it.isEnabled }) {
            withContext(Dispatchers.Default) {
                val rawImg = activeBaseImage.bufferedImage
                val fullRect = Rect(0f, 0f, rawImg.width.toFloat(), rawImg.height.toFloat())
                val binImg = ImageUtils.binarizeImage(rawImg, activeColorRules, fullRect)

                val result = WorkImage(
                    bitmap = binImg.toComposeImageBitmap(),
                    bufferedImage = binImg,
                    name = "Preview_Binary",
                    label = "二值化",
                    isBinary = true
                )
                withContext(Dispatchers.Main) {
                    binaryPreview = result
                }
            }
        } else {
            binaryPreview = null
        }
    }

    // --- 自动计算裁剪框 (Segmentation) ---
    LaunchedEffect(
        currentWorkImage, // 注意：切分是基于当前看到的图片 (可能是原图，也可能是二值化图)
        activeColorRules.toList(),
        activeColorRules.size,
        globalFixedRects.toList(),
        globalFixedRects.size,
        isGridMode,
        gridParams
    ) {
        if (currentWorkImage == null) {
            activeRectsState.value = emptyList()
            return@LaunchedEffect
        }

        withContext(Dispatchers.Default) {
            val rawImg = currentWorkImage.bufferedImage
            val rects = mutableListOf<Rect>()

            // 1. 如果有局部框
            if (!currentWorkImage.localCropRects.isNullOrEmpty()) {
                rects.addAll(currentWorkImage.localCropRects)
            } else {
                if (isGridMode) {
                    rects.addAll(ImageUtils.generateGridRects(
                        gridParams.x, gridParams.y, gridParams.w, gridParams.h,
                        gridParams.colGap, gridParams.rowGap, gridParams.colCount, gridParams.rowCount
                    ))
                } else {
                    // 智能识别：如果是二值化图，直接搜白块；如果是原图，用规则搜
                    val rulesToUse = if (currentWorkImage.isBinary) {
                        // 对于已经是二值化的图，我们可以认为白色(0xFFFFFF)是目标
                        listOf(ColorRule(targetHex = "FFFFFF", biasHex = "000000"))
                    } else {
                        activeColorRules.filter { it.isEnabled }
                    }

                    if (rulesToUse.isNotEmpty()) {
                        val autoRects = ImageUtils.scanConnectedComponents(rawImg, rulesToUse)
                        rects.addAll(autoRects)
                    }
                    rects.addAll(globalFixedRects)
                }
            }
            activeRectsState.value = rects
        }
    }

    // --- 辅助函数 ---
    fun updateRules(action: (MutableList<ColorRule>) -> Unit) {
        if (currentScope == RuleScope.GLOBAL) {
            action(globalColorRules)
        } else {
            if (currentSourceImage != null) {
                val newRules = (currentSourceImage.localColorRules ?: globalColorRules).map { it.copy() }.toMutableList()
                action(newRules)
                val newImage = currentSourceImage.copy(localColorRules = newRules)
                if (selectedSourceIndex in sourceImages.indices) {
                    sourceImages[selectedSourceIndex] = newImage
                }
            }
        }
    }

    // 添加到左侧工程资源
    fun addSourceImage(bufferedImage: BufferedImage, name: String) {
        sourceImages.add(WorkImage(bufferedImage.toComposeImageBitmap(), bufferedImage, name))
        selectedSourceIndex = sourceImages.lastIndex
    }

    fun loadFile(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val img = ImageIO.read(file)
                if (img != null) withContext(Dispatchers.Main) { addSourceImage(img, file.name) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 【修改】添加处理步骤通用方法 (添加到底部流水线)
    fun addProcessingStep(label: String, processor: (BufferedImage) -> BufferedImage) {
        if (activeBaseImage == null) return
        scope.launch(Dispatchers.Default) {
            // 基于当前的 activeBaseImage (流水线末端非二值化图) 进行处理
            val processed = processor(activeBaseImage.bufferedImage)
            val newStep = WorkImage(
                bitmap = processed.toComposeImageBitmap(),
                bufferedImage = processed,
                name = "Step_${pipelineSteps.size}",
                label = label
            )
            withContext(Dispatchers.Main) {
                pipelineSteps.add(newStep)
                // 自动选中新步骤 (Source(1) + OldSteps(N) + NewStep(1) - 1(index))
                // 等价于 1 + pipelineSteps.lastIndex
                selectedPipelineIndex = 1 + pipelineSteps.lastIndex
            }
        }
    }

    // 拖拽支持
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

    // 截图仍然添加为新的工程资源 (因为它是全新的来源)
    if (showScreenCropper && fullScreenCapture != null) {
        ScreenCropperDialog(
            fullScreenImage = fullScreenCapture!!,
            onDismiss = { showScreenCropper = false },
            onCropConfirm = { cropped -> showScreenCropper = false; addSourceImage(cropped, "截图 ${sourceImages.size + 1}") }
        )
    }

    // 映射弹窗
    if (showMappingDialog && mappingBitmap != null) {
        CharMappingDialog(
            bitmap = mappingBitmap!!,
            onDismiss = { showMappingDialog = false },
            onConfirm = { char ->
                if (mappingBufferedImg != null) {
                    fontLibrary.add(FontItem(char, mappingBufferedImg!!))
                }
                showMappingDialog = false
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(currentModule = currentModule, onModuleChange = { currentModule = it })

        Box(Modifier.weight(1f)) {
            when (currentModule) {
                AppModule.IMAGE_PROCESSING -> {
                    ImageProcessingWorkbench(
                        sourceImages = sourceImages,
                        currentImage = currentWorkImage,

                        selectedSourceIndex = selectedSourceIndex,
                        activeSource = ActiveSource.SOURCE,

                        mainScale = mainScale,
                        mainOffset = mainOffset,
                        hoverPixelPos = hoverPixelPos,
                        hoverColor = hoverColor,

                        colorRules = activeColorRules,
                        defaultBias = activeBias,
                        // 只有当显示的不是二值化图时，才把 binaryPreview 传进去作为叠加层
                        binaryBitmap = if (currentWorkImage?.isBinary == false) binaryPreview?.bitmap else null,

                        isGridMode = isGridMode,
                        gridParams = gridParams,
                        charRects = activeRectsState.value,

                        currentScope = currentScope,
                        onToggleScope = {
                            currentScope = if (currentScope == RuleScope.GLOBAL) RuleScope.LOCAL else RuleScope.GLOBAL
                        },

                        onSelectSource = { idx -> selectedSourceIndex = idx },

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

                        onTransformChange = { s, o -> mainScale = s; mainOffset = o },
                        onHoverChange = { p, c -> hoverPixelPos = p; hoverColor = c },

                        onColorPick = { hex ->
                            val targetList = if (currentScope == RuleScope.GLOBAL) globalColorRules else (currentSourceImage?.localColorRules ?: globalColorRules)
                            if (targetList.size < 10 && targetList.none { it.targetHex == hex }) {
                                updateRules { it.add(ColorRule(targetHex = hex, biasHex = activeBias)) }
                            }
                        },

                        // 【修改】裁剪确认后，作为新的步骤添加到流水线 (BottomPanel)
                        // 而不是作为新资源添加到 LeftPanel
                        onCropConfirm = { rect ->
                            addProcessingStep("裁剪区域") { src ->
                                // src 是 activeBaseImage (流水线末端图)
                                ImageUtils.cropImage(src, rect)
                            }
                            if (currentScope == RuleScope.GLOBAL) globalFixedRects.clear()
                        },

                        onCharRectClick = { rect ->
                            if (currentWorkImage != null) {
                                val charImg = ImageUtils.cropImage(currentWorkImage.bufferedImage, rect)
                                mappingBufferedImg = charImg
                                mappingBitmap = charImg.toComposeImageBitmap()
                                showMappingDialog = true
                            }
                        },

                        onScaleChange = { mainScale = it },
                        onDefaultBiasChange = { newBias ->
                            if (currentScope == RuleScope.GLOBAL) globalBias = newBias else currentSourceImage?.let {
                                sourceImages[selectedSourceIndex] = it.copy(localBias = newBias)
                            }
                        },

                        onRuleUpdate = { id, bias -> updateRules { list -> list.find { it.id == id }?.let { list[list.indexOf(it)] = it.copy(biasHex = bias) } } },
                        onRuleToggle = { id, enabled -> updateRules { list -> list.find { it.id == id }?.let { list[list.indexOf(it)] = it.copy(isEnabled = enabled) } } },
                        onRuleRemove = { id -> updateRules { list -> list.removeIf { it.id == id } } },
                        onClearRules = { updateRules { it.clear() } },

                        onAutoSegment = {},
                        onClearSegments = {
                            if (currentScope == RuleScope.GLOBAL) globalFixedRects.clear()
                            else currentSourceImage?.let { sourceImages[selectedSourceIndex] = it.copy(localCropRects = emptyList()) }
                        },

                        onToggleGridMode = { isGridMode = it },
                        onGridParamChange = { x, y, w, h, cg, rg, cc, rc -> gridParams = GridParams(x, y, w, h, cg, rg, cc, rc) },
                        onGridExtract = { },

                        // --- 流水线参数 ---
                        processChain = displayChain,
                        selectedChainIndex = selectedPipelineIndex,
                        onSelectChainItem = { selectedPipelineIndex = it },
                        onDeleteChainItem = { index ->
                            // index 0 是原图，不删
                            // index > 0 对应 pipelineSteps[index-1]
                            val pipelineIndex = index - 1
                            if (pipelineIndex >= 0 && pipelineIndex < pipelineSteps.size) {
                                pipelineSteps.removeAt(pipelineIndex)
                                // 删除后回退选中到原图
                                selectedPipelineIndex = 0
                            }
                        },
                        onAddDenoise = { addProcessingStep("清除杂点") { src -> ImageUtils.dummyDenoise(src) } },
                        onAddSkeleton = { addProcessingStep("细化抽骨") { src -> ImageUtils.dummySkeleton(src) } }
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