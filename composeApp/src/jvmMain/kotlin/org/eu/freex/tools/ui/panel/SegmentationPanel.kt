package org.eu.freex.tools.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.eu.freex.tools.model.GridParams
import org.eu.freex.tools.ui.components.CheckboxLabel
import org.eu.freex.tools.ui.components.FilterGridButton
import org.eu.freex.tools.ui.components.SectionHeader
import org.eu.freex.tools.ui.components.VerticalParamInput

@Composable
fun SegmentationPanel(
    modifier: Modifier,
    isGridMode: Boolean,
    onToggleGridMode: (Boolean) -> Unit,
    gridParams: GridParams,
    onGridParamChange: (Int, Int, Int, Int, Int, Int, Int, Int) -> Unit,
    onStartSegment: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize().background(Color(0xFFB0BEC5))) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionHeader("选择切割识别方式:", Color(0xFF80DEEA))
            Row(Modifier.fillMaxWidth()) {
                FilterGridButton("固定位置", isGridMode, { onToggleGridMode(true) }, Modifier.weight(1f))
                FilterGridButton("随机方位", false, {}, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                FilterGridButton("连通区域", !isGridMode, { onToggleGridMode(false) }, Modifier.weight(1f))
                FilterGridButton("范围投影", false, {}, Modifier.weight(1f))
            }
            FilterGridButton("颜色分层", false, {}, Modifier.fillMaxWidth())

            SectionHeader("切割字符滤镜处理:", Color(0xFF64B5F6))
            FilterGridButton("字符格式化", false, {}, Modifier.fillMaxWidth())

            SectionHeader("通用功能测试:", Color(0xFF9575CD))
            Row(Modifier.fillMaxWidth()) {
                FilterGridButton("快速找字", false, {}, Modifier.weight(1f))
                FilterGridButton("文字识别", false, {}, Modifier.weight(1f))
            }

            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFB0BEC5)).padding(8.dp)) {
                if (isGridMode) {
                    CheckboxLabel("起点位置: x, y", false) {}
                    VerticalParamInput("左:", gridParams.x, { onGridParamChange(it, gridParams.y, gridParams.w, gridParams.h, gridParams.colGap, gridParams.rowGap, gridParams.colCount, gridParams.rowCount) })
                    Spacer(Modifier.height(4.dp))
                    VerticalParamInput("上:", gridParams.y, { onGridParamChange(gridParams.x, it, gridParams.w, gridParams.h, gridParams.colGap, gridParams.rowGap, gridParams.colCount, gridParams.rowCount) })
                    Spacer(Modifier.height(8.dp))
                    Text("切割大小: 宽, 高", fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    VerticalParamInput("宽:", gridParams.w, { onGridParamChange(gridParams.x, gridParams.y, it, gridParams.h, gridParams.colGap, gridParams.rowGap, gridParams.colCount, gridParams.rowCount) })
                    Spacer(Modifier.height(4.dp))
                    VerticalParamInput("高:", gridParams.h, { onGridParamChange(gridParams.x, gridParams.y, gridParams.w, it, gridParams.colGap, gridParams.rowGap, gridParams.colCount, gridParams.rowCount) })
                    Spacer(Modifier.height(8.dp))
                    Text("列字间距: 宽+间隙", fontSize = 12.sp)
                    VerticalParamInput("距:", gridParams.colGap, { onGridParamChange(gridParams.x, gridParams.y, gridParams.w, gridParams.h, it, gridParams.rowGap, gridParams.colCount, gridParams.rowCount) })
                    Spacer(Modifier.height(8.dp))

                    // 【修正】这里添加了 fontWeight = 参数名，修复报错
                    Text("列切割数量:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    VerticalParamInput("数:", gridParams.colCount, { onGridParamChange(gridParams.x, gridParams.y, gridParams.w, gridParams.h, gridParams.colGap, gridParams.rowGap, it, gridParams.rowCount) })
                    Spacer(Modifier.height(8.dp))
                    Text("行字间距: 高+间隙", fontSize = 12.sp)
                    VerticalParamInput("距:", gridParams.rowGap, { onGridParamChange(gridParams.x, gridParams.y, gridParams.w, gridParams.h, gridParams.colGap, it, gridParams.colCount, gridParams.rowCount) })
                    Spacer(Modifier.height(8.dp))

                    // 【修正】这里添加了 fontWeight = 参数名，修复报错
                    Text("行切割数量:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    VerticalParamInput("数:", gridParams.rowCount, { onGridParamChange(gridParams.x, gridParams.y, gridParams.w, gridParams.h, gridParams.colGap, gridParams.rowGap, gridParams.colCount, it) })
                } else {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("当前为智能连通区域模式\n无需配置固定网格", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
        }
        Button(
            onClick = onStartSegment,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF8BC34A), contentColor = Color.Black),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) { Text("开始切割") }
    }
}