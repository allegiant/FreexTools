package org.eu.freex.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
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
    onGridParamChange: (Int, Int, Int, Int, Int, Int, Int, Int) -> Unit,
    // --- 新增参数 ---
    isCropMode: Boolean,
    onToggleCropMode: () -> Unit,
    onGridExtract: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFFEEEEEE))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // [新增] 将二值化预览变成一个醒目的控制开关
        Text("显示设置", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                .clickable { onTogglePreview() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("主画布预览二值化效果", fontSize = 13.sp)
            Switch(
                checked = showBinary,
                onCheckedChange = { onTogglePreview() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00C853))
            )
        }
        Spacer(Modifier.height(12.dp))

        // 2. 画布工具 (新增)
        Text("画布工具", style = MaterialTheme.typography.caption, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onToggleCropMode,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isCropMode) Color(0xFFFF5722) else Color.White,
                contentColor = if (isCropMode) Color.White else Color.Black
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isCropMode) "正在裁剪 (ESC退出)" else "进入裁剪模式", fontSize = 12.sp)
        }

        Divider(Modifier.padding(vertical = 12.dp))

        // 3. 鼠标信息
        val isHoverValid = rawImage != null && hoverPixelPos != null &&
                hoverPixelPos.x >= 0 && hoverPixelPos.x < rawImage.width &&
                hoverPixelPos.y >= 0 && hoverPixelPos.y < rawImage.height

        Row(verticalAlignment = Alignment.CenterVertically) {
            val safeColor = if (isHoverValid) hoverColor else Color.Transparent
            Box(Modifier.size(16.dp).background(safeColor).border(1.dp, Color.Gray))
            Spacer(Modifier.width(8.dp))
            if (isHoverValid) {
                val hex = String.format("#%02X%02X%02X", (safeColor.red*255).toInt(), (safeColor.green*255).toInt(), (safeColor.blue*255).toInt())
                Text(hex, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
            } else {
                Text("#------", style = MaterialTheme.typography.caption, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            if (hoverPixelPos != null) {
                Text("(${hoverPixelPos.x}, ${hoverPixelPos.y})", style = MaterialTheme.typography.caption, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 4. 默认偏色
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

        // 5. 切割模式
        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp))) {
            TabButton(text = "智能识别", isSelected = !isGridMode, onClick = { onToggleGridMode(false) }, modifier = Modifier.weight(1f))
            TabButton(text = "定距切割", isSelected = isGridMode, onClick = { onToggleGridMode(true) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        if (!isGridMode) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = onAutoSegment,
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("自动计算范围", fontSize = 12.sp) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onClearSegments, modifier = Modifier.width(60.dp).height(36.dp), contentPadding = PaddingValues(0.dp)) { Text("清空", fontSize = 12.sp) }
            }
        } else {
            GridSettingsContent(p = gridParams, onChange = onGridParamChange)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onGridExtract,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00C853), contentColor = Color.White)
            ) {
                Text("执行提取 -> 暂存区", fontSize = 12.sp)
            }
        }

        Divider(Modifier.padding(vertical = 12.dp))

        // 6. 规则列表
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("取色规则 (${colorRules.size}/10)", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClearRules, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.Gray)
            }
        }
        Spacer(Modifier.height(8.dp))
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