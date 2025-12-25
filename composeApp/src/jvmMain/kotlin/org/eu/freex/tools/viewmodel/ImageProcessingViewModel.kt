// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/viewmodel/ImageProcessingViewModel.kt
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
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageProcessingViewModel {
    private val scope = CoroutineScope(Dispatchers.Main)

    // --- 核心状态 ---
    var sourceImages = mutableStateListOf<WorkImage>()
    var selectedSourceIndex by mutableStateOf(-1)

    // ... (规则、画布状态保持不变) ...
    var globalColorRules = mutableStateListOf<ColorRule>()
    var globalBias by mutableStateOf("101010")
    var globalFixedRects = mutableStateListOf<Rect>()
    var currentScope by mutableStateOf(RuleScope.GLOBAL)

    var mainScale by mutableStateOf(1f)
    var mainOffset by mutableStateOf(Offset.Zero)
    var hoverPixelPos by mutableStateOf<IntOffset?>(null)
    var hoverColor by mutableStateOf(Color.Transparent)

    var currentFilter by mutableStateOf<ImageFilter>(ViewFilter)

    // 阈值与网格
    var thresholdRange by mutableStateOf(0f..72f)
    var isRgbAvgEnabled by mutableStateOf(true)
    var isGridMode by mutableStateOf(false)
    var gridParams by mutableStateOf(GridParams(0, 0, 15, 15, 0, 0, 1, 1))

    // --- 流水线与结果 ---
    var pipelineSteps = mutableStateListOf<WorkImage>()
    var binaryPreview by mutableStateOf<WorkImage?>(null)
    var selectedPipelineIndex by mutableStateOf(0)

    // 【新增】切割结果列表 (存放拆分出来的字模)
    var segmentationResults = mutableStateListOf<WorkImage>()
    // 【新增】当前右侧面板的 Tab 索引 (0: 滤镜处理, 1: 切割识别)
    var rightPanelTabIndex by mutableStateOf(0)

    // ... (UI 交互状态保持不变) ...
    var showScreenCropper by mutableStateOf(false)
    var fullScreenCapture by mutableStateOf<BufferedImage?>(null)
    var showMappingDialog by mutableStateOf(false)
    var mappingBitmap by mutableStateOf<ImageBitmap?>(null)
    var mappingBufferedImg by mutableStateOf<BufferedImage?>(null)
    val fontLibrary = mutableStateListOf<FontItem>()

    // ... (Getter 属性保持不变) ...
    val currentSourceImage: WorkImage? get() = if (selectedSourceIndex in sourceImages.indices) sourceImages[selectedSourceIndex] else null
    val activeColorRules: List<ColorRule> get() = currentSourceImage?.localColorRules ?: globalColorRules
    val activeBias: String get() = currentSourceImage?.localBias ?: globalBias
    val activeBaseImage: WorkImage? get() = if (pipelineSteps.isNotEmpty()) pipelineSteps.last() else currentSourceImage

    val displayChain: List<WorkImage> get() {
        val list = mutableListOf<WorkImage>()
        if (currentSourceImage != null) list.add(currentSourceImage!!.copy(label = "原图"))
        list.addAll(pipelineSteps)
        return list
    }

    val currentWorkImage: WorkImage? get() = if (displayChain.isNotEmpty() && selectedPipelineIndex in displayChain.indices) {
        displayChain[selectedPipelineIndex]
    } else {
        activeBaseImage
    }

    var activeRects by mutableStateOf<List<Rect>>(emptyList())
        private set

    init {
        // ... (原有的 snapshotFlow 监听保持不变) ...

        snapshotFlow { selectedSourceIndex }.onEach {
            pipelineSteps.clear()
            binaryPreview = null
            selectedPipelineIndex = 0
            currentFilter = ViewFilter
            activeRects = emptyList()
            segmentationResults.clear() // 切换图片清空结果
        }.launchIn(scope)

        snapshotFlow { activeBaseImage }.onEach {
            activeRects = emptyList()
            segmentationResults.clear() // 基础图变化(如添加了新滤镜)清空旧结果
        }.launchIn(scope)

        // ... (其他监听保持不变) ...
        snapshotFlow { selectedPipelineIndex }.onEach { idx ->
            if (idx > 0 && idx <= pipelineSteps.size) {
                val step = pipelineSteps[idx - 1]
                if (step.params.containsKey("type") && step.params["type"] == "BINARIZATION") {
                    val min = (step.params["min"] as? Int) ?: 0
                    val max = (step.params["max"] as? Int) ?: 72
                    val rgb = (step.params["rgbAvg"] as? Boolean) ?: true
                    thresholdRange = min.toFloat()..max.toFloat()
                    isRgbAvgEnabled = rgb
                    currentFilter = ColorFilterType.BINARIZATION
                }
            }
        }.launchIn(scope)

        snapshotFlow { Pair(thresholdRange, isRgbAvgEnabled) }.onEach {
            if (selectedPipelineIndex > 0 && currentFilter == ColorFilterType.BINARIZATION) {
                val stepIndex = selectedPipelineIndex - 1
                if (stepIndex < pipelineSteps.size && pipelineSteps[stepIndex].params["type"] == "BINARIZATION") {
                    updateExistingBinarizationStep(stepIndex)
                }
            }
        }.launchIn(scope)
    }

    // 【修改】执行分割：计算框 + 裁剪小图
    fun performSegmentation() {
        scope.launch {
            computeRectsAndCrop()
        }
    }

    private suspend fun computeRectsAndCrop() {
        val base = activeBaseImage ?: return

        withContext(Dispatchers.Default) {
            val rawImg = base.bufferedImage

            withContext(Dispatchers.Main) {
                binaryPreview = null
                segmentationResults.clear() // 清空旧结果
            }

            // 1. 计算切割框 (Rects)
            val imgToSegment = if (currentWorkImage?.isBinary == true) currentWorkImage!!.bufferedImage else rawImg
            val rects = mutableListOf<Rect>()

            if (!currentSourceImage!!.localCropRects.isNullOrEmpty()) {
                rects.addAll(currentSourceImage!!.localCropRects!!)
            } else {
                if (isGridMode) {
                    rects.addAll(ImageUtils.generateGridRects(gridParams.x, gridParams.y, gridParams.w, gridParams.h, gridParams.colGap, gridParams.rowGap, gridParams.colCount, gridParams.rowCount))
                } else {
                    val rules = if (currentWorkImage?.isBinary == true) {
                        listOf(ColorRule(targetHex = "FFFFFF", biasHex = "000000"))
                    } else {
                        activeColorRules.filter { it.isEnabled }
                    }
                    if (rules.isNotEmpty()) {
                        rects.addAll(ImageUtils.scanConnectedComponents(imgToSegment, rules))
                    }
                    rects.addAll(globalFixedRects)
                }
            }

            // 2. 根据 Rects 裁剪出小图
            val results = rects.mapIndexed { index, rect ->
                // 注意：这里从 imgToSegment (可能是二值化图) 中裁剪，也可以选择从 rawImg (原图) 裁剪
                // 通常字库制作是基于二值化结果的
                val cropped = ImageUtils.cropImage(imgToSegment, rect)
                WorkImage(
                    bitmap = cropped.toComposeImageBitmap(),
                    bufferedImage = cropped,
                    name = "${index}", // 这里的名字可以作为字库的 Key
                    label = "$index",
                    isBinary = currentWorkImage?.isBinary == true
                )
            }

            withContext(Dispatchers.Main) {
                activeRects = rects
                segmentationResults.addAll(results)

                // (可选) 在流水线里加一个记录，模仿截图逻辑
                // addProcessingStep("连通区域") { it }
            }
        }
    }

    // ... (updateExistingBinarizationStep, loadFile, etc. 保持不变) ...
    // 为节省篇幅，省略未变动的辅助函数
    private suspend fun updateExistingBinarizationStep(stepIndex: Int) {
        val inputImage = displayChain[stepIndex].bufferedImage
        val min = thresholdRange.start.toInt()
        val max = thresholdRange.endInclusive.toInt()
        val useRgbAvg = isRgbAvgEnabled
        val newParams = mapOf("type" to "BINARIZATION", "min" to min, "max" to max, "rgbAvg" to useRgbAvg)
        withContext(Dispatchers.Default) {
            val fullRect = Rect(0f, 0f, inputImage.width.toFloat(), inputImage.height.toFloat())
            val newBitmap = if (useRgbAvg) ImageUtils.binarizeByRgbAvg(inputImage, min, max, fullRect) else inputImage
            val updatedStep = pipelineSteps[stepIndex].copy(bitmap = newBitmap.toComposeImageBitmap(), bufferedImage = newBitmap, params = newParams)
            withContext(Dispatchers.Main) { pipelineSteps[stepIndex] = updatedStep }
        }
    }

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
    fun startScreenCapture() { scope.launch(Dispatchers.IO) { kotlinx.coroutines.delay(300); val capture = ImageUtils.captureFullScreen(); withContext(Dispatchers.Main) { fullScreenCapture = capture; showScreenCropper = true } } }
    fun confirmCrop(rect: Rect) { addProcessingStep("裁剪区域") { src -> ImageUtils.cropImage(src, rect) }; if (currentScope == RuleScope.GLOBAL) globalFixedRects.clear() }
    fun confirmScreenCrop(cropped: BufferedImage) { showScreenCropper = false; sourceImages.add(WorkImage(cropped.toComposeImageBitmap(), cropped, "截图 ${sourceImages.size + 1}")); selectedSourceIndex = sourceImages.lastIndex }
    fun addProcessingStep(label: String, isBinary: Boolean = false, params: Map<String, Any> = emptyMap(), processor: (BufferedImage) -> BufferedImage) {
        val base = activeBaseImage ?: return
        scope.launch(Dispatchers.Default) {
            val processed = processor(base.bufferedImage)
            val newStep = WorkImage(processed.toComposeImageBitmap(), processed, "Step_${pipelineSteps.size}", label, isBinary = isBinary, params = params)
            withContext(Dispatchers.Main) { pipelineSteps.add(newStep); selectedPipelineIndex = 1 + pipelineSteps.lastIndex }
        }
    }
    fun handleProcessAdd(filter: ImageFilter) {
        val min = thresholdRange.start.toInt(); val max = thresholdRange.endInclusive.toInt(); val useRgbAvg = isRgbAvgEnabled
        when (filter) {
            ColorFilterType.BINARIZATION -> {
                val params = mapOf("type" to "BINARIZATION", "min" to min, "max" to max, "rgbAvg" to useRgbAvg)
                addProcessingStep("二值化", isBinary = true, params = params) { src ->
                    val fullRect = Rect(0f, 0f, src.width.toFloat(), src.height.toFloat())
                    if (useRgbAvg) ImageUtils.binarizeByRgbAvg(src, min, max, fullRect) else src
                }
            }
            ColorFilterType.GRAYSCALE -> addProcessingStep(filter.label) { src -> ImageUtils.toGrayscale(src) }
            ColorFilterType.COLOR_PICK, ColorFilterType.POSTERIZE -> addProcessingStep(filter.label) { src -> ImageUtils.applyPlaceholderEffect(src, filter.label) }
            BlackWhiteFilterType.DENOISE -> addProcessingStep(filter.label) { src -> ImageUtils.dummyDenoise(src) }
            BlackWhiteFilterType.INVERT -> addProcessingStep(filter.label) { src -> ImageUtils.invertColors(src) }
            BlackWhiteFilterType.SKELETON -> addProcessingStep(filter.label) { src -> ImageUtils.dummySkeleton(src) }
            BlackWhiteFilterType.REMOVE_LINES, BlackWhiteFilterType.CONTOURS, BlackWhiteFilterType.EXTRACT_BLOBS, BlackWhiteFilterType.DESKEW, BlackWhiteFilterType.ROTATE_CORRECT, BlackWhiteFilterType.DILATE_ERODE, BlackWhiteFilterType.FENCE_ADJUST, BlackWhiteFilterType.VALID_IMAGE, BlackWhiteFilterType.KEEP_SIZE -> addProcessingStep(filter.label) { src -> ImageUtils.applyPlaceholderEffect(src, filter.label) }
            is CommonFilterType -> addProcessingStep(filter.label) { src -> ImageUtils.applyPlaceholderEffect(src, filter.label) }
            else -> println("未知滤镜: ${filter.label}")
        }
    }
    fun updateRules(action: (MutableList<ColorRule>) -> Unit) { if (currentScope == RuleScope.GLOBAL) action(globalColorRules) else if (currentSourceImage != null) { val newRules = (currentSourceImage!!.localColorRules ?: globalColorRules).map { it.copy() }.toMutableList(); action(newRules); val newImage = currentSourceImage!!.copy(localColorRules = newRules); if (selectedSourceIndex in sourceImages.indices) sourceImages[selectedSourceIndex] = newImage } }
    fun openMappingDialog(rect: Rect) { if (currentWorkImage != null) { val charImg = ImageUtils.cropImage(currentWorkImage!!.bufferedImage, rect); mappingBufferedImg = charImg; mappingBitmap = charImg.toComposeImageBitmap(); showMappingDialog = true } }
    fun confirmMapping(char: String) { if (mappingBufferedImg != null) fontLibrary.add(FontItem(char, mappingBufferedImg!!)); showMappingDialog = false }
    fun onColorPick(hex: String) { if (currentFilter == ColorFilterType.COLOR_PICK) { val targetList = if (currentScope == RuleScope.GLOBAL) globalColorRules else (currentSourceImage?.localColorRules ?: globalColorRules); if (targetList.size < 10 && targetList.none { it.targetHex == hex }) { updateRules { it.add(ColorRule(targetHex = hex, biasHex = activeBias)) } } } }
}