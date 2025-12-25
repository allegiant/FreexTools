// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/BottomPanel.kt
package org.eu.freex.tools

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.eu.freex.tools.model.WorkImage

@Composable
fun BottomPanel(
    modifier: Modifier,
    processChain: List<WorkImage>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF3C3C3C)) // 深灰色背景
            .drawBehind {
                // 顶部边框线
                drawLine(
                    color = Color(0xFF505050),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        // 顶部小标题
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "处理流水线 (Pipeline)",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 使用 Box 包裹列表和滚动条
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 1. 创建列表状态
            val listState = rememberLazyListState()

            // 2. 列表
            LazyRow(
                state = listState, // 绑定状态
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp), // 底部留出空间给滚动条
                horizontalArrangement = Arrangement.spacedBy(1.dp), // 图片紧挨着
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(processChain) { index, item ->
                    val isSelected = index == selectedIndex

                    // 颜色逻辑：只有选中状态才为绿色，未选中统一为深灰色
                    val labelBgColor = if (isSelected) Color(0xFF7CB342) else Color(0xFF555555)
                    // 文字颜色：选中为白色，未选中为浅灰
                    val labelTextColor = if (isSelected) Color.White else Color(0xFFCCCCCC)

                    // 选中时显示白色边框
                    val borderColor = if (isSelected) Color.White else Color.Transparent

                    Column(
                        modifier = Modifier
                            .width(100.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF2B2B2B))
                            .border(1.dp, borderColor)
                            .clickable { onSelect(index) }
                    ) {
                        // 1. 图片区
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Image(
                                bitmap = item.bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            // 删除按钮 (第一张原图不允许删除，后续步骤可以删)
                            // 注意：根据之前需求，此处不限制 isBinary，只限制 index > 0
                            if (index > 0) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(16.dp)
                                        .clickable { onDelete(index) }
                                        .background(Color.Black.copy(0.5f), RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        // 2. 标签区
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .background(labelBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.label,
                                color = labelTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 3. 底部水平滚动条
            HorizontalScrollbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(8.dp), // 设定高度
                adapter = rememberScrollbarAdapter(listState)
            )
        }
    }
}