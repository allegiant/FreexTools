// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/ImageProcessingWorkbench.kt
package org.eu.freex.tools

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.eu.freex.tools.viewmodel.ImageProcessingViewModel

@Composable
fun ImageProcessingWorkbench(
    viewModel: ImageProcessingViewModel // 【核心变化】只接收 VM
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 1. 左侧面板
        LeftPanel(
            modifier = Modifier.width(260.dp),
            viewModel = viewModel // 传 VM 给 LeftPanel
        )

        // 2. 中间区域
        Column(modifier = Modifier.weight(1f)) {
            Workspace(
                modifier = Modifier.weight(1f),
                workImage = viewModel.currentWorkImage, // 仍可直接传状态给纯 UI 组件
                binaryBitmap = if (viewModel.currentWorkImage?.isBinary == false) viewModel.binaryPreview?.bitmap else null,
                showBinaryPreview = (viewModel.currentWorkImage?.isBinary == false) && (viewModel.binaryPreview != null),
                scale = viewModel.mainScale,
                offset = viewModel.mainOffset,
                onTransformChange = { s, o -> viewModel.mainScale = s; viewModel.mainOffset = o },
                onHoverChange = { p, c -> viewModel.hoverPixelPos = p; viewModel.hoverColor = c },
                onColorPick = { viewModel.onColorPick(it) },
                onCropConfirm = { viewModel.confirmCrop(it) },
                colorRules = viewModel.activeColorRules,
                charRects = viewModel.activeRects,
                onCharRectClick = { viewModel.openMappingDialog(it) }
            )

            BottomPanel(
                modifier = Modifier.height(140.dp),
                processChain = viewModel.displayChain,
                selectedIndex = viewModel.selectedPipelineIndex,
                onSelect = { viewModel.selectedPipelineIndex = it },
                onDelete = { index ->
                    val pipelineIndex = index - 1
                    if (pipelineIndex >= 0 && pipelineIndex < viewModel.pipelineSteps.size) {
                        viewModel.pipelineSteps.removeAt(pipelineIndex)
                        viewModel.selectedPipelineIndex = 0
                    }
                }
            )
        }

        // 3. 右侧面板
        RightPanel(
            modifier = Modifier.width(320.dp),
            viewModel = viewModel // 传 VM 给 RightPanel
        )
    }
}