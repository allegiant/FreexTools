// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/LeftPanel.kt
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
import org.eu.freex.tools.model.ActiveSource
import org.eu.freex.tools.model.WorkImage

@Composable
fun LeftPanel(
    modifier: Modifier,
    sourceImages: List<WorkImage>,
    selectedSourceIndex: Int,
    activeSource: ActiveSource,
    onSelectSource: (Int) -> Unit,
    onDeleteSource: (Int) -> Unit,
    onAddFile: () -> Unit,
    onScreenCapture: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF252526))
            .drawBehind {
                drawLine(
                    color = Color(0xFF3E3E42),
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        // --- 工程资源 ---
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

        Button(
            onClick = onScreenCapture,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(30.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007ACC), contentColor = Color.White)
        ) { Text("截图导入", fontSize = 12.sp) }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(sourceImages) { index, item ->
                val isSelected = (activeSource == ActiveSource.SOURCE && index == selectedSourceIndex)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            if (isSelected) Color(0xFF37373D) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onSelectSource(index) }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = item.bitmap, contentDescription = null,
                        modifier = Modifier.size(32.dp).background(Color.Black),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.name, color = Color.LightGray, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDeleteSource(index) }, modifier = Modifier.size(16.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}