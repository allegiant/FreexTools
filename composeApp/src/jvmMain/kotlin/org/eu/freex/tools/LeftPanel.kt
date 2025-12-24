package org.eu.freex.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import org.eu.freex.tools.model.WorkImage

@Composable
fun LeftPanel(
    modifier: Modifier,
    sourceImages: List<WorkImage>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDeleteSource: (Int) -> Unit,
    onAddFile: () -> Unit,
    onScreenCapture: () -> Unit,
    resultImages: List<WorkImage>,
    onDeleteResult: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF252526)) // 深色背景
            .drawBehind {
                val strokeWidth = 1.dp.toPx() // 边框宽度
                val borderColor = Color(0xFF3E3E42) // 边框颜色

                // 在画布的最右侧画一条垂直线
                drawLine(
                    color = borderColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        // --- 区域 1：工程资源 (源图片) ---
        Column(modifier = Modifier.weight(1f)) {
            // 标题与添加按钮
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("工程资源", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddFile, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Import", tint = Color.LightGray)
                }
            }

            // 截图按钮
            Button(
                onClick = onScreenCapture,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(30.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007ACC), contentColor = Color.White)
            ) {
                Text("截图导入", fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            // 源图片列表
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(sourceImages) { index, item ->
                    val isSelected = index == selectedIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(
                                if (isSelected) Color(0xFF37373D) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onSelect(index) }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = item.bitmap,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).background(Color.Black),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.name,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDeleteSource(index) }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Divider(color = Color(0xFF3E3E42), thickness = 2.dp)

        // --- 区域 2：结果暂存区 ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "结果暂存 (${resultImages.size})",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(resultImages) { index, item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(Color(0xFF333333), RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFF555555), RoundedCornerShape(2.dp))
                    ) {
                        Image(
                            bitmap = item.bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(2.dp)
                        )
                        // 删除悬浮按钮
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                                .background(Color.Black.copy(0.6f))
                                .clickable { onDeleteResult(index) }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                        }
                    }
                }
            }
        }
    }
}