// 文件路径: composeApp/src/jvmMain/kotlin/org/eu/freex/tools/TopBar.kt
package org.eu.freex.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppModule {
    IMAGE_PROCESSING,
    FONT_MANAGER
}

@Composable
fun TopBar(
    currentModule: AppModule,
    onModuleChange: (AppModule) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp) // 稍微调低一点高度
            .background(Color(0xFF333333))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("FreexTools Pro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(40.dp))

        ModuleTab("图像处理", Icons.Default.Image, currentModule == AppModule.IMAGE_PROCESSING) {
            onModuleChange(AppModule.IMAGE_PROCESSING)
        }
        Spacer(Modifier.width(20.dp))
        ModuleTab("字库管理", Icons.Default.FontDownload, currentModule == AppModule.FONT_MANAGER) {
            onModuleChange(AppModule.FONT_MANAGER)
        }
    }
}

@Composable
fun ModuleTab(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF00C853) else Color.LightGray
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = color, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}