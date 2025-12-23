package org.eu.freex.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.eu.freex.tools.model.WorkImage

@Composable
fun BottomBar(
    images: List<WorkImage>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        color = Color(0xFF1E1E1E),
        elevation = 8.dp
    ) {
        Column {
            Text("  工作区图片", color = Color.LightGray, style = MaterialTheme.typography.caption, modifier = Modifier.padding(4.dp))
            LazyRow(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(images) { index, item ->
                    val isSelected = index == selectedIndex
                    Card(
                        modifier = Modifier.width(100.dp).height(90.dp).clickable { onSelect(index) },
                        border = if (isSelected) BorderStroke(2.dp, Color(0xFFFF5722)) else BorderStroke(1.dp, Color.Gray),
                        backgroundColor = Color(0xFF333333)
                    ) {
                        Box {
                            Column {
                                Image(
                                    bitmap = item.bitmap, contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)
                                )
                                Text(
                                    text = item.name, color = Color.White,
                                    style = MaterialTheme.typography.caption,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDelete(index) },
                                modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(bottomStart = 4.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}