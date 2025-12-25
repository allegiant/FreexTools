package org.eu.freex.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.eu.freex.tools.utils.ImageUtils
import org.eu.freex.tools.viewmodel.ImageProcessingViewModel

@Composable
fun LeftPanel(
    modifier: Modifier = Modifier,
    viewModel: ImageProcessingViewModel
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF252526))
            .drawBehind {
                // 绘制右侧分割线
                drawLine(
                    color = Color(0xFF3E3E42),
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        // --- 头部：标题与导入按钮 ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("工程资源", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = {
                    // 调用工具类选择文件，然后交给 ViewModel 加载
                    val file = ImageUtils.pickFile()
                    if (file != null) {
                        viewModel.loadFile(file)
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import", tint = Color.LightGray)
            }
        }

        // --- 截图导入按钮 ---
        Button(
            onClick = { viewModel.startScreenCapture() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(30.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007ACC), contentColor = Color.White)
        ) { Text("截图导入", fontSize = 12.sp) }

        Spacer(Modifier.height(8.dp))

        // --- 图片列表 ---
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(viewModel.sourceImages) { index, item ->
                // 判断是否选中
                val isSelected = (index == viewModel.selectedSourceIndex)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            if (isSelected) Color(0xFF37373D) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            // 切换选中项，ViewModel 会自动触发流水线重置
                            viewModel.selectedSourceIndex = index
                        }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 缩略图
                    Image(
                        bitmap = item.bitmap,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).background(Color.Black),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(8.dp))

                    // 文件名
                    Text(
                        text = item.name,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // 删除按钮
                    IconButton(
                        onClick = {
                            // 直接操作 ViewModel 的列表
                            if (index in viewModel.sourceImages.indices) {
                                viewModel.sourceImages.removeAt(index)
                                // 修正选中索引，防止越界
                                if (viewModel.selectedSourceIndex >= viewModel.sourceImages.size) {
                                    viewModel.selectedSourceIndex = viewModel.sourceImages.lastIndex
                                } else if (viewModel.selectedSourceIndex == index) {
                                    // 如果删除了当前选中的，重置选中状态
                                    viewModel.selectedSourceIndex = -1
                                }
                            }
                        },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}