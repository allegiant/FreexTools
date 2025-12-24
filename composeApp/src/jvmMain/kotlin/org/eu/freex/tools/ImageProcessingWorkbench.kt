package org.eu.freex.tools

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.eu.freex.tools.model.ActiveSource
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.model.GridParams
import org.eu.freex.tools.model.WorkImage

@Composable
fun ImageProcessingWorkbench(
    sourceImages: List<WorkImage>,
    resultImages: List<WorkImage>, // 传入的是实时预览流 previewResults
    currentImage: WorkImage?,

    // 状态
    selectedSourceIndex: Int,
    selectedResultIndex: Int,
    activeSource: ActiveSource,

    mainScale: Float,
    mainOffset: Offset,
    hoverPixelPos: IntOffset?,
    hoverColor: Color,
    colorRules: List<ColorRule>,
    defaultBias: String,

    // 二值化全图预览
    binaryBitmap: ImageBitmap?,

    isGridMode: Boolean,
    gridParams: GridParams,
    charRects: List<Rect>,

    // 新增：作用域控制
    currentScope: RuleScope,
    onToggleScope: () -> Unit,

    // 回调
    onSelectSource: (Int) -> Unit,
    onSelectResult: (Int) -> Unit,

    onAddFile: () -> Unit,
    onScreenCapture: () -> Unit,
    onDeleteSource: (Int) -> Unit,
    onDeleteResult: (Int) -> Unit,

    onTransformChange: (Float, Offset) -> Unit,
    onHoverChange: (IntOffset?, Color) -> Unit,
    onColorPick: (String) -> Unit,
    onCropConfirm: (Rect) -> Unit,

    onScaleChange: (Float) -> Unit,
    onDefaultBiasChange: (String) -> Unit,
    onRuleUpdate: (Long, String) -> Unit,
    onRuleToggle: (Long, Boolean) -> Unit,
    onRuleRemove: (Long) -> Unit,
    onClearRules: () -> Unit,

    // 这些在流水线模式下可能不再直接操作开关，但保留回调
    onAutoSegment: () -> Unit,
    onClearSegments: () -> Unit,

    onToggleGridMode: (Boolean) -> Unit,
    onGridParamChange: (Int, Int, Int, Int, Int, Int, Int, Int) -> Unit,
    onGridExtract: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        LeftPanel(
            modifier = Modifier.width(260.dp),
            sourceImages = sourceImages,
            resultImages = resultImages,
            selectedSourceIndex = selectedSourceIndex,
            selectedResultIndex = selectedResultIndex,
            activeSource = activeSource,
            onSelectSource = onSelectSource,
            onSelectResult = onSelectResult,
            onAddFile = onAddFile,
            onScreenCapture = onScreenCapture,
            onDeleteSource = onDeleteSource,
            onDeleteResult = onDeleteResult
        )

        Workspace(
            modifier = Modifier.weight(1f),
            workImage = currentImage,
            binaryBitmap = binaryBitmap, // 传递二值化图层
            showBinaryPreview = binaryBitmap != null, // 只要有 bitmap 就显示
            scale = mainScale,
            offset = mainOffset,
            onTransformChange = onTransformChange,
            onHoverChange = onHoverChange,
            onColorPick = onColorPick,
            onCropConfirm = onCropConfirm,
            colorRules = colorRules,
            charRects = charRects,
            onCharRectClick = {}
        )

        RightPanel(
            modifier = Modifier.width(320.dp),
            rawImage = currentImage?.bufferedImage, // 用于取色逻辑
            hoverPixelPos = hoverPixelPos,
            hoverColor = hoverColor,
            mainScale = mainScale,
            onScaleChange = onScaleChange,
            colorRules = colorRules,
            defaultBias = defaultBias,
            onDefaultBiasChange = onDefaultBiasChange,
            onRuleUpdate = onRuleUpdate,
            onRuleToggle = onRuleToggle,
            onRuleRemove = onRuleRemove,
            onClearRules = onClearRules,

            // 移除了显式的 showBinaryPreview 开关控制，由 App 逻辑自动决定
            onAutoSegment = onAutoSegment,
            onClearSegments = onClearSegments,

            isGridMode = isGridMode,
            onToggleGridMode = onToggleGridMode,
            gridParams = gridParams,
            onGridParamChange = onGridParamChange,
            onGridExtract = onGridExtract,

            // 传递 Scope 控制给右侧面板
            currentScope = currentScope,
            onToggleScope = onToggleScope
        )
    }
}