package org.eu.freex.tools.model

import androidx.compose.ui.graphics.ImageBitmap
import java.awt.image.BufferedImage

data class WorkImage(
    val bitmap: ImageBitmap,
    val bufferedImage: BufferedImage,
    val name: String
)