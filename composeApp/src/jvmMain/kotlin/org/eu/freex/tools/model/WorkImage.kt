package org.eu.freex.tools.model

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import java.awt.image.BufferedImage

data class WorkImage(
    val bitmap: ImageBitmap,
    val bufferedImage: BufferedImage,
    val name: String,

    // --- [新增] 混合架构支持 ---
    // 如果为 null，表示跟随全局规则；如果不为 null，表示该图片有私有参数（重载）
    val localColorRules: List<ColorRule>? = null,
    val localBias: String? = null,
    val localCropRects: List<Rect>? = null // 手动指定的裁剪框
)