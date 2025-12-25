// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/ImageProcessingWorkbench.kt
package org.eu.freex.tools

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.eu.freex.tools.viewmodel.ImageProcessingViewModel

@Composable
fun ImageProcessingWorkbench(
    viewModel: ImageProcessingViewModel
) {
    val workImage = viewModel.currentWorkImage
    val binaryPreview = viewModel.binaryPreview

    val showPreview = (workImage?.isBinary == false) && (binaryPreview != null)
    val binBitmap = if (showPreview) binaryPreview?.bitmap else null

    // 【新增】根据右侧 Tab 决定底部面板显示什么内容
    // Tab 0 (滤镜): 显示流水线步骤 (displayChain)
    // Tab 1 (切割): 显示切割结果 (segmentationResults)
    val bottomPanelData = if (viewModel.rightPanelTabIndex == 0) {
        viewModel.displayChain
    } else {
        viewModel.segmentationResults
    }

    // 【新增】底部的选中索引逻辑也需要分离，暂时为了简单，切割结果复用一个索引，或者您可以加一个 separate index
    // 这里为了 UI 简单，切割结果列表点击时暂时不改变主视图，或者您可以绑定到另一个 View 逻辑
    val selectedIndex = if (viewModel.rightPanelTabIndex == 0) viewModel.selectedPipelineIndex else -1

    Row(modifier = Modifier.fillMaxSize()) {
        LeftPanel(modifier = Modifier.width(260.dp), viewModel = viewModel)

        Column(modifier = Modifier.weight(1f)) {
            Workspace(
                modifier = Modifier.weight(1f),
                workImage = workImage,
                binaryBitmap = binBitmap,
                showBinaryPreview = showPreview,
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
                processChain = bottomPanelData, // 动态数据源
                selectedIndex = selectedIndex,
                onSelect = { idx ->
                    if (viewModel.rightPanelTabIndex == 0) {
                        viewModel.selectedPipelineIndex = idx
                    } else {
                        // 如果点击的是切割结果，可以实现“查看该字模”的逻辑
                        // 目前暂留空
                    }
                },
                onDelete = { index ->
                    if (viewModel.rightPanelTabIndex == 0) {
                        val pipelineIndex = index - 1
                        if (pipelineIndex >= 0 && pipelineIndex < viewModel.pipelineSteps.size) {
                            viewModel.pipelineSteps.removeAt(pipelineIndex)
                            viewModel.selectedPipelineIndex = 0
                        }
                    } else {
                        // 删除单个字模
                        if (index in viewModel.segmentationResults.indices) {
                            viewModel.segmentationResults.removeAt(index)
                        }
                    }
                }
            )
        }

        RightPanel(modifier = Modifier.width(320.dp), viewModel = viewModel)
    }
}