package org.eu.freex.tools.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eu.freex.tools.model.*
import org.eu.freex.tools.utils.ImageUtils
import java.awt.Robot
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageProcessingViewModel {
    // --- 协程作用域 (由 UI 层创建并传入，或者在此处管理) ---
    // 为了简单起见，我们在 init 中接收 scope，或者让 UI 调用函数
    private val scope = CoroutineScope(Dispatchers.Main)

    // --- 核心状态 ---
    var sourceImages = mutableStateListOf<WorkImage>()
    var selectedSourceIndex by mutableStateOf(-1)

    // 规则相关
    var globalColorRules = mutableStateListOf<ColorRule>()
    var globalBias by mutableStateOf("101010")
    var globalFixedRects = mutableStateListOf<Rect>()
    var currentScope by mutableStateOf(RuleScope.GLOBAL)

    // 画布状态
    var mainScale by mutableStateOf(1f)
    var mainOffset by mutableStateOf(Offset.Zero)
    var hoverPixelPos by mutableStateOf<IntOffset?>(null)
    var hoverColor by mutableStateOf(Color.Transparent)

    // 滤镜与阈值
    var currentFilter by mutableStateOf<ImageFilter>(ColorFilterType.BINARIZATION)
    var thresholdRange by mutableStateOf(0f..72f)
    var isRgbAvgEnabled by mutableStateOf(true)

    // 网格
    var isGridMode by mutableStateOf(false)
    var gridParams by mutableStateOf(GridParams(0, 0, 15, 15, 0, 0, 1, 1))

    // 流水线
    var pipelineSteps = mutableStateListOf<WorkImage>()
    var binaryPreview by mutableStateOf<WorkImage?>(null)
    var selectedPipelineIndex by mutableStateOf(0)

    // UI 交互状态 (弹窗等)
    var showScreenCropper by mutableStateOf(false)
    var fullScreenCapture by mutableStateOf<BufferedImage?>(null)
    var showMappingDialog by mutableStateOf(false)
    var mappingBitmap by mutableStateOf<ImageBitmap?>(null)
    var mappingBufferedImg by mutableStateOf<BufferedImage?>(null)
    val fontLibrary = mutableStateListOf<FontItem>()

    // 触发器
    private var processingTrigger by mutableStateOf(0L)

    // --- 衍生状态 (Derived State) ---
    val currentSourceImage: WorkImage? get() = if (selectedSourceIndex in sourceImages.indices) sourceImages[selectedSourceIndex] else null

    val activeColorRules: List<ColorRule> get() = currentSourceImage?.localColorRules ?: globalColorRules
    val activeBias: String get() = currentSourceImage?.localBias ?: globalBias

    // 基础图：流水线最后一步或原图
    val activeBaseImage: WorkImage? get() = if (pipelineSteps.isNotEmpty()) pipelineSteps.last() else currentSourceImage

    // 结果链
    val displayChain: List<WorkImage> get() {
        val list = mutableListOf<WorkImage>()
        if (currentSourceImage != null) list.add(currentSourceImage!!.copy(label = "原图"))
        list.addAll(pipelineSteps)
        if (binaryPreview != null) list.add(binaryPreview!!)
        return list
    }

    // 当前工作图
    val currentWorkImage: WorkImage? get() = if (displayChain.isNotEmpty() && selectedPipelineIndex in displayChain.indices) {
        displayChain[selectedPipelineIndex]
    } else {
        activeBaseImage
    }

    // 自动计算出的矩形框
    var activeRects by mutableStateOf<List<Rect>>(emptyList())
        private set

    // --- 初始化: 启动自动计算监听 ---
    init {
        // 使用 snapshotFlow 监听依赖状态的变化
        snapshotFlow {
            ProcessingRequest(
                baseImage = activeBaseImage,
                trigger = processingTrigger
            )
        }.onEach {
            computePreviewAndRects()
        }.launchIn(scope)

        // 监听源切换，重置流水线
        snapshotFlow { selectedSourceIndex }.onEach {
            pipelineSteps.clear()
            binaryPreview = null
            selectedPipelineIndex = 0
        }.launchIn(scope)
    }

    // 数据类用于组合监听参数
    private data class ProcessingRequest(val baseImage: WorkImage?, val trigger: Long)

    // --- 业务逻辑 ---

    private suspend fun computePreviewAndRects() {
        val base = activeBaseImage ?: return
        withContext(Dispatchers.Default) {
            val rawImg = base.bufferedImage
            val fullRect = Rect(0f, 0f, rawImg.width.toFloat(), rawImg.height.toFloat())

            // 1. 二值化
            val binImg = if (isRgbAvgEnabled) {
                ImageUtils.binarizeByRgbAvg(rawImg, thresholdRange.start.toInt(), thresholdRange.endInclusive.toInt(), fullRect)
            } else {
                val rules = activeColorRules.filter { it.isEnabled }
                if (rules.isNotEmpty()) ImageUtils.binarizeImage(rawImg, rules, fullRect) else null
            }

            // 更新预览
            withContext(Dispatchers.Main) {
                binaryPreview = binImg?.let {
                    WorkImage(it.toComposeImageBitmap(), it, "Preview_Binary", "二值化", isBinary = true)
                }
            }

            // 2. 切割框 (基于当前显示图)
            val imgToSegment = if (currentWorkImage?.isBinary == true) currentWorkImage!!.bufferedImage else rawImg
            val rects = mutableListOf<Rect>()

            if (!currentSourceImage!!.localCropRects.isNullOrEmpty()) {
                rects.addAll(currentSourceImage!!.localCropRects!!)
            } else {
                if (isGridMode) {
                    rects.addAll(ImageUtils.generateGridRects(gridParams.x, gridParams.y, gridParams.w, gridParams.h, gridParams.colGap, gridParams.rowGap, gridParams.colCount, gridParams.rowCount))
                } else {
                    val rules = if (currentWorkImage?.isBinary == true) listOf(ColorRule(targetHex = "FFFFFF", biasHex = "000000")) else activeColorRules.filter { it.isEnabled }
                    if (rules.isNotEmpty()) rects.addAll(ImageUtils.scanConnectedComponents(imgToSegment, rules))
                    rects.addAll(globalFixedRects)
                }
            }
            withContext(Dispatchers.Main) { activeRects = rects }
        }
    }

    // --- 动作 (Actions) ---

    fun loadFile(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val img = ImageIO.read(file)
                if (img != null) withContext(Dispatchers.Main) {
                    sourceImages.add(WorkImage(img.toComposeImageBitmap(), img, file.name))
                    selectedSourceIndex = sourceImages.lastIndex
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun startScreenCapture() {
        scope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(300)
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val capture = Robot().createScreenCapture(Rectangle(screenSize))
            withContext(Dispatchers.Main) {
                fullScreenCapture = capture
                showScreenCropper = true
            }
        }
    }

    fun confirmCrop(rect: Rect) {
        addProcessingStep("裁剪区域") { ImageUtils.cropImage(it, rect) }
        if (currentScope == RuleScope.GLOBAL) globalFixedRects.clear()
    }

    fun confirmScreenCrop(cropped: BufferedImage) {
        showScreenCropper = false
        sourceImages.add(WorkImage(cropped.toComposeImageBitmap(), cropped, "截图 ${sourceImages.size + 1}"))
        selectedSourceIndex = sourceImages.lastIndex
    }

    fun addProcessingStep(label: String, processor: (BufferedImage) -> BufferedImage) {
        val base = activeBaseImage ?: return
        scope.launch(Dispatchers.Default) {
            val processed = processor(base.bufferedImage)
            val newStep = WorkImage(processed.toComposeImageBitmap(), processed, "Step_${pipelineSteps.size}", label)
            withContext(Dispatchers.Main) {
                pipelineSteps.add(newStep)
                selectedPipelineIndex = 1 + pipelineSteps.lastIndex
            }
        }
    }

    fun handleProcessAdd(filter: ImageFilter) {
        when (filter) {
            ColorFilterType.BINARIZATION -> processingTrigger++
            BlackWhiteFilterType.DENOISE -> addProcessingStep(filter.label) { ImageUtils.dummyDenoise(it) }
            BlackWhiteFilterType.SKELETON -> addProcessingStep(filter.label) { ImageUtils.dummySkeleton(it) }
            else -> println("功能 [${filter.label}] 尚未实现")
        }
    }

    // 规则更新辅助
    fun updateRules(action: (MutableList<ColorRule>) -> Unit) {
        if (currentScope == RuleScope.GLOBAL) {
            action(globalColorRules)
        } else if (currentSourceImage != null) {
            val newRules = (currentSourceImage!!.localColorRules ?: globalColorRules).map { it.copy() }.toMutableList()
            action(newRules)
            val newImage = currentSourceImage!!.copy(localColorRules = newRules)
            if (selectedSourceIndex in sourceImages.indices) sourceImages[selectedSourceIndex] = newImage
        }
    }

    // 映射
    fun openMappingDialog(rect: Rect) {
        if (currentWorkImage != null) {
            val charImg = ImageUtils.cropImage(currentWorkImage!!.bufferedImage, rect)
            mappingBufferedImg = charImg
            mappingBitmap = charImg.toComposeImageBitmap()
            showMappingDialog = true
        }
    }

    fun confirmMapping(char: String) {
        if (mappingBufferedImg != null) fontLibrary.add(FontItem(char, mappingBufferedImg!!))
        showMappingDialog = false
    }

    fun onColorPick(hex: String) {
        if (currentFilter == ColorFilterType.COLOR_PICK) {
            val targetList = if (currentScope == RuleScope.GLOBAL) globalColorRules else (currentSourceImage?.localColorRules ?: globalColorRules)
            if (targetList.size < 10 && targetList.none { it.targetHex == hex }) {
                updateRules { it.add(ColorRule(targetHex = hex, biasHex = activeBias)) }
            }
        }
    }
}