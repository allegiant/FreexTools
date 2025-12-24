package org.eu.freex.tools.model

import java.awt.image.BufferedImage

data class FontItem(
    val char: String,
    val image: BufferedImage,
    val timestamp: Long = System.currentTimeMillis()
)