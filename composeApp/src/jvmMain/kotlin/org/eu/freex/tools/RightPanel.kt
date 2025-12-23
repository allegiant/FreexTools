package org.eu.freex.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.model.GridParams
import org.eu.freex.tools.ui.components.TabButton
import org.eu.freex.tools.ui.panel.BinaryPreviewContent
import org.eu.freex.tools.ui.panel.ColorRuleItem
import org.eu.freex.tools.ui.panel.GridSettingsContent
import org.eu.freex.tools.utils.ColorUtils
import java.awt.image.BufferedImage

@Composable
fun RightPanel(
    modifier: Modifier,
    rawImage: BufferedImage?,
    hoverPixelPos: IntOffset?,
    hoverColor: Color,
    mainScale: Float,
    onScaleChange: (Float) -> Unit,
    colorRules: List<ColorRule>,
    defaultBias: String,
    onDefaultBiasChange: (String) -> Unit,
    onRuleUpdate: (Long, String) -> Unit,
    onRuleToggle: (Long, Boolean) -> Unit,
    onRuleRemove: (Long) -> Unit,
    onClearRules: () -> Unit,
    showBinary: Boolean,
    onTogglePreview: () -> Unit,
    onAutoSegment: () -> Unit,
    onClearSegments: () -> Unit,
    isGridMode: Boolean,
    onToggleGridMode: (Boolean) -> Unit,
    gridParams: GridParams,
    onGridParamChange: (Int, Int, Int, Int, Int, Int, Int, Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFFEEEEEE))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. 全图二值化预览
        Text("全图二值化预览", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .background(Color.DarkGray)
                .border(2.dp, Color.DarkGray)
        ) {
            if (rawImage != null) {
                BinaryPreviewContent(rawImage = rawImage, colorRules = colorRules)
            } else {
                Text("暂无图片", color = Color.Gray, modifier = Modifier.align(Alignment.Center), fontSize = 12.sp)
            }
        }

        // 鼠标位置状态检查逻辑
        val isHoverValid = rawImage != null && hoverPixelPos != null &&
                hoverPixelPos.x >= 0 && hoverPixelPos.x < rawImage.width &&
                hoverPixelPos.y >= 0 && hoverPixelPos.y < rawImage.height

        if (isHoverValid) {
            val rgb = rawImage!!.getRGB(hoverPixelPos!!.x, hoverPixelPos.y)
            val isMatch = ColorUtils.isMatchAny(rgb, colorRules)
            Text(
                text = if (isMatch) "鼠标处: 有效 (匹配)" else "鼠标处: 无效 (背景)",
                color = if (isMatch) Color(0xFF00C853) else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
            )
        } else {
            Spacer(Modifier.height(20.dp))
        }

        Divider(Modifier.padding(vertical = 12.dp))

        // 2. 鼠标信息区 (此处逻辑简单，暂不拆分，或可提取为 MouseInfoPanel)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val safeColor = if (isHoverValid) hoverColor else Color.Transparent
            Box(Modifier.size(16.dp).background(safeColor).border(1.dp, Color.Gray))
            Spacer(Modifier.width(8.dp))
            if (isHoverValid) {
                val r = (safeColor.red * 255).toInt()
                val g = (safeColor.green * 255).toInt()
                val b = (safeColor.blue * 255).toInt()
                Text(
                    String.format("#%02X%02X%02X", r, g, b),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text("#------", style = MaterialTheme.typography.caption, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            if (hoverPixelPos != null) {
                Text(
                    "(${hoverPixelPos.x}, ${hoverPixelPos.y})",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 3. 默认偏色设置
        Text("新取色默认偏色", style = MaterialTheme.typography.caption, color = Color.Gray)
        OutlinedTextField(
            value = defaultBias,
            onValueChange = { if (it.length <= 6) onDefaultBiasChange(it.uppercase()) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = Color.White)
        )

        Spacer(Modifier.height(12.dp))

        // 4. 切割模式切换区
        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp))) {
            TabButton(text = "智能识别", isSelected = !isGridMode, onClick = { onToggleGridMode(false) }, modifier = Modifier.weight(1f))
            TabButton(text = "定距切割", isSelected = isGridMode, onClick = { onToggleGridMode(true) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        if (!isGridMode) {
            // 智能识别按钮组
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = onAutoSegment,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("自动计算点阵范围", fontSize = 12.sp) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onClearSegments, modifier = Modifier.width(60.dp).height(36.dp), contentPadding = PaddingValues(0.dp)) { Text("清空", fontSize = 12.sp) }
            }
        } else {
            // 网格设置面板
            GridSettingsContent(p = gridParams, onChange = onGridParamChange)
        }

        Divider(Modifier.padding(vertical = 12.dp))

        // 5. 规则列表
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "取色规则 (${colorRules.size}/10)",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClearRules, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Gray)
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
        ) {
            LazyColumn(contentPadding = PaddingValues(4.dp)) {
                items(colorRules, key = { it.id }) { rule ->
                    ColorRuleItem(
                        rule = rule,
                        onBiasChange = { newBias -> onRuleUpdate(rule.id, newBias) },
                        onRemove = { onRuleRemove(rule.id) },
                        onToggle = { isEnabled -> onRuleToggle(rule.id, isEnabled) }
                    )
                }
            }
        }
    }
}