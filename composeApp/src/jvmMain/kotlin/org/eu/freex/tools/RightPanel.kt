// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/RightPanel.kt
package org.eu.freex.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.eu.freex.tools.model.GridParams
import org.eu.freex.tools.model.RuleScope
import org.eu.freex.tools.ui.panel.FilterPanel
import org.eu.freex.tools.ui.panel.SegmentationPanel
import org.eu.freex.tools.viewmodel.ImageProcessingViewModel

@Composable
fun RightPanel(
    modifier: Modifier,
    viewModel: ImageProcessingViewModel
) {
    // 【修改】使用 ViewModel 中的状态，而不是本地 remember
    val selectedTab = viewModel.rightPanelTabIndex

    Column(modifier = modifier.fillMaxHeight()) {
        Row(modifier = Modifier.fillMaxWidth().height(32.dp).background(Color(0xFF555555))) {
            RightPanelTab("2. 滤镜处理", selectedTab == 0, { viewModel.rightPanelTabIndex = 0 }, Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            RightPanelTab("3. 切割识别", selectedTab == 1, { viewModel.rightPanelTabIndex = 1 }, Modifier.weight(1f))
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> FilterPanel(
                    modifier = Modifier.fillMaxSize(),
                    currentFilter = viewModel.currentFilter,
                    onFilterChange = { viewModel.currentFilter = it },
                    colorRules = viewModel.activeColorRules,
                    defaultBias = viewModel.activeBias,
                    onDefaultBiasChange = { newBias ->
                        if (viewModel.currentScope == RuleScope.GLOBAL) viewModel.globalBias = newBias
                        else viewModel.currentSourceImage?.let {
                            val idx = viewModel.selectedSourceIndex
                            if(idx != -1) viewModel.sourceImages[idx] = it.copy(localBias = newBias)
                        }
                    },
                    onRuleUpdate = { id, bias -> viewModel.updateRules { list -> list.find { it.id == id }?.let { list[list.indexOf(it)] = it.copy(biasHex = bias) } } },
                    onRuleToggle = { id, enabled -> viewModel.updateRules { list -> list.find { it.id == id }?.let { list[list.indexOf(it)] = it.copy(isEnabled = enabled) } } },
                    onRuleRemove = { id -> viewModel.updateRules { list -> list.removeIf { it.id == id } } },
                    onClearRules = { viewModel.updateRules { it.clear() } },

                    thresholdRange = viewModel.thresholdRange,
                    onThresholdChange = { viewModel.thresholdRange = it },
                    isRgbAvgEnabled = viewModel.isRgbAvgEnabled,
                    onIsRgbAvgEnabledChange = { viewModel.isRgbAvgEnabled = it },

                    onProcessAdd = { viewModel.handleProcessAdd(it) }
                )
                1 -> SegmentationPanel(
                    modifier = Modifier.fillMaxSize(),
                    isGridMode = viewModel.isGridMode,
                    onToggleGridMode = { viewModel.isGridMode = it },
                    gridParams = viewModel.gridParams,
                    onGridParamChange = { x, y, w, h, cg, rg, cc, rc -> viewModel.gridParams = GridParams(x, y, w, h, cg, rg, cc, rc) },
                    onStartSegment = { viewModel.performSegmentation() }
                )
            }
        }
    }
}

@Composable
fun RightPanelTab(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxHeight().background(if (isSelected) Color(0xFFEEEEEE) else Color(0xFF555555)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp) }
}