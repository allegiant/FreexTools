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
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.model.GridParams
import org.eu.freex.tools.model.WorkImage

@Composable
fun ImageProcessingWorkbench(
    sourceImages: List<WorkImage>,
    currentImageIndex: Int,
    resultImages: List<WorkImage>,
    currentImage: WorkImage?,
    mainScale: Float,
    mainOffset: Offset,
    hoverPixelPos: IntOffset?,
    hoverColor: Color,
    isCropMode: Boolean,
    colorRules: List<ColorRule>,
    defaultBias: String,
    showBinaryPreview: Boolean,
    isGridMode: Boolean,
    gridParams: GridParams,
    charRects: List<Rect>,

    // Callbacks
    onSelectImage: (Int) -> Unit,
    onAddFile: () -> Unit,
    onScreenCapture: () -> Unit,
    onDeleteSource: (Int) -> Unit,
    onDeleteResult: (Int) -> Unit,

    onTransformChange: (Float, Offset) -> Unit,
    onHoverChange: (IntOffset?, Color) -> Unit,
    onColorPick: (String) -> Unit,
    onCropConfirm: (Rect) -> Unit,
    onToggleCropMode: () -> Unit,

    onScaleChange: (Float) -> Unit,
    onDefaultBiasChange: (String) -> Unit,
    onRuleUpdate: (Long, String) -> Unit,
    onRuleToggle: (Long, Boolean) -> Unit,
    onRuleRemove: (Long) -> Unit,
    onClearRules: () -> Unit,
    onTogglePreview: () -> Unit,
    onAutoSegment: () -> Unit,
    onClearSegments: () -> Unit,
    onToggleGridMode: (Boolean) -> Unit,
    onGridParamChange: (Int, Int, Int, Int, Int, Int, Int, Int) -> Unit,
    onGridExtract: () -> Unit,
    binaryBitmap: ImageBitmap?,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 1. 左侧资产 (固定宽 260dp)
        LeftPanel(
            modifier = Modifier.width(260.dp),
            sourceImages = sourceImages,
            selectedIndex = currentImageIndex,
            onSelect = onSelectImage,
            onAddFile = onAddFile,
            onScreenCapture = onScreenCapture,
            onDeleteSource = onDeleteSource,
            resultImages = resultImages,
            onDeleteResult = onDeleteResult
        )

        // 2. 中间画布 (自适应)
        Workspace(
            modifier = Modifier.weight(1f),
            workImage = currentImage,
            isCropMode = isCropMode,
            scale = mainScale,
            offset = mainOffset,
            onTransformChange = onTransformChange,
            onHoverChange = onHoverChange,
            onColorPick = onColorPick,
            onCropConfirm = onCropConfirm,
            colorRules = colorRules,
            charRects = charRects,
            onCharRectClick = {},
            binaryBitmap = binaryBitmap,
            showBinaryPreview = showBinaryPreview,
        )

        // 3. 右侧控制 (固定宽 320dp)
        RightPanel(
            modifier = Modifier.width(320.dp),
            rawImage = currentImage?.bufferedImage,
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
            showBinary = showBinaryPreview,
            onTogglePreview = onTogglePreview,
            onAutoSegment = onAutoSegment,
            onClearSegments = onClearSegments,
            isGridMode = isGridMode,
            onToggleGridMode = onToggleGridMode,
            gridParams = gridParams,
            onGridParamChange = onGridParamChange,
            isCropMode = isCropMode,
            onToggleCropMode = onToggleCropMode,
            onGridExtract = onGridExtract
        )
    }
}