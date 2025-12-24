// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/ImageProcessingWorkbench.kt
package org.eu.freex.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
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
    currentImage: WorkImage?,

    selectedSourceIndex: Int,
    activeSource: ActiveSource,

    mainScale: Float,
    mainOffset: Offset,
    hoverPixelPos: IntOffset?,
    hoverColor: Color,
    colorRules: List<ColorRule>,
    defaultBias: String,

    binaryBitmap: ImageBitmap?,
    isGridMode: Boolean,
    gridParams: GridParams,
    charRects: List<Rect>,
    currentScope: RuleScope,
    onToggleScope: () -> Unit,

    onSelectSource: (Int) -> Unit,
    onAddFile: () -> Unit,
    onScreenCapture: () -> Unit,
    onDeleteSource: (Int) -> Unit,

    onTransformChange: (Float, Offset) -> Unit,
    onHoverChange: (IntOffset?, Color) -> Unit,
    onColorPick: (String) -> Unit,
    onCropConfirm: (Rect) -> Unit,
    onCharRectClick: (Rect) -> Unit,

    onScaleChange: (Float) -> Unit,
    onDefaultBiasChange: (String) -> Unit,
    onRuleUpdate: (Long, String) -> Unit,
    onRuleToggle: (Long, Boolean) -> Unit,
    onRuleRemove: (Long) -> Unit,
    onClearRules: () -> Unit,

    onAutoSegment: () -> Unit,
    onClearSegments: () -> Unit,

    onToggleGridMode: (Boolean) -> Unit,
    onGridParamChange: (Int, Int, Int, Int, Int, Int, Int, Int) -> Unit,
    onGridExtract: () -> Unit,

    // --- 新增: 流水线参数 ---
    processChain: List<WorkImage>,
    selectedChainIndex: Int,
    onSelectChainItem: (Int) -> Unit,
    onDeleteChainItem: (Int) -> Unit,

    // --- 新增: 模拟处理按钮 ---
    onAddDenoise: () -> Unit,
    onAddSkeleton: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 1. 左侧面板 (工程资源)
        LeftPanel(
            modifier = Modifier.width(260.dp),
            sourceImages = sourceImages,
            selectedSourceIndex = selectedSourceIndex,
            activeSource = activeSource,
            onSelectSource = onSelectSource,
            onAddFile = onAddFile,
            onScreenCapture = onScreenCapture,
            onDeleteSource = onDeleteSource
        )

        // 2. 中间区域 (画布 + 流水线)
        Column(modifier = Modifier.weight(1f)) {
            // 2.1 顶部工具栏 (模拟功能入口)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onAddDenoise) { Text("清除杂点") }
                Button(onClick = onAddSkeleton) { Text("细化抽骨") }
            }

            // 2.2 画布
            Workspace(
                modifier = Modifier.weight(1f),
                workImage = currentImage,
                // 如果当前正在查看的就是二值化结果图(isBinary=true)，则不再叠加二值化预览层(showBinaryPreview=false)
                // 只有在查看非二值化图（原图或中间步骤）且有规则时，才叠加预览
                showBinaryPreview = (currentImage?.isBinary == false) && (binaryBitmap != null),
                binaryBitmap = binaryBitmap,
                scale = mainScale,
                offset = mainOffset,
                onTransformChange = onTransformChange,
                onHoverChange = onHoverChange,
                onColorPick = onColorPick,
                onCropConfirm = onCropConfirm,
                colorRules = colorRules,
                charRects = charRects,
                onCharRectClick = onCharRectClick
            )

            // 2.3 底部流水线
            BottomPanel(
                modifier = Modifier.height(140.dp),
                processChain = processChain,
                selectedIndex = selectedChainIndex,
                onSelect = onSelectChainItem,
                onDelete = onDeleteChainItem
            )
        }

        // 3. 右侧面板 (参数)
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
            onAutoSegment = onAutoSegment,
            onClearSegments = onClearSegments,
            isGridMode = isGridMode,
            onToggleGridMode = onToggleGridMode,
            gridParams = gridParams,
            onGridParamChange = onGridParamChange,
            onGridExtract = onGridExtract,
            currentScope = currentScope,
            onToggleScope = onToggleScope
        )
    }
}