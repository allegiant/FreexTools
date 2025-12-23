package org.eu.freex.tools.ui.panel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.eu.freex.tools.model.GridParams
import org.eu.freex.tools.ui.components.LabelRow
import org.eu.freex.tools.ui.components.NumberInput

@Composable
fun GridSettingsContent(
    p: GridParams,
    onChange: (Int, Int, Int, Int, Int, Int, Int, Int) -> Unit
) {
    Card(elevation = 2.dp, backgroundColor = Color(0xFFF5F5F5), border = BorderStroke(1.dp, Color.Gray)) {
        Column(Modifier.padding(8.dp)) {
            // 1. 起点位置
            LabelRow("起点位置: x, y")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NumberInput("左:", p.x, { onChange(it, p.y, p.w, p.h, p.colGap, p.rowGap, p.colCount, p.rowCount) }, Modifier.weight(1f))
                NumberInput("上:", p.y, { onChange(p.x, it, p.w, p.h, p.colGap, p.rowGap, p.colCount, p.rowCount) }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(6.dp))

            // 2. 切割大小
            LabelRow("切割大小: 宽, 高")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NumberInput("宽:", p.w, { onChange(p.x, p.y, it, p.h, p.colGap, p.rowGap, p.colCount, p.rowCount) }, Modifier.weight(1f))
                NumberInput("高:", p.h, { onChange(p.x, p.y, p.w, it, p.colGap, p.rowGap, p.colCount, p.rowCount) }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(6.dp))

            // 3. 列设置
            LabelRow("列间距 (Gap)")
            NumberInput("距:", p.colGap, { onChange(p.x, p.y, p.w, p.h, it, p.rowGap, p.colCount, p.rowCount) }, Modifier.fillMaxWidth())

            LabelRow("列切割数量")
            NumberInput("数:", p.colCount, { onChange(p.x, p.y, p.w, p.h, p.colGap, p.rowGap, it, p.rowCount) }, Modifier.fillMaxWidth())

            Spacer(Modifier.height(6.dp))

            // 4. 行设置
            LabelRow("行间距 (Gap)")
            NumberInput("距:", p.rowGap, { onChange(p.x, p.y, p.w, p.h, p.colGap, it, p.colCount, p.rowCount) }, Modifier.fillMaxWidth())

            LabelRow("行切割数量")
            NumberInput("数:", p.rowCount, { onChange(p.x, p.y, p.w, p.h, p.colGap, p.rowGap, p.colCount, it) }, Modifier.fillMaxWidth())
        }
    }
}