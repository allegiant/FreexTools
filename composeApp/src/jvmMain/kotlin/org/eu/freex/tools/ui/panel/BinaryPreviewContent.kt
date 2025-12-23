package org.eu.freex.tools.ui.panel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eu.freex.tools.model.ColorRule
import org.eu.freex.tools.utils.ColorUtils
import java.awt.image.BufferedImage

@Composable
fun BinaryPreviewContent(
    rawImage: BufferedImage,
    colorRules: List<ColorRule>
) {
    var binaryBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(rawImage, colorRules.size, colorRules.toList()) {
        withContext(Dispatchers.Default) {
            val activeRules = colorRules.filter { it.isEnabled }
            if (activeRules.isEmpty()) {
                binaryBitmap = null
                return@withContext
            }
            val w = rawImage.width
            val h = rawImage.height
            val resultImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val rgb = rawImage.getRGB(x, y)
                    val isMatch = ColorUtils.isMatchAny(rgb, activeRules)
                    resultImg.setRGB(x, y, if (isMatch) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
                }
            }
            binaryBitmap = resultImg.toComposeImageBitmap()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        if (colorRules.none { it.isEnabled }) {
            Text("无启用规则", color = Color.Gray, fontSize = 12.sp)
        } else if (binaryBitmap != null) {
            Image(
                bitmap = binaryBitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None
            )
        } else {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}