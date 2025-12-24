// composeApp/src/jvmMain/kotlin/org/eu/freex/tools/RightPanel.kt
package org.eu.freex.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.eu.freex.tools.ui.panel.ColorRuleItem
import org.eu.freex.tools.ui.panel.GridSettingsContent
import java.awt.image.BufferedImage

@Composable
fun RightPanel(
    modifier: Modifier,
    // [新增] 作用域控制参数
    currentScope: RuleScope,
    onToggleScope: () -> Unit,

    // 基础图像与状态参数
    rawImage: BufferedImage?,
    hoverPixelPos: IntOffset?,
    hoverColor: Color,
    mainScale: Float,
    onScaleChange: (Float) -> Unit,

    // 规则与偏色
    colorRules: List<ColorRule>,
    defaultBias: String,
    onDefaultBiasChange: (String) -> Unit,
    onRuleUpdate: (Long, String) -> Unit,
    onRuleToggle: (Long, Boolean) -> Unit,
    onRuleRemove: (Long) -> Unit,
    onClearRules: () -> Unit,

    // [移除] showBinary 和 onTogglePreview 参数已不再需要

    // 分割与网格相关
    onAutoSegment: () -> Unit,
    onClearSegments: () -> Unit,
    isGridMode: Boolean,
    onToggleGridMode: (Boolean) -> Unit,
    gridParams: GridParams,
    onGridParamChange: (Int, Int, Int, Int, Int, Int, Int, Int) -> Unit,
    onGridExtract: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFFEEEEEE))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. 参数作用域切换 (Scope Switch) ---
        Text("参数作用域", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(
                text = "全局 (Global)",
                isSelected = currentScope == RuleScope.GLOBAL,
                onClick = { if (currentScope != RuleScope.GLOBAL) onToggleScope() },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "仅当前 (Local)",
                isSelected = currentScope == RuleScope.LOCAL,
                onClick = { if (currentScope != RuleScope.LOCAL) onToggleScope() },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = if (currentScope == RuleScope.GLOBAL) "修改将影响所有未锁定的图片" else "修改仅影响当前选中的图片",
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Divider(Modifier.padding(vertical = 4.dp))

        // --- 2. 画布操作指南 ---
        Text("画布操作指南", style = MaterialTheme.typography.caption, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Card(
            backgroundColor = Color.White,
            elevation = 0.dp,
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(Modifier.padding(8.dp)) {
                Text("• 左键拖拽：新建选区", fontSize = 12.sp, color = Color.DarkGray)
                Text("• 双击选区：确认裁剪", fontSize = 12.sp, color = Color(0xFFFF5722), fontWeight = FontWeight.Bold)
                Text("• 右键拖拽：平移画布", fontSize = 12.sp, color = Color.DarkGray)
                Text("• 滚轮滚动：缩放", fontSize = 12.sp, color = Color.DarkGray)
            }
        }

        Divider(Modifier.padding(vertical = 12.dp))

        // --- 3. 默认偏色设置 ---
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

        // --- 4. 切割模式 (智能识别 / 网格) ---
        Row(
            Modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
        ) {
            TabButton(
                text = "智能识别",
                isSelected = !isGridMode,
                onClick = { onToggleGridMode(false) },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "定距切割",
                isSelected = isGridMode,
                onClick = { onToggleGridMode(true) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))

        if (!isGridMode) {
            // 智能识别模式
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = onAutoSegment,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("自动计算范围", fontSize = 12.sp) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onClearSegments,
                    modifier = Modifier.width(60.dp).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("清空", fontSize = 12.sp) }
            }
        } else {
            // 网格模式
            GridSettingsContent(p = gridParams, onChange = onGridParamChange)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onGridExtract,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF00C853),
                    contentColor = Color.White
                )
            ) {
                Text("执行提取 -> 暂存区", fontSize = 12.sp)
            }
        }

        // 智能识别模式下也增加提取按钮 (之前讨论中补充的需求)
        if (!isGridMode) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onGridExtract,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF00C853),
                    contentColor = Color.White
                )
            ) {
                Text("执行提取 -> 暂存区", fontSize = 12.sp)
            }
        }

        Divider(Modifier.padding(vertical = 12.dp))

        // --- 5. 规则列表 ---
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

        // 规则列表容器
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
        ) {
            LazyColumn(contentPadding = PaddingValues(4.dp)) {
                items(colorRules, key = { it.id }) { rule ->
                    ColorRuleItem(
                        rule = rule,
                        onBiasChange = { onRuleUpdate(rule.id, it) },
                        onRemove = { onRuleRemove(rule.id) },
                        onToggle = { onRuleToggle(rule.id, it) }
                    )
                }
            }
        }
    }
}