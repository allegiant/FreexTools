package org.eu.freex.tools.model

import androidx.compose.ui.graphics.Color

interface ImageFilter {
    val label: String
}

// 【新增】默认的浏览模式，不进行任何处理
object ViewFilter : ImageFilter {
    override val label: String = "浏览模式"
}

enum class ColorFilterType(override val label: String) : ImageFilter {
    BINARIZATION("二值化"),
    COLOR_PICK("颜色选取"),
    POSTERIZE("色调分离"),
    GRAYSCALE("灰度");

    companion object {
        const val TITLE = "针对彩色进行处理:"
        val COLOR = Color(0xFFFF8A80)
    }
}

enum class BlackWhiteFilterType(override val label: String) : ImageFilter {
    DENOISE("清除杂点"),
    REMOVE_LINES("去掉直线"),
    CONTOURS("获取轮廓"),
    EXTRACT_BLOBS("提取色块"),
    DESKEW("倾斜矫正"),
    ROTATE_CORRECT("旋转纠正"),
    INVERT("颠倒颜色"),
    DILATE_ERODE("膨胀腐蚀"),
    SKELETON("细化抽骨"),
    FENCE_ADJUST("栅栏调整"),
    VALID_IMAGE("有效图像"),
    KEEP_SIZE("保留大小");

    companion object {
        const val TITLE = "针对黑白进行处理:"
        val COLOR = Color(0xFFFFD54F)
    }
}

enum class CommonFilterType(override val label: String) : ImageFilter {
    SCALE_RATIO("等比缩放"),
    SCALE_NORM("缩放归一化"),
    FIXED_ROTATE("固定旋转"),
    EXTEND_CROP("延伸裁剪"),
    FIXED_SMOOTH("固定柔化"),
    MEDIAN_BLUR("中值滤波");

    companion object {
        const val TITLE = "通用处理:"
        val COLOR = Color(0xFF81C784)
    }
}