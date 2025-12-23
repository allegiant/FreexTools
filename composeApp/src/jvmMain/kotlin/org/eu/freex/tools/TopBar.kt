package org.eu.freex.tools


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TopBar(
    onLoadFile: () -> Unit,
    onScreenCapture: () -> Unit,
    isCropMode: Boolean,
    onToggleCropMode: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onLoadFile) { Text("打开本地图片") }
        Spacer(Modifier.width(16.dp))
        Button(
            onClick = onScreenCapture,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF673AB7), contentColor = Color.White)
        ) { Text("全屏截图导入") }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onToggleCropMode,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isCropMode) Color(0xFFFF5722) else Color.Gray,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isCropMode) "退出裁剪模式" else "工作区二次裁剪")
        }
    }
}