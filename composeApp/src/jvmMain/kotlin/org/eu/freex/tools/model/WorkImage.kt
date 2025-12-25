// composeApp/src/jvmMain/kotlin/org/eu/freex/tools/model/WorkImage.kt
package org.eu.freex.tools.model

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import java.awt.image.BufferedImage

data class WorkImage(
    val bitmap: ImageBitmap,
    val bufferedImage: BufferedImage,
    val name: String,

    // UI显示的标签，如 "原图", "清除杂点", "二值化"
    val label: String = name,
    // 是否是二值化结果 (UI显示绿色，逻辑上作为最终输出)
    val isBinary: Boolean = false,
    val params: Map<String, Any> = emptyMap(),

    // 继承的参数
    val localColorRules: List<ColorRule>? = null,
    val localBias: String? = null,
    val localCropRects: List<Rect>? = null
)